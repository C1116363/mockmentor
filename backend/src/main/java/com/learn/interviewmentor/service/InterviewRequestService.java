package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.AssignMentorRequest;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.dto.InterviewRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@Transactional(readOnly = true)
public class InterviewRequestService {

    private final InterviewRequestRepository requestRepository;
    private final SlotService slotService;
    private final MentorProfileService mentorProfileService;
    private final UserRepository userRepository;
    private final MeetingLinkGenerator meetingLinkGenerator;
    private final PaymentService paymentService;

    public InterviewRequestService(InterviewRequestRepository requestRepository,
                                   SlotService slotService,
                                   MentorProfileService mentorProfileService,
                                   UserRepository userRepository,
                                   MeetingLinkGenerator meetingLinkGenerator,
                                   PaymentService paymentService) {
        this.requestRepository = requestRepository;
        this.slotService = slotService;
        this.mentorProfileService = mentorProfileService;
        this.userRepository = userRepository;
        this.meetingLinkGenerator = meetingLinkGenerator;
        this.paymentService = paymentService;
    }

    /**
     * Use the link that was supplied, or create one.
     *
     * The room is made at assignment time, so by the time either person opens
     * their dashboard the Join button already works.
     */
    private String resolveMeetingLink(String supplied, InterviewRequest request) {
        if (supplied != null && !supplied.isBlank()) {
            return supplied.trim();
        }
        return meetingLinkGenerator.generateFor(request);
    }

    // ---------- student side ----------

    @Transactional
    public InterviewRequestDto create(CreateRequestDto dto, User student) {
        // Never trust the slot the browser sent - re-check it here. The grid the
        // candidate saw may be stale, or the request may not have come from our
        // frontend at all.
        slotService.assertBookable(dto.preferredSlot());

        InterviewRequest request = new InterviewRequest(
                student,
                dto.topic(),
                dto.experienceLevel(),
                dto.preferredSlot(),
                dto.notes()
        );
        InterviewRequest saved = requestRepository.save(request);

        // The booking and its payment are created together, in one transaction,
        // so a request can never exist with nothing to pay against.
        paymentService.createFor(saved);

        return InterviewRequestDto.from(saved);
    }

    /** No email parameter any more - you can only ever see your own. */
    public List<InterviewRequestDto> findMyRequests(User student) {
        return requestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    // ---------- mentor side ----------

    public List<InterviewRequestDto> findPending(User mentor) {
        // An unverified mentor must not even see the queue.
        mentorProfileService.assertApproved(mentor);
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    public List<InterviewRequestDto> findMyInterviews(User mentor) {
        mentorProfileService.assertApproved(mentor);
        return requestRepository.findByMentorIdOrderByScheduledAtAsc(mentor.getId()).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

    @Transactional
    public InterviewRequestDto accept(Long requestId, AcceptRequestDto dto, User mentor) {
        mentorProfileService.assertApproved(mentor);
        InterviewRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Request " + requestId + " is already " + request.getStatus() + ", it cannot be accepted again");
        }

        request.assignTo(mentor, dto.scheduledAt(), resolveMeetingLink(dto.meetingLink(), request));
        return InterviewRequestDto.from(request); // dirty checking flushes on commit
    }

    @Transactional
    public InterviewRequestDto complete(Long requestId, CompleteRequestDto dto, User mentor) {
        mentorProfileService.assertApproved(mentor);
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

    /**
     * An admin attaches a mentor to a student's request.
     *
     * This is the second way a request gets scheduled. A mentor can pick one up
     * themselves from the queue, or an admin can hand it to a specific person -
     * useful when a request needs someone particular, or has been sitting
     * unclaimed.
     */
    @Transactional
    public InterviewRequestDto assignMentor(Long requestId, AssignMentorRequest dto, User admin) {
        InterviewRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Request " + requestId + " is already " + request.getStatus()
                            + ", so it cannot be assigned.");
        }

        User mentor = userRepository.findById(dto.mentorId())
                .orElseThrow(() -> new NotFoundException("No user with id " + dto.mentorId()));

        if (mentor.getRole() != Role.MENTOR) {
            throw new BadRequestException(mentor.getFullName() + " is not a mentor.");
        }
        if (!mentor.isActive()) {
            throw new BadRequestException(mentor.getFullName() + "'s account is deactivated.");
        }
        // Assigning an unverified mentor would sidestep the whole verification
        // step, so the admin cannot do it either.
        if (!mentorProfileService.isApproved(mentor)) {
            throw new BadRequestException(
                    mentor.getFullName() + " is not verified yet. Approve their profile first.");
        }

        // Default to the slot the student actually asked for.
        LocalDateTime when = dto.scheduledAt() != null ? dto.scheduledAt() : request.getPreferredSlot();

        request.assignTo(mentor, when, resolveMeetingLink(dto.meetingLink(), request));
        return InterviewRequestDto.from(request);
    }

    /** Requests still waiting for a mentor - the admin's assignment queue. */
    public List<InterviewRequestDto> findPendingForAdmin() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream()
                .map(InterviewRequestDto::from)
                .toList();
    }

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
