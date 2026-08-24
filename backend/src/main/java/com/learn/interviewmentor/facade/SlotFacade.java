package com.learn.interviewmentor.facade;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.model.SessionType;
import com.learn.interviewmentor.vo.SlotVo;

import java.time.LocalDate;
import java.util.List;

/**
 * The bookable-hours grid for one day.
 *
 * Takes the session type because the grid differs by it: a mentor may offer an
 * hour for mock interviews but not for mentoring discussions.
 */
public interface SlotFacade {
    ApiResult<List<SlotVo>> slotsFor(LocalDate date, SessionType sessionType);
}
