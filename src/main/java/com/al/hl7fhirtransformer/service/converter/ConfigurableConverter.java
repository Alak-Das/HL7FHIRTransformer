package com.al.hl7fhirtransformer.service.converter;

import ca.uhn.hl7v2.util.Terser;
import com.al.hl7fhirtransformer.model.MappingDefinition;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic converter that uses external mapping definitions to transform HL7
 * segments to FHIR resources.
 * This serves as the foundation for the "Configurable Mapping Rules" feature.
 */
@Component
@RequiredArgsConstructor
public class ConfigurableConverter {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableConverter.class);

    public List<Resource> convert(Terser terser, Bundle bundle, MappingDefinition definition) {
        List<Resource> resources = new ArrayList<>();

        try {
            // In a full implementation, this would:
            // 1. Iterate over segment repetitions (e.g. OBX)
            // 2. Instantiate the target FHIR Resource (using reflection or HAPI factory)
            // 3. Apply each FieldMapping from the definition
            // - Extract value from Terser using sourceField
            // - Apply transformation
            // - Set value on FHIR resource using targetField path

            log.info("Processing configurable mapping for {}", definition.getId());

        } catch (Exception e) {
            log.error("Error executing configurable mapping {}: {}", definition.getId(), e.getMessage());
        }

        return resources;
    }
}
