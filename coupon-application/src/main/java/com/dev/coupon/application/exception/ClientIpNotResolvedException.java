package com.dev.coupon.application.exception;

public class ClientIpNotResolvedException extends RuntimeException {

	private final String ipAddress;

	public ClientIpNotResolvedException(String ipAddress) {
		super("Client IP address could not be resolved");
		this.ipAddress = ipAddress;
	}

	public String ipAddress() {
		return ipAddress;
	}
}
