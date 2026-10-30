package com.dev.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CountryCodeTest {

	@Test
	void normalizesToUppercase() {
		assertThat(CountryCode.of(" pl ").value()).isEqualTo("PL");
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> CountryCode.of(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("required");
	}

	@Test
	void rejectsUnknownCountry() {
		assertThatThrownBy(() -> CountryCode.of("XX"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("ISO");
	}

	@Test
	void rejectsCodeWithWrongLength() {
		assertThatThrownBy(() -> CountryCode.of("POL"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("ISO");
	}
}
