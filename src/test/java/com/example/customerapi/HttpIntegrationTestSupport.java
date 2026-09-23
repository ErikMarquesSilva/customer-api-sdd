package com.example.customerapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.customerapi.customer.CustomerRepository;
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

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JsonMapper jsonMapper;

	@MockitoSpyBean
	protected CustomerRepository repository;

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
