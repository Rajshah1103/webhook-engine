package com.raj.platform.webhook.dto;

import java.util.UUID;

public record WebhookCreateResponse(
        UUID eventId,
        String status
) {
}
