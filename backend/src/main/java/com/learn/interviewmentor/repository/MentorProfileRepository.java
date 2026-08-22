package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.MentorProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    /** @EntityGraph pulls the user in the same query instead of a second one. */
    @EntityGraph(attributePaths = "user")
    Optional<MentorProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<MentorProfile> findAllByOrderByYearsOfExperienceDesc();
}
