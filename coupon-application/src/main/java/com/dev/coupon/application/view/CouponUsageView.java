package com.dev.coupon.application.view;

import java.time.Instant;
import java.util.UUID;

public record CouponUsageView(
		UUID id,
		UUID couponId,
		String userId,
		Instant usedAt,
		String country
) {
}
