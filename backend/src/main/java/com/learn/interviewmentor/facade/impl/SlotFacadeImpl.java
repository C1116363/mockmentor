package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.facade.SlotFacade;
import com.learn.interviewmentor.service.SlotService;
import com.learn.interviewmentor.vo.SlotVo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * A one-method facade, and yes it only delegates.
 *
 * Kept for the same reason a table has four legs: every controller reaching for
 * a facade, with no exceptions, is what makes the layering something you can
 * trust without reading each class. The day slots need a second source - a
 * mentor's own availability, a holiday calendar - this is where that goes, and
 * nothing above has to change.
 */
@Component
public class SlotFacadeImpl implements SlotFacade {

    private final SlotService slotService;

    public SlotFacadeImpl(SlotService slotService) {
        this.slotService = slotService;
    }

    @Override
    public ApiResult<List<SlotVo>> slotsFor(LocalDate date) {
        return ApiResult.ok(slotService.slotsFor(date));
    }
}
