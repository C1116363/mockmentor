package com.learn.interviewmentor.service.impl;

import com.learn.interviewmentor.service.PlanService;

import com.learn.interviewmentor.vo.plan.PlanVo;
import com.learn.interviewmentor.dto.plan.PlanPriceRequestDto;
import com.learn.interviewmentor.dto.plan.PlanRequestDto;
import com.learn.interviewmentor.exception.ConflictException;
import com.learn.interviewmentor.exception.NotFoundException;
import com.learn.interviewmentor.model.Plan;
import com.learn.interviewmentor.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@Transactional(readOnly = true)
public class PlanServiceImpl implements PlanService {

    private static final Logger log = LoggerFactory.getLogger(PlanServiceImpl.class);

    private final PlanRepository planRepository;

    public PlanServiceImpl(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /** What students and the public site see. Retired plans are not in here. */
    @Override
    public List<PlanVo> activePlans() {
        return planRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream().map(PlanVo::from).toList();
    }

    /** The admin list: inactive plans included, so they can be switched back on. */
    @Override
    public List<PlanVo> allPlans() {
        return planRepository.findAllByOrderByDisplayOrderAscIdAsc()
                .stream().map(PlanVo::from).toList();
    }

    @Override
    public PlanVo one(Long id) {
        return PlanVo.from(getOrThrow(id));
    }

    /** The entity, for other services. Students can only buy a live plan. */
    @Override
    public Plan activeEntity(Long id) {
        Plan plan = getOrThrow(id);
        if (!plan.isActive()) {
            throw new ConflictException("That plan is no longer available");
        }
        return plan;
    }

    @Transactional
    @Override
    public PlanVo create(PlanRequestDto dto) {
        if (planRepository.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A plan called \"" + dto.name().trim() + "\" already exists");
        }

        Plan plan = new Plan(
                dto.name().trim(),
                trimOrNull(dto.tagline()),
                trimOrNull(dto.description()),
                trimOrNull(dto.features()),
                dto.price(),
                dto.durationDays() == 0 ? 90 : dto.durationDays(),
                dto.displayOrder(),
                dto.highlighted());
        plan.setActive(dto.active());

        Plan saved = planRepository.save(plan);
        log.info("Plan created: '{}' at {}", saved.getName(), saved.getPrice());
        return PlanVo.from(saved);
    }

    @Transactional
    @Override
    public PlanVo update(Long id, PlanRequestDto dto) {
        Plan plan = getOrThrow(id);

        String newName = dto.name().trim();
        // Renaming onto another plan's name would trip the unique index and come
        // back as a 409 from the constraint anyway; catching it here means the
        // message names the actual problem.
        planRepository.findByNameIgnoreCase(newName).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ConflictException("A plan called \"" + newName + "\" already exists");
            }
        });

        plan.setName(newName);
        plan.setTagline(trimOrNull(dto.tagline()));
        plan.setDescription(trimOrNull(dto.description()));
        plan.setFeatures(trimOrNull(dto.features()));
        plan.setPrice(dto.price());
        if (dto.durationDays() > 0) {
            plan.setDurationDays(dto.durationDays());
        }
        plan.setDisplayOrder(dto.displayOrder());
        plan.setHighlighted(dto.highlighted());
        plan.setActive(dto.active());

        log.info("Plan {} updated: '{}' at {}", id, plan.getName(), plan.getPrice());
        return PlanVo.from(plan);
    }

    /**
     * The one an admin reaches for most: change the price, leave everything else
     * alone.
     *
     * Logged at info with the old and new value. A price is the kind of field
     * where "who changed this and when" gets asked later, and this log line is
     * the only record of it - the entity keeps just the current value.
     */
    @Transactional
    @Override
    public PlanVo changePrice(Long id, PlanPriceRequestDto dto) {
        Plan plan = getOrThrow(id);
        var previous = plan.getPrice();
        plan.setPrice(dto.price());
        log.info("Plan {} ('{}') price changed {} -> {}", id, plan.getName(), previous, dto.price());
        return PlanVo.from(plan);
    }

    /**
     * Retire or revive a plan.
     *
     * There is no delete. Enrollments point at this row, and deleting it would
     * either break them or - worse, with the foreign key removed - leave a
     * student holding a plan that nothing can name.
     */
    @Transactional
    @Override
    public PlanVo setActive(Long id, boolean active) {
        Plan plan = getOrThrow(id);
        plan.setActive(active);
        log.info("Plan {} ('{}') is now {}", id, plan.getName(), active ? "active" : "retired");
        return PlanVo.from(plan);
    }

    @Override
    public long activeCount() {
        return planRepository.countByActiveTrue();
    }

    private Plan getOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No plan with id " + id));
    }

    /** Blank strings are noise in the database; null says "not set" honestly. */
    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
