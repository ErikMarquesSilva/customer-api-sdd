package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.example.customerapi.HttpIntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

class CustomerCreateAndGetIntegrationTest extends HttpIntegrationTestSupport {

	// --- create: happy path (CUST-01, CUST-02)

	@Test
	void createReturns201WithCustomerAndLocationAndPersistsIt() throws Exception {
		MvcResult result = postCustomer(validCustomer()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Ana Souza"))
			.andExpect(jsonPath("$.email").value("ana@example.com"))
			.andExpect(jsonPath("$.cpf").value("52998224725"))
			.andExpect(jsonPath("$.phone").value("11987654321"))
			.andExpect(jsonPath("$.birthDate").value("1990-05-20"))
			.andExpect(jsonPath("$.city").value("São Paulo"))
			.andExpect(jsonPath("$.state").value("SP"))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		UUID id = UUID.fromString(JsonPath.read(body, "$.id"));
		Instant createdAt = Instant.parse(JsonPath.read(body, "$.createdAt"));
		Instant updatedAt = Instant.parse(JsonPath.read(body, "$.updatedAt"));
		assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/v1/customers/" + id);
		assertThat(updatedAt).isEqualTo(createdAt);

		CustomerDetails stored = stored(id).getDetails();
		assertThat(stored).extracting(CustomerDetails::name, CustomerDetails::email, CustomerDetails::cpf,
				CustomerDetails::phone, CustomerDetails::birthDate, CustomerDetails::city, CustomerDetails::state)
			.containsExactly("Ana Souza", "ana@example.com", "52998224725", "11987654321", LocalDate.of(1990, 5, 20),
					"São Paulo", "SP");
	}

	@Test
	void createWithoutOptionalFieldsReturnsNullPhoneAndBirthDate() throws Exception {
		postCustomer(without(without(validCustomer(), "phone"), "birthDate")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.phone").doesNotExist())
			.andExpect(jsonPath("$.birthDate").doesNotExist());
	}

	// --- create: normalization (CUST-03, CUST-04, CUST-50, CUST-51)

	@Test
	void createStoresAndReturnsNormalizedEmailCpfStateAndCity() throws Exception {
		Map<String, Object> body = with(with(with(with(validCustomer(), "email", " Ana@Example.COM "), "cpf",
				"529.982.247-25"), "state", "sp"), "city", "  São   Paulo ");

		String response = postCustomer(body).andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("ana@example.com"))
			.andExpect(jsonPath("$.cpf").value("52998224725"))
			.andExpect(jsonPath("$.state").value("SP"))
			.andExpect(jsonPath("$.city").value("São Paulo"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		CustomerDetails stored = stored(UUID.fromString(JsonPath.read(response, "$.id"))).getDetails();
		assertThat(stored)
			.extracting(CustomerDetails::email, CustomerDetails::cpf, CustomerDetails::state, CustomerDetails::city)
			.containsExactly("ana@example.com", "52998224725", "SP", "São Paulo");
	}

	// --- create: validation (CUST-05 to CUST-09, CUST-48, CUST-49)

	static Stream<Arguments> invalidFields() {
		String today = LocalDate.now(ZoneOffset.UTC).toString();
		String tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();
		return Stream.of(
				Arguments.of("name", "missing", null),
				Arguments.of("name", "blank", "   "),
				Arguments.of("name", "1 char after trim", " A "),
				Arguments.of("name", "121 chars", "A".repeat(121)),
				Arguments.of("email", "missing", null),
				Arguments.of("email", "not an address", "ana.example.com"),
				Arguments.of("email", "255 chars", "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "."
						+ "d".repeat(58) + ".com"),
				Arguments.of("cpf", "missing", null),
				Arguments.of("cpf", "10 digits", "5299822472"),
				Arguments.of("cpf", "all digits equal", "111.111.111-11"),
				Arguments.of("cpf", "wrong check digits", "52998224726"),
				Arguments.of("phone", "9 digits", "119876543"),
				Arguments.of("phone", "with punctuation", "(11) 98765-4321"),
				Arguments.of("birthDate", "today in UTC", today),
				Arguments.of("birthDate", "in the future", tomorrow),
				Arguments.of("city", "missing", null),
				Arguments.of("city", "blank", "   "),
				Arguments.of("city", "101 chars", "C".repeat(101)),
				Arguments.of("state", "missing", null),
				Arguments.of("state", "not a UF", "XX"));
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("invalidFields")
	void createWithInvalidFieldReturns400WithErrorForThatField(String field, String description, Object value)
			throws Exception {
		Map<String, Object> body = value == null ? without(validCustomer(), field) : with(validCustomer(), field, value);

		postCustomer(body).andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains(field)))
			.andExpect(jsonPath("$.errors[0].message").isNotEmpty());
		assertThat(repository.count()).isZero();
	}

	@Test
	void createAcceptsNameOf2And120CharactersAndBirthDateYesterdayInUtc() throws Exception {
		String yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString();

		postCustomer(with(validCustomer("Al", 1), "birthDate", yesterday))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Al"))
			.andExpect(jsonPath("$.birthDate").value(yesterday));
		postCustomer(validCustomer("B".repeat(120), 2)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("B".repeat(120)));
	}

	// --- create: uniqueness (CUST-10, CUST-11, CUST-12)

	@Test
	void createWithEmailOfAnotherCustomerDifferingOnlyByCaseAndSpacesReturns409() throws Exception {
		createCustomer(validCustomer());

		postCustomer(with(with(validCustomer(), "email", "  ANA@example.com "), "cpf", "11144477735"))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("email")));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void createWithCpfOfAnotherCustomerReturns409() throws Exception {
		createCustomer(validCustomer());

		postCustomer(with(with(validCustomer(), "email", "other@example.com"), "cpf", "529.982.247-25"))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("cpf")));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void emailUniqueConstraintViolationPassingThePreCheckReturns409NotServerError() throws Exception {
		createCustomer(validCustomer());
		preCheckMisses(r -> r.existsByEmail(anyString()));

		postCustomer(with(validCustomer(), "cpf", "11144477735")).andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("email")));
		assertWriteReachedTheDatabase();
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void cpfUniqueConstraintViolationPassingThePreCheckReturns409NotServerError() throws Exception {
		createCustomer(validCustomer());
		preCheckMisses(r -> r.existsByCpf(anyString()));

		postCustomer(with(validCustomer(), "email", "other@example.com")).andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("cpf")));
		assertWriteReachedTheDatabase();
		assertThat(repository.count()).isEqualTo(1);
	}

	// --- create: server-managed fields (CUST-13)

	@Test
	void createIgnoresIdCreatedAtAndUpdatedAtFromTheBody() throws Exception {
		UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		String clientInstant = "2000-01-01T00:00:00Z";
		Map<String, Object> body = with(with(with(validCustomer(), "id", clientId.toString()), "createdAt",
				clientInstant), "updatedAt", clientInstant);

		String response = postCustomer(body).andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(not(clientId.toString())))
			.andExpect(jsonPath("$.createdAt").value(not(clientInstant)))
			.andExpect(jsonPath("$.updatedAt").value(not(clientInstant)))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(repository.existsById(clientId)).isFalse();
		Customer stored = stored(UUID.fromString(JsonPath.read(response, "$.id")));
		assertThat(stored.getCreatedAt()).isAfter(Instant.parse(clientInstant));
	}

	// --- retrieve (CUST-14, CUST-15, CUST-16)

	@Test
	void getExistingCustomerReturns200WithThatCustomer() throws Exception {
		String created = postCustomer(validCustomer()).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String id = JsonPath.read(created, "$.id");

		String fetched = mockMvc.perform(get(CUSTOMERS + "/" + id))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(jsonMapper.readTree(fetched)).isEqualTo(jsonMapper.readTree(created));
	}

	@Test
	void getUnknownIdReturns404() throws Exception {
		mockMvc.perform(get(CUSTOMERS + "/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void getWithNonUuidIdReturns400() throws Exception {
		mockMvc.perform(get(CUSTOMERS + "/not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

}
