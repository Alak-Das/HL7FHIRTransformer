package com.al.hl7fhirtransformer.service.converter;

import ca.uhn.hl7v2.model.GenericSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.util.Terser;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.hl7.fhir.r4.model.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converter for all proprietary Z-Segments.
 * Dynamically converts any segment starting with 'Z' into a FHIR Basic resource.
 */
@Component
public class ZSegmentConverter implements SegmentConverter<Basic> {

    private static final Logger log = LoggerFactory.getLogger(ZSegmentConverter.class);

    @Override
    public List<Basic> convert(Terser terser, Bundle bundle, ConversionContext context) {
        try {
            List<Basic> basicResources = new ArrayList<>();
            Message hapiMsg = context.getHapiMessage();

            // Recursively find all GenericSegments in the message
            List<GenericSegment> zSegments = new ArrayList<>();
            findZSegments(hapiMsg, zSegments);

            for (GenericSegment segment : zSegments) {
                String segmentName = segment.getName();
                
                Basic basic = new Basic();
                basic.setId(UUID.randomUUID().toString());
                basic.getCode().addCoding()
                        .setSystem("http://example.org/hl7/z-segment")
                        .setCode(segmentName)
                        .setDisplay("Custom Z-Segment " + segmentName);

                // Set subject to Patient if available
                if (context.getPatientId() != null) {
                    basic.setSubject(new org.hl7.fhir.r4.model.Reference("Patient/" + context.getPatientId()));
                }

                int numFields = segment.numFields();
                boolean hasData = false;

                for (int i = 1; i <= numFields; i++) {
                    Type[] fieldInstances = segment.getField(i);
                    for (int rep = 0; rep < fieldInstances.length; rep++) {
                        String value = fieldInstances[rep].encode();
                        if (value != null && !value.isBlank()) {
                            Extension ext = new Extension();
                            ext.setUrl("http://example.org/hl7/z-segment/" + segmentName + "/field/" + i);
                            ext.setValue(new StringType(value));
                            basic.addExtension(ext);
                            hasData = true;
                        }
                    }
                }

                if (hasData) {
                    basicResources.add(basic);
                    log.debug("Converted Z-Segment {} into Basic resource", segmentName);
                }
            }

            return basicResources;
        } catch (Exception e) {
            log.error("Error converting Z-Segments: {}", e.getMessage());
            throw new RuntimeException("Z-Segment conversion failed", e);
        }
    }

    private void findZSegments(Group group, List<GenericSegment> zSegments) {
        try {
            String[] names = group.getNames();
            for (String name : names) {
                Structure[] structures = group.getAll(name);
                for (Structure structure : structures) {
                    if (structure instanceof GenericSegment) {
                        GenericSegment segment = (GenericSegment) structure;
                        if (segment.getName() != null && segment.getName().startsWith("Z")) {
                            zSegments.add(segment);
                        }
                    } else if (structure instanceof Group) {
                        findZSegments((Group) structure, zSegments);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error scanning for Z-Segments: {}", e.getMessage());
        }
    }
}
