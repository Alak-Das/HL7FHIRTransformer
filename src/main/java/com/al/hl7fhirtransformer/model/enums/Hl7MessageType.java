package com.al.hl7fhirtransformer.model.enums;

/**
 * HL7 v2 message types supported for FHIR→HL7 conversion.
 * Separated from the audit {@link MessageType} enum to avoid namespace collision.
 */
public enum Hl7MessageType {
    ADT_A01,  // Admit/Visit Notification
    ORM_O01,  // General Order Message
    ORU_R01,  // Unsolicited Observation Message
    SIU_S12,  // Scheduling Information Unsolicited
    MDM_T02   // Document Status Notification
}
