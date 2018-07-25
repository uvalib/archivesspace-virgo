package edu.virginia.lib.indexing;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.virginia.lib.indexing.helpers.UvaHelper.extractManifestUrl;

/**
 * A class representing a digita_object in ArchivesSpace.  This class includes helper functions to pull
 * data from the underlying json model.
 */
public class ASpaceDigitalObject extends ASpaceObject {

    public ASpaceDigitalObject(ArchivesSpaceClient client, String refId) throws IOException {
        super(client, refId);
    }

    /**
     * @return an empty list because ASpaceDigitalObject objects cannot have children.
     */
    public List<ASpaceArchivalObject> getChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    protected Pattern getRefIdPattern() {
        return Pattern.compile("/?repositories/\\d+/digital_objects/\\d+");
    }

    @Override
    public boolean isShadowed() throws IOException {
        return !isPublished();
    }

    @Override
    public boolean isPublished() {
        return getRecord().getBoolean("publish");
    }

    public String getIIIFURL() {
        for (JsonValue v : getRecord().getJsonArray("file_versions")) {
            JsonObject ver = (JsonObject) v;
            if (ver.getBoolean("publish") && ver.getString("use_statement").startsWith("image-service")) {
                 return extractManifestUrl(ver.getString("file_uri"));
            }
        }
        return null;
    }

}
