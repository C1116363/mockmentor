package com.learn.interviewmentor.service;

import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.vo.SlotVo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Which one-hour slots a student can book, and the rules around them.
 *
 * <b>A slot exists because a verified mentor declared that hour</b> for that kind
 * of session - see {@link AvailabilityService}. Nothing here invents availability.
 *
 * The constants live on the interface rather than the implementation because both
 * halves of the booking rule read them: this service checks a student is booking
 * far enough ahead, and AvailabilityService checks a mentor is declaring far
 * enough ahead. Same numbers, one home.
 */
public interface SlotService {

    /** Bookable window: 09:00 up to (but not including) 21:00. */
    LocalTime DAY_START = LocalTime.of(9, 0);
    LocalTime DAY_END = LocalTime.of(21, 0);

    /** How far ahead anyone can declare or book. */
    int MAX_DAYS_AHEAD = 30;

    /**
     * A day's notice, both ways.
     *
     * A mentor declares an hour at least this far ahead; a student books at least
     * this far ahead. It is one requirement seen from two ends - an admin needs a
     * day in hand to map an interviewer onto a booking, and nobody should be
     * arranging an interview for tonight.
     */
    int MIN_LEAD_HOURS = 24;

    DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    DateTimeFormatter FULL_LABEL =
            DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);

    /**
     * The earliest slot anyone can declare or book right now.
     *
     * Rounded up to the next whole hour, because slots start on the hour: at
     * 14:30 the raw cutoff is tomorrow 14:30, which no slot matches, and
     * comparing against it would silently make tomorrow 14:00 look bookable.
     */
    static LocalDateTime earliestBookableSlot() {
        LocalDateTime raw = LocalDateTime.now().plusHours(MIN_LEAD_HOURS);
        return raw.getMinute() == 0 && raw.getSecond() == 0
                ? raw.withNano(0)
                : raw.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    static boolean isWithinWorkingHours(LocalDateTime slot) {
        LocalTime time = slot.toLocalTime();
        return !time.isBefore(DAY_START) && time.isBefore(DAY_END);
    }

    static String workingHoursLabel() {
        return DAY_START.format(LABEL) + " and " + DAY_END.format(LABEL);
    }

    /**
     * The grid for one day and one kind of session.
     *
     * Only hours a mentor actually offered appear. An empty list means nobody put
     * their hand up for that day - a truthful answer, and more useful than a row
     * of greyed-out buttons.
     */
    List<SlotVo> slotsFor(LocalDate date, SessionType sessionType);

    /**
     * Re-validates a slot inside the booking transaction.
     *
     * The grid is advisory - the browser can be bypassed, and the last matching
     * mentor can be claimed while a student is deciding.
     */
    void assertBookable(LocalDateTime slot, SessionType sessionType);
}
