package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.SlotVo;
import com.learn.interviewmentor.facade.SlotFacade;
import com.learn.interviewmentor.model.SessionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@Tag(name = "2. Interview requests",
        description = "The core workflow: a candidate books a one-hour slot, "
                + "then a mentor picks it up and completes it with feedback.")
public class SlotController {

    private final SlotFacade slotFacade;

    public SlotController(SlotFacade slotFacade) {
        this.slotFacade = slotFacade;
    }

    @GetMapping
    @Operation(
            summary = "Hours a mentor has offered on a given day",
            description = """
                    **Only hours a verified mentor actually declared** for this kind of
                    session. Nothing is generated - an empty list means nobody has put their
                    hand up for that day, which is a truthful answer and more useful than a
                    row of greyed-out buttons.

                    `mentorsOffering` is how many mentors declared that hour; `remaining` is
                    how many more students can still book it. Cancelled bookings release
                    their slot, and the mentor's hour goes back on the market.

                    A slot comes back unavailable for one of two different reasons, and the
                    difference matters:

                    - **"Needs 24 hours' notice"** - it has not passed, it is just inside the
                      notice period. Sessions need a day in hand so an admin can map an
                      interviewer onto the booking.
                    - **"Fully booked"** - every mentor who offered that hour is taken.

                    You can book from 24 hours ahead up to 30 days ahead.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The slot grid for that day"),
            @ApiResponse(responseCode = "400", description = "Date is in the past or too far ahead",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<SlotVo>> slots(
            @Parameter(description = "Day to show slots for, yyyy-MM-dd", example = "2026-09-20")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "Which kind of session. A mentor may offer an hour for "
                    + "one and not the other. Defaults to MOCK_INTERVIEW.",
                    example = "MENTORING")
            @RequestParam(required = false) SessionType sessionType) {
        return slotFacade.slotsFor(date, sessionType);
    }
}
