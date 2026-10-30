package com.dev.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class CouponCodeTest {

	@Test
	void normalizesCode() {
		assertThat(CouponCode.of("  wiosna  ").value()).isEqualTo("WIOSNA");
	}

	@Test
	void acceptsDigitsUnderscoresAndHyphens() {
		assertThat(CouponCode.of("spring_2026-1").value()).isEqualTo("SPRING_2026-1");
	}

	@Test
	void usesLocaleRootForUppercase() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			assertThat(CouponCode.of("i").value()).isEqualTo("I");
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> CouponCode.of(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("required");
	}

	@Test
	void rejectsBlank() {
		assertThatThrownBy(() -> CouponCode.of("   "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("blank");
	}

	@Test
	void rejectsTooLongCode() {
		assertThatThrownBy(() -> CouponCode.of("A".repeat(65)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("64");
	}

	@Test
	void rejectsInvalidCharacters() {
		assertThatThrownBy(() -> CouponCode.of("WIOSNA!"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("letters");
	}
}
