package com.learn.interviewmentor.dto.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Just the price.
 *
 * A separate endpoint from the full edit because changing a price is the thing
 * an admin does most often, and it should not require sending the description
 * and every feature line back - a round trip where a partly-filled form can
 * silently blank a field is how plans lose their contents.
 */
@Schema(description = "Change one plan's price. ADMIN only.")
public record PlanPriceRequestDto(

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @DecimalMax(value = "999999.99", message = "That price looks like a typo")
        @Schema(example = "3499.00")
        BigDecimal price
) {
}
