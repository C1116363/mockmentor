package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.facade.PublicFacade;
import com.learn.interviewmentor.service.PlanService;
import com.learn.interviewmentor.service.PublicStatsService;
import com.learn.interviewmentor.vo.PublicMentorVo;
import com.learn.interviewmentor.vo.plan.PlanVo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PublicFacadeImpl implements PublicFacade {

    /** Guards against a caller asking for the whole mentor table. */
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 12;

    private final PublicStatsService publicStatsService;
    private final PlanService planService;

    public PublicFacadeImpl(PublicStatsService publicStatsService, PlanService planService) {
        this.publicStatsService = publicStatsService;
        this.planService = planService;
    }

    @Override
    public ApiResult<Map<String, Long>> stats() {
        return ApiResult.ok(publicStatsService.counts());
    }

    @Override
    public ApiResult<List<PublicMentorVo>> featuredMentors(int limit) {
        // Clamped here rather than in the controller: "how much of this may the
        // open internet take in one call" is a rule about the use case, not
        // about parsing a query string.
        return ApiResult.ok(publicStatsService.featuredMentors(
                Math.clamp(limit, MIN_LIMIT, MAX_LIMIT)));
    }

    @Override
    public ApiResult<List<PlanVo>> plans() {
        return ApiResult.ok(planService.activePlans());
    }
}
