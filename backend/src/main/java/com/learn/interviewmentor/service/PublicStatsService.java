package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.PublicMentorVo;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import com.learn.interviewmentor.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts for the public website. Deliberately only aggregate numbers -
 * never a list of people.
 */
public interface PublicStatsService {

    /**
    * Mentors for the public site, most experienced first.
    * Capped so the marketing page can never accidentally dump the whole table.
    */
    List<PublicMentorVo> featuredMentors(int limit);

    Map<String, Long> counts();
}
