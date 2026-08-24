package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.MentorAvailability;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorAvailabilityRepository;
import com.learn.interviewmentor.service.SlotService;
import com.learn.interviewmentor.vo.SlotVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Which one-hour slots a student can actually book.
 *
 * <h2>What changed, and why</h2>
 * This used to generate a 09:00-21:00 grid every day and set capacity to "how
 * many verified mentors exist". That sold slots nobody had agreed to: 7 AM on a
 * Sunday looked bookable because five mentors existed somewhere, and an admin was
 * left to find someone afterwards.
 *
 * Now <b>a slot exists because a verified mentor declared that hour</b> for that
 * kind of session. The grid is the union of real commitments, so a student picking
 * a time is picking somebody's actual offer.
 *
 * <h2>The two lead-time rules are the same rule</h2>
 * A mentor declares at least {@link SlotService#MIN_LEAD_HOURS} ahead; a student
 * books at least that far ahead. One constant, because it is one requirement seen
 * from both ends - there has to be a day in hand for an admin to map an
 * interviewer to a booking.
 *
 * <h2>Still re-checked on save</h2>
 * The grid is advisory. {@link #assertBookable} runs the same rules again inside
 * the booking transaction, because the browser can be bypassed and because the
 * last matching mentor can be claimed while a student is deciding.
 */
@Service
@Transactional(readOnly = true)
public class SlotServiceImpl implements SlotService {

    /** Statuses that still occupy a slot. Cancelled ones free it up again. */
    private static final List<RequestStatus> ACTIVE = List.of(
            // An unpaid booking still holds its slot. Without this a student
            // could pay and then find somebody else had taken the time.
            RequestStatus.AWAITING_PAYMENT,
            RequestStatus.PENDING,
            RequestStatus.SCHEDULED,
            RequestStatus.COMPLETED);

    private final InterviewRequestRepository requestRepository;
    private final MentorAvailabilityRepository availabilityRepository;

    public SlotServiceImpl(InterviewRequestRepository requestRepository,
                           MentorAvailabilityRepository availabilityRepository) {
        this.requestRepository = requestRepository;
        this.availabilityRepository = availabilityRepository;
    }

    /**
     * The grid for one day, for one kind of session.
     *
     * Only hours a mentor offered appear at all - an empty list means nobody has
     * put their hand up for that day, which is a truthful answer and a much more
     * useful one than twelve greyed-out buttons.
     */
    @Override
    public List<SlotVo> slotsFor(LocalDate date, SessionType sessionType) {
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

        SessionType type = sessionType == null ? SessionType.MOCK_INTERVIEW : sessionType;
        LocalDateTime earliest = SlotService.earliestBookableSlot();

        // One query for the whole day, grouped by hour - rather than a query per
        // hour, which was 12 round trips for one grid.
        Map<LocalDateTime, List<MentorAvailability>> offered = availabilityRepository
                .findOpenOnDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                        type != SessionType.MENTORING)
                .stream()
                .collect(Collectors.groupingBy(MentorAvailability::getSlotStart));

        List<SlotVo> slots = new ArrayList<>();
        for (LocalDateTime start : offered.keySet().stream().sorted().toList()) {
            int mentorsFree = offered.get(start).size();
            long alreadyBooked = requestRepository.countByPreferredSlotAndStatusIn(start, ACTIVE);
            long remaining = mentorsFree - alreadyBooked;

            String reason = null;
            if (start.isBefore(earliest)) {
                // Named precisely rather than "already passed" - a slot tomorrow
                // morning has not passed, it is just inside the notice period, and
                // saying so is the difference between "try later" and "try sooner".
                reason = "Needs " + MIN_LEAD_HOURS + " hours' notice";
            } else if (remaining <= 0) {
                reason = "Fully booked";
            }

            slots.add(new SlotVo(
                    start,
                    start.plusMinutes(InterviewRequest.SLOT_MINUTES),
                    start.format(LABEL),
                    reason == null,
                    reason,
                    Math.max(remaining, 0),
                    mentorsFree));
        }
        return slots;
    }

    /**
     * Re-validates a slot inside the booking transaction.
     *
     * Everything the grid checked, checked again - a hand-crafted request must not
     * be able to book 3 AM, an hour nobody offered, or one that filled up while
     * the student was deciding.
     */
    @Override
    public void assertBookable(LocalDateTime slot, SessionType sessionType) {
        if (slot == null) {
            throw new BadRequestException("Pick a slot");
        }
        if (slot.getMinute() != 0 || slot.getSecond() != 0 || slot.getNano() != 0) {
            throw new BadRequestException("Slots start on the hour");
        }
        if (slot.toLocalDate().isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new BadRequestException("You can only book up to " + MAX_DAYS_AHEAD + " days ahead");
        }
        if (slot.isBefore(SlotService.earliestBookableSlot())) {
            throw new BadRequestException(
                    "Sessions need at least " + MIN_LEAD_HOURS + " hours' notice, so an admin can "
                            + "arrange an interviewer. The earliest you can book is "
                            + SlotService.earliestBookableSlot().format(FULL_LABEL) + ".");
        }
        if (!SlotService.isWithinWorkingHours(slot)) {
            throw new BadRequestException("Sessions run between " + SlotService.workingHoursLabel());
        }

        SessionType type = sessionType == null ? SessionType.MOCK_INTERVIEW : sessionType;
        int mentorsFree = availabilityRepository
                .findOpenForSlot(slot, type != SessionType.MENTORING).size();

        if (mentorsFree == 0) {
            // 409, not 400: nothing about the request is malformed. Either nobody
            // ever offered this hour, or the last mentor who did was just claimed.
            throw new ConflictException(
                    "No mentor is available for that slot any more. Pick another one - the "
                            + "list only shows hours a mentor has offered.");
        }

        long alreadyBooked = requestRepository.countByPreferredSlotAndStatusIn(slot, ACTIVE);
        if (alreadyBooked >= mentorsFree) {
            throw new ConflictException("That slot just filled up. Please pick another one.");
        }
    }
}
