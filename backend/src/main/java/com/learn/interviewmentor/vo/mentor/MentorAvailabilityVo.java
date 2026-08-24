package com.learn.interviewmentor.vo.mentor;

import com.learn.interviewmentor.model.AvailabilityStatus;
import com.learn.interviewmentor.model.MentorAvailability;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** One hour a mentor declared, as they and the admin see it. */
@Schema(description = "An hour a mentor said they were free.")
public record MentorAvailabilityVo(

        Long id,

        @Schema(example = "2026-09-22T15:00:00")
        LocalDateTime slotStart,

        @Schema(example = "2026-09-22T16:00:00")
        LocalDateTime slotEnd,

        @Schema(description = "Short label", example = "3:00 PM")
        String label,

        boolean forInterviews,
        boolean forMentoring,

        @Schema(description = "OPEN, BOOKED or WITHDRAWN")
        AvailabilityStatus status,

        String note,

        // ---- who, for the admin's reference view ----

        @Schema(example = "Ananya Rao")
        String mentorName,

        @Schema(example = "ananya@example.com")
        String mentorEmail,

        Long mentorId,

        // ---- what took the hour, when something has ----

        @Schema(description = "The booking an admin mapped onto this hour, if any")
        Long bookedRequestId,

        @Schema(description = "Who that booking belongs to", example = "Rahul Sharma")
        String bookedFor,

        @Schema(description = "What they booked", example = "Spring Boot backend round")
        String bookedTopic,

        LocalDateTime createdAt
) {

    public static MentorAvailabilityVo from(MentorAvailability a) {
        var booked = a.getBookedRequest();
        return new MentorAvailabilityVo(
                a.getId(),
                a.getSlotStart(),
                a.getSlotEnd(),
                // Formatted here so a mentor's list and the admin's grid cannot
                // print the same hour two different ways.
                a.getSlotStart().format(com.learn.interviewmentor.service.SlotService.LABEL),
                a.isForInterviews(),
                a.isForMentoring(),
                a.getStatus(),
                a.getNote(),
                a.getMentor().getFullName(),
                a.getMentor().getEmail(),
                a.getMentor().getId(),
                booked == null ? null : booked.getId(),
                booked == null ? null : booked.getStudent().getFullName(),
                booked == null ? null : booked.getTopic(),
                a.getCreatedAt()
        );
    }
}
