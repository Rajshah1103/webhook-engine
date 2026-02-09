package com.raj.platform.webhook.repository;

import com.raj.platform.webhook.model.WebhookDeliveryAttempt;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.awt.print.Pageable;
import java.util.List;
import java.util.UUID;

public interface WebhookDeliveryAttemptRepository extends JpaRepository<WebhookDeliveryAttempt, UUID> {


    @Query("""
            SELECT a FROM
            WebhookDeliveryAttempt a
            WHERE a.createdAt (
            SELECT MAX(a2.createdAt)
            FROM WebhookDeliveryAttempt a2
            WHERE a2.eventId = a.eventId
            )
            AND a.status = 'FAILED'
                    ORDER BY a.createdAt DESC
            """)
    Page<WebhookDeliveryAttempt> findLatestFailedAttempt(Pageable pageable);

    List<WebhookDeliveryAttempt> findByEventIdOrderByCreatedAtDesc(UUID eventId);

}
