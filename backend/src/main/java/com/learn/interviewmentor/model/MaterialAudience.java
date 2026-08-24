package com.learn.interviewmentor.model;

/**
 * Who a piece of study material is for.
 *
 * ALL_STUDENTS     -> every student sees it
 * SPECIFIC_STUDENT -> only the one named in StudyMaterial.targetStudent
 * PLAN_MEMBERS     -> only students with an ACTIVE enrollment on that plan
 *
 * PLAN_MEMBERS is what makes a paid plan worth paying for, and it is enforced in
 * the query rather than in the UI - hiding a row on the client is decoration,
 * not access control.
 */
public enum MaterialAudience {
    ALL_STUDENTS,
    SPECIFIC_STUDENT,
    PLAN_MEMBERS
}
