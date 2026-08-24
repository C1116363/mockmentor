package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.project.LiveProjectRequestDto;
import com.learn.interviewmentor.model.LiveProject;
import com.learn.interviewmentor.vo.project.LiveProjectVo;

import java.util.List;

/**
 * The catalogue of private codebases students can pay to contribute to.
 *
 * The repository path is private data here - see {@link LiveProjectVo}. Every
 * read method makes an explicit choice about whether to include it.
 */
public interface LiveProjectService {

    /**
     * Open projects, for browsing.
     *
     * @param withAccessTo project ids this caller currently holds. Those come
     *                     back with the repository included; everything else has
     *                     it withheld.
     */
    List<LiveProjectVo> openProjects(List<Long> withAccessTo);

    /** Every project, repository included. ADMIN only. */
    List<LiveProjectVo> allProjects();

    LiveProjectVo one(Long id, boolean includeRepo);

    /** The entity. Throws 409 if the project is not accepting contributors. */
    LiveProject openEntity(Long id);

    /** Throws 409 if every contributor seat is taken. For a brand-new request. */
    void assertSeatAvailable(LiveProject project);

    /**
     * Same check, ignoring one request's own seat.
     *
     * Used when approving: that row is already SUBMITTED and therefore already
     * counted, so without the exclusion a full project refuses to approve the
     * very request holding one of its seats.
     */
    void assertSeatAvailable(LiveProject project, Long excludingRequestId);

    LiveProjectVo create(LiveProjectRequestDto dto);

    LiveProjectVo update(Long id, LiveProjectRequestDto dto);

    /** Just the price. Reuses PlanPriceRequestDto - it is the same one field. */
    LiveProjectVo changePrice(Long id, PlanPriceRequestDto dto);

    /** Open or close to new contributors. Does not revoke existing access. */
    LiveProjectVo setActive(Long id, boolean active);

    long openCount();
}
