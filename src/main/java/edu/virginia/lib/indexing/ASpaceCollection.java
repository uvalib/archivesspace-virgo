package edu.virginia.lib.indexing;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by md5wz on 11/9/17.
 */
public class ASpaceCollection extends ASpaceObject {

    public ASpaceCollection(ArchivesSpaceClient c, String refId) throws IOException {
        super(c, refId);
    }

    @Override
    protected Pattern getRefIdPattern() {
        return Pattern.compile("/?repositories/\\d+/resources/\\d+");
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
        JsonObject cm = getRecord().getJsonObject("collection_management");
        return getRecord().getBoolean("publish") && cm != null && cm.get("processing_status") != null
                && "completed".equals(cm.getString("processing_status")) && !getTopContainers().isEmpty();
    }

}
