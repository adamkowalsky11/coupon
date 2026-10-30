package com.dev.coupon.application.exception;

public class GeoLocationUnavailableException extends RuntimeException {

	public GeoLocationUnavailableException(String message) {
		super(message);
	}

	public GeoLocationUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
