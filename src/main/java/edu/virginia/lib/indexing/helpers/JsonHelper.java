package edu.virginia.lib.indexing.helpers;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Static methods to help with Json manipulation.
 */
public class JsonHelper {

    public static void writeOutJson(JsonStructure o) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriter w = Json.createWriterFactory(properties).createWriter(System.out);
        w.write(o);
    }

    public static JsonObject parseJsonObject(InputStream is) throws IOException {
        try {
            return Json.createReader(is).readObject();
        } finally {
            is.close();
        }
    }

    public static boolean hasValue(JsonObject o, final String name) {
        return o.get(name) != null;
    }
}
