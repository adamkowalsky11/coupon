package com.dev.coupon.config;

import com.dev.coupon.adapter.out.geolocation.GeoLocationSettings;
import com.dev.coupon.adapter.out.geolocation.IpWhoIsGeoLocationAdapter;
import com.dev.coupon.application.port.out.GeoLocationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class GeoLocationConfiguration {

	@Bean
	GeoLocationPort geoLocationPort(
			@Value("${coupon.geo.base-url:https://ipwho.is}") String baseUrl,
			Clock clock
	) {
		GeoLocationSettings settings = new GeoLocationSettings(
				baseUrl,
				Duration.ofSeconds(2),
				Duration.ofSeconds(3),
				Duration.ofHours(1),
				Duration.ofHours(24),
				Duration.ofSeconds(3),
				3,
				Duration.ofMillis(250)
		);
		return new IpWhoIsGeoLocationAdapter(settings, clock);
	}
}
