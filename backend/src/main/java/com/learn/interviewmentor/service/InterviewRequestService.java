package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.AssignMentorRequestDto;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.meeting.MeetingLinkGenerator;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * All the business rules live here.
 *
 * Every method that acts on behalf of somebody now takes the logged-in User.
 * The URL-level rules in SecurityConfig answer "is this person a MENTOR?";
 * the ownership checks in here answer "is this person's *own* request?".
 * You need both - role checks alone would let one student cancel another
 * student's interview.
 */
public interface InterviewRequestService {

    InterviewRequestVo create(CreateRequestDto dto, User student);

    /** No email parameter any more - you can only ever see your own. */
    List<InterviewRequestVo> findMyRequests(User student);

    List<InterviewRequestVo> findPending(User mentor);

    List<InterviewRequestVo> findMyInterviews(User mentor);

    InterviewRequestVo accept(Long requestId, AcceptRequestDto dto, User mentor);

    InterviewRequestVo complete(Long requestId, CompleteRequestDto dto, User mentor);

    /** The student who raised it, the mentor who took it, or an admin. */
    InterviewRequestVo cancel(Long requestId, User actor);

    /**
    * An admin attaches a mentor to a student's request.
    *
    * This is the second way a request gets scheduled. A mentor can pick one up
    * themselves from the queue, or an admin can hand it to a specific person -
    * useful when a request needs someone particular, or has been sitting
    * unclaimed.
    */
    InterviewRequestVo assignMentor(Long requestId, AssignMentorRequestDto dto, User admin);

    /** Requests still waiting for a mentor - the admin's assignment queue. */
    List<InterviewRequestVo> findPendingForAdmin();

    /** One booking, whoever is asking. Used to read its slot and session type. */
    InterviewRequestVo findOne(Long requestId);

    List<InterviewRequestVo> findAll();

    long countByStatus(RequestStatus status);

    long countAll();
}
