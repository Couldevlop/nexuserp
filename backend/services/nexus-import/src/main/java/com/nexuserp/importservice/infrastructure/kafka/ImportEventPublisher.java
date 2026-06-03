package com.nexuserp.importservice.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ImportEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ImportEventPublisher.class);
    private static final String TOPIC = "nexus.import.complete";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ImportEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishImportComplete(String tenantId, String importType, int totalRows, int successRows, int errorRows) {
        try {
            Map<String, Object> event = Map.of(
                "tenantId", tenantId,
                "importType", importType,
                "totalRows", totalRows,
                "successRows", successRows,
                "errorRows", errorRows
            );
            kafkaTemplate.send(TOPIC, tenantId, objectMapper.writeValueAsString(event));
            log.info("Published import.complete event: tenant={}, type={}, success={}/{}", tenantId, importType, successRows, totalRows);
        } catch (Exception e) {
            log.error("Failed to publish import event: {}", e.getMessage(), e);
        }
    }
}
