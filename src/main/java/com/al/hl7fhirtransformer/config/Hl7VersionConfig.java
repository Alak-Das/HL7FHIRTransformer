package com.al.hl7fhirtransformer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for HL7 version-specific settings.
 */
@Configuration
@ConfigurationProperties(prefix = "hl7-version-config")
public class Hl7VersionConfig {

    /**
     * Version-specific configurations keyed by HL7 version (e.g., "2.3", "2.4",
     * "2.5")
     */
    private Map<String, VersionSettings> versions = new HashMap<>();

    public Map<String, VersionSettings> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, VersionSettings> versions) {
        this.versions = versions;
    }

    /**
     * Get settings for a specific HL7 version.
     */
    public VersionSettings getVersion(String version) {
        return versions.getOrDefault(version, getDefaultSettings());
    }

    /**
     * Get default settings (v2.5)
     */
    public VersionSettings getDefaultSettings() {
        VersionSettings defaults = new VersionSettings();
        defaults.setEncodingCharacters("^~\\&");
        defaults.setDateFormat("yyyyMMdd");
        defaults.setDatetimeFormat("yyyyMMddHHmmss");
        defaults.setSegmentTerminator("\r");
        defaults.setUseEscapeSequences(true);
        return defaults;
    }

    public static class VersionSettings {
        /**
         * HL7 encoding characters (typically ^~\&)
         */
        private String encodingCharacters = "^~\\&";

        /**
         * Date format for TS fields
         */
        private String dateFormat = "yyyyMMdd";

        /**
         * DateTime format for TS fields
         */
        private String datetimeFormat = "yyyyMMddHHmmss";

        /**
         * Segment terminator character
         */
        private String segmentTerminator = "\r";

        /**
         * Whether to use escape sequences for special characters
         */
        private boolean useEscapeSequences = true;

        /**
         * Maximum field length (0 = unlimited)
         */
        private int maxFieldLength = 0;

        /**
         * Maximum segment repetitions
         */
        private int maxSegmentRepetitions = 999;

        public VersionSettings() {
        }

        public String getEncodingCharacters() {
            return encodingCharacters;
        }

        public void setEncodingCharacters(String encodingCharacters) {
            this.encodingCharacters = encodingCharacters;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public String getDatetimeFormat() {
            return datetimeFormat;
        }

        public void setDatetimeFormat(String datetimeFormat) {
            this.datetimeFormat = datetimeFormat;
        }

        public String getSegmentTerminator() {
            return segmentTerminator;
        }

        public void setSegmentTerminator(String segmentTerminator) {
            this.segmentTerminator = segmentTerminator;
        }

        public boolean isUseEscapeSequences() {
            return useEscapeSequences;
        }

        public void setUseEscapeSequences(boolean useEscapeSequences) {
            this.useEscapeSequences = useEscapeSequences;
        }

        public int getMaxFieldLength() {
            return maxFieldLength;
        }

        public void setMaxFieldLength(int maxFieldLength) {
            this.maxFieldLength = maxFieldLength;
        }

        public int getMaxSegmentRepetitions() {
            return maxSegmentRepetitions;
        }

        public void setMaxSegmentRepetitions(int maxSegmentRepetitions) {
            this.maxSegmentRepetitions = maxSegmentRepetitions;
        }
    }
}
