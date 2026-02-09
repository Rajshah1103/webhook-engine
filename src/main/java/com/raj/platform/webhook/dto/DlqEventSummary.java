package com.raj.platform.webhook.dto;

import java.time.Instant;
import java.util.UUID;

public record DlqEventSummary(
        UUID eventId,
        String eventType,
        String targetUrl,
        int lastAttempt,
        Integer lastResponseCode,
        Instant createdAt
) {}
