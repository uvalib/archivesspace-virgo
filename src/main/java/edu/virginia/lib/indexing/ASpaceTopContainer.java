package edu.virginia.lib.indexing;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a top_container in ArchivesSpace.  This class includes helper functions to pull
 * data from the underlying json model.
 *
 * Created by md5wz on 6/14/18.
 */
public class ASpaceTopContainer extends ASpaceObject {

    private String location;

    public ASpaceTopContainer(ArchivesSpaceClient client, String refId) throws IOException {
        super(client, refId);
    }

    /**
     * @return an empty list because ASpaceTopContainer objects cannot have children.
     */
    public List<ASpaceArchivalObject> getChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    protected Pattern getRefIdPattern() {
        return Pattern.compile("/?repositories/\\d+/top_containers/\\d+");
    }

    @Override
    public boolean isShadowed() throws IOException {
        return !isPublished();
    }

    @Override
    public boolean isPublished() {
        return getRecord().getBoolean("is_linked_to_published_record");
    }

    /**
     * Gets the call number for the container, which is based off of the
     * supplied call number of the collection or accession to which the
     * container belongs.
     */
    public String getContainerCallNumber(final String owningCallNumber) {
        return owningCallNumber + " " + getRecord().getString("display_string");
    }

    /**
     * Gets the current location.
     */
    public String getCurrentLocation() throws IOException {
        if (location == null) {
            JsonArray loc = getRecord().getJsonArray("container_locations");
            for (JsonValue v : loc) {
                JsonObject l = (JsonObject) v;
                if (l.getString("status").equals("current")) {
                    location = c.resolveReference(l.getString("ref")).getString("title");
                }
            }
            if (location == null) {
                return "";
            }
        }
        return location;
    }

    /**
     * Gets a barcode if one exists, otherwise returns a compatible identifier
     * derived from the top container reference id.
     */
    public String getBarcode() {
        JsonValue barcode = getRecord().get("barcode");
        if (barcode != null) {
            return getRecord().getString("barcode");
        } else {
            Matcher m = Pattern.compile("/repositories/(\\d+)/top_containers/(\\d+)").matcher(getRecord().getString("uri"));
            if (m.matches()) {
                return "AS:" + m.group(1) + "C" + m.group(2);
            } else {
                return "UNKNOWN";
            }
        }
    }

    public String getLocation() {
        JsonValue room = getRecord().get("room");
        if (room == null) {
            return "STACKS";
        } else {
            return room.toString();
        }
    }
}
