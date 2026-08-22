package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.dto.InterviewRequestDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@Transactional(readOnly = true)
public class InterviewRequestService {

    private final InterviewRequestRepository requestRepository;

    public InterviewRequestService(InterviewRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    // ---------- student side ----------

    @Transactional
    public InterviewRequestDto create(CreateRequestDto dto, User student) {
        InterviewRequest request = new InterviewRequest(
                student,
                dto.topic(),
                dto.experienceLevel(),
                dto.preferredDate(),
                dto.notes()
        );
        return InterviewRequestDto.from(requestRepository.save(request));
    }

    /** No email parameter any more - you can only ever see your own. */
    public List<InterviewRequestDto> findMyRequests(User student) {
        return requestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    // ---------- mentor side ----------

    public List<InterviewRequestDto> findPending() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    public List<InterviewRequestDto> findMyInterviews(User mentor) {
        return requestRepository.findByMentorIdOrderByScheduledAtAsc(mentor.getId()).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    @Transactional
    public InterviewRequestDto accept(Long requestId, AcceptRequestDto dto, User mentor) {
        InterviewRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Request " + requestId + " is already " + request.getStatus() + ", it cannot be accepted again");
        }

        request.assignTo(mentor, dto.scheduledAt(), dto.meetingLink());
        return InterviewRequestDto.from(request); // dirty checking flushes on commit
    }

    @Transactional
    public InterviewRequestDto complete(Long requestId, CompleteRequestDto dto, User mentor) {
        InterviewRequest request = getRequestOrThrow(requestId);

        // Being a MENTOR is not enough - it has to be YOUR interview.
        if (!request.isMentoredBy(mentor)) {
            throw new ForbiddenException("This interview was accepted by another mentor");
        }
        if (request.getStatus() != RequestStatus.SCHEDULED) {
            throw new BadRequestException("Only a SCHEDULED interview can be completed");
        }

        request.complete(dto.feedback());
        return InterviewRequestDto.from(request);
    }

    /** The student who raised it, the mentor who took it, or an admin. */
    @Transactional
    public InterviewRequestDto cancel(Long requestId, User actor) {
        InterviewRequest request = getRequestOrThrow(requestId);

        boolean allowed = actor.getRole() == Role.ADMIN
                || request.isOwnedBy(actor)
                || request.isMentoredBy(actor);

        if (!allowed) {
            throw new ForbiddenException("You cannot cancel this request");
        }
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BadRequestException("A completed interview cannot be cancelled");
        }

        request.cancel();
        return InterviewRequestDto.from(request);
    }

    // ---------- admin ----------

    public List<InterviewRequestDto> findAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    public long countByStatus(RequestStatus status) {
        return requestRepository.countByStatus(status);
    }

    public long countAll() {
        return requestRepository.count();
    }

    private InterviewRequest getRequestOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Interview request not found with id " + id));
    }
}
