package edu.virginia.lib.indexing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import edu.virginia.lib.indexing.tools.IndexRecordsForV4;

public class IndexRecordsForV4Test {

    @Test
    public void testSCXslt() throws Exception {
        final String doc = new IndexRecordsForV4().getV4DocFromV3Doc(new File("src/test/resources/v3index/as:3r754.xml"));
        assertTrue("Index document must contain source_f_stored for Special Collections.", doc.contains("<field name=\"source_f_stored\">Special Collections</field>"));
    }
    
    @Test
    public void testLawXslt() throws Exception {
        final String doc = new IndexRecordsForV4().getV4DocFromV3Doc(new File("src/test/resources/v3index/as:4r686.xml"));
        assertFalse("Index document must not source_f_stored for Law Library.", doc.contains("<field name=\"source_f_stored\">Law"));
    }
    
}
