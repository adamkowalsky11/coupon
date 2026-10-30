package com.dev.coupon.domain;

public class CouponExpiredException extends CouponDomainException {

	private final CouponId couponId;
	private final int currentUsages;
	private final int maxUsages;

	public CouponExpiredException(CouponId couponId, int currentUsages, int maxUsages) {
		super("Coupon " + couponId.value() + " is expired");
		this.couponId = couponId;
		this.currentUsages = currentUsages;
		this.maxUsages = maxUsages;
	}

	public CouponId couponId() {
		return couponId;
	}

	public int currentUsages() {
		return currentUsages;
	}

	public int maxUsages() {
		return maxUsages;
	}
}
