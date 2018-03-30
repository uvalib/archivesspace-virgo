package edu.virginia.lib.indexing;

import edu.virginia.lib.indexing.helpers.JsonHelper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.virginia.lib.indexing.helpers.JsonHelper.hasValue;
import static edu.virginia.lib.indexing.helpers.SolrHelper.getIdFromRef;
import static edu.virginia.lib.indexing.helpers.SolrHelper.getSolrOutputFile;
import static edu.virginia.lib.indexing.helpers.SolrHelper.isUniqueVirgoId;
import static edu.virginia.lib.indexing.helpers.UvaHelper.extractManifestUrl;
import static edu.virginia.lib.indexing.helpers.UvaHelper.normalizeLocation;

/**
 * An abstract base class that encapsulates logic to pull data from the ArchviesSpace REST API.
 */
public abstract class ASpaceObject {

    final static public String RIGHTS_WRAPPER_URL = "http://rightswrapper2.lib.virginia.edu:8090/rights-wrapper/";

    protected ArchivesSpaceClient c;

    protected JsonObject record;

    public ASpaceObject(ArchivesSpaceClient aspaceClient, final String refId) throws IOException {
        this.c = aspaceClient;
        record = c.resolveReference(refId);
    }

    public static ASpaceObject parseObject(final ArchivesSpaceClient client, final String refId) throws IOException {
        if (refId.contains("/accessions/")) {
            return new ASpaceAccession(client, refId);
        } else if (refId.contains("/resources/")) {
            return new ASpaceCollection(client, refId);
        } else {
            throw new RuntimeException("Unable to guess resource type from refID! (" + refId + ")");
        }
    }

    public abstract boolean isShadowed() throws IOException;

    public abstract boolean isPublished();

    public int getLockVersion() {
        return record.getInt("lock_version");
    }

    /**
     * Gets a solr-ready identifier for the resource that comes from the ASpace ID.
     */
    public String getId() {
        return getCallNumber().replace("-", "_").replace("/", "").replace(" ", "").toUpperCase();
    }

    public String getCallNumber() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 6; i++) {
            if (record.get("id_" + i) != null) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(record.getString("id_" + i).trim());
            }
        }
        return sb.toString();
    }

    public File generateSolrAddDoc(final File outputDir, final String dbHost, final String dbUser, final String dbPassword) throws IOException, XMLStreamException, SQLException {
        final String shortRefId = getIdFromRef(record.getString("uri"));
        final String callNumber = getCallNumber();
        final String title = record.getString("title");

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        final File outputFile = getSolrOutputFile(outputDir, record.getString("uri"));
        outputFile.getParentFile().mkdirs();
        XMLStreamWriter xmlOut = xmlOutputFactory.createXMLStreamWriter(new FileOutputStream(outputFile));
        xmlOut.writeStartDocument("UTF-8", "1.0");
        xmlOut.writeCharacters("\n");
        xmlOut.writeStartElement("add");
        xmlOut.writeCharacters("  ");
        xmlOut.writeStartElement("doc");
        xmlOut.writeCharacters("\n");

        addField(xmlOut, "id", shortRefId);
        String hid = getId();
        // Despite it's name, "alternate_id_facet" currently must only be an alternate id that represents a
        // distinct digital object for which there's an IIIF manifest, rights_wrapper_url, etc.
        //
        //if (isUniqueVirgoId(hid)) {
        //    addField(xmlOut, "alternate_id_facet", hid);
        //}
        addField(xmlOut, "aspace_version_facet", String.valueOf(getLockVersion()));
        addField(xmlOut, "call_number_facet", callNumber);
        addField(xmlOut, "main_title_display", title);
        addField(xmlOut, "title_text", title);
        addField(xmlOut, "source_facet", "ArchivesSpace");
        final boolean shadowed = isShadowed();
        addField(xmlOut, "shadowed_location_facet", shadowed ? "HIDDEN" : "VISIBLE");
        if (!shadowed) {

            // TODO: get this from the data
            //addRightsFields("http://rightsstatements.org/vocab/InC-EDU/1.0/", xmlOut, id, tracksysDbHost, tracksysDbUsername, tracksysDbPassword);

            // TODO: do something with finding aid status

            final String library = getLibrary(record);
            addField(xmlOut, "library_facet", library);

            // TODO location_facet

            // subjects
            final JsonValue subjects = record.get("subjects");
            if (subjects != null && subjects.getValueType() == JsonValue.ValueType.ARRAY) {
                for (JsonValue sub : (JsonArray) subjects) {
                    final String ref = ((JsonObject) sub).getString("ref");
                    final JsonObject subject = c.resolveReference(ref);
                    // TODO: break up these subjects
                    if (subject.getBoolean("publish")) {
                        addField(xmlOut, "subject_facet", subject.getString("title"));
                        addField(xmlOut, "subject_text", subject.getString("title"));
                    }
                }
            }

            // extents
            final JsonValue extents = record.get("extents");
            if (extents != null && extents.getValueType() == JsonValue.ValueType.ARRAY) {
                for (JsonValue extent : (JsonArray) extents) {
                    JsonObject e = (JsonObject) extent;

                    StringBuffer extentString = new StringBuffer();
                    extentString.append(e.getString("number"));
                    extentString.append(" ");
                    final String type = e.getString("extent_type");
                    extentString.append(type.replace("_", " "));
                    if (e.get("container_summary") != null) {
                        extentString.append(" (" + e.getString("container_summary") + ")");
                    }
                    addField(xmlOut, "extent_display", extentString.toString());
                }
            }

            // dates
            boolean sortDateSet = false;
            final JsonValue dates = record.get("dates");
            if (dates != null && dates.getValueType() == JsonValue.ValueType.ARRAY) {
                for (JsonValue date : (JsonArray) dates) {
                    try {
                        final JsonObject dateObj = (JsonObject) date;
                        if (hasValue(dateObj, "expression")) {
                            final String dateStr = ((JsonObject) date).getString("expression");
                            int year = -1;
                            if (dateStr.matches("\\d\\d\\d\\d")) {
                                year = Integer.parseInt(dateStr);
                            } else if (dateStr.matches("\\d\\d\\d\\d-\\d\\d\\d\\d")) {
                                year = Integer.parseInt(dateStr.substring(5));
                            }
                            if (year != 0) {
                                if (!sortDateSet) {
                                    addField(xmlOut, "date_multisort_i", String.valueOf(year));
                                    sortDateSet = true;
                                }
                                final int yearsAgo = Calendar.getInstance().get(Calendar.YEAR) - year;
                                if (yearsAgo > 50) {
                                    addField(xmlOut, "published_date_facet", "More than 50 years ago");
                                }
                                if (yearsAgo <= 50) {
                                    addField(xmlOut, "published_date_facet", "Last 50 years");
                                }
                                if (yearsAgo <= 10) {
                                    addField(xmlOut, "published_date_facet", "Last 10 years");
                                }
                                if (yearsAgo <= 3) {
                                    addField(xmlOut, "published_date_facet", "Last 3 years");
                                }
                                if (yearsAgo <= 1) {
                                    addField(xmlOut, "published_date_facet", "Last 12 months");
                                }
                            } else {
                                throw new RuntimeException("Cannot parse date! (" + dateStr + ")");
                            }
                            addField(xmlOut, "date_display", dateStr);
                        } else if (hasValue(dateObj, "begin") && hasValue(dateObj, "end")) {
                            final String begin = ((JsonObject) date).getString("begin");
                            final String end = ((JsonObject) date).getString("end");
                            if (begin != null && end != null) {
                                addField(xmlOut, "date_display", begin + "-" + end);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        printOutRawData();
                        System.out.println("SKIPPING DATE FOR " + shortRefId);
                    }
                }
            }

            // linked agents
            final JsonValue agents = record.get("linked_agents");
            if (agents != null && agents.getValueType() == JsonValue.ValueType.ARRAY) {
                for (JsonValue agentLink : (JsonArray) agents) {
                    final String ref = ((JsonObject) agentLink).getString("ref");
                    final String role = ((JsonObject) agentLink).getString("role");
                    final JsonObject agent = c.resolveReference(ref);
                    try {
                        if (agent.getBoolean("publish")) {
                            if (role.equals("creator")) {
                                final String name = agent.getString("title");
                                addField(xmlOut, "author_facet", name);
                                addField(xmlOut, "author_text", name);
                            }
                        }
                    } catch (NullPointerException e) {
                        // TODO: do something better than skipping it
                    }
                }
            }

            // instances
            final JsonArrayBuilder scCirclationAspaceContainers = Json.createArrayBuilder();
            final ArrayList<String> iiif = new ArrayList<String>();
            parseInstances(scCirclationAspaceContainers, iiif, xmlOut, library, callNumber);
            JsonArray containers = dedupeContainerArray(scCirclationAspaceContainers.build());
            addField(xmlOut, "feature_facet", "archivesspace");
            if (containers.size() > 0) {
                addField(xmlOut, "special_collections_holding_display", containers.toString());
            } else {
                JsonObjectBuilder b = Json.createObjectBuilder();
                b.add("library", library);
                b.add("location", library);
                b.add("call_number", callNumber);
                b.add("special_collections_location", callNumber);
                JsonArray defaultContainers = Json.createArrayBuilder().add(b.build()).build();
                addField(xmlOut, "special_collections_holding_display", defaultContainers.toString());
            }

            int manifestsIncluded = 0;
            if (iiif.size() <= 5) {
                for (int i = 0; i < iiif.size(); i++) {
                    final String url = iiif.get(i);
                    try {
                        addDigitalImages(url, xmlOut, true, dbHost, dbUser, dbPassword);
                        manifestsIncluded++;
                    } catch (IOException ex) {
                        System.err.println("Unable to fetch manifest: " + url);
                    }
                }
            }
            if (manifestsIncluded > 0) {
                addField(xmlOut, "feature_facet", "iiif");
                addField(xmlOut, "format_facet", "Online");
            } else {
                addField(xmlOut, "thumbnail_url_display", "http://iiif.lib.virginia.edu/iiif/static:6/full/!115,125/0/default.jpg");
            }

            // Despite it's name, "alternate_id_facet" currently must only be an alternate id that represents a
            // distinct digital object for which there's an IIIF manifest, rights_wrapper_url, etc.
            //
            // related accessions (use for alternate ids)
            //final JsonValue accessions = record.get("related_accessions");
            //if (accessions != null && accessions.getValueType() == JsonValue.ValueType.ARRAY) {
            //    for (JsonValue a : (JsonArray) accessions) {
            //        final String ref = ((JsonObject) a).getString("ref");
            //        final ASpaceAccession accession = new ASpaceAccession(c, ref);
            //        addField(xmlOut, "alternate_id_facet", accession.getId());
            //    }
            //}

            // notes (right now, we only include the scope notes)
            final JsonValue notes = record.get("notes");
            if (notes != null && notes.getValueType() == JsonValue.ValueType.ARRAY) {
                for (JsonValue n : (JsonArray) notes) {
                    JsonObject note = (JsonObject) n;
                    if (note.getBoolean("publish")) {
                        JsonArray subnotes = note.getJsonArray("subnotes");
                        if (subnotes != null) {
                            StringBuffer noteText = new StringBuffer();
                            for (int i = 0; i < subnotes.size(); i++) {
                                JsonObject subnote = subnotes.getJsonObject(i);
                                if (subnote.getBoolean("publish")) {
                                    if (noteText.length() > 0) {
                                        noteText.append("\n");
                                    }
                                    noteText.append(subnote.getString("content"));
                                }
                            }
                            if (noteText.length() > 0) {
                                if (note.getString("type").equals("scopecontent")) {
                                    addField(xmlOut, "note_display", noteText.toString());
                                }
                                addField(xmlOut, "note_text", noteText.toString());
                            }
                        }
                    }
                }
            }
        }

        if (record.get("content_description") != null) {
            final String noteText = record.getString("content_description");
            addField(xmlOut, "note_text", noteText.toString());
            addField(xmlOut, "note_display", noteText.toString());
        }

        addField(xmlOut, "online_url_display", "https://archives.lib.virginia.edu" + record.getString("uri"));
        xmlOut.writeCharacters("  ");
        xmlOut.writeEndElement(); // doc
        xmlOut.writeCharacters("\n");
        xmlOut.writeEndElement(); // add

        xmlOut.close();

        return outputFile;

    }

    private JsonArray dedupeContainerArray(JsonArray containers) {
        HashSet<String> callNumbers = new HashSet<String>();
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (JsonValue v : containers) {
            JsonObject container = (JsonObject) v;
            final String callNumber = container.getString("call_number");
            if (!callNumbers.contains(callNumber)) {
                callNumbers.add(callNumber);
                b.add(v);
            }
        }
        return b.build();
    }

    /**
     * @param scCirclationAspaceContainers an empty JsonArrayBuilder to contain the resulting circulation information
     * @param manifestUrls the IIIF manifest URLs for digital objects
     */
    private void parseInstances(JsonArrayBuilder scCirclationAspaceContainers, List<String> manifestUrls, XMLStreamWriter xmlOut, String library, String callNumber) throws IOException, XMLStreamException {
        final JsonValue instances = record.get("instances");
        if (instances != null && instances.getValueType() == JsonValue.ValueType.ARRAY) {
            for (JsonValue i : (JsonArray) instances) {
                final JsonObject instance = (JsonObject) i;
                if (instance.getString("instance_type").equals("digital_object")) {
                    // digital object
                    final JsonObject digitalObject = c.resolveReference(instance.getJsonObject("digital_object").getString("ref"));
                    if (digitalObject.getBoolean("publish")) {
                        // look through file versions for any embeddable formats
                        for (JsonValue v : digitalObject.getJsonArray("file_versions")) {
                            JsonObject ver = (JsonObject) v;
                            if (ver.getBoolean("publish") && ver.getString("use_statement").startsWith("image-service")) {
                                manifestUrls.add(extractManifestUrl(ver.getString("file_uri")));
                            }
                        }
                    }
                } else {
                    // container
                    final JsonObject container = c.resolveReference(instance.getJsonObject("sub_container").getJsonObject("top_container").getString("ref"));
                    JsonObjectBuilder b = Json.createObjectBuilder();
                    b.add("library", library);
                    b.add("location", library);
                    b.add("call_number", callNumber + " " + container.getString("display_string"));

                    JsonArray loc = container.getJsonArray("container_locations");
                    for (JsonValue v : loc) {
                        JsonObject l = (JsonObject) v;
                        if (l.getString("status").equals("current")) {
                            JsonObject location = c.resolveReference(l.getString("ref"));
                            b.add("special_collections_location", location.getString("title"));
                        }

                    }

                    scCirclationAspaceContainers.add(b.build());
                }
            }
        }

        // recurse to children
        final JsonObject treeObj = record.getJsonObject("tree");
        if (treeObj != null) {
            final JsonObject tree = c.resolveReference(treeObj.getString("ref"));
            final JsonArray children = tree.getJsonArray("children");
            if (children != null) {
                for (JsonValue c : children) {
                    final JsonObject child = (JsonObject) c;
                    final ASpaceObject o = new ASpaceArchivalObject(this.c, child.getString("record_uri"));
                    if (o.isPublished()) {
                        o.parseInstances(scCirclationAspaceContainers, manifestUrls, xmlOut, library, callNumber);
                    }
                }
            }
        }

    }

    private static void addDigitalImages(final String manifestUrl, final XMLStreamWriter xmlOut, boolean thumbnail, final String dbHost, final String dbUser, final String dbPassword) throws IOException, XMLStreamException, SQLException {
        HttpGet httpGet = new HttpGet(manifestUrl);
        try (CloseableHttpResponse response = HttpClients.createDefault().execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Unable to get IIIF manifest at " + manifestUrl + " (" + response.getStatusLine().toString() + ")");
            }
            JsonObject iiifManifest = Json.createReader(response.getEntity().getContent()).readObject();
            final String manifestId = iiifManifest.getString("@id");
            String shortManifestId = manifestId.substring(manifestId.lastIndexOf('/') + 1);
            if (shortManifestId.equals("iiif-manifest.json")) {
                // hack for Shepherd until it's in the tracking system
                shortManifestId = "MSS16152";
            }

            final String rsUri = iiifManifest.getString("license");
            addRightsFields(rsUri, xmlOut, shortManifestId, dbHost, dbUser, dbPassword);

            addField(xmlOut, "alternate_id_facet", shortManifestId);
            addField(xmlOut, "individual_call_number_display", iiifManifest.getString("label"));
            if (thumbnail) {
                String thumbnailUrl = iiifManifest.getJsonArray("sequences").getJsonObject(0).getJsonArray("canvases").getJsonObject(0).getString("thumbnail");
                Matcher resizeMatcher = Pattern.compile("(https://.*/full/)[^/]*(/.*)").matcher(thumbnailUrl);
                if (resizeMatcher.matches()) {
                    thumbnailUrl = resizeMatcher.group(1) + "!115,125" + resizeMatcher.group(2);
                    addField(xmlOut, "thumbnail_url_display", thumbnailUrl);

                    // TODO: maybe use this as the thumbnail, maybe don't...
                } else {
                    throw new RuntimeException("Unexpected thumbnail URL! (" + thumbnailUrl + ")");
                }

                // TODO: you can pull out the rights statement and apply it to the record
            }

            addField(xmlOut, "iiif_presentation_metadata_display", iiifManifest.toString());
        } catch (JsonParsingException e) {
            throw new RuntimeException("Unable to parse IIIF manifest at " + manifestUrl);
        }
    }

    private String getLibrary(JsonObject c) throws IOException {
        final String repositoryRef = c.getJsonObject("repository").getString("ref");

        JsonObject repo = this.c.resolveReference(repositoryRef);
        final String name = repo.getString("name");
        return normalizeLocation(name);
    }

    private static void addRightsFields(final String uri, XMLStreamWriter w, final String pid, final String tracksysDbHost, final String tracksysDbUsername, final String tracksysDbPassword) throws SQLException, XMLStreamException {
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        String connectionUrl = "jdbc:mysql://" + tracksysDbHost + "/tracksys_production?user=" + tracksysDbUsername + "&password=" + tracksysDbPassword;
        Connection conn = DriverManager.getConnection(connectionUrl);
        try {
            final String query = "SELECT name, uri, statement, commercial_use, educational_use, modifications from use_rights where uri=?";
            final PreparedStatement s = conn.prepareStatement(query);
            s.setString(1, uri);
            final ResultSet rs = s.executeQuery();
            try {
                if (rs.next()) {
                    addField(w, "feature_facet", "rights_wrapper");
                    addField(w, "rights_wrapper_url_display", RIGHTS_WRAPPER_URL + "?pid=" + pid + "&pagePid=");
                    addField(w, "rs_uri_display", uri);
                    // TODO: add citation below... preferably generated from ASPACE using a DOI
                    addField(w, "rights_wrapper_display", rs.getString("statement"));
                    if (rs.getInt("commercial_use") == 1) {
                        addField(w, "use_facet", "Commercial Use Permitted");
                    }
                    if (rs.getInt("educational_use") == 1) {
                        addField(w, "use_facet", "Educational Use Permitted");
                    }
                    if (rs.getInt("modifications") == 1) {
                        addField(w, "use_facet", "Modifications Permitted");
                    }
                } else {
                    throw new RuntimeException("Unable to find rights statement " + uri + " in tracksys db.");
                }
            } finally {
                rs.close();
            }
        } finally {
            conn.close();
        }
    }


    static void addField(XMLStreamWriter w, final String name, final String value) throws XMLStreamException {
        w.writeCharacters("    ");
        w.writeStartElement("field");
        w.writeAttribute("name", name);
        w.writeCharacters(value);
        w.writeEndElement();
        w.writeCharacters("\n");

    }

    public void printOutRawData() {
        JsonHelper.writeOutJson(record);
    }

}
