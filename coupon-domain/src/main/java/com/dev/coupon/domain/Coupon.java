package com.dev.coupon.domain;

import java.time.Instant;
import java.util.Objects;

public final class Coupon {

	private final CouponId id;
	private final CouponCode code;
	private final Instant createdAt;
	private final UsageLimit maxUsages;
	private final CountryCode country;
	private final long version;
	private int currentUsages;

	private Coupon(
			CouponId id,
			CouponCode code,
			Instant createdAt,
			UsageLimit maxUsages,
			int currentUsages,
			CountryCode country,
			long version
	) {
		this.id = Objects.requireNonNull(id, "Coupon id is required");
		this.code = Objects.requireNonNull(code, "Coupon code is required");
		this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp is required");
		this.maxUsages = Objects.requireNonNull(maxUsages, "Usage limit is required");
		this.country = Objects.requireNonNull(country, "Country is required");

		if (currentUsages < 0 || currentUsages > maxUsages.value()) {
			throw new IllegalArgumentException("Current usages must be between 0 and max usages");
		}
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}

		this.currentUsages = currentUsages;
		this.version = version;
	}

	public static Coupon create(CouponCode code, UsageLimit maxUsages, CountryCode country, Instant createdAt) {
		return new Coupon(CouponId.random(), code, createdAt, maxUsages, 0, country, 0);
	}

	public static Coupon restore(
			CouponId id,
			CouponCode code,
			Instant createdAt,
			UsageLimit maxUsages,
			int currentUsages,
			CountryCode country,
			long version
	) {
		return new Coupon(id, code, createdAt, maxUsages, currentUsages, country, version);
	}

	public CouponUsage use(UserId userId, CountryCode requestCountry, Instant usedAt) {
		Objects.requireNonNull(userId, "User id is required");
		Objects.requireNonNull(requestCountry, "Request country is required");
		Objects.requireNonNull(usedAt, "Usage timestamp is required");

		if (isExpired()) {
			throw new CouponExpiredException(id, currentUsages, maxUsages.value());
		}
		if (!country.equals(requestCountry)) {
			throw new CountryNotAllowedException(id, country, requestCountry);
		}

		currentUsages++;
		return CouponUsage.create(id, userId, usedAt, requestCountry);
	}

	public boolean isExpired() {
		return currentUsages >= maxUsages.value();
	}

	public int remainingUsages() {
		return maxUsages.value() - currentUsages;
	}

	public CouponId id() {
		return id;
	}

	public CouponCode code() {
		return code;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public UsageLimit maxUsages() {
		return maxUsages;
	}

	public int currentUsages() {
		return currentUsages;
	}

	public CountryCode country() {
		return country;
	}

	public long version() {
		return version;
	}

	@Override
	public boolean equals(Object other) {
		return this == other || (other instanceof Coupon coupon && id.equals(coupon.id));
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Coupon{id=" + id
				+ ", code=" + code
				+ ", currentUsages=" + currentUsages
				+ ", maxUsages=" + maxUsages
				+ ", country=" + country
				+ "}";
	}
}
