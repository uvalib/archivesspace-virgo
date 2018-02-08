package edu.virginia.lib.indexing.tools;

import edu.virginia.lib.indexing.ASpaceAccession;
import edu.virginia.lib.indexing.ASpaceCollection;
import edu.virginia.lib.indexing.ArchivesSpaceClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static edu.virginia.lib.indexing.helpers.SolrHelper.getSolrOutputFile;
import static edu.virginia.lib.indexing.helpers.SolrHelper.getSolrXmlFieldValues;
import static edu.virginia.lib.indexing.helpers.XmlHelper.parseXmlFile;

/**
 * Created by md5wz on 1/12/18.
 */
public class IndexRecords {

    public static void main(String [] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            p.load(fis);
        }
        ArchivesSpaceClient c = new ArchivesSpaceClient(
                p.getProperty("archivesSpaceUrl"),
                p.getProperty("username"),
                p.getProperty("password"));

        final File output = new File(p.getProperty("indexOutputDir"));
        final File logs = new File(p.getProperty("logOutputDir"));

        // TODO: generate a report that includes
        // items updated
        // items published
        // items not published and why
        // items in error
        // processing time
        final File report = new File(logs, new SimpleDateFormat("yyyy-MM-dd-").format(new Date()) + "updated.txt");
        final PrintWriter published = new PrintWriter(new OutputStreamWriter(new FileOutputStream(report, true)));

        final long start = System.currentTimeMillis();
        published.write("Started at " + new Date());

        for (String repoId : c.listRepositoryIds()) {
            for (String accessionId : c.listAccessionIds(repoId)) {
                ASpaceAccession a = new ASpaceAccession(c, accessionId);
                final File f = getSolrOutputFile(output, accessionId);
                // TODO: it's important to reindex the accession record when the collection record changes
                //       reindexing all the accession records every day is the easy way to accomplish this
                //       but should be reconsidered later
                //if (f.exists() && getSolrXmlFieldValues("aspace_version_facet", parseXmlFile(f)).contains(String.valueOf(a.getLockVersion()))) {
                //    published.println(accessionId + ": skipped because lock number hasn't changed since last index");
                //} else {
                    try {
                        a.generateSolrAddDoc(output);
                        published.println(accessionId + ": " + a.getId());
                        published.flush();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        a.printOutRawData();
                        published.println(accessionId + ": skipped due to runtime error " + t.toString());
                    }
                //}
            }
            for (String resourceId : c.listResourceIds(repoId)) {
                final File f = getSolrOutputFile(output, resourceId);
                ASpaceCollection r = new ASpaceCollection(c, resourceId);
                Document solrDoc = null;
                if (f.exists()) {
                    try {
                        solrDoc = parseXmlFile(f);
                    } catch (SAXException e) {
                        System.out.println("Unable to parse existing SOLR document, overwriting! (" + f.getName() + ")");
                    }
                }
                if (solrDoc != null && getSolrXmlFieldValues("aspace_version_facet", solrDoc).contains(String.valueOf(r.getLockVersion()))) {
                    published.println(resourceId + ": skipped because lock number hasn't changed since last index");
                } else {
                    try {
                        r.generateSolrAddDoc(output);
                        published.println(resourceId + ": " + r.getId());
                        published.flush();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        r.printOutRawData();
                        published.println(resourceId + ": skipped due to runtime error " + t.toString());
                    }
                }
            }

        }
        published.println("Completed at " + new Date());
        published.println(((System.currentTimeMillis() - start) / 1000 / 60) + " minutes elapsed");
        published.close();
    }

}
