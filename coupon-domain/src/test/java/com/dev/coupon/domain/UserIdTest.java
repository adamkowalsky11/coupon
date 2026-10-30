package com.dev.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserIdTest {

	@Test
	void trimsUserId() {
		assertThat(UserId.of(" user-1 ").value()).isEqualTo("user-1");
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> UserId.of(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("required");
	}

	@Test
	void rejectsBlank() {
		assertThatThrownBy(() -> UserId.of("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("blank");
	}

	@Test
	void rejectsTooLongUserId() {
		assertThatThrownBy(() -> UserId.of("a".repeat(129)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("128");
	}
}
