package edu.virginia.lib.indexing;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchivesSpaceClient {

    public static final String UVA_EAD_EXT_NS = "http://indexing.virginia.edu/ead-extensions";

    private String baseUrl;

    private CloseableHttpClient httpClient;

    private String sessionToken;

    public ArchivesSpaceClient(final String baseUrl, final String username, final String password) throws IOException {
        this.baseUrl = baseUrl;
        httpClient = HttpClients.createDefault();
        authenticate(username, password);
    }

    private void authenticate(final String username, final String password) throws IOException {
        HttpPost httpPost = new HttpPost(baseUrl + "users/" + username + "/login");
        httpPost.setEntity(MultipartEntityBuilder.create().addTextBody("password", password).build());
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Unable to authenticate! (response " + response.getStatusLine().toString() + ")");
            }
            this.sessionToken = Json.createReader(response.getEntity().getContent()).readObject().getString("session");
        }
    }

    public void writeEnhancedEADXML(final String repoId, final String id, final OutputStream out) throws IOException, XMLStreamException {
        HttpGet httpGet = new HttpGet(baseUrl + "repositories/" + repoId + "/resource_descriptions/" + id + ".xml?numbered_cs=true&include_daos=true&include_unpublished=true");
        httpGet.addHeader("X-ArchivesSpace-Session", sessionToken);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(response.getStatusLine().toString());
            }
            XMLEventWriter w = XMLOutputFactory.newInstance().createXMLEventWriter(out);
            XMLEventReader r = XMLInputFactory.newInstance().createXMLEventReader(response.getEntity().getContent());
            while (r.hasNext()) {
                XMLEvent event = r.nextEvent();
                if (isComponentStart(event)) {
                    processComponent(repoId, event, r, w);
                } else {
                    w.add(event);
                }
            }
            w.flush();
        }
    }

    private JsonObject getArchivalObjectForRefId(final String repoId, final String refId) throws IOException {
        final String ref = lookupRefId(repoId, refId);
        HttpGet httpGet = new HttpGet(baseUrl + ref + "?resolve[]=digital_object");
        httpGet.addHeader("X-ArchivesSpace-Session", sessionToken);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(response.getStatusLine().toString());
            }
            return Json.createReader(response.getEntity().getContent()).readObject();
        }
    }

    private String lookupRefId(final String repoId, final String refId) throws IOException {
        HttpGet httpGet = new HttpGet(baseUrl + "repositories/" + repoId + "/find_by_id/archival_objects?ref_id[]=" + refId);
        httpGet.addHeader("X-ArchivesSpace-Session", sessionToken);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(response.getStatusLine().toString());
            }
            return Json.createReader(response.getEntity().getContent()).readObject().getJsonArray("archival_objects").getJsonObject(0).getString("ref");
        }
    }

    private boolean isComponentStart(final XMLEvent event) {
        return (event.isStartElement() && isComponentName(event.asStartElement().getName().getLocalPart()));
    }

    private boolean isComponentName(final String name) {
        return name.equals("c") || Pattern.matches("c\\d\\d", name);
    }

    private void processComponent(final String repoId, XMLEvent event, final XMLEventReader r, final XMLEventWriter w) throws IOException, XMLStreamException {
        final String localName = event.asStartElement().getName().getLocalPart();
        final String id = event.asStartElement().getAttributeByName(new QName(null, "id")).getValue();
        final JsonObject archivalObject = getArchivalObjectForRefId(repoId, id.replace("aspace_", ""));
        do {
            if (event.isEndElement() && event.asEndElement().getName().getLocalPart().equals(localName)) {
                addIIIFManifests(w, archivalObject);
                addMODSRecord(w, archivalObject);
                w.add(event);
                return;
            } else {
                w.add(event);
                event = r.nextEvent();
            }
        } while (r.hasNext());
    }

    private void addMODSRecord(final XMLEventWriter w, final JsonObject archivalObject) throws IOException, XMLStreamException {
        for (JsonValue i : archivalObject.getJsonArray("external_ids")) {
            if (i instanceof JsonObject) {
                JsonObject o = (JsonObject) i;
                if (o.getString("source").equals("tracksys")) {
                    final String metadataUrl = o.getString("external_id");
                    Matcher m = Pattern.compile(".*/metadata/(.+:\\d+)\\?type=desc_metadata").matcher(metadataUrl);
                    if (!m.matches()) {
                        throw new RuntimeException("Unable to parse pid from tracksys external id! (" + metadataUrl + ")");
                    }
                    final String pid = m.group(1);
                    HttpGet httpGet = new HttpGet(metadataUrl);
                    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                        if (response.getStatusLine().getStatusCode() != 200) {
                            throw new RuntimeException("Unable to get tracksys metadata at " + metadataUrl + " (" + response.getStatusLine().toString() + ")");
                        }
                        XMLEventFactory f = XMLEventFactory.newFactory();
                        w.add(f.createStartElement("uva", UVA_EAD_EXT_NS, "mods-metadata"));
                        w.add(f.createAttribute("xmlns:uva", UVA_EAD_EXT_NS));
                        XMLEventReader r = XMLInputFactory.newInstance().createXMLEventReader(response.getEntity().getContent());
                        while (r.hasNext()) {
                            XMLEvent event = r.nextEvent();
                            if (!event.isStartDocument() && !event.isEndDocument()) {
                                w.add(event);
                            }
                        }
                        w.add(f.createEndElement("uva", UVA_EAD_EXT_NS, "mods-metadata"));
                        w.add(f.createStartElement("uva", UVA_EAD_EXT_NS, "pid"));
                        w.add(f.createAttribute("xmlns:uva", UVA_EAD_EXT_NS));
                        w.add(f.createCharacters(pid));
                        w.add(f.createEndElement("uva", UVA_EAD_EXT_NS, "pid"));
                    }
                }
            }
        }
    }

    private void addIIIFManifests(final XMLEventWriter w, final JsonObject archivalObject) throws IOException, XMLStreamException {
        for (JsonValue i : archivalObject.getJsonArray("instances")) {
            if (i instanceof JsonObject) {
                JsonObject o = (JsonObject) i;
                final JsonObject dao = o.getJsonObject("digital_object");
                if (dao != null) {
                    final JsonObject resolved = dao.getJsonObject("_resolved");
                    if (resolved != null) {
                        final JsonArray fileVersions = resolved.getJsonArray("file_versions");
                        for (int fi = 0; fi < fileVersions.size(); fi ++) {
                            JsonObject fileVersion = fileVersions.getJsonObject(fi);
                            if (fileVersion.getString("use_statement").equals("image-service-manifest")) {
                                final String manifestUrl = fileVersion.getString("file_uri");
                                HttpGet httpGet = new HttpGet(manifestUrl);
                                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                                    if (response.getStatusLine().getStatusCode() != 200) {
                                        throw new RuntimeException("Unable to get IIIF manifest at " + manifestUrl + " (" + response.getStatusLine().toString() + ")");
                                    }
                                    JsonObject iiifManifest = Json.createReader(response.getEntity().getContent()).readObject();
                                    String thumbnailUrl = iiifManifest.getJsonArray("sequences").getJsonObject(0).getJsonArray("canvases").getJsonObject(0).getString("thumbnail");
                                    Matcher resizeMatcher = Pattern.compile("(http://.*/full/)[^/]*(/.*)").matcher(thumbnailUrl);
                                    if (resizeMatcher.matches()) {
                                        thumbnailUrl = resizeMatcher.group(1) + "!115,125" + resizeMatcher.group(2);
                                    } else {
                                        throw new RuntimeException("Unexpected thumbnail URL! (" + thumbnailUrl + ")");
                                    }
                                    XMLEventFactory f = XMLEventFactory.newFactory();
                                    w.add(f.createStartElement("uva", UVA_EAD_EXT_NS, "iiif-manifest"));
                                    w.add(f.createAttribute("xmlns:uva", UVA_EAD_EXT_NS));
                                    w.add(f.createCharacters(iiifManifest.toString()));
                                    w.add(f.createEndElement("uva", UVA_EAD_EXT_NS, "iiif-manifest"));
                                    w.add(f.createStartElement("uva", UVA_EAD_EXT_NS, "thumbnail"));
                                    w.add(f.createAttribute("xmlns:uva", UVA_EAD_EXT_NS));
                                    w.add(f.createCharacters(thumbnailUrl));
                                    w.add(f.createEndElement("uva", UVA_EAD_EXT_NS, "thumbnail"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
