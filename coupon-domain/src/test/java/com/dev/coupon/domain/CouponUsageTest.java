package com.dev.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CouponUsageTest {

	private static final Instant USED_AT = Instant.parse("2026-01-02T10:00:00Z");

	@Test
	void createsUsageWithGeneratedId() {
		CouponId couponId = CouponId.random();
		CouponUsage usage = CouponUsage.create(couponId, UserId.of("user-1"), USED_AT, CountryCode.of("PL"));

		assertThat(usage.id()).isNotNull();
		assertThat(usage.couponId()).isEqualTo(couponId);
		assertThat(usage.userId()).isEqualTo(UserId.of("user-1"));
		assertThat(usage.usedAt()).isEqualTo(USED_AT);
		assertThat(usage.country()).isEqualTo(CountryCode.of("PL"));
	}

	@Test
	void restoresUsageWithGivenId() {
		CouponUsageId usageId = CouponUsageId.of(UUID.randomUUID());
		CouponId couponId = CouponId.random();

		CouponUsage usage = CouponUsage.restore(usageId, couponId, UserId.of("user-1"), USED_AT, CountryCode.of("PL"));

		assertThat(usage.id()).isEqualTo(usageId);
		assertThat(usage.couponId()).isEqualTo(couponId);
	}
}
