package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.LiveProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiveProjectRepository extends JpaRepository<LiveProject, Long> {

    /** What students see: open projects, in the order an admin arranged them. */
    List<LiveProject> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    /** The admin list - retired projects included, they can be reopened. */
    List<LiveProject> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<LiveProject> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    /**
     * Two projects must not point at the same repo.
     *
     * Not a hard rule of the domain, but selling the same repository twice under
     * two names and two prices is a mistake worth catching at the point somebody
     * makes it.
     */
    boolean existsByRepoOwnerIgnoreCaseAndRepoNameIgnoreCase(String repoOwner, String repoName);

    long countByActiveTrue();
}
