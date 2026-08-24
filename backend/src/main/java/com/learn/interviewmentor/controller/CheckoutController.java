package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;
import com.learn.interviewmentor.dto.checkout.CheckoutCallbackDto;
import com.learn.interviewmentor.facade.CheckoutFacade;
import com.learn.interviewmentor.model.PaymentPurpose;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.vo.checkout.CheckoutOptionsVo;
import com.learn.interviewmentor.vo.checkout.CheckoutResultVo;
import com.learn.interviewmentor.vo.checkout.CheckoutVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@Tag(name = "5. Checkout (gateway)",
        description = """
                Paying by card, netbanking or UPI through a payment gateway.

                Runs alongside the manual UPI flow rather than replacing it - `GET
                /api/checkout/options` says which of the two this server can actually offer,
                so the payment screen can show a gateway button only when the keys are
                really configured.

                **The amount is never sent by the client.** It is read from the row being
                paid for. A checkout that accepted a price from the browser would be a shop
                where the customer writes their own price tag.
                """)
public class CheckoutController {

    private final CheckoutFacade checkoutFacade;

    public CheckoutController(CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    @GetMapping("/options")
    @Operation(
            summary = "How this server can be paid",
            description = "Which gateway is configured, whether its keys are actually present, "
                    + "and whether manual UPI is still on. The payment screen calls this before "
                    + "deciding what to show.")
    public ApiResult<CheckoutOptionsVo> options() {
        return checkoutFacade.options();
    }

    @PostMapping("/{purpose}/{targetId}")
    @Operation(
            summary = "Open a checkout",
            description = """
                    Creates an order at the gateway for one thing the caller is buying, and
                    returns what the browser needs to open the checkout window.

                    `purpose` says what kind of thing, and `targetId` is its id:

                    | purpose | targetId | what it buys |
                    | --- | --- | --- |
                    | `INTERVIEW` | payment id | a mock interview or mentoring session |
                    | `PLAN` | enrollment id | a study plan |
                    | `PROJECT` | access request id | contributor access to a private repo |

                    Ownership and price are decided on the server. Opening a checkout against
                    somebody else's row is a 403, and one that is already paid for is a 400.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created - open the checkout"),
            @ApiResponse(responseCode = "400", description = "Already paid for",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not yours",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "409", description = "A screenshot is already being reviewed",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "502", description = "The gateway is unreachable or not configured",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<CheckoutVo> start(
            @Parameter(description = "What is being bought", example = "PLAN")
            @PathVariable PaymentPurpose purpose,
            @Parameter(description = "Id of the row being paid for", example = "14")
            @PathVariable Long targetId,
            @CurrentUser User caller) {
        return checkoutFacade.start(purpose, targetId, caller);
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "The browser reporting a successful payment",
            description = """
                    Called by the frontend with what the checkout window handed it. The
                    signature is verified against our key secret before anything is believed -
                    without that check, posting a made-up payment id would mark an order paid.

                    **This is a convenience, not the source of truth.** The gateway's webhook
                    is what actually settles a payment; this endpoint exists so the student
                    still looking at the screen gets an answer immediately instead of watching
                    a spinner until the webhook lands. If the webhook got here first, this
                    reports what already happened rather than doing it twice.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment confirmed"),
            @ApiResponse(responseCode = "403", description = "Bad signature, or not your payment",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No such order",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<CheckoutResultVo> confirm(@Valid @RequestBody CheckoutCallbackDto callback,
                                               @CurrentUser User caller) {
        return checkoutFacade.confirm(callback, caller);
    }
}
