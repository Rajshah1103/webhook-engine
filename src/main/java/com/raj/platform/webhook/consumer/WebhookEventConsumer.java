package com.raj.platform.webhook.consumer;

import com.raj.platform.webhook.dispatcher.WebhookDispatcher;
import com.raj.platform.webhook.dto.WebhookEventMessage;
import com.raj.platform.webhook.metrics.WebhookMetrics;
import com.raj.platform.webhook.model.WebhookDeliveryAttempt;
import com.raj.platform.webhook.model.WebhookEvent;
import com.raj.platform.webhook.repository.WebhookDeliveryAttemptRepository;
import com.raj.platform.webhook.repository.WebhookEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class WebhookEventConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private static final long RETRY_1_DELAY_MS = 60_000;   // 1 min
    private static final long RETRY_2_DELAY_MS = 300_000;  // 5 min

    private final WebhookDispatcher dispatcher;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebhookMetrics metrics;

    public WebhookEventConsumer(
            WebhookDispatcher dispatcher,
            WebhookEventRepository webhookEventRepository,
            WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            WebhookMetrics metrics
    ) {
        this.dispatcher = dispatcher;
        this.webhookEventRepository = webhookEventRepository;
        this.webhookDeliveryAttemptRepository = webhookDeliveryAttemptRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    private boolean isSuccess(int code) {
        return code >= 200 && code < 300;
    }

    private boolean isClientError(int code) {
        return code >= 400 && code < 500;
    }

    private WebhookEventMessage nextAttempt(WebhookEventMessage message) {
        return new WebhookEventMessage(
                message.eventId(),
                message.eventType(),
                message.targetUrl(),
                message.payload(),
                message.idempotencyKey(),
                message.attemptNumber() + 1,
                message.createdAt()
        );
    }

    @KafkaListener(topics = "webhook.events")
    public void consume(ConsumerRecord<String, String> record) throws Exception {
        processRecord(record);
    }

    @KafkaListener(topics = "webhook.events.retry.1")
    public void consumeRetry1(ConsumerRecord<String, String> record) throws Exception {
        Thread.sleep(RETRY_1_DELAY_MS);
        processRecord(record);
    }

    @KafkaListener(topics = "webhook.events.retry.2")
    public void consumeRetry2(ConsumerRecord<String, String> record) throws Exception {
        Thread.sleep(RETRY_2_DELAY_MS);
        processRecord(record);
    }

    private void processRecord(ConsumerRecord<String, String> record) throws Exception {

        metrics.eventsReceived.increment();
        WebhookEventMessage message = objectMapper.readValue(record.value(), WebhookEventMessage.class);

        UUID eventId = message.eventId();

        WebhookEvent event = WebhookEvent.builder()
                .eventId(message.eventId())
                .eventType(message.eventType())
                .payload(message.payload())
                .createdAt(message.createdAt())
                .build();

        try {
            webhookEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate event detected, eventId={}", eventId);
        }

        int responseCode = metrics.deliveryLatency.record(() ->
                dispatcher.dispatch(message.targetUrl(), message.payload())
                );

        WebhookDeliveryAttempt attempt = WebhookDeliveryAttempt.builder()
                .eventId(eventId)
                .targetUrl(message.targetUrl())
                .attemptNumber(message.attemptNumber())
                .status(isSuccess(responseCode) ? "SUCCESS" : "FAILED" )
                .responseCode(responseCode)
                .createdAt(Instant.now())
                .build();

        webhookDeliveryAttemptRepository.save(attempt);

        log.info(
                "Webhook attempt processed. eventId={}, attempt={}, responseCode={}",
                eventId,
                message.attemptNumber(),
                responseCode
        );

        if ((isSuccess(responseCode))) {
            metrics.success.increment();
            return;
        }
        metrics.failure.increment();
        if(isClientError(responseCode)) {
            publishDlq(record);
            return;
        }

        if (message.attemptNumber() >= MAX_ATTEMPTS) {
            publishDlq(record);
            return;
        }

        publishRetry(message);

    }

    private void publishRetry(WebhookEventMessage message) throws Exception {
        metrics.retries.increment();
        WebhookEventMessage retryMessage = nextAttempt(message);

        String topic = switch (retryMessage.attemptNumber()) {
            case 2 -> "webhook.events.retry.1";
            case 3 -> "webhook.events.retry.2";
            default -> "webhook.events.dlq";
        };

        kafkaTemplate.send(
                topic,
                retryMessage.eventId().toString(),
                objectMapper.writeValueAsString(retryMessage)
        );

        log.info(
                "Retry scheduled. eventId={}, nextAttempt={}",
                retryMessage.eventId(),
                retryMessage.attemptNumber()
        );
    }

    private void publishDlq(ConsumerRecord<String, String> record) {
        metrics.dlq.increment();
        kafkaTemplate.send(
                "webhook.events.dlq",
                record.key(),
                record.value()
        );

        log.warn("Message sent to DLQ. key={}", record.key());
    }
}
