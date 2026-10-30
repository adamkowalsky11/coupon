package com.dev.coupon.application.exception;

import com.dev.coupon.domain.CouponId;
import com.dev.coupon.domain.UserId;

public class CouponAlreadyUsedException extends RuntimeException {

	private final CouponId couponId;
	private final UserId userId;

	public CouponAlreadyUsedException(CouponId couponId, UserId userId) {
		super("User " + userId.value() + " already used coupon " + couponId.value());
		this.couponId = couponId;
		this.userId = userId;
	}

	public CouponId couponId() {
		return couponId;
	}

	public UserId userId() {
		return userId;
	}
}
