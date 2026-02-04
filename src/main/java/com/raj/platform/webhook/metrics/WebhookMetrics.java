package com.raj.platform.webhook.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class WebhookMetrics {

    public final Counter success;
    public final Counter failure;

    public WebhookMetrics(MeterRegistry registry) {
        this.success = registry.counter("webhook_delivery_success_total");
        this.failure = registry.counter("webhook_delivery_failure_total");
    }
}
