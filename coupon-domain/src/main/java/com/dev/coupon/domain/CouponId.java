package com.dev.coupon.domain;

import java.util.UUID;

public record CouponId(UUID value) {

	public CouponId {
		if (value == null) {
			throw new IllegalArgumentException("Coupon id is required");
		}
	}

	public static CouponId random() {
		return new CouponId(UUID.randomUUID());
	}

	public static CouponId of(UUID value) {
		return new CouponId(value);
	}
}
