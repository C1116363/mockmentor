package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.MentorPayout;
import com.learn.interviewmentor.model.MentorPayoutStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MentorPayoutRepository extends JpaRepository<MentorPayout, Long> {

    /**
     * mentor and the two admin references are LAZY, and every payout screen
     * shows all three - so they come in the same query rather than as three
     * extra SELECTs per row.
     */
    @EntityGraph(attributePaths = {"mentor", "createdBy", "paidBy"})
    List<MentorPayout> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"mentor", "createdBy", "paidBy"})
    List<MentorPayout> findByMentorIdOrderByCreatedAtDesc(Long mentorId);

    @EntityGraph(attributePaths = {"mentor", "createdBy", "paidBy"})
    List<MentorPayout> findByStatusOrderByCreatedAtDesc(MentorPayoutStatus status);

    @EntityGraph(attributePaths = {"mentor", "createdBy", "paidBy"})
    Optional<MentorPayout> findById(Long id);

    /**
     * An unpaid payout already open for this mentor.
     *
     * Checked before raising another. Two PENDING payouts for one person is
     * never what anyone meant - the second would be for zero sessions, because
     * the first already claimed them all - and it turns the "owed" figure on
     * screen into a puzzle.
     */
    boolean existsByMentorIdAndStatus(Long mentorId, MentorPayoutStatus status);

    /**
     * Total actually paid to one mentor, ever.
     *
     * coalesce because SUM over no rows is null, not zero - and a null landing
     * in a BigDecimal on a payslip is an NPE rather than a "₹0".
     */
    @Query("""
            select coalesce(sum(p.amount), 0)
              from MentorPayout p
             where p.mentor.id = :mentorId
               and p.status = com.learn.interviewmentor.model.MentorPayoutStatus.PAID
            """)
    BigDecimal totalPaidTo(@Param("mentorId") Long mentorId);

    long countByStatus(MentorPayoutStatus status);

    @Query("""
            select coalesce(sum(p.amount), 0)
              from MentorPayout p
             where p.status = :status
            """)
    BigDecimal totalIn(@Param("status") MentorPayoutStatus status);
}
