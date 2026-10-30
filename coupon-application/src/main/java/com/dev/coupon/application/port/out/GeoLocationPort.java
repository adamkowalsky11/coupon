package com.dev.coupon.application.port.out;

import com.dev.coupon.domain.CountryCode;

public interface GeoLocationPort {

	CountryCode resolveCountry(String ipAddress);
}
