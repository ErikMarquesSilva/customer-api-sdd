package com.example.customerapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.customerapi.customer.adapter.out.persistence.SpringDataCustomerRepository;
import com.example.customerapi.customer.application.port.out.CustomerPersistencePort;
import com.example.customerapi.customer.domain.Customer;
import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.json.JsonMapper;

/**
 * Shared setup for HTTP integration tests. Every HTTP test class extends it so they all reuse one Spring context and
 * one PostgreSQL container. The repository is a spy that behaves like the real one unless a test stubs it; Mockito
 * resets the stubs after each test.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
public abstract class HttpIntegrationTestSupport {

	protected static final String CUSTOMERS = "/api/v1/customers";

	/** CUST-35: problem+json with type, title, status, detail and instance (the request path). */
	protected static void assertProblem(ResultActions result, int status, String path) throws Exception {
		result.andExpect(status().is(status))
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value(not(emptyOrNullString())))
			.andExpect(jsonPath("$.title").value(not(emptyOrNullString())))
			.andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.detail").value(not(emptyOrNullString())))
			.andExpect(jsonPath("$.instance").value(path));
	}

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JsonMapper jsonMapper;

	/** Spied so race tests can make a uniqueness pre-check miss (CUST-12, CUST-24). */
	@MockitoSpyBean
	protected SpringDataCustomerRepository repository;

	/** Reads stored state the way the application does, through the output port. */
	@Autowired
	protected CustomerPersistencePort persistence;

	/**
	 * Makes a uniqueness pre-check miss, as in a race with a concurrent request, then forgets the arrangement's
	 * repository calls so {@link #assertWriteReachedTheDatabase()} sees only the request under test. Usage:
	 * {@code preCheckMisses(r -> r.existsByEmail(anyString()))}.
	 */
	protected void preCheckMisses(Consumer<SpringDataCustomerRepository> preCheck) {
		preCheck.accept(doReturn(false).when(repository));
		clearInvocations(repository);
	}

	/**
	 * Race tests stub a uniqueness pre-check to miss. This proves the request then really tried to write, so its 409
	 * came from the database constraint and not from a pre-check the stub failed to disable.
	 */
	protected void assertWriteReachedTheDatabase() {
		assertThat(Mockito.mockingDetails(repository).getInvocations())
			.extracting(invocation -> invocation.getMethod().getName())
			.contains("saveAndFlush");
	}

	protected Customer stored(UUID id) {
		return persistence.findById(id).orElseThrow();
	}

	@BeforeEach
	void cleanDatabase() {
		repository.deleteAllInBatch();
	}

	/** A valid, already-normalized customer body. */
	protected static Map<String, Object> validCustomer() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", "Ana Souza");
		body.put("email", "ana@example.com");
		body.put("cpf", "52998224725");
		body.put("phone", "11987654321");
		body.put("birthDate", "1990-05-20");
		body.put("city", "São Paulo");
		body.put("state", "SP");
		return body;
	}

	/** A valid customer body whose email and cpf are unique per {@code seed}. */
	protected static Map<String, Object> validCustomer(String name, int seed) {
		Map<String, Object> body = validCustomer();
		body.put("name", name);
		body.put("email", "customer" + seed + "@example.com");
		body.put("cpf", cpf(seed));
		return body;
	}

	protected static Map<String, Object> with(Map<String, Object> body, String field, Object value) {
		Map<String, Object> copy = new LinkedHashMap<>(body);
		copy.put(field, value);
		return copy;
	}

	protected static Map<String, Object> without(Map<String, Object> body, String field) {
		Map<String, Object> copy = new LinkedHashMap<>(body);
		copy.remove(field);
		return copy;
	}

	protected String json(Object body) {
		return jsonMapper.writeValueAsString(body);
	}

	protected ResultActions postCustomer(Map<String, Object> body) throws Exception {
		return mockMvc.perform(post(CUSTOMERS).contentType(MediaType.APPLICATION_JSON).content(json(body)));
	}

	protected UUID createCustomer(Map<String, Object> body) throws Exception {
		String response = postCustomer(body).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return UUID.fromString(JsonPath.read(response, "$.id"));
	}

	/** A CPF with valid check digits derived from {@code seed} (0 to 999_999). */
	protected static String cpf(int seed) {
		String base = String.format("%09d", 100_000_000 + seed);
		int first = checkDigit(base, 10);
		int second = checkDigit(base + first, 11);
		return base + first + second;
	}

	private static int checkDigit(String digits, int startWeight) {
		int sum = 0;
		for (int i = 0; i < digits.length(); i++) {
			sum += (digits.charAt(i) - '0') * (startWeight - i);
		}
		int rest = (sum * 10) % 11;
		return rest == 10 ? 0 : rest;
	}

}
