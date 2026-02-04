package com.raj.platform.webhook.repository;

import com.raj.platform.webhook.model.WebhookDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttempt, UUID> {
}
