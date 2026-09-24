package com.example.customerapi.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** CUST-07: the CPF rule, owned by the domain (ARCH-03). */
class CpfTest {

	@ParameterizedTest
	@ValueSource(strings = { "52998224725", "10000000108", "10000002810" })
	void acceptsValidCpfsIncludingBothRemainder10Branches(String cpf) {
		assertThat(Cpf.isValid(cpf)).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "52998224735", "52998224726", "5299822472", "529982247250", "5299822472a",
			"529.982.247-25", "11111111111" })
	void rejectsNullWrongCheckDigitsWrongLengthNonDigitsAndRepeatedDigits(String cpf) {
		assertThat(Cpf.isValid(cpf)).isFalse();
	}

}
