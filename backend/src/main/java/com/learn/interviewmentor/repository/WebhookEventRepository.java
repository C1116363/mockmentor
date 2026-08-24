package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    /** Most recent first - what you open when a payment is being disputed. */
    List<WebhookEvent> findTop50ByOrderByReceivedAtDesc();

    List<WebhookEvent> findByGatewayOrderIdOrderByReceivedAtDesc(String gatewayOrderId);
}
