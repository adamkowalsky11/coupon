package com.dev.coupon.adapter.in.rest;

import com.dev.coupon.application.view.CouponView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
		@Schema(description = "Coupon identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
		UUID id,

		@Schema(description = "Normalized coupon code", example = "SUMMER-2026")
		String code,

		@Schema(description = "Maximum number of usages", example = "100")
		int maxUsages,

		@Schema(description = "Number of usages already recorded", example = "1")
		int currentUsages,

		@Schema(description = "Number of usages still available", example = "99")
		int remainingUsages,

		@Schema(description = "Country code assigned to the coupon", example = "PL")
		String country,

		@Schema(description = "Creation timestamp in UTC", example = "2026-01-01T00:00:00Z")
		Instant createdAt
) {

	public static CouponResponse from(CouponView view) {
		return new CouponResponse(
				view.id(),
				view.code(),
				view.maxUsages(),
				view.currentUsages(),
				view.remainingUsages(),
				view.country(),
				view.createdAt()
		);
	}
}
