package com.al.hl7fhirtransformer.model;

import java.util.List;
import java.util.Map;

public class MappingDefinition {
    private String id;
    private String description;
    private String sourceSegment; // e.g. "PID"
    private String targetResource; // e.g. "Patient"
    private List<FieldMapping> mappings;

    public MappingDefinition() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSegment() {
        return sourceSegment;
    }

    public void setSourceSegment(String sourceSegment) {
        this.sourceSegment = sourceSegment;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public void setTargetResource(String targetResource) {
        this.targetResource = targetResource;
    }

    public List<FieldMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<FieldMapping> mappings) {
        this.mappings = mappings;
    }

    public static class FieldMapping {
        private String sourceField; // e.g. "PID-5-1"
        private String targetField; // e.g. "name[0].family"
        private String transformation; // e.g. "UPPERCASE", "DATE_FORMAT"
        private Map<String, String> params;

        public FieldMapping() {
        }

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public String getTargetField() {
            return targetField;
        }

        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }

        public String getTransformation() {
            return transformation;
        }

        public void setTransformation(String transformation) {
            this.transformation = transformation;
        }

        public Map<String, String> getParams() {
            return params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }
    }
}
