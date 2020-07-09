package edu.virginia.lib.indexing.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

public class IndexRecordsForV4 {

    public static void main(String [] args) throws Exception {
        Date now = new Date();
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            p.load(fis);
        }
       
        final File output = new File(p.getProperty("indexOutputDir"));
        final File logs = new File(p.getProperty("logOutputDir"));
        logs.mkdirs();

        final File report = new File(logs, new SimpleDateFormat("yyyy-MM-dd-").format(new Date()) + "indexed.txt");
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(report, true)));

        
        long since = -1;
        if (p.getProperty("interval") != null) {
            since = System.currentTimeMillis() - (24*60*60*1000*Integer.parseInt(p.getProperty("interval")));
        }
        
        File aspaceDoc = File.createTempFile("aspace", "index-for-v4-pipeline.xml");
        File avalonDoc = File.createTempFile("avalon", "index-for-v4-pipeline.xml");
        
        PrintWriter aspacePw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(aspaceDoc)));
        PrintWriter avalonPw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(avalonDoc)));
        
        IndexRecordsForV4 indexer = new IndexRecordsForV4();
        
        // find all files since the given date
        int aspaceSize = 0;
        int avalonSize = 0;
        for (File f : output.listFiles()) {
            if (f.lastModified() > since) {
                // transform and concatenate them into a single document
                try {
                    if (IndexRecordsForV4.isASpaceRecord(f)) {
                        aspacePw.print(indexer.getV4DocFromV3Doc(f));
                        aspaceSize ++;
                    } else {
                        avalonPw.print(indexer.getV4DocFromV3Doc(f));;
                        avalonSize ++;
                    }
                } catch (Exception ex) {
                    pw.println("Error producing V4 solr add doc from " + f.getName() + "! (SKIPPED FROM UPDATE)");
                }
            }
        }
        
        // write out that document
        aspacePw.flush();
        aspacePw.close();
        avalonPw.flush();
        avalonPw.close();
        
        try {
            // send it to S3
            DateFormat YYYY = new SimpleDateFormat("YYYY");
            DateFormat SECOND = new SimpleDateFormat("yyyy-MM-dd_HHmm");
            if (aspaceSize > 0) {
                transferFileToS3(pw, aspaceDoc, p.getProperty("bucketName"), p.getProperty("bucketPath") + YYYY.format(now) + "/aspace/" + SECOND.format(now) + ".xml"); 
            } else {
                pw.println("No aspace records modified in previous " + p.getProperty("interval") + " days.");
            }
            if (avalonSize > 0) {
                transferFileToS3(pw, avalonDoc, p.getProperty("bucketName"), p.getProperty("bucketPath") + YYYY.format(now) + "/avalon/" + SECOND.format(now) + ".xml"); 
            } else {
                pw.println("No avalon records modified in previous " + p.getProperty("interval") + " days.");
            }
        } catch (Throwable t) {
            t.printStackTrace(pw);
            System.err.println("Error transmitting index updates to S3!");
            System.exit(1);
        }
        System.out.println(aspaceSize + " aspace records sent to S3, " + avalonSize + " avalon records sent to S3.");
    }

    private static void transferFileToS3(PrintWriter pw, final File file, final String bucket, final String path) throws Exception {
        pw.println("Copying " + file.getAbsolutePath() + " to s3://" + bucket + "/" + path);
        TransferManager xfer_mgr = TransferManagerBuilder.standard().build();
        try {
            Upload xfer = xfer_mgr.upload(bucket, path, file);
            xfer.waitForCompletion();
        } finally {
            xfer_mgr.shutdownNow();
        }
    }
    
    private Transformer aspaceV3ToV4;

    private Transformer avalonV3ToV4;
    
    public IndexRecordsForV4() throws Exception {
        SAXTransformerFactory f = (SAXTransformerFactory) TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        aspaceV3ToV4 = f.newTemplates(new StreamSource(getClass().getClassLoader().getResourceAsStream("aspace-solr-v3-to-v4.xsl"))).newTransformer();
        avalonV3ToV4 = f.newTemplates(new StreamSource(getClass().getClassLoader().getResourceAsStream("avalon-solr-v3-to-v4.xsl"))).newTransformer();
        
    }
    
    private String getV4DocFromV3Doc(final File v3) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamSource s = new StreamSource(new FileInputStream(v3));
        try {
            if (isASpaceRecord(v3)) {
                aspaceV3ToV4.transform(s, new StreamResult(out));
            } else {
                avalonV3ToV4.transform(s, new StreamResult(out));
            }
        } finally {
            s.getInputStream().close();
        }
        return out.toString("UTF-8");
    }
    
    public static boolean isASpaceRecord(File v3) {
        return (v3.getName().startsWith("as:"));
        //TODO: it might be worth a better check, as a mistake here would likely go unnoticed for a while and represent a significantly poorer experience...
    }
}
