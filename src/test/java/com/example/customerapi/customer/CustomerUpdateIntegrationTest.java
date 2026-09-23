package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.customerapi.HttpIntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

class CustomerUpdateIntegrationTest extends HttpIntegrationTestSupport {

	@Autowired
	private PlatformTransactionManager transactionManager;

	private UUID anaId;

	/** Ana as stored (and as returned by GET) before each test. */
	private String anaBefore;

	@BeforeEach
	void createAna() throws Exception {
		anaId = createCustomer(validCustomer());
		anaBefore = getCustomer(anaId);
	}

	private static Map<String, Object> newData() {
		Map<String, Object> body = validCustomer();
		body.put("name", "Ana Lima");
		body.put("email", "ana.lima@example.com");
		body.put("cpf", "11144477735");
		body.put("phone", "+5521912345678");
		body.put("birthDate", "1991-06-21");
		body.put("city", "Rio de Janeiro");
		body.put("state", "RJ");
		return body;
	}

	private ResultActions putCustomer(UUID id, Map<String, Object> body) throws Exception {
		return mockMvc.perform(put(CUSTOMERS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json(body)));
	}

	private String getCustomer(UUID id) throws Exception {
		return mockMvc.perform(get(CUSTOMERS + "/" + id))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private void assertAnaUnchanged() throws Exception {
		assertThat(jsonMapper.readTree(getCustomer(anaId))).isEqualTo(jsonMapper.readTree(anaBefore));
	}

	// --- CUST-17, CUST-19

	@Test
	void putReplacesAllFieldsKeepsIdAndCreatedAtAndAdvancesUpdatedAt() throws Exception {
		String createdAt = JsonPath.read(anaBefore, "$.createdAt");

		String response = putCustomer(anaId, newData()).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(anaId.toString()))
			.andExpect(jsonPath("$.name").value("Ana Lima"))
			.andExpect(jsonPath("$.email").value("ana.lima@example.com"))
			.andExpect(jsonPath("$.cpf").value("11144477735"))
			.andExpect(jsonPath("$.phone").value("+5521912345678"))
			.andExpect(jsonPath("$.birthDate").value("1991-06-21"))
			.andExpect(jsonPath("$.city").value("Rio de Janeiro"))
			.andExpect(jsonPath("$.state").value("RJ"))
			.andExpect(jsonPath("$.createdAt").value(createdAt))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Instant updatedAt = Instant.parse(JsonPath.read(response, "$.updatedAt"));
		assertThat(updatedAt).isAfter(Instant.parse(createdAt));

		String stored = getCustomer(anaId);
		assertThat((String) JsonPath.read(stored, "$.name")).isEqualTo("Ana Lima");
		assertThat((String) JsonPath.read(stored, "$.email")).isEqualTo("ana.lima@example.com");
		assertThat((String) JsonPath.read(stored, "$.cpf")).isEqualTo("11144477735");
		assertThat((String) JsonPath.read(stored, "$.phone")).isEqualTo("+5521912345678");
		assertThat((String) JsonPath.read(stored, "$.birthDate")).isEqualTo("1991-06-21");
		assertThat((String) JsonPath.read(stored, "$.city")).isEqualTo("Rio de Janeiro");
		assertThat((String) JsonPath.read(stored, "$.state")).isEqualTo("RJ");
		assertThat((String) JsonPath.read(stored, "$.createdAt")).isEqualTo(createdAt);
		assertThat(repository.count()).isEqualTo(1);
	}

	// --- CUST-18

	@Test
	void putWithoutPhoneAndBirthDateStoresThemAsNull() throws Exception {
		putCustomer(anaId, without(without(validCustomer(), "phone"), "birthDate")).andExpect(status().isOk())
			.andExpect(jsonPath("$.phone").doesNotExist())
			.andExpect(jsonPath("$.birthDate").doesNotExist());

		CustomerDetails stored = stored(anaId).getDetails();
		assertThat(stored.phone()).isNull();
		assertThat(stored.birthDate()).isNull();
	}

	// --- CUST-13 applied to PUT

	@Test
	void putIgnoresIdAndCreatedAtFromTheBody() throws Exception {
		String createdAt = JsonPath.read(anaBefore, "$.createdAt");
		Map<String, Object> body = with(with(newData(), "id", UUID.randomUUID().toString()), "createdAt",
				"2000-01-01T00:00:00Z");

		putCustomer(anaId, body).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(anaId.toString()))
			.andExpect(jsonPath("$.createdAt").value(createdAt));
		assertThat(repository.count()).isEqualTo(1);
	}

	// --- CUST-20

	static Stream<Arguments> invalidFields() {
		return Stream.of(
				Arguments.of("name", " "),
				Arguments.of("email", "not-an-email"),
				Arguments.of("cpf", "111.111.111-11"),
				Arguments.of("phone", "12345"),
				Arguments.of("birthDate", LocalDate.now(ZoneOffset.UTC).toString()),
				Arguments.of("city", "X"),
				Arguments.of("state", "XX"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidFields")
	void putWithInvalidFieldReturns400AndLeavesCustomerUnchanged(String field, String value) throws Exception {
		putCustomer(anaId, with(newData(), field, value)).andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains(field)));

		assertAnaUnchanged();
	}

	@Test
	void putWithTextPlainBodyReturns415AndLeavesCustomerUnchanged() throws Exception {
		mockMvc.perform(put(CUSTOMERS + "/" + anaId).contentType(MediaType.TEXT_PLAIN).content(json(newData())))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

		assertAnaUnchanged();
	}

	// --- CUST-21, CUST-22

	@Test
	void putWithEmailOfAnotherCustomerReturns409AndLeavesCustomerUnchanged() throws Exception {
		createCustomer(validCustomer("Bruno Reis", 1));

		putCustomer(anaId, with(newData(), "email", " CUSTOMER1@example.com ")).andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("email")));

		assertAnaUnchanged();
	}

	@Test
	void putWithCpfOfAnotherCustomerReturns409AndLeavesCustomerUnchanged() throws Exception {
		createCustomer(validCustomer("Bruno Reis", 1));

		putCustomer(anaId, with(newData(), "cpf", cpf(1))).andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("cpf")));

		assertAnaUnchanged();
	}

	// --- CUST-12 on the update path: the database constraint catches what the pre-check missed

	@Test
	void putWithEmailOfAnotherCustomerPassingThePreCheckReturns409NotServerError() throws Exception {
		createCustomer(validCustomer("Bruno Reis", 1));
		preCheckMisses(r -> r.existsByEmailAndIdNot(anyString(), eq(anaId)));

		putCustomer(anaId, with(newData(), "email", "customer1@example.com")).andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("email")));

		assertWriteReachedTheDatabase();
		assertAnaUnchanged();
	}

	@Test
	void putWithCpfOfAnotherCustomerPassingThePreCheckReturns409NotServerError() throws Exception {
		createCustomer(validCustomer("Bruno Reis", 1));
		preCheckMisses(r -> r.existsByCpfAndIdNot(anyString(), eq(anaId)));

		putCustomer(anaId, with(newData(), "cpf", cpf(1))).andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[*].field").value(contains("cpf")));

		assertWriteReachedTheDatabase();
		assertAnaUnchanged();
	}

	@Test
	void putKeepingOwnEmailAndCpfIsNotAConflict() throws Exception {
		putCustomer(anaId, with(validCustomer(), "name", "Ana Maria Souza")).andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Ana Maria Souza"))
			.andExpect(jsonPath("$.email").value("ana@example.com"))
			.andExpect(jsonPath("$.cpf").value("52998224725"));
	}

	// --- CUST-23

	@Test
	void putUnknownIdReturns404AndCreatesNothing() throws Exception {
		UUID unknown = UUID.randomUUID();

		putCustomer(unknown, newData()).andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

		assertThat(repository.existsById(unknown)).isFalse();
		assertThat(repository.count()).isEqualTo(1);
		assertAnaUnchanged();
	}

	// --- CUST-24

	@Test
	void concurrentUpdateCommittedFirstMakesThisUpdateReturn409AndKeepsTheFirstCommit() throws Exception {
		// The service reads Ana, then checks email uniqueness. At that point another transaction commits its own
		// update of Ana, so this request's write carries a stale version.
		doAnswer(invocation -> {
			TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
			concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			concurrent.executeWithoutResult(status -> {
				Customer ana = persistence.findById(anaId).orElseThrow();
				persistence.update(ana.update(new CustomerDetails("First Writer", "ana@example.com", "52998224725",
						null, null, "Curitiba", "PR"), Instant.now()));
			});
			// Real answer for this data: nobody else uses the new email. The spy wraps a JDK proxy, so it
			// cannot call through.
			return false;
		}).when(repository).existsByEmailAndIdNot(anyString(), eq(anaId));

		assertProblem(putCustomer(anaId, newData()), 409, CUSTOMERS + "/" + anaId);

		String stored = getCustomer(anaId);
		assertThat((String) JsonPath.read(stored, "$.name")).isEqualTo("First Writer");
		assertThat((String) JsonPath.read(stored, "$.email")).isEqualTo("ana@example.com");
		assertThat((String) JsonPath.read(stored, "$.cpf")).isEqualTo("52998224725");
		assertThat((String) JsonPath.read(stored, "$.city")).isEqualTo("Curitiba");
		assertThat((String) JsonPath.read(stored, "$.state")).isEqualTo("PR");
	}

}
