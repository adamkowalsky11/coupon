package com.dev.coupon.application.view;

import java.time.Instant;
import java.util.UUID;

public record CouponView(
		UUID id,
		String code,
		int maxUsages,
		int currentUsages,
		int remainingUsages,
		String country,
		Instant createdAt
) {
}
