package com.dev.coupon.domain;

import java.time.Instant;
import java.util.Objects;

public record CouponUsage(
		CouponUsageId id,
		CouponId couponId,
		UserId userId,
		Instant usedAt,
		CountryCode country
) {

	public CouponUsage {
		Objects.requireNonNull(id, "Coupon usage id is required");
		Objects.requireNonNull(couponId, "Coupon id is required");
		Objects.requireNonNull(userId, "User id is required");
		Objects.requireNonNull(usedAt, "Usage timestamp is required");
		Objects.requireNonNull(country, "Country is required");
	}

	public static CouponUsage create(CouponId couponId, UserId userId, Instant usedAt, CountryCode country) {
		return new CouponUsage(CouponUsageId.random(), couponId, userId, usedAt, country);
	}

	public static CouponUsage restore(
			CouponUsageId id,
			CouponId couponId,
			UserId userId,
			Instant usedAt,
			CountryCode country
	) {
		return new CouponUsage(id, couponId, userId, usedAt, country);
	}
}
