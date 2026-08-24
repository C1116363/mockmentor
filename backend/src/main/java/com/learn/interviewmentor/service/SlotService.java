package com.learn.interviewmentor.service;

import com.learn.interviewmentor.service.InterviewRequestService;
import com.learn.interviewmentor.vo.SlotVo;
import com.learn.interviewmentor.exception.BadRequestException;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorProfileRepository;
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
public interface SlotService {

    /** The grid the candidate sees for one day. */
    List<SlotVo> slotsFor(LocalDate date);

    /**
    * Re-validates a slot at booking time. Called by InterviewRequestService, so
    * a hand-crafted request cannot book 3am or a slot that filled up while the
    * candidate was deciding.
    */
    void assertBookable(LocalDateTime slot);
}
