package com.raj.platform.webhook.consumer;

import com.raj.platform.webhook.dispatcher.WebhookDispatcher;
import com.raj.platform.webhook.model.WebhookDeliveryAttempt;
import com.raj.platform.webhook.model.WebhookEvent;
import com.raj.platform.webhook.repository.WebhookDeliveryAttemptRepository;
import com.raj.platform.webhook.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class WebhookEventConsumer {

    private final WebhookDispatcher dispatcher;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository;

    // TEMP hardcoded target (FastAPI receiver comes later)
    private static final String TARGET_URL = "http://localhost:8081/webhook";

    public WebhookEventConsumer(
            WebhookDispatcher dispatcher,
            WebhookEventRepository webhookEventRepository,
            WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository
    ) {
        this.dispatcher = dispatcher;
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDeliveryAttemptRepository = webhookDeliveryAttemptRepository;
    }

    @KafkaListener(topics = "webhook.events")
    public void consume(ConsumerRecord<String, String> record) {
        UUID eventId = UUID.randomUUID();

        WebhookEvent event = WebhookEvent.builder()
                .eventId(eventId)
                .eventType("TEST_EVENT")
                .payload(record.value())
                .createdAt(Instant.now())
                .build();

        webhookEventRepository.save(event);

        int responseCode = dispatcher.dispatch(TARGET_URL, record.value());

        WebhookDeliveryAttempt attempt = WebhookDeliveryAttempt.builder()
                .eventId(eventId)
                .targetUrl(TARGET_URL)
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
