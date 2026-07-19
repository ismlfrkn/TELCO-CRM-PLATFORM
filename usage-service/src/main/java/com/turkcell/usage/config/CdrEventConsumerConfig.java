package com.turkcell.usage.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.usage.dto.request.CdrEventIngestRequest;
import com.turkcell.usage.service.CdrEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
public class CdrEventConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(CdrEventConsumerConfig.class);
    private static final String SOURCE_TOPIC = "telco.cdr.events";

    private final CdrEventService cdrEventService;
    private final ObjectMapper objectMapper;

    public CdrEventConsumerConfig(CdrEventService cdrEventService, ObjectMapper objectMapper) {
        this.cdrEventService = cdrEventService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Consumer<Message<String>> cdrEvents() {
        return message -> {
            JsonNode envelope = readEnvelope(message);
            if (envelope == null) {
                return;
            }

            String eventType = envelope.get("eventType").asText();
            if (!"CdrRecorded".equals(eventType)) {
                return;
            }

            try {
                CdrEventIngestRequest request = objectMapper.treeToValue(envelope.get("payload"), CdrEventIngestRequest.class);
                cdrEventService.ingest(request);
            } catch (Exception ex) {
                log.error("Failed to process CdrRecorded event from {}: {}", SOURCE_TOPIC, message.getPayload(), ex);
            }
        };
    }

    private JsonNode readEnvelope(Message<String> message) {
        try {
            return objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            log.error("Malformed event envelope from {}: {}", SOURCE_TOPIC, message.getPayload(), ex);
            return null;
        }
    }
}
