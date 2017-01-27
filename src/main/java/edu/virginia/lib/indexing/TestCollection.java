package edu.virginia.lib.indexing;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class TestCollection {

    public static void main(String [] args) throws IOException, XMLStreamException, TransformerException {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            p.load(fis);
        }

        final File eadXML = getEADXML(p.getProperty("archivesSpaceUrl"),
                                      p.getProperty("username"),
                                      p.getProperty("password"),
                                      p.getProperty("repositoryId"),
                                      p.getProperty("archivalObjectId"));

        final File solrDocDir = produceSolrDocuments(eadXML);
        postDirectoryToSolr(p.getProperty("solrUrl"), solrDocDir);

    }

    private static File getEADXML(final String archivesSpaceUrl, final String username, final String password, final String repoId, final String archivalObject) throws IOException, XMLStreamException {
        File eadXML = new File("example.xml");
        ArchivesSpaceClient c = new ArchivesSpaceClient(archivesSpaceUrl, username, password);
        if (!eadXML.exists()) {
            try (FileOutputStream fos = new FileOutputStream(eadXML)) {
                System.out.println("Generating EAD XML from ArchivesSpace, " + eadXML.getPath() + "...");
                c.writeEnhancedEADXML(repoId, archivalObject, fos);
            }
        } else {
            System.out.println("Using previously generated EAD XML, " + eadXML.getPath() + ".");
        }
        return eadXML;
    }

    private static File produceSolrDocuments(final File source) throws TransformerException, IOException {
        File outputDir = new File("solr-output");
        if (outputDir.exists()) {
            System.out.println("Using previously generated Solr docs, " + outputDir.getPath() + "...");
        } else {
            System.out.println("Creating Solr docs, " + outputDir.getPath() + "...");
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Templates templates = tFactory.newTemplates(
                    new StreamSource(TestCollection.class.getClassLoader().getResourceAsStream("EADToSolr.xsl")));
            Transformer t = templates.newTransformer();
            t.setParameter("output", outputDir.getPath());
            try (FileInputStream sourceStream = new FileInputStream(source); OutputStream out = new NullOutputStream()) {
                t.transform(new StreamSource(sourceStream), new StreamResult(out));
            }
        }
        return outputDir;
    }

    private static void postDirectoryToSolr(final String solrUrl, final File solrDocDir) throws IOException {
        System.out.println("Writing docs to solr " + solrUrl + "...");
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(solrUrl + "?commit=true");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            for (File solrDoc : solrDocDir.listFiles()) {
                builder.addBinaryBody(solrDoc.getName(), solrDoc, ContentType.create("text/xml", "UTF-8"), solrDoc.getName());
            }
            post.setEntity(builder.build());
            try (CloseableHttpResponse response = client.execute(post)) {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Unable to write documents to solr at " + solrUrl + ". (" + response.getStatusLine().toString() + ")");
                }
            }
        }
        System.out.println("DONE!");
    }
}
