package com.raj.platform.webhook.dto;

import jakarta.validation.constraints.NotBlank;

public record WebhookCreateRequest(
        @NotBlank String eventType,
        @NotBlank String targetUrl,
        @NotBlank String payload
) {}
