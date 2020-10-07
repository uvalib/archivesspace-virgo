package edu.virginia.lib.indexing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.virginia.lib.indexing.tools.IndexRecordsForV4;

public class IndexRecordsForV4Test {

    //@Test disabled because it produces temp files and may send stuff to s3... but useful for development and
    // could later be updated for a dry-run mode
    public void testXslt() throws Exception {
        assertEquals("Program didn't return 0.", 0, IndexRecordsForV4.main(new String[] { "src/test/resources/test-config.properties"}));   
    }
    
}
