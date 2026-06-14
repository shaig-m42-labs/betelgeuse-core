package com.m42.betelgeuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Nats;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final ObjectMapper objectMapper;
    private final String natsUrl;
    private Connection connection;

    EventPublisher(ObjectMapper objectMapper, @Value("${app.nats.url:${NATS_URL:nats://localhost:4222}}") String natsUrl) {
        this.objectMapper = objectMapper;
        this.natsUrl = natsUrl;
    }

    @PostConstruct
    void connect() {
        try {
            connection = Nats.connect(natsUrl);
        } catch (Exception ex) {
            log.warn("NATS connection unavailable: {}", ex.getMessage());
        }
    }

    void publish(String subject, String eventType, String correlationId, Map<String, Object> payload) {
        EventEnvelope envelope = new EventEnvelope(UUID.randomUUID().toString(), eventType, "betelgeuse-core", Instant.now(), correlationId, payload);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(envelope);
            if (connection != null) {
                connection.publish(subject, bytes);
                connection.flush(Duration.ofSeconds(2));
            }
        } catch (Exception ex) {
            log.warn("Could not publish event {}: {}", eventType, ex.getMessage());
        }
    }

    void subscribeFailures(CoreService coreService) {
        if (connection == null) {
            return;
        }
        try {
            connection.createDispatcher(msg -> {
                try {
                    EventEnvelope envelope = objectMapper.readValue(new String(msg.getData(), StandardCharsets.UTF_8), EventEnvelope.class);
                    Object value = envelope.payload().get("healthCheckConfigId");
                    if (value != null) {
                        coreService.handleHealthCheckFailed(UUID.fromString(value.toString()), envelope.correlationId(), envelope.payload());
                    }
                } catch (Exception ex) {
                    log.warn("Could not handle health failure event: {}", ex.getMessage());
                }
            }).subscribe("orion.healthcheck.failed");
        } catch (Exception ex) {
            log.warn("Could not subscribe to health failures: {}", ex.getMessage());
        }
    }

    @PreDestroy
    void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

@Component
class HealthFailureSubscriber {
    private final EventPublisher events;
    private final CoreService coreService;

    HealthFailureSubscriber(EventPublisher events, CoreService coreService) {
        this.events = events;
        this.coreService = coreService;
    }

    @PostConstruct
    void subscribe() {
        events.subscribeFailures(coreService);
    }
}
