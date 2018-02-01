package edu.virginia.lib.indexing;

import java.io.IOException;

/**
 * Created by md5wz on 11/9/17.
 */
public class ASpaceCollection extends ASpaceObject {

    public ASpaceCollection(ArchivesSpaceClient c, String refId) throws IOException {
        super(c, refId);
    }

    @Override
    public boolean isShadowed() throws IOException {
        return !isPublished();
    }

    private String assertNotNull(final String value) {
        if (value == null) {
            throw new NullPointerException();
        } else {
            return value;
        }
    }

    public boolean isPublished() {
        return record.getBoolean("publish") && record.get("finding_aid_status") != null
                && "completed".equals(record.getString("finding_aid_status"));
    }

}
