package com.dev.coupon.adapter.out.geolocation;

import java.time.Duration;
import java.util.Objects;

public record GeoLocationSettings(
		String baseUrl,
		Duration connectTimeout,
		Duration readTimeout,
		Duration cacheRefreshAfter,
		Duration cacheExpireAfter,
		Duration timeLimit,
		int retryMaxAttempts,
		Duration retryWaitDuration
) {

	public GeoLocationSettings {
		Objects.requireNonNull(baseUrl, "baseUrl");
		if (baseUrl.isBlank()) {
			throw new IllegalArgumentException("baseUrl must not be blank");
		}
		requirePositive(connectTimeout, "connectTimeout");
		requirePositive(readTimeout, "readTimeout");
		requirePositive(cacheRefreshAfter, "cacheRefreshAfter");
		requirePositive(cacheExpireAfter, "cacheExpireAfter");
		requirePositive(timeLimit, "timeLimit");
		if (cacheExpireAfter.compareTo(cacheRefreshAfter) < 0) {
			throw new IllegalArgumentException("cacheExpireAfter must not be shorter than cacheRefreshAfter");
		}
		if (retryMaxAttempts < 1) {
			throw new IllegalArgumentException("retryMaxAttempts must be greater than zero");
		}
		if (retryWaitDuration == null || retryWaitDuration.isNegative()) {
			throw new IllegalArgumentException("retryWaitDuration must not be negative");
		}
	}

	private static void requirePositive(Duration duration, String name) {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
	}
}
