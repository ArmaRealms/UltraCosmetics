package be.isach.ultracosmetics.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version.
 *
 * @author iSach
 * @since 08-16-2015
 */
public class Version implements Comparable<Version> {

    // Searches version string for something like "d.d" or "d.d.d" and so on where d is one or more digits
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?:\\d+\\.)+\\d+");

    // only numbers (ex. 2.6.1)
    private final String version;
    // full version (ex. 2.6.1-RELEASE)
    private final String versionString;

    public final String get() {
        return this.version;
    }

    public String getFull() {
        return versionString;
    }

    public Version(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Version can not be null");
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not parse version string: '" + version + "'");
        }
        this.version = matcher.group();
        this.versionString = version;
    }

    @Override
    public int compareTo(Version otherVersion) {
        String[] thisParts = this.get().split("\\.");
        String[] thatParts = otherVersion.get().split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            int cmp = Integer.compare(thisPart, thatPart);
            if (cmp != 0) {
                return cmp;
            }
        }
        // release > dev build of same version
        return Boolean.compare(this.isRelease(), otherVersion.isRelease());
    }

    public boolean isDev() {
        return versionString.toLowerCase().contains("dev");
    }

    public boolean isRelease() {
        // Versions from Spigot do not contain -DEV or -RELEASE classifiers,
        // so just assume it's a release version if it doesn't say otherwise.
        return !isDev();
    }

    @Override
    public boolean equals(Object that) {
        return this == that || that != null && this.getClass() == that.getClass() && this.compareTo((Version) that) == 0;
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public String toString() {
        return get();
    }
}
