package com.raj.platform.webhook.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookEventMessage (
    UUID eventId,
    String eventType,
    String targetUrl,
    String payload,
    String idempotencyKey,
    int attemptNumber,
    Instant createdAt
) {}
