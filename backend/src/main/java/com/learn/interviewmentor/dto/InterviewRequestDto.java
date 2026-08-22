package com.learn.interviewmentor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.learn.interviewmentor.dto.auth.UserDto;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** What we send back to the browser for an interview request. */
@Schema(description = "A mock-interview request in whatever state it is currently in.")
public record InterviewRequestDto(
        Long id,
        UserDto student,
        String topic,
        String experienceLevel,
        LocalDate preferredDate,
        String notes,
        @Schema(description = "PENDING -> SCHEDULED -> COMPLETED, or CANCELLED", example = "PENDING")
        RequestStatus status,
        @Schema(description = "null until a mentor accepts it")
        MentorDto mentor,
        LocalDateTime scheduledAt,
        @Schema(description = "Set when a mentor accepts", example = "https://meet.google.com/abc-defg-hij")
        String meetingLink,
        @Schema(description = "Written by the mentor once COMPLETED")
        String feedback,
        LocalDateTime createdAt
) {
    public static InterviewRequestDto from(InterviewRequest request) {
        return new InterviewRequestDto(
                request.getId(),
                UserDto.from(request.getStudent()),
                request.getTopic(),
                request.getExperienceLevel(),
                request.getPreferredDate(),
                request.getNotes(),
                request.getStatus(),
                request.getMentor() == null ? null : MentorDto.fromUserOnly(request.getMentor()),
                request.getScheduledAt(),
                request.getMeetingLink(),
                request.getFeedback(),
                request.getCreatedAt()
        );
    }
}
