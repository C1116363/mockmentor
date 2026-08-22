package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.SlotDto;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Works out which one-hour slots a candidate can book on a given day.
 *
 * The rules live here rather than in the browser, because the frontend can be
 * bypassed. Whatever the slot grid shows, createRequest() re-checks the same
 * rules server-side before saving anything.
 */
@Service
@Transactional(readOnly = true)
public class SlotService {

    /** Bookable window: 09:00 up to (but not including) 21:00. */
    public static final LocalTime DAY_START = LocalTime.of(9, 0);
    public static final LocalTime DAY_END = LocalTime.of(21, 0);

    /** How far ahead someone can book. */
    public static final int MAX_DAYS_AHEAD = 30;

    private static final DateTimeFormatter LABEL =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    /** Statuses that still occupy a slot. Cancelled ones free it up again. */
    private static final List<RequestStatus> ACTIVE =
            List.of(RequestStatus.PENDING, RequestStatus.SCHEDULED, RequestStatus.COMPLETED);

    private final InterviewRequestRepository requestRepository;
    private final UserRepository userRepository;

    public SlotService(InterviewRequestRepository requestRepository, UserRepository userRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    /** The grid the candidate sees for one day. */
    public List<SlotDto> slotsFor(LocalDate date) {
        if (date == null) {
            throw new BadRequestException("Pick a date first");
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new BadRequestException("That date has already passed");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new BadRequestException("You can only book up to " + MAX_DAYS_AHEAD + " days ahead");
        }

        // How many interviews can run at once = how many mentors exist.
        long capacity = Math.max(userRepository.countByRole(Role.MENTOR), 1);
        LocalDateTime now = LocalDateTime.now();

        List<SlotDto> slots = new ArrayList<>();
        for (LocalTime t = DAY_START; t.isBefore(DAY_END); t = t.plusMinutes(InterviewRequest.SLOT_MINUTES)) {
            LocalDateTime start = LocalDateTime.of(date, t);
            LocalDateTime end = start.plusMinutes(InterviewRequest.SLOT_MINUTES);

            String reason = null;
            if (!start.isAfter(now)) {
                reason = "Already passed";
            } else if (requestRepository.countByPreferredSlotAndStatusIn(start, ACTIVE) >= capacity) {
                reason = "Fully booked";
            }

            slots.add(new SlotDto(start, end, start.format(LABEL), reason == null, reason));
        }
        return slots;
    }

    /**
     * Re-validates a slot at booking time. Called by InterviewRequestService, so
     * a hand-crafted request cannot book 3am or a slot that filled up while the
     * candidate was deciding.
     */
    public void assertBookable(LocalDateTime slot) {
        if (slot == null) {
            throw new BadRequestException("Pick a slot");
        }
        if (slot.getMinute() != 0 || slot.getSecond() != 0 || slot.getNano() != 0) {
            throw new BadRequestException("Slots start on the hour");
        }
        if (!slot.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("That slot has already passed");
        }
        if (slot.toLocalDate().isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new BadRequestException("You can only book up to " + MAX_DAYS_AHEAD + " days ahead");
        }

        LocalTime time = slot.toLocalTime();
        if (time.isBefore(DAY_START) || !time.isBefore(DAY_END)) {
            throw new BadRequestException(
                    "Interviews run between " + DAY_START.format(LABEL) + " and " + DAY_END.format(LABEL));
        }

        long capacity = Math.max(userRepository.countByRole(Role.MENTOR), 1);
        if (requestRepository.countByPreferredSlotAndStatusIn(slot, ACTIVE) >= capacity) {
            throw new BadRequestException("That slot just filled up. Please pick another one.");
        }
    }
}
