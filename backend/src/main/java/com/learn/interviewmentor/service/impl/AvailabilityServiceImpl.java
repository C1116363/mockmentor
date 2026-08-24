package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.dto.mentor.AvailabilityRequestDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.ForbiddenException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.AvailabilityStatus;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.MentorAvailability;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.repository.MentorAvailabilityRepository;
import com.learn.interviewmentor.service.AvailabilityService;
import com.learn.interviewmentor.service.MentorProfileService;
import com.learn.interviewmentor.service.SlotService;
import com.learn.interviewmentor.vo.mentor.MentorAvailabilityVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mentors declaring the hours they are free, and the admin reading them.
 *
 * <h2>The rule this whole class exists for</h2>
 * A slot is offered to students only because a real, verified mentor said they
 * were free for that kind of session. Before this, the grid was generated
 * 09:00-21:00 with capacity = "how many verified mentors exist" - so the app
 * would cheerfully sell 7 AM on a Sunday to a student and leave an admin to find
 * somebody. Now availability is the source, not an assumption.
 *
 * <h2>Both sides need a day's notice</h2>
 * A mentor must declare an hour at least {@link SlotService#MIN_LEAD_HOURS} ahead,
 * and a student must book that far ahead too. Same constant for both, because it
 * is the same requirement seen from two ends: nobody should be arranging an
 * interview for tonight.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityServiceImpl.class);

    private final MentorAvailabilityRepository availabilityRepository;
    private final MentorProfileService profileService;

    public AvailabilityServiceImpl(MentorAvailabilityRepository availabilityRepository,
                                   MentorProfileService profileService) {
        this.availabilityRepository = availabilityRepository;
        this.profileService = profileService;
    }

    // ---------------- mentor ----------------

    /**
     * Declare a day's hours.
     *
     * Partial success is the point: a mentor ticking Tuesday afternoon should not
     * have the whole request rejected because one of those hours is already
     * declared or now too close. The accepted hours are saved and
     * {@link DeclareResult#skipped} says what was not, and why.
     */
    @Override
    @Transactional
    public DeclareResult declare(AvailabilityRequestDto dto, User mentor) {
        // An unverified mentor's availability must never reach the slot grid, so
        // it is refused at the point of declaring rather than filtered later.
        profileService.assertApproved(mentor);

        if (!dto.forInterviews() && !dto.forMentoring()) {
            throw new BadRequestException(
                    "Pick at least one kind of session - an hour you will take neither "
                            + "interviews nor mentoring in is not availability.");
        }

        LocalDateTime earliest = SlotService.earliestBookableSlot();
        LocalDate latest = LocalDate.now().plusDays(SlotService.MAX_DAYS_AHEAD);

        if (dto.date().isAfter(latest)) {
            throw new BadRequestException(
                    "You can only declare availability up to " + SlotService.MAX_DAYS_AHEAD
                            + " days ahead");
        }

        List<MentorAvailabilityVo> saved = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (Integer hour : dto.hours().stream().distinct().sorted().toList()) {
            if (hour == null || hour < 0 || hour > 23) {
                skipped.add(hour + ":00 - not a real hour");
                continue;
            }

            LocalDateTime slot = LocalDateTime.of(dto.date(), LocalTime.of(hour, 0));
            String label = slot.format(SlotService.LABEL);

            if (slot.isBefore(earliest)) {
                skipped.add(label + " - less than " + SlotService.MIN_LEAD_HOURS
                        + " hours away, so a student could not book it in time");
                continue;
            }
            if (!SlotService.isWithinWorkingHours(slot)) {
                skipped.add(label + " - outside " + SlotService.workingHoursLabel());
                continue;
            }

            Optional<MentorAvailability> existing =
                    availabilityRepository.findByMentorIdAndSlotStart(mentor.getId(), slot);

            if (existing.isPresent()) {
                MentorAvailability row = existing.get();
                if (row.getStatus() == AvailabilityStatus.BOOKED) {
                    skipped.add(label + " - already booked, so it cannot be changed");
                    continue;
                }
                // Re-declaring a withdrawn hour, or changing what you will take in
                // one you already offered. Both are updates, not duplicates - the
                // unique constraint on (mentor, slot) means inserting would fail.
                row.updateOffering(dto.forInterviews(), dto.forMentoring(), trimOrNull(dto.note()));
                if (row.getStatus() == AvailabilityStatus.WITHDRAWN) {
                    row.release();
                }
                saved.add(MentorAvailabilityVo.from(row));
                continue;
            }

            MentorAvailability row = availabilityRepository.save(new MentorAvailability(
                    mentor, slot, dto.forInterviews(), dto.forMentoring(), trimOrNull(dto.note())));
            saved.add(MentorAvailabilityVo.from(row));
        }

        log.info("{} declared {} hour(s) on {} (interviews={}, mentoring={}); {} skipped",
                mentor.getEmail(), saved.size(), dto.date(),
                dto.forInterviews(), dto.forMentoring(), skipped.size());

        return new DeclareResult(saved, skipped);
    }

    @Override
    public List<MentorAvailabilityVo> mine(User mentor) {
        // From now, not from the start of today: hours that have already gone are
        // history, and a mentor's own list is a to-do, not a record.
        return availabilityRepository.findMineFrom(mentor.getId(), LocalDateTime.now())
                .stream().map(MentorAvailabilityVo::from).toList();
    }

    /** Take an hour back. Only while nobody has been mapped onto it. */
    @Override
    @Transactional
    public MentorAvailabilityVo withdraw(Long id, User mentor) {
        MentorAvailability row = getOrThrow(id);

        if (!row.isOwnedBy(mentor)) {
            throw new ForbiddenException("That isn't your availability");
        }
        if (row.getStatus() == AvailabilityStatus.BOOKED) {
            throw new ConflictException(
                    "A student is already booked into this hour. Ask an admin to move it - "
                            + "withdrawing here would leave them with a session and no interviewer.");
        }
        if (row.getSlotStart().isBefore(SlotService.earliestBookableSlot())) {
            // Past the lead time an admin needs to arrange it. Letting a mentor
            // pull out inside that window is how a booking loses its interviewer
            // with nobody left to reassign it to.
            throw new ConflictException(
                    "This hour is less than " + SlotService.MIN_LEAD_HOURS
                            + " hours away. Contact an admin rather than withdrawing it here.");
        }

        row.withdraw();
        log.info("{} withdrew availability at {}", mentor.getEmail(), row.getSlotStart());
        return MentorAvailabilityVo.from(row);
    }

    // ---------------- admin ----------------

    /** Every mentor's declared hours in a window. The admin's reference view. */
    @Override
    public List<MentorAvailabilityVo> allInWindow(LocalDate from, int days) {
        LocalDate start = from == null ? LocalDate.now() : from;
        int span = days <= 0 ? 7 : Math.min(days, SlotService.MAX_DAYS_AHEAD);
        return availabilityRepository.findAllInWindow(
                        start.atStartOfDay(), start.plusDays(span).atStartOfDay())
                .stream().map(MentorAvailabilityVo::from).toList();
    }

    /**
     * Mentors free for one exact hour and session type.
     *
     * This is what the admin's assign dropdown reads, so they are choosing among
     * people who said yes to that hour rather than every verified mentor.
     */
    @Override
    public List<MentorAvailabilityVo> openForSlot(LocalDateTime slot, SessionType sessionType) {
        return availabilityRepository
                .findOpenForSlot(slot, sessionType != SessionType.MENTORING)
                .stream().map(MentorAvailabilityVo::from).toList();
    }

    /**
     * Claim a mentor's hour for a booking.
     *
     * @return true if a matching open hour was found and consumed. False means the
     *         mentor never declared this hour - the caller decides whether that is
     *         allowed, because an admin sometimes needs to override.
     */
    @Override
    @Transactional
    public boolean claimFor(User mentor, LocalDateTime slot, InterviewRequest request) {
        Optional<MentorAvailability> row =
                availabilityRepository.findByMentorIdAndSlotStart(mentor.getId(), slot);

        if (row.isEmpty() || !row.get().isOpen()) {
            return false;
        }
        row.get().bookFor(request);
        return true;
    }

    /**
     * Give the hour back when a booking is cancelled.
     *
     * Without this a cancelled interview would leave the mentor's hour marked
     * BOOKED forever - they are free, and the slot would stay dark to every
     * student for no reason.
     */
    @Override
    @Transactional
    public void releaseFor(InterviewRequest request) {
        availabilityRepository.findByBookedRequestId(request.getId()).ifPresent(row -> {
            row.release();
            log.info("Released {}'s availability at {} after booking {} was cancelled",
                    row.getMentor().getEmail(), row.getSlotStart(), request.getId());
        });
    }

    @Override
    public long countOpen() {
        return availabilityRepository.countByStatus(AvailabilityStatus.OPEN);
    }

    private MentorAvailability getOrThrow(Long id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No availability with id " + id));
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
