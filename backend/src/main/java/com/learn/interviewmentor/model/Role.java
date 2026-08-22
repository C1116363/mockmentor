package com.learn.interviewmentor.model;

/**
 * Who someone is in the system.
 *
 * Spring Security expects authorities to be prefixed with "ROLE_" when you use
 * hasRole("ADMIN"), so User.getAuthorities() adds that prefix. We keep the enum
 * itself clean.
 */
public enum Role {
    STUDENT,
    MENTOR,
    ADMIN
}
