package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.vo.SlotVo;

import java.time.LocalDate;
import java.util.List;

/** The bookable-hours grid for one day. */
public interface SlotFacade {
    ApiResult<List<SlotVo>> slotsFor(LocalDate date);
}
