package edu.virginia.lib.indexing;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by md5wz on 11/9/17.
 */
public class ASpaceArchivalObject extends ASpaceObject {

    public ASpaceArchivalObject(ArchivesSpaceClient c, String refId) throws IOException {
        super(c, refId);
    }


    @Override
    protected Pattern getRefIdPattern() {
        return Pattern.compile("/?repositories/\\d+/archival_objects/\\d+");
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
