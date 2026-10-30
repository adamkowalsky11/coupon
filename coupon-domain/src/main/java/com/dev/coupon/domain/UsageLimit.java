package com.dev.coupon.domain;

public record UsageLimit(int value) {

	public UsageLimit {
		if (value < 1) {
			throw new IllegalArgumentException("Usage limit must be greater than zero");
		}
	}

	public static UsageLimit of(int value) {
		return new UsageLimit(value);
	}
}
