package com.learn.interviewmentor.repository;

import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Login and the JWT filter both look users up by email. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleOrderByFullNameAsc(Role role);

    long countByRole(Role role);
}
