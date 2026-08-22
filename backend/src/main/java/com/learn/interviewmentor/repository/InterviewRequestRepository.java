package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
