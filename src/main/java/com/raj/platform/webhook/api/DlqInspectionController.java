package com.raj.platform.webhook.api;

import com.raj.platform.webhook.dto.DlqEventDetail;
import com.raj.platform.webhook.dto.DlqEventSummary;
import com.raj.platform.webhook.model.WebhookDeliveryAttempt;
import com.raj.platform.webhook.model.WebhookEvent;
import com.raj.platform.webhook.repository.WebhookDeliveryAttemptRepository;
import com.raj.platform.webhook.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/dlq")
@RequiredArgsConstructor
public class DlqInspectionController {

    private final WebhookDeliveryAttemptRepository webhookDeliveryAttemptRepository;
    private final WebhookEventRepository webhookEventRepository;

    @GetMapping("/events")
    public List<DlqEventSummary> listDlqEvents () {
        return webhookEventRepository.findAll().stream()
                .map(webhookEvent -> {
                    List<WebhookDeliveryAttempt> attempts = webhookDeliveryAttemptRepository.findByEventIdOrderByCreatedAtDesc(webhookEvent.getEventId());
                    if (attempts.isEmpty()) return null;

                    WebhookDeliveryAttempt last = attempts.get(0);

                    if (!"FAILED".equals(last.getStatus())) return null;

                    return new DlqEventSummary(
                            webhookEvent.getEventId(),
                            webhookEvent.getEventType(),
                            last.getTargetUrl(),
                            last.getAttemptNumber(),
                            last.getResponseCode(),
                            webhookEvent.getCreatedAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }


    @GetMapping("/events/{eventId}")
    public DlqEventDetail getDlqEvent(@PathVariable UUID eventId) {

        WebhookEvent event = webhookEventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<WebhookDeliveryAttempt> attempts =
                webhookDeliveryAttemptRepository.findByEventIdOrderByCreatedAtDesc(eventId);

        return new DlqEventDetail(
                event.getEventId(),
                event.getEventType(),
                event.getPayload(),
                attempts
        );
    }

}
