package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.customerapi.common.config.ClockConfig;

import jakarta.validation.Configuration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class CustomerRequestTest {

	// 01:00 UTC on 2026-03-10 is still 2026-03-09 in America/Sao_Paulo, so a local-date check would differ.
	private static final Clock FIXED_UTC = Clock.fixed(Instant.parse("2026-03-10T01:00:00Z"), ZoneOffset.UTC);

	private static final LocalDate TODAY_UTC = LocalDate.of(2026, 3, 10);

	private static ValidatorFactory factory;

	private static Validator validator;

	@BeforeAll
	static void buildValidatorWithClockConfig() {
		Configuration<?> configuration = Validation.byDefaultProvider().configure();
		new ClockConfig().clockProviderCustomizer(FIXED_UTC).customize(configuration);
		factory = configuration.buildValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void closeFactory() {
		factory.close();
	}

	// --- normalization (CUST-03, CUST-04, CUST-05, CUST-50, CUST-51)

	@Test
	void emailIsTrimmedAndLowercased() {
		assertThat(withEmail(" Ana@Example.COM ").email()).isEqualTo("ana@example.com");
		assertThat(invalidProperties(withEmail(" Ana@Example.COM "))).isEmpty();
	}

	@Test
	void cpfDotsAndDashAreRemoved() {
		assertThat(withCpf("529.982.247-25").cpf()).isEqualTo("52998224725");
		assertThat(invalidProperties(withCpf("529.982.247-25"))).isEmpty();
	}

	@Test
	void stateIsUppercased() {
		assertThat(withState("sp").state()).isEqualTo("SP");
		assertThat(invalidProperties(withState("sp"))).isEmpty();
	}

	@Test
	void cityIsTrimmedAndInnerWhitespaceCollapsed() {
		assertThat(withCity("  São   Paulo ").city()).isEqualTo("São Paulo");
	}

	@Test
	void nameIsTrimmed() {
		assertThat(withName("  Ana Souza  ").name()).isEqualTo("Ana Souza");
	}

	// --- baseline

	@Test
	void validRequestHasNoViolations() {
		assertThat(invalidProperties(valid())).isEmpty();
	}

	@Test
	void phoneAndBirthDateAreOptional() {
		CustomerRequest request = new CustomerRequest("Ana Souza", "ana@example.com", "52998224725", null, null,
				"São Paulo", "SP");

		assertThat(invalidProperties(request)).isEmpty();
	}

	// --- name (CUST-05)

	@Test
	void nameMissingIsInvalid() {
		assertThat(invalidProperties(withName(null))).containsExactly("name");
	}

	@Test
	void nameBlankIsInvalid() {
		assertThat(invalidProperties(withName("   "))).containsExactly("name");
	}

	@Test
	void nameWithTwoCharactersIsValid() {
		assertThat(invalidProperties(withName("Al"))).isEmpty();
	}

	@Test
	void nameWithOneCharacterAfterTrimIsInvalid() {
		assertThat(invalidProperties(withName("  A  "))).containsExactly("name");
	}

	@Test
	void nameWith120CharactersIsValid() {
		assertThat(invalidProperties(withName("a".repeat(120)))).isEmpty();
	}

	@Test
	void nameWith121CharactersIsInvalid() {
		assertThat(invalidProperties(withName("a".repeat(121)))).containsExactly("name");
	}

	// --- email (CUST-06)

	@Test
	void emailMissingIsInvalid() {
		assertThat(invalidProperties(withEmail(null))).containsExactly("email");
	}

	@Test
	void emailNotAnAddressIsInvalid() {
		assertThat(invalidProperties(withEmail("not-an-email"))).containsExactly("email");
	}

	@Test
	void emailWith254CharactersIsValid() {
		String email = emailOfLength(254);

		assertThat(email).hasSize(254);
		assertThat(invalidProperties(withEmail(email))).isEmpty();
	}

	@Test
	void emailWith255CharactersIsInvalid() {
		String email = emailOfLength(255);

		assertThat(email).hasSize(255);
		assertThat(invalidProperties(withEmail(email))).containsExactly("email");
	}

	// --- cpf (CUST-07)

	@Test
	void cpfMissingIsInvalid() {
		assertThat(invalidProperties(withCpf(null))).containsExactly("cpf");
	}

	@Test
	void cpfWithWrongCheckDigitIsInvalid() {
		assertThat(invalidProperties(withCpf("529.982.247-24"))).containsExactly("cpf");
	}

	@Test
	void cpfWithAllDigitsEqualIsInvalid() {
		assertThat(invalidProperties(withCpf("111.111.111-11"))).containsExactly("cpf");
	}

	@Test
	void cpfWithTenDigitsIsInvalid() {
		assertThat(invalidProperties(withCpf("5299822472"))).containsExactly("cpf");
	}

	// --- phone (CUST-08)

	@Test
	void phoneWithTenDigitsIsValid() {
		assertThat(invalidProperties(withPhone("1133334444"))).isEmpty();
	}

	@Test
	void phoneWithPlusAndThirteenDigitsIsValid() {
		assertThat(invalidProperties(withPhone("+5511987654321"))).isEmpty();
	}

	@Test
	void phoneWithNineDigitsIsInvalid() {
		assertThat(invalidProperties(withPhone("113333444"))).containsExactly("phone");
	}

	@Test
	void phoneWithFourteenDigitsIsInvalid() {
		assertThat(invalidProperties(withPhone("55119876543210"))).containsExactly("phone");
	}

	@Test
	void phoneWithPunctuationIsInvalid() {
		assertThat(invalidProperties(withPhone("(11) 98765-4321"))).containsExactly("phone");
	}

	// --- birthDate (CUST-09), against the fixed UTC clock

	@Test
	void birthDateYesterdayInUtcIsValid() {
		assertThat(invalidProperties(withBirthDate(TODAY_UTC.minusDays(1)))).isEmpty();
	}

	@Test
	void birthDateTodayInUtcIsInvalid() {
		assertThat(invalidProperties(withBirthDate(TODAY_UTC))).containsExactly("birthDate");
	}

	@Test
	void birthDateInTheFutureIsInvalid() {
		assertThat(invalidProperties(withBirthDate(TODAY_UTC.plusDays(1)))).containsExactly("birthDate");
	}

	@Test
	void clockBeanIsUtc() {
		assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}

	// --- city (CUST-48)

	@Test
	void cityMissingIsInvalid() {
		assertThat(invalidProperties(withCity(null))).containsExactly("city");
	}

	@Test
	void cityBlankIsInvalid() {
		assertThat(invalidProperties(withCity("   "))).containsExactly("city");
	}

	@Test
	void cityWithTwoCharactersIsValid() {
		assertThat(invalidProperties(withCity("Ib"))).isEmpty();
	}

	@Test
	void cityWithOneCharacterAfterNormalizationIsInvalid() {
		assertThat(invalidProperties(withCity("  I  "))).containsExactly("city");
	}

	@Test
	void cityWith100CharactersIsValid() {
		assertThat(invalidProperties(withCity("c".repeat(100)))).isEmpty();
	}

	@Test
	void cityWith101CharactersIsInvalid() {
		assertThat(invalidProperties(withCity("c".repeat(101)))).containsExactly("city");
	}

	// --- state (CUST-49)

	@Test
	void stateMissingIsInvalid() {
		assertThat(invalidProperties(withState(null))).containsExactly("state");
	}

	@Test
	void stateNotABrazilianUfIsInvalid() {
		assertThat(invalidProperties(withState("XX"))).containsExactly("state");
	}

	@ParameterizedTest
	@ValueSource(strings = { "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB",
			"PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO" })
	void everyBrazilianUfIsValidInAnyCase(String uf) {
		assertThat(invalidProperties(withState(uf))).isEmpty();
		assertThat(invalidProperties(withState(uf.toLowerCase()))).isEmpty();
	}

	// --- helpers

	private static Set<String> invalidProperties(CustomerRequest request) {
		return validator.validate(request)
			.stream()
			.map(violation -> violation.getPropertyPath().toString())
			.collect(Collectors.toSet());
	}

	private static String emailOfLength(int length) {
		// 64-char local part, labels of at most 63 chars: all valid for @Email, so only the length matters.
		String local = "l".repeat(64);
		String domainPrefix = "a".repeat(63) + "." + "b".repeat(63) + ".";
		int lastLabel = length - local.length() - 1 - domainPrefix.length() - ".com".length();
		return local + "@" + domainPrefix + "c".repeat(lastLabel) + ".com";
	}

	private static CustomerRequest valid() {
		return new CustomerRequest("Ana Souza", "ana@example.com", "52998224725", "11987654321",
				LocalDate.of(1990, 5, 20), "São Paulo", "SP");
	}

	private static CustomerRequest withName(String name) {
		CustomerRequest v = valid();
		return new CustomerRequest(name, v.email(), v.cpf(), v.phone(), v.birthDate(), v.city(), v.state());
	}

	private static CustomerRequest withEmail(String email) {
		CustomerRequest v = valid();
		return new CustomerRequest(v.name(), email, v.cpf(), v.phone(), v.birthDate(), v.city(), v.state());
	}

	private static CustomerRequest withCpf(String cpf) {
		CustomerRequest v = valid();
		return new CustomerRequest(v.name(), v.email(), cpf, v.phone(), v.birthDate(), v.city(), v.state());
	}

	private static CustomerRequest withPhone(String phone) {
		CustomerRequest v = valid();
		return new CustomerRequest(v.name(), v.email(), v.cpf(), phone, v.birthDate(), v.city(), v.state());
	}

	private static CustomerRequest withBirthDate(LocalDate birthDate) {
		CustomerRequest v = valid();
		return new CustomerRequest(v.name(), v.email(), v.cpf(), v.phone(), birthDate, v.city(), v.state());
	}

	private static CustomerRequest withCity(String city) {
		CustomerRequest v = valid();
		return new CustomerRequest(v.name(), v.email(), v.cpf(), v.phone(), v.birthDate(), city, v.state());
	}

	private static CustomerRequest withState(String state) {
		CustomerRequest v = valid();
		return new CustomerRequest(v.name(), v.email(), v.cpf(), v.phone(), v.birthDate(), v.city(), state);
	}

}
