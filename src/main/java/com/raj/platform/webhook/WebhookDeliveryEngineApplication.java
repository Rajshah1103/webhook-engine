package com.raj.platform.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class WebhookDeliveryEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookDeliveryEngineApplication.class, args);
	}

}
