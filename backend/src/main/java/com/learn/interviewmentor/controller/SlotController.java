package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.SlotVo;
import com.learn.interviewmentor.facade.SlotFacade;
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
            summary = "One-hour slots available on a given day",
            description = """
                    Returns every slot between 9:00 AM and 9:00 PM for that date, each one
                    hour long, and marks which are bookable.

                    A slot comes back unavailable when it has already passed, or when it is
                    fully booked - capacity is the number of mentors in the system, since
                    that is how many interviews can run at the same time. Cancelled requests
                    release their slot again.

                    You can book up to 30 days ahead.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The slot grid for that day"),
            @ApiResponse(responseCode = "400", description = "Date is in the past or too far ahead",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<SlotVo>> slots(
            @Parameter(description = "Day to show slots for, yyyy-MM-dd", example = "2026-09-20")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotFacade.slotsFor(date);
    }
}
