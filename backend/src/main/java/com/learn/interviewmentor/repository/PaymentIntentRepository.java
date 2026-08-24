package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.PaymentIntent;
import com.learn.interviewmentor.model.PaymentIntentStatus;
import com.learn.interviewmentor.model.PaymentPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

    /**
     * The webhook's lookup, with a row lock.
     *
     * <h2>Why the lock</h2>
     * Razorpay's retry and the student's browser callback race each other by
     * design - both report the same payment, and on a fast connection they
     * arrive milliseconds apart. Without the lock both transactions read
     * status=CREATED, both decide they are the one that settled it, and the
     * purchased thing is activated twice. PESSIMISTIC_WRITE makes the second one
     * wait, re-read, see PAID, and do nothing.
     *
     * This is the correctness centre of the whole integration.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PaymentIntent i where i.gatewayOrderId = :orderId")
    Optional<PaymentIntent> findByOrderIdForUpdate(@Param("orderId") String orderId);

    /** The same lookup without a lock, for read-only screens. */
    Optional<PaymentIntent> findByGatewayOrderId(String gatewayOrderId);

    /**
     * An intent already open against this row.
     *
     * Reused rather than replaced when a student reopens a checkout they walked
     * away from. Creating a fresh order every time works, but it leaves a trail
     * of orders at the gateway that look like failed payments in their
     * dashboard - and makes a real duplicate charge harder to spot among them.
     */
    @Query("""
            select i from PaymentIntent i
            where i.purpose = :purpose and i.targetId = :targetId
              and i.status = :status
            order by i.createdAt desc
            """)
    List<PaymentIntent> findByTargetAndStatus(@Param("purpose") PaymentPurpose purpose,
                                              @Param("targetId") Long targetId,
                                              @Param("status") PaymentIntentStatus status);

    /** Has this thing already been paid for? Guards against a second charge. */
    boolean existsByPurposeAndTargetIdAndStatus(PaymentPurpose purpose, Long targetId,
                                                PaymentIntentStatus status);

    /** For the sweep that ages out checkouts nobody ever came back to. */
    List<PaymentIntent> findByStatusAndCreatedAtBefore(PaymentIntentStatus status, LocalDateTime before);
}
