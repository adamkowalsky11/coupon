package com.dev.coupon.adapter.in.rest;

import com.dev.coupon.application.view.CouponUsageView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record CouponUsageResponse(
		@Schema(description = "Usage identifier", example = "1b3e4c5d-6f70-4a8b-9c0d-1e2f3a4b5c6d")
		UUID id,

		@Schema(description = "Coupon identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
		UUID couponId,

		@Schema(description = "Identifier of the user using the coupon", example = "user-123")
		String userId,

		@Schema(description = "Usage timestamp in UTC", example = "2026-01-01T00:00:00Z")
		Instant usedAt,

		@Schema(description = "Country resolved from the client IP", example = "PL")
		String country
) {

	public static CouponUsageResponse from(CouponUsageView view) {
		return new CouponUsageResponse(
				view.id(),
				view.couponId(),
				view.userId(),
				view.usedAt(),
				view.country()
		);
	}
}
