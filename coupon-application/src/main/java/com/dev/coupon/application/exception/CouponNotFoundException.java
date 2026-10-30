package com.dev.coupon.application.exception;

public class CouponNotFoundException extends RuntimeException {

	private final String code;

	public CouponNotFoundException(String code) {
		super("Coupon " + code + " not found");
		this.code = code;
	}

	public String code() {
		return code;
	}
}
