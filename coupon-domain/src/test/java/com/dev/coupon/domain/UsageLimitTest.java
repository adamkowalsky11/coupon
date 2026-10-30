package com.dev.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UsageLimitTest {

	@Test
	void acceptsPositiveLimit() {
		assertThat(UsageLimit.of(1).value()).isEqualTo(1);
	}

	@Test
	void rejectsZero() {
		assertThatThrownBy(() -> UsageLimit.of(0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("greater than zero");
	}

	@Test
	void rejectsNegativeLimit() {
		assertThatThrownBy(() -> UsageLimit.of(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("greater than zero");
	}
}
