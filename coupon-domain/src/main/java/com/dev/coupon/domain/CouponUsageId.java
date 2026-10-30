package com.dev.coupon.domain;

import java.util.UUID;

public record CouponUsageId(UUID value) {

	public CouponUsageId {
		if (value == null) {
			throw new IllegalArgumentException("Coupon usage id is required");
		}
	}

	public static CouponUsageId random() {
		return new CouponUsageId(UUID.randomUUID());
	}

	public static CouponUsageId of(UUID value) {
		return new CouponUsageId(value);
	}
}
