package edu.virginia.lib.indexing.helpers;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static methods to interact with a Solr server's HTTP API.
 */
public class SolrHelper {

    public static void postFileToSolr(final String solrUrl, final File solrDoc, boolean commit) throws IOException {
        System.out.println("Writing doc to solr " + solrUrl + "...");
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(solrUrl + (commit ? "?commit=true" : ""));
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody(solrDoc.getName(), solrDoc, ContentType.create("text/xml", "UTF-8"), solrDoc.getName());
            post.setEntity(builder.build());
            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Unable to write documents to solr at " + solrUrl + ". (" + response.getStatusLine().toString() + ")");
                }
            }
        }
        System.out.println("DONE!");
    }

    public static File getSolrOutputFile(final File outputDir, final String refId) {
        return new File(outputDir, getIdFromRef(refId) + ".xml");
    }

    /**
     * Gets a solr-ready identifier from the reference id for the resource.
     */
    public static String getIdFromRef(final String refId) {
        final String id = refId.replace("/repositories/", "as:").replace("/accessions/", "a").replace("/resources/", "r");
        if (!Pattern.compile("as:\\d+[ar]\\d+").matcher(id).matches()) {
            throw new RuntimeException("refId " + refId + " maps to improper pid " + id);
        }
        return id;
    }

    public static String getRefIdForFile(final File solrFile) {
        Matcher m = Pattern.compile("as:(\\d+)([ar])(\\d+)\\.xml").matcher(solrFile.getName());
        if (m.matches()) {
            return "/repositories/" + m.group(1) + (m.group(2).equals("r") ? "/resources/" : "/accessions/") + m.group(3);
        } else{
            throw new RuntimeException("Invalid filename: " + solrFile.getName());
        }
    }

    public static boolean isUniqueVirgoId(final String id) {
        if (id.contains(" ") || id.contains("/")) {
            return false;
        }
        if (id.startsWith("VIU") || id.startsWith("MSS") || id.startsWith("VACVUCN") || id.startsWith("RG_") || id.startsWith("MS_")) {
            return true;
        }
        return false;
    }

    public static List<String> getSolrXmlFieldValues(final String fieldName, final Document doc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression ex = xpath.compile("/add/doc/field[@name='" + fieldName + "']/text()");
        final List<String> values = new ArrayList<>();
        NodeList nl = (NodeList) ex.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nl.getLength(); i ++) {
            Node n = nl.item(i);
            values.add(n.getNodeValue());
        }
        return values;
    }
}
