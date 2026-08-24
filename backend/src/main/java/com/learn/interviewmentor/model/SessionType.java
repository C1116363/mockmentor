package com.learn.interviewmentor.model;

/**
 * What kind of hour the student booked.
 *
 * MOCK_INTERVIEW -> a real interview, under pressure, ending in a scorecard with
 *                   ratings and a readiness verdict.
 * MENTORING      -> a discussion. Career advice, a code review, "how would you
 *                   design this", help getting unstuck. No ratings, because
 *                   scoring somebody out of 5 for asking good questions is
 *                   nonsense and would make people ask fewer of them.
 *
 * This is a field on the booking rather than a second entity: picking a slot,
 * paying, being assigned a mentor and being completed are identical for both.
 * Only the shape of what the mentor writes at the end differs, and a whole
 * parallel table to vary one screen would be two of everything for nothing.
 */
public enum SessionType {

    MOCK_INTERVIEW("Mock interview", true),
    MENTORING("Mentoring session", false);

    private final String label;

    /**
     * Whether completing this kind of session requires ratings and a verdict.
     *
     * Bean validation on the DTO cannot express "required, but only for one
     * session type" - it never sees the booking. So the rule lives here, on the
     * type itself, and InterviewRequestService enforces it. Keeping it as data
     * rather than an `if` in the service means a third session type declares its
     * own answer instead of hiding inside a condition somebody has to find.
     */
    private final boolean scored;

    SessionType(String label, boolean scored) {
        this.label = label;
        this.scored = scored;
    }

    public String getLabel() {
        return label;
    }

    public boolean isScored() {
        return scored;
    }
}
