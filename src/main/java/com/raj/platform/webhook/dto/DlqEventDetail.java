package com.raj.platform.webhook.dto;

import com.raj.platform.webhook.model.WebhookDeliveryAttempt;

import java.util.List;
import java.util.UUID;

public record DlqEventDetail (

    UUID eventId,
    String eventType,
    String payload,
    List<WebhookDeliveryAttempt>attempts
) {}
