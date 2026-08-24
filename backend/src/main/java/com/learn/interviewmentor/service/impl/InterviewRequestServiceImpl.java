package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.service.MentorProfileService;
import com.learn.interviewmentor.service.PaymentService;
import com.learn.interviewmentor.service.AvailabilityService;
import com.learn.interviewmentor.service.SlotService;

import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.AssignMentorRequestDto;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.meeting.MeetingLinkGenerator;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InterviewRequestServiceImpl implements InterviewRequestService {

    private static final Logger log =
            LoggerFactory.getLogger(InterviewRequestServiceImpl.class);

    private final InterviewRequestRepository requestRepository;
    private final SlotService slotService;
    private final AvailabilityService availabilityService;
    private final MentorProfileService mentorProfileService;
    private final UserRepository userRepository;
    private final MeetingLinkGenerator meetingLinkGenerator;
    private final PaymentService paymentService;

    public InterviewRequestServiceImpl(InterviewRequestRepository requestRepository,
                                   SlotService slotService,
                                   MentorProfileService mentorProfileService,
                                   UserRepository userRepository,
                                   MeetingLinkGenerator meetingLinkGenerator,
                                   PaymentService paymentService,
                                      AvailabilityService availabilityService) {
        this.requestRepository = requestRepository;
        this.slotService = slotService;
        this.availabilityService = availabilityService;
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
    @Override
    public InterviewRequestVo create(CreateRequestDto dto, User student) {
        // Never trust the slot the browser sent - re-check it here. The grid the
        // candidate saw may be stale, or the request may not have come from our
        // frontend at all.
        slotService.assertBookable(dto.preferredSlot(), dto.sessionTypeOrDefault());

        InterviewRequest request = new InterviewRequest(
                student,
                dto.sessionTypeOrDefault(),
                dto.topic(),
                dto.experienceLevel(),
                dto.preferredSlot(),
                dto.notes()
        );
        InterviewRequest saved = requestRepository.save(request);

        // The booking and its payment are created together, in one transaction,
        // so a request can never exist with nothing to pay against.
        paymentService.createFor(saved);

        return InterviewRequestVo.from(saved);
    }

    /** No email parameter any more - you can only ever see your own. */
    @Override
    public List<InterviewRequestVo> findMyRequests(User student) {
        return requestRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .map(InterviewRequestVo::from)
                .toList();
    }

    // ---------- mentor side ----------

    @Override
    public List<InterviewRequestVo> findPending(User mentor) {
        // An unverified mentor must not even see the queue.
        mentorProfileService.assertApproved(mentor);
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream()
                .map(InterviewRequestVo::from)
                .toList();
    }

    @Override
    public List<InterviewRequestVo> findMyInterviews(User mentor) {
        mentorProfileService.assertApproved(mentor);
        return requestRepository.findByMentorIdOrderByScheduledAtAsc(mentor.getId()).stream()
                .map(InterviewRequestVo::from)
                .toList();
    }

    @Transactional
    @Override
    public InterviewRequestVo accept(Long requestId, AcceptRequestDto dto, User mentor) {
        mentorProfileService.assertApproved(mentor);
        InterviewRequest request = getRequestOrThrow(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException(
                    "Request " + requestId + " is already " + request.getStatus() + ", it cannot be accepted again");
        }

        // A mentor may only take an hour they declared. Without this the
        // availability grid would be advisory only: a mentor could grab any slot
        // from the open queue, and the hours students were shown would stop
        // meaning anything.
        boolean claimed = availabilityService.claimFor(
                mentor, request.getPreferredSlot(), request);
        if (!claimed) {
            throw new ConflictException(
                    "You haven't declared availability for "
                            + request.getPreferredSlot().format(SlotService.FULL_LABEL)
                            + ", or somebody already took that hour. Add it under "
                            + "My availability first.");
        }

        request.assignTo(mentor, dto.scheduledAt(), resolveMeetingLink(dto.meetingLink(), request));
        return InterviewRequestVo.from(request); // dirty checking flushes on commit
    }

    @Transactional
    @Override
    public InterviewRequestVo complete(Long requestId, CompleteRequestDto dto, User mentor) {
        mentorProfileService.assertApproved(mentor);
        InterviewRequest request = getRequestOrThrow(requestId);

        // Being a MENTOR is not enough - it has to be YOUR interview.
        if (!request.isMentoredBy(mentor)) {
            throw new ForbiddenException("This session was accepted by another mentor");
        }
        if (request.getStatus() != RequestStatus.SCHEDULED) {
            throw new BadRequestException("Only a SCHEDULED session can be completed");
        }

        /*
         * The rule bean validation cannot express.
         *
         * A mock interview must end in a rating and a verdict - that is the
         * product. A mentoring session must not: scoring somebody out of 5 for
         * asking good questions is nonsense. Because @NotNull on the DTO would
         * apply to both, it was removed from those two fields, which means this
         * check is now the ONLY thing keeping interview scorecards mandatory.
         * Delete it and interviews silently start completing with no score.
         */
        if (request.getSessionType().isScored()) {
            if (dto.overallRating() == null) {
                throw new BadRequestException("Give an overall rating out of 5");
            }
            if (dto.recommendation() == null) {
                throw new BadRequestException("Pick a verdict: READY, ALMOST_READY or NEEDS_WORK");
            }
        }

        // Anything scored that arrives for an unscored session is discarded
        // inside complete() rather than trusted - see InterviewRequest.
        request.complete(
                dto.feedback(),
                dto.strengths(),
                dto.improvements(),
                dto.overallRating(),
                dto.technicalRating(),
                dto.communicationRating(),
                dto.problemSolvingRating(),
                dto.recommendation());
        return InterviewRequestVo.from(request);
    }

    /** The student who raised it, the mentor who took it, or an admin. */
    @Transactional
    @Override
    public InterviewRequestVo cancel(Long requestId, User actor) {
        InterviewRequest request = getRequestOrThrow(requestId);

        boolean allowed = actor.getRole() == Role.ADMIN
                || request.isOwnedBy(actor)
                || request.isMentoredBy(actor);

        if (!allowed) {
            throw new ForbiddenException("You cannot cancel this request");
        }
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BadRequestException("A completed session cannot be cancelled");
        }

        request.cancel();
        // The mentor is free again, so their hour goes back on the market. Without
        // this a cancelled session would leave that slot dark to every student for
        // no reason.
        availabilityService.releaseFor(request);
        return InterviewRequestVo.from(request);
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
    @Override
    public InterviewRequestVo assignMentor(Long requestId, AssignMentorRequestDto dto, User admin) {
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

        // Claim the mentor's declared hour. If they never declared it, the admin
        // has to say so explicitly - the whole point of availability is that the
        // person being assigned agreed to the time, and an assignment that skips
        // that quietly is how somebody finds an interview on their calendar they
        // never said yes to.
        boolean claimed = availabilityService.claimFor(mentor, when, request);
        if (!claimed && !dto.override()) {
            throw new ConflictException(
                    mentor.getFullName() + " has not offered "
                            + when.format(SlotService.FULL_LABEL)
                            + ". Pick a mentor who has, or resend with override=true to assign "
                            + "them anyway - check with them first.");
        }
        if (!claimed) {
            log.warn("Admin {} assigned {} to {} without declared availability (override)",
                    admin.getEmail(), mentor.getEmail(), when);
        }

        request.assignTo(mentor, when, resolveMeetingLink(dto.meetingLink(), request));
        return InterviewRequestVo.from(request);
    }

    /** Requests still waiting for a mentor - the admin's assignment queue. */
    @Override
    public List<InterviewRequestVo> findPendingForAdmin() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING).stream()
                .map(InterviewRequestVo::from)
                .toList();
    }

    @Override
    public InterviewRequestVo findOne(Long requestId) {
        return InterviewRequestVo.from(getRequestOrThrow(requestId));
    }

    @Override
    public List<InterviewRequestVo> findAll() {
        return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InterviewRequestVo::from)
                .toList();
    }

    @Override
    public long countByStatus(RequestStatus status) {
        return requestRepository.countByStatus(status);
    }

    @Override
    public long countAll() {
        return requestRepository.count();
    }

    private InterviewRequest getRequestOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Interview request not found with id " + id));
    }
}
