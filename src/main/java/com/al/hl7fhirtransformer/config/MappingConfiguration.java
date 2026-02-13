package com.al.hl7fhirtransformer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "fhir-to-hl7")
public class MappingConfiguration {

    private MessageTypeDetection messageTypeDetection;
    private PatientMapping patient;

    public MessageTypeDetection getMessageTypeDetection() {
        return messageTypeDetection;
    }

    public void setMessageTypeDetection(MessageTypeDetection messageTypeDetection) {
        this.messageTypeDetection = messageTypeDetection;
    }

    public PatientMapping getPatient() {
        return patient;
    }

    public void setPatient(PatientMapping patient) {
        this.patient = patient;
    }

    public static class MessageTypeDetection {
        private List<MessageTypeRule> rules;

        public List<MessageTypeRule> getRules() {
            return rules;
        }

        public void setRules(List<MessageTypeRule> rules) {
            this.rules = rules;
        }
    }

    public static class MessageTypeRule {
        private List<String> resources;
        private String messageType;

        public List<String> getResources() {
            return resources;
        }

        public void setResources(List<String> resources) {
            this.resources = resources;
        }

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }

    public static class PatientMapping {
        private Map<String, String> genderMap;
        private Map<String, String> maritalStatusMap;
        private Map<String, String> identifierTypeCodeMap;

        public Map<String, String> getGenderMap() {
            return genderMap;
        }

        public void setGenderMap(Map<String, String> genderMap) {
            this.genderMap = genderMap;
        }

        public Map<String, String> getMaritalStatusMap() {
            return maritalStatusMap;
        }

        public void setMaritalStatusMap(Map<String, String> maritalStatusMap) {
            this.maritalStatusMap = maritalStatusMap;
        }

        public Map<String, String> getIdentifierTypeCodeMap() {
            return identifierTypeCodeMap;
        }

        public void setIdentifierTypeCodeMap(Map<String, String> identifierTypeCodeMap) {
            this.identifierTypeCodeMap = identifierTypeCodeMap;
        }
    }
}
