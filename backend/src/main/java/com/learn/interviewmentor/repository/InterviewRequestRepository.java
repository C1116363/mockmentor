package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.MentorPayout;
import com.learn.interviewmentor.model.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InterviewRequestRepository extends JpaRepository<InterviewRequest, Long> {

    /**
     * Every finder pulls student + mentor in the same query with @EntityGraph.
     * Both are LAZY @ManyToOne, so without this each row would fire extra
     * SELECTs - the classic N+1 problem.
     */
    @EntityGraph(attributePaths = {"student", "mentor"})
    List<InterviewRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    @EntityGraph(attributePaths = {"student", "mentor"})
    List<InterviewRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"student", "mentor"})
    List<InterviewRequest> findByMentorIdOrderByScheduledAtAsc(Long mentorId);

    @EntityGraph(attributePaths = {"student", "mentor"})
    List<InterviewRequest> findAllByOrderByCreatedAtDesc();

    /** Overridden purely to attach the entity graph to the by-id lookup too. */
    @EntityGraph(attributePaths = {"student", "mentor"})
    Optional<InterviewRequest> findById(Long id);

    long countByStatus(RequestStatus status);

    /** How many live bookings already sit on a given slot. Drives availability. */
    long countByPreferredSlotAndStatusIn(LocalDateTime preferredSlot, Collection<RequestStatus> statuses);

    // ------------------------------------------------------------------
    // Payroll
    // ------------------------------------------------------------------

    /**
     * Claim every unpaid completed session for one mentor into one payout.
     *
     * <h2>This single statement is what makes payroll safe</h2>
     * {@code payout_id is null} lives in the WHERE clause, so the database
     * decides who wins. Two admins pressing "create payout" for the same mentor
     * at the same moment cannot both claim the same session: one UPDATE matches
     * the rows and the other matches none, and the return value says which
     * happened. Read-then-write in Java loses that race, and losing it means a
     * mentor is paid twice for the same work.
     *
     * @return how many sessions this payout actually won - not how many were
     *         expected. The caller totals from this, never from an earlier count.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update InterviewRequest r
               set r.payout = :payout
             where r.mentor.id = :mentorId
               and r.status = com.learn.interviewmentor.model.RequestStatus.COMPLETED
               and r.payout is null
            """)
    int claimUnpaidSessions(@Param("mentorId") Long mentorId,
                            @Param("payout") MentorPayout payout);

    /** Put a cancelled payout's sessions back in the pot. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InterviewRequest r set r.payout = null where r.payout = :payout")
    int releaseSessions(@Param("payout") MentorPayout payout);

    List<InterviewRequest> findByPayout(MentorPayout payout);

    /**
     * Unpaid completed sessions per type, for the "what is owed" screen.
     *
     * Counted by the database rather than by loading the rows and grouping in
     * Java - this runs once per mentor on a page that lists all of them, and
     * the only thing wanted is two numbers.
     */
    @Query("""
            select r.sessionType, count(r)
              from InterviewRequest r
             where r.mentor.id = :mentorId
               and r.status = com.learn.interviewmentor.model.RequestStatus.COMPLETED
               and r.payout is null
             group by r.sessionType
            """)
    List<Object[]> countUnpaidByType(@Param("mentorId") Long mentorId);

    /** Same shape, for the sessions already inside one payout. */
    @Query("""
            select r.sessionType, count(r)
              from InterviewRequest r
             where r.payout = :payout
             group by r.sessionType
            """)
    List<Object[]> countByTypeInPayout(@Param("payout") MentorPayout payout);

    /**
     * The window a payout's sessions span, for the payslip.
     *
     * The coalesce mirrors {@code InterviewRequest.getCompletedAt()}, and has to
     * be repeated here because JPQL cannot call a getter - it reads columns.
     * Without it, sessions completed before the completed_at column existed
     * carry null, min and max over nulls are null, and a payout covering only
     * older work gets a payslip with no dates on it. Rather than looking like a
     * bug, that looks like a blank field nobody filled in.
     */
    @Query("""
            select min(coalesce(r.completedAt, r.scheduledAt, r.createdAt)),
                   max(coalesce(r.completedAt, r.scheduledAt, r.createdAt))
              from InterviewRequest r
             where r.payout = :payout
            """)
    List<Object[]> completedRangeIn(@Param("payout") MentorPayout payout);
}
