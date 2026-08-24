package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.mentor.AvailabilityRequestDto;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.mentor.MentorAvailabilityVo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mentors declaring the hours they are free, and the admin reading them.
 *
 * A student is only ever offered an hour a verified mentor actually said yes to.
 */
public interface AvailabilityService {

    /**
     * What came of a bulk declaration.
     *
     * Two lists rather than throwing on the first problem: a mentor ticking a
     * whole afternoon should not lose the lot because one hour is already booked
     * or now too close.
     *
     * @param saved   the hours now on offer
     * @param skipped human-readable reasons, one per rejected hour
     */
    record DeclareResult(List<MentorAvailabilityVo> saved, List<String> skipped) {
    }

    DeclareResult declare(AvailabilityRequestDto dto, User mentor);

    /** A mentor's own upcoming hours. */
    List<MentorAvailabilityVo> mine(User mentor);

    /** Take an hour back, while nobody is mapped onto it. */
    MentorAvailabilityVo withdraw(Long id, User mentor);

    /** Every mentor's hours in a window. The admin's reference view. */
    List<MentorAvailabilityVo> allInWindow(LocalDate from, int days);

    /** Mentors free for one exact hour and session type. Drives the assign dropdown. */
    List<MentorAvailabilityVo> openForSlot(LocalDateTime slot, SessionType sessionType);

    /**
     * Claim a mentor's hour for a booking.
     *
     * @return false when the mentor never declared this hour. The caller decides
     *         whether to allow that - an admin sometimes needs to override.
     */
    boolean claimFor(User mentor, LocalDateTime slot, InterviewRequest request);

    /** Put the hour back when a booking is cancelled. */
    void releaseFor(InterviewRequest request);

    long countOpen();
}
