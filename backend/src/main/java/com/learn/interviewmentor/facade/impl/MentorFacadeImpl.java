package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.mentor.AvailabilityRequestDto;
import com.learn.interviewmentor.dto.mentor.MentorProfileRequestDto;
import com.learn.interviewmentor.facade.MentorFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.AvailabilityService;
import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.service.MentorProfileService;
import com.learn.interviewmentor.service.MentorService;
import com.learn.interviewmentor.vo.MentorVo;
import com.learn.interviewmentor.vo.mentor.MentorAvailabilityVo;
import com.learn.interviewmentor.vo.mentor.MentorProfileVo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class MentorFacadeImpl implements MentorFacade {

    private final MentorService mentorService;
    private final MentorProfileService profileService;
    private final AvailabilityService availabilityService;
    private final InterviewRequestService requestService;

    public MentorFacadeImpl(MentorService mentorService,
                            MentorProfileService profileService,
                            AvailabilityService availabilityService,
                            InterviewRequestService requestService) {
        this.mentorService = mentorService;
        this.profileService = profileService;
        this.availabilityService = availabilityService;
        this.requestService = requestService;
    }

    @Override
    public ApiResult<List<MentorVo>> approvedMentors() {
        return ApiResult.ok(mentorService.findAll());
    }

    @Override
    public ApiResult<MentorVo> mentor(Long userId) {
        return ApiResult.ok(mentorService.findByUserId(userId));
    }

    @Override
    public ApiResult<MentorProfileVo> myProfile(User mentor) {
        return ApiResult.ok(profileService.myProfile(mentor));
    }

    @Override
    public ApiResult<MentorProfileVo> submitProfile(MentorProfileRequestDto request, User mentor) {
        return ApiResult.ok(profileService.submit(request, mentor),
                "Profile submitted. An admin will verify it before you appear to students.");
    }

    // ---- availability ----

    @Override
    public ApiResult<List<MentorAvailabilityVo>> declareAvailability(AvailabilityRequestDto request,
                                                                     User mentor) {
        var result = availabilityService.declare(request, mentor);

        // The skipped hours are the interesting half of the answer - a mentor who
        // ticked six and got four needs to know which two and why, and a silent
        // partial success is how they find out at the worst moment.
        String message = result.skipped().isEmpty()
                ? result.saved().size() + " hour(s) offered."
                : result.saved().size() + " hour(s) offered. Skipped: "
                        + String.join("; ", result.skipped());

        return ApiResult.ok(result.saved(), message);
    }

    @Override
    public ApiResult<List<MentorAvailabilityVo>> myAvailability(User mentor) {
        return ApiResult.ok(availabilityService.mine(mentor));
    }

    @Override
    public ApiResult<MentorAvailabilityVo> withdrawAvailability(Long id, User mentor) {
        var row = availabilityService.withdraw(id, mentor);
        return ApiResult.ok(row, row.label() + " withdrawn.");
    }

    @Override
    public ApiResult<List<MentorAvailabilityVo>> allAvailability(LocalDate from, int days) {
        return ApiResult.ok(availabilityService.allInWindow(from, days));
    }

    /**
     * Composes two services: the booking says which hour and which kind of
     * session, and availability says who offered it. Neither needs to know about
     * the other to answer that.
     */
    @Override
    public ApiResult<List<MentorAvailabilityVo>> availableForRequest(Long requestId) {
        var request = requestService.findOne(requestId);
        return ApiResult.ok(
                availabilityService.openForSlot(request.preferredSlot(), request.sessionType()));
    }
}
