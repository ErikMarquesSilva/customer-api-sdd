package com.example.customerapi.customer.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfValidatorTest {

	private final CpfValidator validator = new CpfValidator();

	@Test
	void acceptsValidCpf() {
		assertThat(validator.isValid("52998224725", null)).isTrue();
	}

	@Test
	void acceptsNullBecauseRequiredIsCheckedElsewhere() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

	@Test
	void rejectsWrongFirstCheckDigit() {
		assertThat(validator.isValid("52998224735", null)).isFalse();
	}

	@Test
	void rejectsWrongSecondCheckDigit() {
		assertThat(validator.isValid("52998224724", null)).isFalse();
	}

	@Test
	void rejectsTenDigits() {
		assertThat(validator.isValid("5299822472", null)).isFalse();
	}

	@Test
	void rejectsTwelveDigits() {
		assertThat(validator.isValid("529982247250", null)).isFalse();
	}

	@Test
	void rejectsLetters() {
		assertThat(validator.isValid("5299822472a", null)).isFalse();
	}

	@Test
	void rejectsAllDigitsEqual() {
		assertThat(validator.isValid("11111111111", null)).isFalse();
	}

}
