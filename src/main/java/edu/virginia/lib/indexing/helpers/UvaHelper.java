package edu.virginia.lib.indexing.helpers;

/**
 * Created by md5wz on 1/26/18.
 */
public class UvaHelper {

    public static String normalizeLocation(final String location) {
        if (location.equals("Albert and Shirley Small Special Collections Library") || location.equals("University of Virginia, Special Collections Dept.")) {
            return "Special Collections";
        } else if (location.equals("University of Virginia, Law Library")) {
            return "Law Library";
        } else if (location.equals("Claude Moore Health Sciences Library")) {
            return "Health Sciences";
        } else if (location.equals("The Eleanor Crowder Bjoring Center for Nursing Historical Inquiry")) {
            return "Nursing";
        } else {
            throw new RuntimeException("Unknown location: " + location);
        }
    }

    public static String extractManifestUrl(final String location) {
        if (location.startsWith("http://mirador.lib")) {
            return location.substring(location.indexOf("=") + 1);
        } else {
            return location;
        }
    }
}
