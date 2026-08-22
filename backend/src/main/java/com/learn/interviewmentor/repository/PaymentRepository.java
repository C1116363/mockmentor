package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.Payment;
import com.learn.interviewmentor.model.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** request + student are needed to render every row, so pull them in one query. */
    @EntityGraph(attributePaths = {"request", "request.student", "reviewedBy"})
    Optional<Payment> findByRequestId(Long requestId);

    @EntityGraph(attributePaths = {"request", "request.student", "reviewedBy"})
    Optional<Payment> findById(Long id);

    /** The admin's queue: proof sent, waiting to be checked. Oldest first. */
    @EntityGraph(attributePaths = {"request", "request.student", "reviewedBy"})
    List<Payment> findByStatusOrderBySubmittedAtAsc(PaymentStatus status);

    long countByStatus(PaymentStatus status);
}
