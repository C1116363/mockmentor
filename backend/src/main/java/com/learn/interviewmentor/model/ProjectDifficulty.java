package com.learn.interviewmentor.model;

/**
 * How much a contributor needs to know before this project is useful to them.
 *
 * Shown as a badge so a first-year does not pick the distributed-tracing rewrite
 * and conclude they cannot code.
 */
public enum ProjectDifficulty {
    BEGINNER("Good first project"),
    INTERMEDIATE("Some experience needed"),
    ADVANCED("For strong contributors");

    private final String label;

    ProjectDifficulty(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
