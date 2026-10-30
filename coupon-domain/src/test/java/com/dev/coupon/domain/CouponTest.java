package com.dev.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CouponTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant USED_AT = Instant.parse("2026-01-02T10:00:00Z");

	@Test
	void createsCouponWithInitialState() {
		Coupon coupon = Coupon.create(CouponCode.of("WIOSNA"), UsageLimit.of(5), CountryCode.of("PL"), CREATED_AT);

		assertThat(coupon.id()).isNotNull();
		assertThat(coupon.code()).isEqualTo(CouponCode.of("wiosna"));
		assertThat(coupon.createdAt()).isEqualTo(CREATED_AT);
		assertThat(coupon.maxUsages()).isEqualTo(UsageLimit.of(5));
		assertThat(coupon.currentUsages()).isZero();
		assertThat(coupon.country()).isEqualTo(CountryCode.of("pl"));
		assertThat(coupon.version()).isZero();
		assertThat(coupon.remainingUsages()).isEqualTo(5);
		assertThat(coupon.isExpired()).isFalse();
	}

	@Test
	void useIncrementsUsageAndCreatesCouponUsage() {
		Coupon coupon = Coupon.create(CouponCode.of("WIOSNA"), UsageLimit.of(2), CountryCode.of("PL"), CREATED_AT);
		CouponUsage usage = coupon.use(UserId.of("user-1"), CountryCode.of("PL"), USED_AT);

		assertThat(coupon.currentUsages()).isEqualTo(1);
		assertThat(coupon.remainingUsages()).isEqualTo(1);
		assertThat(usage.id()).isNotNull();
		assertThat(usage.couponId()).isEqualTo(coupon.id());
		assertThat(usage.userId()).isEqualTo(UserId.of("user-1"));
		assertThat(usage.usedAt()).isEqualTo(USED_AT);
		assertThat(usage.country()).isEqualTo(CountryCode.of("PL"));
	}

	@Test
	void useThrowsWhenExpired() {
		Coupon coupon = Coupon.create(CouponCode.of("WIOSNA"), UsageLimit.of(1), CountryCode.of("PL"), CREATED_AT);
		coupon.use(UserId.of("user-1"), CountryCode.of("PL"), USED_AT);

		assertThatThrownBy(() -> coupon.use(UserId.of("user-2"), CountryCode.of("PL"), USED_AT))
				.isInstanceOf(CouponExpiredException.class)
				.hasMessageContaining("expired");
		assertThat(coupon.currentUsages()).isEqualTo(1);
	}

	@Test
	void useThrowsWhenCountryDoesNotMatch() {
		Coupon coupon = Coupon.create(CouponCode.of("WIOSNA"), UsageLimit.of(2), CountryCode.of("PL"), CREATED_AT);

		assertThatThrownBy(() -> coupon.use(UserId.of("user-1"), CountryCode.of("DE"), USED_AT))
				.isInstanceOf(CountryNotAllowedException.class)
				.hasMessageContaining("PL");
		assertThat(coupon.currentUsages()).isZero();
	}

	@Test
	void restoreKeepsCurrentState() {
		CouponId couponId = CouponId.random();
		Coupon coupon = Coupon.restore(
				couponId,
				CouponCode.of("WIOSNA"),
				CREATED_AT,
				UsageLimit.of(3),
				2,
				CountryCode.of("PL"),
				7
		);

		assertThat(coupon.id()).isEqualTo(couponId);
		assertThat(coupon.currentUsages()).isEqualTo(2);
		assertThat(coupon.remainingUsages()).isEqualTo(1);
		assertThat(coupon.version()).isEqualTo(7);
		assertThat(coupon.isExpired()).isFalse();
	}

	@Test
	void restoreRejectsCurrentUsagesAboveLimit() {
		assertThatThrownBy(() -> Coupon.restore(
				CouponId.random(),
				CouponCode.of("WIOSNA"),
				CREATED_AT,
				UsageLimit.of(2),
				3,
				CountryCode.of("PL"),
				0
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Current usages");
	}

	@Test
	void restoreRejectsNegativeVersion() {
		assertThatThrownBy(() -> Coupon.restore(
				CouponId.random(),
				CouponCode.of("WIOSNA"),
				CREATED_AT,
				UsageLimit.of(2),
				0,
				CountryCode.of("PL"),
				-1
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Version");
	}
}
