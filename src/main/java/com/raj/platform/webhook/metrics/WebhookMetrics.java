package com.raj.platform.webhook.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class WebhookMetrics {

    public final Counter eventsReceived;
    public final Counter success;
    public final Counter failure;
    public final Counter retries;
    public final Counter dlq;
    public final Timer deliveryLatency;

    public WebhookMetrics(MeterRegistry registry) {
        this.eventsReceived = registry.counter("webhook_events_received_total");
        this.success = registry.counter("webhook_delivery_success_total");
        this.failure = registry.counter("webhook_delivery_failure_total");
        this.retries = registry.counter("webhook_delivery_retry_total");
        this.dlq = registry.counter("webhook_delivery_dtq_total");
        this.deliveryLatency = registry.timer("webhook_delivery_latency");
    }
}
