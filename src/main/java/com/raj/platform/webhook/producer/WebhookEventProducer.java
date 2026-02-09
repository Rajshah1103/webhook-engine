package com.raj.platform.webhook.producer;

import com.raj.platform.webhook.dto.WebhookCreateRequest;
import com.raj.platform.webhook.dto.WebhookEventMessage;
import com.raj.platform.webhook.model.IdempotencyKey;
import com.raj.platform.webhook.repository.IdempotencyKeyRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class WebhookEventProducer {

    private static final String TOPIC = "webhook.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public WebhookEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            IdempotencyKeyRepository idempotencyKeyRepository
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    public UUID publish(
            WebhookCreateRequest request,
            String idempotencyKey
    ) {
        if(idempotencyKey != null) {
            return idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)
                    .map(IdempotencyKey::getEventId)
                    .orElseGet(() -> createAndPublish(request, idempotencyKey));
        }
        return createAndPublish(request, null);
    }

    private UUID createAndPublish(
            WebhookCreateRequest webhookCreateRequest,
            String idempotencyKey
    ) {
        UUID eventId = UUID.randomUUID();

        WebhookEventMessage message = new WebhookEventMessage(
                eventId,
                webhookCreateRequest.eventType(),
                webhookCreateRequest.targetUrl(),
                webhookCreateRequest.payload(),
                idempotencyKey,
                1,
                Instant.now()
        );
        try {
            String value = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(
                    TOPIC,
                    eventId.toString(),
                    value
            );

            IdempotencyKey key = IdempotencyKey.builder()
                    .eventId(null)
                    .idempotencyKey(idempotencyKey)
                    .eventId(eventId)
                    .createdAt(Instant.now())
                    .build();

            idempotencyKeyRepository.save(key);

            return eventId;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish webhook", e);
        }
    }

}
