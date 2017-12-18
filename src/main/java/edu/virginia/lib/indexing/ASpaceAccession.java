package edu.virginia.lib.indexing;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.io.IOException;

/**
 * Created by md5wz on 12/18/17.
 */
public class ASpaceAccession extends ASpaceObject {

    public ASpaceAccession(ArchivesSpaceClient aspaceClient, final String accessionId) throws IOException {
        super(aspaceClient, accessionId);
    }

    public boolean isShadowed() throws IOException {
        return !(isPublished() && !hasPublishedCollectionRecord());
    }

    public boolean isPublished() {
        return record.getBoolean("publish");
    }

    public boolean hasPublishedCollectionRecord() throws IOException {
        final JsonArray relatedResources = record.getJsonArray("related_resources");
        if (relatedResources.size() == 0) {
            return false;
        }
        for (JsonValue v : relatedResources) {
            ASpaceCollection col = new ASpaceCollection(c, ((JsonObject) v).getString("ref"));
            if (col.isPublished()) {
                return true;
            }
        }
        return false;
    }

}
