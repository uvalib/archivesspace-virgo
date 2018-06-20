package edu.virginia.lib.indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.xml.stream.XMLStreamException;

import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import edu.virginia.lib.indexing.helpers.JsonHelper;



public class Marc4jTester {
    public static void main(String[] args) {
        //testMARC();
        try {
            //System.out.println(createMarcRecord(7, 262).toString());
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream("config.properties")) {
                p.load(fis);
            }
            ArchivesSpaceClient c = new ArchivesSpaceClient(
                    p.getProperty("archivesSpaceUrl"),
                    p.getProperty("username"),
                    p.getProperty("password"));

            ASpaceCollection rec = new ASpaceCollection(c, "/repositories/3/resources/488");
            //"/repositories/7/resources/156" "/repositories/3/resources/488"
            File file = new File("result.mrc");
            rec.writeCirculationRecord(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("All done.");
    }
    
    
    
    public static void testMARC() {
        //create marc object
        System.out.print("Creating MARC object... ");
        MarcFactory f = MarcFactory.newInstance();
        Record r = f.newRecord();
        DataField df;
        Subfield sf;
        
        df = f.newDataField("590", '1', ' ');
        sf = f.newSubfield('a', "From ArchivesSpace: (resource/accession) X, Repository Y");
        df.addSubfield(sf);
        r.addVariableField(df);
        
        df = f.newDataField("999", ' ', ' ');
        sf = f.newSubfield('a', "indicator from top_container in ArchivesSpace");
        df.addSubfield(sf);
        sf = f.newSubfield('i', "barcode from top_container or a placeholder");
        df.addSubfield(sf);
        sf = f.newSubfield('m', "library short name");
        df.addSubfield(sf);
        r.addVariableField(df);
        
        System.out.println("done.");
        //System.out.println(r.toString());
        System.out.println("Valid: " + f.validateRecord(r));
        
        
        
        //write marc object to file
        System.out.print("Writing file... ");
        try {
            File file = new File("example.mrc");
            if (!file.exists())
                file.createNewFile();
            
            FileOutputStream o = new FileOutputStream(file);
            MarcWriter w = new MarcStreamWriter(o);
            
            w.write(r);
            
            w.close();
            o.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("done.");
    }
    
    public static void testAPI() throws IOException, XMLStreamException, SQLException {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            p.load(fis);
        }
        ArchivesSpaceClient c = new ArchivesSpaceClient(
                p.getProperty("archivesSpaceUrl"),
                p.getProperty("username"),
                p.getProperty("password"));

        ASpaceCollection rec = new ASpaceCollection(c, "/repositories/7/resources/156/tree");
        JsonHelper.writeOutJson(rec.record, new FileOutputStream("dump.json"));
        JsonArray children = rec.record.getJsonArray("children");
        /*
        for every child in tree
            if it's an archival object
                grab its json
                for each entry in that jsons instances
                    if its sub container has top_container
                        add value of top_container to the list
        */
        HashSet<String> set = new HashSet<String>();
        
        for (JsonValue child : children) {
            if (((JsonObject)child).getString("node_type").equals("archival_object")) {
                ASpaceCollection temp = new ASpaceCollection(c, ((JsonObject)child).getString("record_uri"));
                JsonArray instances = temp.record.getJsonArray("instances");
                for (JsonValue instance : instances) {
                    JsonObject subContainer = ((JsonObject)instance).getJsonObject("sub_container");
                    set.add(subContainer.getJsonObject("top_container").getString("ref"));
                }
            }
        }
        
        System.out.println(set.toString());
        //System.out.println(rec.record.get("top_container").toString());
        
        //TODO: create marc files with 999 entries for each top_container found.
    }
    
    /**
     * Returns a MARC record object representing the given ArchivesSpace repository and resource.
     * @param repository the ArchivesSpace repository to pull from
     * @param resource the specific resource in the ArchivesSpace repository to pull
     * @return a MARC record object representing the given resource
     * @throws IOException
     * @throws XMLStreamException
     * @throws SQLException
     */
    public static Record createMarcRecord(int repository, int resource) throws IOException, XMLStreamException, SQLException {
        //make MARC record with 245/590 field
        MarcFactory factory = MarcFactory.newInstance();
        Record r = factory.newRecord();
        DataField df;
        Subfield sf;
        
        df = factory.newDataField("590", '1', ' ');
        sf = factory.newSubfield('a', "From ArchivesSpace: /repositories/" + repository + "/resources/" + resource);
        df.addSubfield(sf);
        r.addVariableField(df);
        
        
        
        //pull desired resource from ArchivesSpace, get reference for each unique top_container
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            p.load(fis);
        }
        ArchivesSpaceClient c = new ArchivesSpaceClient(
                p.getProperty("archivesSpaceUrl"),
                p.getProperty("username"),
                p.getProperty("password"));

        ASpaceCollection rec = new ASpaceCollection(c, "/repositories/7/resources/156/tree");
        JsonHelper.writeOutJson(rec.record, new FileOutputStream("dump.json"));
        HashSet<String> topContainers = new HashSet<String>();
        
        //add 245 field with title
        String title = rec.record.getString("title");
        char nonIndexChars = '0';
        if (title.startsWith("A "))
            nonIndexChars = '2';
        else if (title.startsWith("The "))
            nonIndexChars = '4';
        
        df = factory.newDataField("245", '0', nonIndexChars);
        sf = factory.newSubfield('a', title);
        df.addSubfield(sf);
        r.addVariableField(df);
        
        JsonArray children = rec.record.getJsonArray("children");
        
        for (JsonValue child : children) {
            if (((JsonObject)child).getString("node_type").equals("archival_object")) {
                ASpaceCollection temp = new ASpaceCollection(c, ((JsonObject)child).getString("record_uri"));
                JsonArray instances = temp.record.getJsonArray("instances");
                for (JsonValue instance : instances) {
                    JsonObject subContainer = ((JsonObject)instance).getJsonObject("sub_container");
                    topContainers.add(subContainer.getJsonObject("top_container").getString("ref"));
                    //ASpaceCollection temptemp = new ASpaceCollection(c, subContainer.getJsonObject("top_container").getString("ref"));
                    //JsonHelper.writeOutJson(temptemp.record, new FileOutputStream("dump.json"));
                }
            }
        }
        
        
        
        //generate a 999 field for each top_container
        for (String s : topContainers) {
            ASpaceCollection topContainer = new ASpaceCollection(c, s);
            
            df = factory.newDataField("999", ' ', ' ');
            //sf = factory.newSubfield('a', topContainer.record.getString("indicator"));
            //df.addSubfield(sf);
            
            String barcode;
            if (topContainer.record.get("barcode") != null) {
                barcode = topContainer.record.getString("barcode");
            } else {
                String uri = topContainer.record.getString("uri");
                String topContainerNumber = uri.substring(uri.lastIndexOf("/") + 1);
                barcode = "AS:R" + repository + "C" + topContainerNumber;
            }
            sf = factory.newSubfield('i', barcode);
            df.addSubfield(sf);
            
            sf = factory.newSubfield('m', topContainer.record.getString("display_string"));
            df.addSubfield(sf);
            
            r.addVariableField(df);
        }
        
        
        
      //write marc object to file
          System.out.print("Writing file... ");
          try {
              File file = new File("example.mrc");
              if (!file.exists())
                  file.createNewFile();
              
              FileOutputStream o = new FileOutputStream(file);
              MarcWriter w = new MarcStreamWriter(o);
              
              w.write(r);
              
              w.close();
              o.close();
          } catch (Exception e) {
              e.printStackTrace();
          }
          System.out.println("done.");
        
        return r;
    }
}