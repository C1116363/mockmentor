package com.learn.interviewmentor.service;

import com.learn.interviewmentor.vo.plan.PlanVo;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.plan.PlanRequestDto;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.Plan;
import com.learn.interviewmentor.repository.PlanRepository;
import java.util.List;

/**
 * The price list.
 *
 * Prices live in the database, not in application.properties, so an admin can
 * change one from the admin panel and every student sees the new number on their
 * next page load - no redeploy, no restart.
 *
 * The rule that makes that safe: a price change only ever affects what happens
 * *next*. {@link com.learn.interviewmentor.model.PlanEnrollment} copies the
 * price when the student starts buying, so nobody's completed purchase is ever
 * rewritten underneath them.
 */
public interface PlanService {

    /** What students and the public site see. Retired plans are not in here. */
    List<PlanVo> activePlans();

    /** The admin list: inactive plans included, so they can be switched back on. */
    List<PlanVo> allPlans();

    PlanVo one(Long id);

    /** The entity, for other services. Students can only buy a live plan. */
    Plan activeEntity(Long id);

    PlanVo create(PlanRequestDto dto);

    PlanVo update(Long id, PlanRequestDto dto);

    /**
    * The one an admin reaches for most: change the price, leave everything else
    * alone.
    *
    * Logged at info with the old and new value. A price is the kind of field
    * where "who changed this and when" gets asked later, and this log line is
    * the only record of it - the entity keeps just the current value.
    */
    PlanVo changePrice(Long id, PlanPriceRequestDto dto);

    /**
    * Retire or revive a plan.
    *
    * There is no delete. Enrollments point at this row, and deleting it would
    * either break them or - worse, with the foreign key removed - leave a
    * student holding a plan that nothing can name.
    */
    PlanVo setActive(Long id, boolean active);

    long activeCount();
}
