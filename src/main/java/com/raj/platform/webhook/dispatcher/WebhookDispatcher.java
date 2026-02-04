package com.raj.platform.webhook.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class WebhookDispatcher {

    private final WebClient webClient;

    public WebhookDispatcher(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public int dispatch(String targetUrl, String payload) {
        try {
            return webClient.post()
                    .uri(targetUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block()
                    .getStatusCode()
                    .value();
        } catch (Exception e) {
            log.error("Webhook delivery failed", e);
            return -1;
        }
    }
}
