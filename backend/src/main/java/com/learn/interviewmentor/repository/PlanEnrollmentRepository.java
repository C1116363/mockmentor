package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.EnrollmentStatus;
import com.learn.interviewmentor.model.PlanEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanEnrollmentRepository extends JpaRepository<PlanEnrollment, Long> {

    List<PlanEnrollment> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** The admin review queue, oldest submission first so nobody waits longest. */
    List<PlanEnrollment> findByStatusOrderBySubmittedAtAsc(EnrollmentStatus status);

    long countByStatus(EnrollmentStatus status);

    /**
     * An enrollment this student could still pay for or is already using.
     *
     * Used to stop somebody starting a second purchase of a plan they already
     * hold. REJECTED and CANCELLED are deliberately excluded - after either of
     * those they should be able to start again.
     */
    @Query("""
            select e from PlanEnrollment e
            where e.student.id = :studentId
              and e.plan.id = :planId
              and e.status in (
                    com.learn.interviewmentor.model.EnrollmentStatus.AWAITING_PAYMENT,
                    com.learn.interviewmentor.model.EnrollmentStatus.SUBMITTED,
                    com.learn.interviewmentor.model.EnrollmentStatus.ACTIVE)
            order by e.createdAt desc
            """)
    List<PlanEnrollment> findLiveForStudentAndPlan(@Param("studentId") Long studentId,
                                                   @Param("planId") Long planId);

    default Optional<PlanEnrollment> findLatestLive(Long studentId, Long planId) {
        return findLiveForStudentAndPlan(studentId, planId).stream().findFirst();
    }

    /**
     * Plan ids this student currently holds. Drives PLAN_MEMBERS material.
     *
     * Checks the expiry date as well as the status: nothing sweeps the table at
     * midnight, so an ACTIVE row can be past its window.
     */
    @Query("""
            select e.plan.id from PlanEnrollment e
            where e.student.id = :studentId
              and e.status = com.learn.interviewmentor.model.EnrollmentStatus.ACTIVE
              and (e.expiresAt is null or e.expiresAt > :now)
            """)
    List<Long> findActivePlanIds(@Param("studentId") Long studentId, @Param("now") LocalDateTime now);
}
