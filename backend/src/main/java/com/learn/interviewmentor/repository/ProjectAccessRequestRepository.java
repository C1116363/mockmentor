package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.ProjectAccessRequest;
import com.learn.interviewmentor.model.ProjectAccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectAccessRequestRepository extends JpaRepository<ProjectAccessRequest, Long> {

    List<ProjectAccessRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** The payment review queue, oldest submission first. */
    List<ProjectAccessRequest> findByStatusOrderBySubmittedAtAsc(ProjectAccessStatus status);

    long countByStatus(ProjectAccessStatus status);

    /**
     * A request this student could still pay for, or is already using.
     *
     * Stops a second purchase of a project they already hold. REJECTED and
     * CANCELLED are excluded so they can start again after either.
     */
    @Query("""
            select r from ProjectAccessRequest r
            where r.student.id = :studentId
              and r.project.id = :projectId
              and r.status in (
                    com.learn.interviewmentor.model.ProjectAccessStatus.AWAITING_PAYMENT,
                    com.learn.interviewmentor.model.ProjectAccessStatus.SUBMITTED,
                    com.learn.interviewmentor.model.ProjectAccessStatus.ACTIVE)
            order by r.createdAt desc
            """)
    List<ProjectAccessRequest> findLiveForStudentAndProject(@Param("studentId") Long studentId,
                                                            @Param("projectId") Long projectId);

    default Optional<ProjectAccessRequest> findLatestLive(Long studentId, Long projectId) {
        return findLiveForStudentAndProject(studentId, projectId).stream().findFirst();
    }

    /**
     * How many contributors currently hold a seat on one project.
     *
     * SUBMITTED counts as taken: somebody who has paid and is waiting for
     * verification has effectively claimed a seat, and overselling it means
     * telling one of them no after taking their money.
     *
     * <b>{@code excludeId} is why this takes a parameter it looks like it should
     * not.</b> At approval time the row being approved is itself SUBMITTED, so
     * it is already inside this count - and on a full project that made approving
     * fail with "the project is full", where the seat it was competing for was
     * its own. Pass the request's id when approving; pass a value that matches
     * nothing (-1) when checking whether a brand-new request can be taken.
     */
    @Query("""
            select count(r) from ProjectAccessRequest r
            where r.project.id = :projectId
              and r.id <> :excludeId
              and (
                   (r.status = com.learn.interviewmentor.model.ProjectAccessStatus.ACTIVE
                    and (r.expiresAt is null or r.expiresAt > :now))
                or r.status = com.learn.interviewmentor.model.ProjectAccessStatus.SUBMITTED
              )
            """)
    long countTakenSeats(@Param("projectId") Long projectId,
                         @Param("now") LocalDateTime now,
                         @Param("excludeId") Long excludeId);

    /** Seats taken, counting everything. For display and for a new request. */
    default long countTakenSeats(Long projectId, LocalDateTime now) {
        return countTakenSeats(projectId, now, -1L);
    }

    /**
     * Approved and paid, but nobody has added them on GitHub yet.
     *
     * The queue that matters most operationally - these people have paid and
     * cannot see the code.
     */
    @Query("""
            select r from ProjectAccessRequest r
            where r.status = com.learn.interviewmentor.model.ProjectAccessStatus.ACTIVE
              and r.collaboratorGranted = false
              and (r.expiresAt is null or r.expiresAt > :now)
            order by r.grantedAt asc
            """)
    List<ProjectAccessRequest> findAwaitingCollaboratorInvite(@Param("now") LocalDateTime now);

    /**
     * Still marked ACTIVE but past its expiry.
     *
     * Nothing sweeps this table automatically, so these are people who should
     * have been removed from the repo already. Surfaced to the admin rather than
     * silently left - access that outlives what was paid for is the failure mode
     * worth catching.
     */
    @Query("""
            select r from ProjectAccessRequest r
            where r.status = com.learn.interviewmentor.model.ProjectAccessStatus.ACTIVE
              and r.expiresAt is not null and r.expiresAt <= :now
            order by r.expiresAt asc
            """)
    List<ProjectAccessRequest> findPastExpiry(@Param("now") LocalDateTime now);

    /** Active contributors on one project, for the project's own admin view. */
    @Query("""
            select r from ProjectAccessRequest r
            where r.project.id = :projectId
              and r.status = com.learn.interviewmentor.model.ProjectAccessStatus.ACTIVE
              and (r.expiresAt is null or r.expiresAt > :now)
            order by r.grantedAt desc
            """)
    List<ProjectAccessRequest> findActiveContributors(@Param("projectId") Long projectId,
                                                       @Param("now") LocalDateTime now);
}
