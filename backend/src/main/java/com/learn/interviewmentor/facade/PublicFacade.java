package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.vo.PublicMentorVo;
import com.learn.interviewmentor.vo.plan.PlanVo;

import java.util.List;
import java.util.Map;

/**
 * What the marketing site can read with no token at all.
 *
 * Everything here is world-readable, so it is worth having one place that
 * decides what belongs on that list: counts, products, and a mentor's public
 * profile - never an email address, never a person's private data.
 */
public interface PublicFacade {

    ApiResult<Map<String, Long>> stats();

    ApiResult<List<PublicMentorVo>> featuredMentors(int limit);

    ApiResult<List<PlanVo>> plans();
}
