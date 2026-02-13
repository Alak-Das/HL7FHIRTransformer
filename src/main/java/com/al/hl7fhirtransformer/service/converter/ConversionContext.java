package com.al.hl7fhirtransformer.service.converter;

import ca.uhn.hl7v2.model.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;

public class ConversionContext {
    private String patientId;
    private String encounterId;
    private String transactionId;
    private Message hapiMessage;

    // Location and Organization context
    private String locationId;
    private String sendingOrganizationId;
    private String receivingOrganizationId;

    private Map<String, ServiceRequest> serviceRequests = new HashMap<>();
    private Map<String, MedicationRequest> medicationRequests = new HashMap<>();
    private Map<Integer, List<Observation>> observationsByObr = new HashMap<>();
    private String triggerEvent;

    public ConversionContext() {
    }

    public ConversionContext(String patientId, String encounterId, String transactionId, Message hapiMessage,
            String locationId, String sendingOrganizationId, String receivingOrganizationId,
            Map<String, ServiceRequest> serviceRequests,
            Map<String, MedicationRequest> medicationRequests,
            Map<Integer, List<Observation>> observationsByObr,
            String triggerEvent) {
        this.patientId = patientId;
        this.encounterId = encounterId;
        this.transactionId = transactionId;
        this.hapiMessage = hapiMessage;
        this.locationId = locationId;
        this.sendingOrganizationId = sendingOrganizationId;
        this.receivingOrganizationId = receivingOrganizationId;
        this.serviceRequests = serviceRequests != null ? serviceRequests : new HashMap<>();
        this.medicationRequests = medicationRequests != null ? medicationRequests : new HashMap<>();
        this.observationsByObr = observationsByObr != null ? observationsByObr : new HashMap<>();
        this.triggerEvent = triggerEvent;
    }

    public static ConversionContextBuilder builder() {
        return new ConversionContextBuilder();
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getEncounterId() {
        return encounterId;
    }

    public void setEncounterId(String encounterId) {
        this.encounterId = encounterId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Message getHapiMessage() {
        return hapiMessage;
    }

    public void setHapiMessage(Message hapiMessage) {
        this.hapiMessage = hapiMessage;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getSendingOrganizationId() {
        return sendingOrganizationId;
    }

    public void setSendingOrganizationId(String sendingOrganizationId) {
        this.sendingOrganizationId = sendingOrganizationId;
    }

    public String getReceivingOrganizationId() {
        return receivingOrganizationId;
    }

    public void setReceivingOrganizationId(String receivingOrganizationId) {
        this.receivingOrganizationId = receivingOrganizationId;
    }

    public Map<String, ServiceRequest> getServiceRequests() {
        return serviceRequests;
    }

    public void setServiceRequests(Map<String, ServiceRequest> serviceRequests) {
        this.serviceRequests = serviceRequests;
    }

    public Map<String, MedicationRequest> getMedicationRequests() {
        return medicationRequests;
    }

    public void setMedicationRequests(Map<String, MedicationRequest> medicationRequests) {
        this.medicationRequests = medicationRequests;
    }

    public Map<Integer, List<Observation>> getObservationsByObr() {
        return observationsByObr;
    }

    public void setObservationsByObr(Map<Integer, List<Observation>> observationsByObr) {
        this.observationsByObr = observationsByObr;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public static class ConversionContextBuilder {
        private String patientId;
        private String encounterId;
        private String transactionId;
        private Message hapiMessage;
        private String locationId;
        private String sendingOrganizationId;
        private String receivingOrganizationId;
        private Map<String, ServiceRequest> serviceRequests = new HashMap<>();
        private Map<String, MedicationRequest> medicationRequests = new HashMap<>();
        private Map<Integer, List<Observation>> observationsByObr = new HashMap<>();
        private String triggerEvent;

        ConversionContextBuilder() {
        }

        public ConversionContextBuilder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public ConversionContextBuilder encounterId(String encounterId) {
            this.encounterId = encounterId;
            return this;
        }

        public ConversionContextBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public ConversionContextBuilder hapiMessage(Message hapiMessage) {
            this.hapiMessage = hapiMessage;
            return this;
        }

        public ConversionContextBuilder locationId(String locationId) {
            this.locationId = locationId;
            return this;
        }

        public ConversionContextBuilder sendingOrganizationId(String sendingOrganizationId) {
            this.sendingOrganizationId = sendingOrganizationId;
            return this;
        }

        public ConversionContextBuilder receivingOrganizationId(String receivingOrganizationId) {
            this.receivingOrganizationId = receivingOrganizationId;
            return this;
        }

        public ConversionContextBuilder serviceRequests(Map<String, ServiceRequest> serviceRequests) {
            this.serviceRequests = serviceRequests;
            return this;
        }

        public ConversionContextBuilder medicationRequests(Map<String, MedicationRequest> medicationRequests) {
            this.medicationRequests = medicationRequests;
            return this;
        }

        public ConversionContextBuilder observationsByObr(Map<Integer, List<Observation>> observationsByObr) {
            this.observationsByObr = observationsByObr;
            return this;
        }

        public ConversionContextBuilder triggerEvent(String triggerEvent) {
            this.triggerEvent = triggerEvent;
            return this;
        }

        public ConversionContext build() {
            return new ConversionContext(patientId, encounterId, transactionId, hapiMessage, locationId,
                    sendingOrganizationId, receivingOrganizationId, serviceRequests, medicationRequests,
                    observationsByObr, triggerEvent);
        }

        public String toString() {
            return "ConversionContext.ConversionContextBuilder(patientId=" + this.patientId + ", encounterId="
                    + this.encounterId + ", transactionId=" + this.transactionId + ", hapiMessage=" + this.hapiMessage
                    + ", locationId=" + this.locationId + ", sendingOrganizationId=" + this.sendingOrganizationId
                    + ", receivingOrganizationId=" + this.receivingOrganizationId + ", serviceRequests="
                    + this.serviceRequests + ", medicationRequests=" + this.medicationRequests + ", observationsByObr="
                    + this.observationsByObr + ", triggerEvent=" + this.triggerEvent + ")";
        }
    }
}
