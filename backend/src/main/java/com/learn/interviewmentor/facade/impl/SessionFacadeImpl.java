package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.facade.SessionFacade;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SessionFacadeImpl implements SessionFacade {

    private final InterviewRequestService requestService;

    public SessionFacadeImpl(InterviewRequestService requestService) {
        this.requestService = requestService;
    }

    @Override
    public ApiResult<InterviewRequestVo> book(CreateRequestDto request, User student) {
        InterviewRequestVo booked = requestService.create(request, student);
        return ApiResult.created(booked);
    }

    @Override
    public ApiResult<List<InterviewRequestVo>> mine(User student) {
        return ApiResult.ok(requestService.findMyRequests(student));
    }

    @Override
    public ApiResult<InterviewRequestVo> cancel(Long id, User actor) {
        return ApiResult.ok(requestService.cancel(id, actor), "Cancelled.");
    }

    @Override
    public ApiResult<List<InterviewRequestVo>> openQueue(User mentor) {
        return ApiResult.ok(requestService.findPending(mentor));
    }

    @Override
    public ApiResult<List<InterviewRequestVo>> assignedTo(User mentor) {
        return ApiResult.ok(requestService.findMyInterviews(mentor));
    }

    @Override
    public ApiResult<InterviewRequestVo> accept(Long id, AcceptRequestDto request, User mentor) {
        InterviewRequestVo accepted = requestService.accept(id, request, mentor);
        return ApiResult.ok(accepted, "Accepted. The student can see it now.");
    }

    @Override
    public ApiResult<InterviewRequestVo> complete(Long id, CompleteRequestDto request, User mentor) {
        InterviewRequestVo done = requestService.complete(id, request, mentor);
        return ApiResult.ok(done, done.scored()
                ? "Scorecard sent."
                : "Notes sent.");
    }

    @Override
    public ApiResult<InterviewRequestVo> attachCv(Long id, MultipartFile cv, User student) {
        InterviewRequestVo saved = requestService.attachCv(id, cv, student);
        return ApiResult.ok(saved,
                "CV attached. Your interviewer will read it before the session.");
    }

    @Override
    public InterviewRequest cvFor(Long id, User caller) {
        return requestService.cvFor(id, caller);
    }

    @Override
    public Path cvPath(InterviewRequest request) {
        return requestService.cvPath(request);
    }
}
