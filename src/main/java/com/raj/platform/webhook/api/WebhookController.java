package com.raj.platform.webhook.api;

import com.raj.platform.webhook.dto.WebhookCreateRequest;
import com.raj.platform.webhook.dto.WebhookCreateResponse;
import com.raj.platform.webhook.producer.WebhookEventProducer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookEventProducer webhookEventProducer;

    public WebhookController(WebhookEventProducer webhookEventProducer) {
        this.webhookEventProducer = webhookEventProducer;
    }

    @PostMapping
    public ResponseEntity<WebhookCreateResponse> createWebhook(
            @Valid @RequestBody WebhookCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
            ) {
        UUID eventId = webhookEventProducer.publish(
                request,
                idempotencyKey
        );
        return ResponseEntity.accepted().body(new WebhookCreateResponse(eventId, "ACCEPTED"));
    }
}
