package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.mentor.AvailabilityRequestDto;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequestDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.vo.mentor.MentorAvailabilityVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;

import java.time.LocalDate;
import java.util.List;

/** The mentor directory, and a mentor's own onboarding profile. */
public interface MentorFacade {

    ApiResult<List<MentorVo>> approvedMentors();

    ApiResult<MentorVo> mentor(Long userId);

    ApiResult<MentorProfileVo> myProfile(User mentor);

    ApiResult<MentorProfileVo> submitProfile(MentorProfileRequestDto request, User mentor);

    // ---- availability ----

    /**
     * Declare a day's free hours.
     *
     * Partial success by design: a mentor ticking a whole afternoon should not
     * lose the lot because one hour is already booked or now inside the notice
     * period. The message names what was skipped and why.
     */
    ApiResult<List<MentorAvailabilityVo>> declareAvailability(AvailabilityRequestDto request,
                                                              User mentor);

    ApiResult<List<MentorAvailabilityVo>> myAvailability(User mentor);

    ApiResult<MentorAvailabilityVo> withdrawAvailability(Long id, User mentor);

    /** Every mentor's hours in a window. ADMIN - their reference view. */
    ApiResult<List<MentorAvailabilityVo>> allAvailability(LocalDate from, int days);

    /**
     * Mentors free for one booking's exact slot and session type.
     *
     * Drives the admin's assign dropdown, so they pick from people who said yes
     * to that hour rather than every verified mentor on the books.
     */
    ApiResult<List<MentorAvailabilityVo>> availableForRequest(Long requestId);
}
