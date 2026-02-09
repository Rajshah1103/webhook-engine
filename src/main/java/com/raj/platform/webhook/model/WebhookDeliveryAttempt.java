package com.raj.platform.webhook.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @org.hibernate.annotations.UuidGenerator
    private UUID attemptId;

    private UUID eventId;

    private String targetUrl;

    private int attemptNumber;

    private String status; // SUCCESS FAILED

    private Integer responseCode;

    private String errorMessage;

    private Instant createdAt;
}
