package com.dev.coupon.domain;

public class CountryNotAllowedException extends CouponDomainException {

	private final CouponId couponId;
	private final CountryCode couponCountry;
	private final CountryCode requestCountry;

	public CountryNotAllowedException(CouponId couponId, CountryCode couponCountry, CountryCode requestCountry) {
		super("Coupon " + couponId.value() + " is restricted to country " + couponCountry.value());
		this.couponId = couponId;
		this.couponCountry = couponCountry;
		this.requestCountry = requestCountry;
	}

	public CouponId couponId() {
		return couponId;
	}

	public CountryCode couponCountry() {
		return couponCountry;
	}

	public CountryCode requestCountry() {
		return requestCountry;
	}
}
