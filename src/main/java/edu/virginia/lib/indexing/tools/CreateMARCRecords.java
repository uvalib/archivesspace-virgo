package edu.virginia.lib.indexing.tools;

import edu.virginia.lib.indexing.ASpaceAccession;
import edu.virginia.lib.indexing.ASpaceCollection;
import edu.virginia.lib.indexing.ASpaceObject;
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
public class CreateMARCRecords {

    public static void main(String [] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            p.load(fis);
        }
        ArchivesSpaceClient c = new ArchivesSpaceClient(
                p.getProperty("archivesSpaceUrl"),
                p.getProperty("username"),
                p.getProperty("password"));

        final String host = p.getProperty("tracksysDbHost");
        final String user = p.getProperty("tracksysDbUsername");
        final String pass = p.getProperty("tracksysDbPassword");


        int aCtr = 0, rCtr = 0;
        for (String repoId : c.listRepositoryIds()) {
            for (String accessionId : c.listAccessionIds(repoId)) {
                ASpaceAccession a = new ASpaceAccession(c, accessionId);
                a.writeCirculationRecord(new File("files/a" + aCtr++));
            }
            for (String resourceId : c.listResourceIds(repoId)) {
                ASpaceCollection r = new ASpaceCollection(c, resourceId);
                r.writeCirculationRecord(new File("files/r" + rCtr++));
            }

        }

    }

}
