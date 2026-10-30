package com.dev.coupon.application.exception;

import com.dev.coupon.domain.CouponCode;

public class CouponCodeAlreadyExistsException extends RuntimeException {

	private final CouponCode code;

	public CouponCodeAlreadyExistsException(CouponCode code) {
		super("Coupon code " + code.value() + " already exists");
		this.code = code;
	}

	public CouponCode code() {
		return code;
	}
}
