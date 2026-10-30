package com.dev.coupon.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UseCouponRequest(
		@Schema(description = "Identifier of the user using the coupon", example = "user-123", maxLength = 128)
		@NotBlank @Size(max = 128)
		String userId
) {
}
