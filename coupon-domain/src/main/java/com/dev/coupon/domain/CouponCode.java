package com.dev.coupon.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record CouponCode(String value) {

	private static final int MAX_LENGTH = 64;
	private static final Pattern ALLOWED = Pattern.compile("[A-Z0-9_-]+");

	public CouponCode {
		if (value == null) {
			throw new IllegalArgumentException("Coupon code is required");
		}

		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Coupon code must not be blank");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Coupon code must be at most " + MAX_LENGTH + " characters long");
		}
		if (!ALLOWED.matcher(normalized).matches()) {
			throw new IllegalArgumentException("Coupon code may contain only letters, digits, underscores and hyphens");
		}

		value = normalized;
	}

	public static CouponCode of(String value) {
		return new CouponCode(value);
	}
}
