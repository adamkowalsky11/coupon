package com.dev.coupon.domain;

public record UserId(String value) {

	private static final int MAX_LENGTH = 128;

	public UserId {
		if (value == null) {
			throw new IllegalArgumentException("User id is required");
		}

		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("User id must not be blank");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("User id must be at most " + MAX_LENGTH + " characters long");
		}

		value = normalized;
	}

	public static UserId of(String value) {
		return new UserId(value);
	}
}
