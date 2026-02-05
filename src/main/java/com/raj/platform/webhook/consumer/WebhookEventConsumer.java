package com.raj.platform.webhook.consumer;

import com.raj.platform.webhook.dispatcher.WebhookDispatcher;
import com.raj.platform.webhook.dto.WebhookEventMessage;
import com.raj.platform.webhook.model.WebhookDeliveryAttempt;
import com.raj.platform.webhook.model.WebhookEvent;
import com.raj.platform.webhook.repository.WebhookDeliveryAttemptRepository;
import com.raj.platform.webhook.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class WebhookEventConsumer {

    private final WebhookDispatcher dispatcher;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository;
    private final ObjectMapper objectMapper;

    // TEMP hardcoded target (FastAPI receiver comes later)
    private static final String TARGET_URL = "http://localhost:8081/webhook";

    public WebhookEventConsumer(
            WebhookDispatcher dispatcher,
            WebhookEventRepository webhookEventRepository,
            WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository,
            ObjectMapper objectMapper
    ) {
        this.dispatcher = dispatcher;
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDeliveryAttemptRepository = webhookDeliveryAttemptRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "webhook.events")
    public void consume(ConsumerRecord<String, String> record) {

        WebhookEventMessage message = objectMapper.readValue(record.value(), WebhookEventMessage.class);

        UUID eventId = message.eventId();

        WebhookEvent event = WebhookEvent.builder()
                .eventId(message.eventId())
                .eventType(message.eventType())
                .payload(message.payload())
                .createdAt(message.createdAt())
                .build();

        webhookEventRepository.save(event);

        int responseCode = dispatcher.dispatch(message.targetUrl(), message.payload());

        WebhookDeliveryAttempt attempt = WebhookDeliveryAttempt.builder()
                .eventId(eventId)
                .targetUrl(message.targetUrl())
                .attemptNumber(1)
                .status(responseCode >= 200 && responseCode < 300 ? "SUCCESS" : "FAILED")
                .responseCode(responseCode)
                .createdAt(Instant.now())
                .build();

        webhookDeliveryAttemptRepository.save(attempt);

        log.info(
                "Webhook processed. eventId={}, responseCode={}",
                eventId,
                responseCode
        );
    }
}
