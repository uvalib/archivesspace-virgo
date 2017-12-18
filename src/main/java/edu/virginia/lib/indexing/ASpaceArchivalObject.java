package edu.virginia.lib.indexing;

import java.io.IOException;

/**
 * Created by md5wz on 11/9/17.
 */
public class ASpaceArchivalObject extends ASpaceObject {

    public ASpaceArchivalObject(ArchivesSpaceClient c, String refId) throws IOException {
        super(c, refId);
    }

    @Override
    public boolean isShadowed() throws IOException {
        throw new UnsupportedOperationException();
    }

    private String assertNotNull(final String value) {
        if (value == null) {
            throw new NullPointerException();
        } else {
            return value;
        }
    }

    public boolean isPublished() {
        return record.getBoolean("publish");
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getCallNumber() {
        return null;
    }

}
