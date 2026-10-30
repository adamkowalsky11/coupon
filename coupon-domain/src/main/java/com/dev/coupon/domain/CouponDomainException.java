package com.dev.coupon.domain;

public abstract class CouponDomainException extends RuntimeException {

	protected CouponDomainException(String message) {
		super(message);
	}
}
