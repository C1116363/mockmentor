package com.learn.interviewmentor.facade.impl;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.facade.SlotFacade;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.service.SlotService;
import com.learn.interviewmentor.vo.SlotVo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * A one-method facade, and yes it only delegates.
 *
 * Worth noting: the comment here used to say "the day slots need a second source -
 * a mentor's own availability - this is where that goes". That day came, and the
 * change landed entirely inside SlotService. Nothing above this line moved.
 */
@Component
public class SlotFacadeImpl implements SlotFacade {

    private final SlotService slotService;

    public SlotFacadeImpl(SlotService slotService) {
        this.slotService = slotService;
    }

    @Override
    public ApiResult<List<SlotVo>> slotsFor(LocalDate date, SessionType sessionType) {
        return ApiResult.ok(slotService.slotsFor(date, sessionType));
    }
}
