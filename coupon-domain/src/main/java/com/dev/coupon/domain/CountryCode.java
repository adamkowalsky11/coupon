package com.dev.coupon.domain;

import java.util.Locale;
import java.util.Set;

public record CountryCode(String value) {

	private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

	public CountryCode {
		if (value == null) {
			throw new IllegalArgumentException("Country code is required");
		}

		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (!ISO_COUNTRIES.contains(normalized)) {
			throw new IllegalArgumentException("Country code must be a valid ISO 3166-1 alpha-2 code");
		}

		value = normalized;
	}

	public static CountryCode of(String value) {
		return new CountryCode(value);
	}
}
