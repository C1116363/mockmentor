package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.PublicMentorDto;
import com.learn.interviewmentor.model.RequestStatus;
import com.learn.interviewmentor.model.VerificationStatus;
import com.learn.interviewmentor.model.Role;
import com.learn.interviewmentor.repository.InterviewRequestRepository;
import com.learn.interviewmentor.repository.MentorProfileRepository;
import com.learn.interviewmentor.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts for the public website. Deliberately only aggregate numbers -
 * never a list of people.
 */
@Service
@Transactional(readOnly = true)
public class PublicStatsService {

    private final UserRepository userRepository;
    private final InterviewRequestRepository requestRepository;
    private final MentorProfileRepository mentorProfileRepository;

    public PublicStatsService(UserRepository userRepository,
                              InterviewRequestRepository requestRepository,
                              MentorProfileRepository mentorProfileRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.mentorProfileRepository = mentorProfileRepository;
    }

    /**
     * Mentors for the public site, most experienced first.
     * Capped so the marketing page can never accidentally dump the whole table.
     */
    public List<PublicMentorDto> featuredMentors(int limit) {
        return mentorProfileRepository
                .findByVerificationStatusOrderByYearsOfExperienceDesc(VerificationStatus.APPROVED).stream()
                .map(PublicMentorDto::from)
                .limit(limit)
                .toList();
    }

    public Map<String, Long> counts() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("mentors", mentorProfileRepository.countByVerificationStatus(VerificationStatus.APPROVED));
        stats.put("students", userRepository.countByRole(Role.STUDENT));
        stats.put("interviewsDone", requestRepository.countByStatus(RequestStatus.COMPLETED));
        stats.put("openRequests", requestRepository.countByStatus(RequestStatus.PENDING));
        return stats;
    }
}
