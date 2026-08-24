package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    /** What students and the marketing site see: live plans, in admin's order. */
    List<Plan> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    /** The admin list - retired plans included, they can be switched back on. */
    List<Plan> findAllByOrderByDisplayOrderAscIdAsc();

    Optional<Plan> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    long countByActiveTrue();
}
