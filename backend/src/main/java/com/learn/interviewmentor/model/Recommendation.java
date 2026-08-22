package com.learn.interviewmentor.model;

/**
 * The mentor's overall verdict.
 *
 * Framed around readiness rather than hire / no-hire: this is practice, and a
 * candidate reading "NO HIRE" on their own scorecard learns less than one
 * reading "needs more work in these areas".
 */
public enum Recommendation {
    READY,          // would pass a real round today
    ALMOST_READY,   // close - a few gaps to close first
    NEEDS_WORK      // significant preparation still needed
}
