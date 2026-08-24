package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.AcceptRequestDto;
import com.learn.interviewmentor.dto.CompleteRequestDto;
import com.learn.interviewmentor.dto.CreateRequestDto;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.model.InterviewRequest;
import com.learn.interviewmentor.vo.InterviewRequestVo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Booked sessions - mock interviews and mentoring discussions alike.
 *
 * Named for what it handles rather than for the entity: {@code InterviewRequest}
 * has covered both kinds since SessionType was added, and a facade called
 * InterviewFacade would keep suggesting otherwise to whoever reads it next.
 */
public interface SessionFacade {

    // ---- student ----
    ApiResult<InterviewRequestVo> book(CreateRequestDto request, User student);

    ApiResult<List<InterviewRequestVo>> mine(User student);

    ApiResult<InterviewRequestVo> cancel(Long id, User actor);

    // ---- mentor ----
    ApiResult<List<InterviewRequestVo>> openQueue(User mentor);

    ApiResult<List<InterviewRequestVo>> assignedTo(User mentor);

    ApiResult<InterviewRequestVo> accept(Long id, AcceptRequestDto request, User mentor);

    ApiResult<InterviewRequestVo> complete(Long id, CompleteRequestDto request, User mentor);

    // ---- the candidate's CV ----

    ApiResult<InterviewRequestVo> attachCv(Long id, MultipartFile cv, User student);

    /**
     * The booking, checked. Returns the entity rather than a VO because the
     * controller needs the content type and download name to build the headers,
     * and those are properties of the stored file, not of a response body.
     */
    InterviewRequest cvFor(Long id, User caller);

    Path cvPath(InterviewRequest request);
}
