package com.dev.coupon.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
		@Schema(description = "Coupon code", example = "SUMMER-2026", maxLength = 64)
		@NotBlank @Size(max = 64)
		String code,

		@Schema(description = "Maximum number of usages", example = "100", minimum = "1")
		@Min(1)
		int maxUsages,

		@Schema(description = "ISO 3166-1 alpha-2 country code", example = "PL", pattern = "[A-Za-z]{2}")
		@NotBlank @Size(min = 2, max = 2) @Pattern(regexp = "[A-Za-z]{2}")
		String country
) {
}
