package edu.virginia.lib.indexing;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ASpaceArchivalObject extends ASpaceObject {

    public ASpaceArchivalObject(ArchivesSpaceClient aspaceClient, final String refId, final JsonObject tree) throws IOException {
        super(aspaceClient, refId);
        if (!tree.getString("node_type").equals("archival_object")) {
            throw new IllegalArgumentException("Unexpected node_type \"" + tree.getString("node_type") + "\"");
        }
        this.tree = tree;
    }

    @Override
    protected Pattern getRefIdPattern() {
        return Pattern.compile("/?repositories/\\d+/archival_objects/\\d+");
    }

    @Override
    public boolean isShadowed() throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isPublished() {
        return getRecord().getBoolean("publish");
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
