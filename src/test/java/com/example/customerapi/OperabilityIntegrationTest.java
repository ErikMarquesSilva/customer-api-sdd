package com.example.customerapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class OperabilityIntegrationTest extends HttpIntegrationTestSupport {

	private static final Map<String, Object> PII_CUSTOMER = with(with(with(validCustomer(), "email",
			"pii.check@example.com"), "cpf", "390.533.447-05"), "phone", "+5511955554444");

	private static final Map<String, Object> PII_UPDATE = with(with(with(validCustomer(), "email",
			"pii.update@example.com"), "cpf", "153.509.460-56"), "phone", "21966667777");

	// --- CUST-43

	@Test
	void healthReturns200WithStatusUp() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
	}

	// --- CUST-44

	@ParameterizedTest
	@ValueSource(strings = { "/actuator/env", "/actuator/beans" })
	void actuatorEndpointsOtherThanHealthAreNotExposed(String path) throws Exception {
		mockMvc.perform(get(path)).andExpect(status().isNotFound());
	}

	@Test
	void actuatorDiscoveryListsOnlyHealth() throws Exception {
		String body = mockMvc.perform(get("/actuator")).andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		Map<String, Object> links = JsonPath.read(body, "$._links");
		assertThat(links.keySet()).containsExactlyInAnyOrder("self", "health", "health-path");
	}

	// --- CUST-45, CUST-46

	@Test
	void createLogsOneInfoLineWithIdAndNoPii(CapturedOutput output) throws Exception {
		UUID id = createCustomer(PII_CUSTOMER);

		assertSingleInfoLine(output, "Customer created id=" + id);
		assertNoPii(output, PII_CUSTOMER);
	}

	@Test
	void updateLogsOneInfoLineWithIdAndNoPii(CapturedOutput output) throws Exception {
		UUID id = createCustomer(PII_CUSTOMER);

		mockMvc.perform(put(CUSTOMERS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(json(PII_UPDATE)))
			.andExpect(status().isOk());

		assertSingleInfoLine(output, "Customer updated id=" + id);
		assertNoPii(output, PII_CUSTOMER);
		assertNoPii(output, PII_UPDATE);
	}

	@Test
	void deleteLogsOneInfoLineWithIdAndNoPii(CapturedOutput output) throws Exception {
		UUID id = createCustomer(PII_CUSTOMER);

		mockMvc.perform(delete(CUSTOMERS + "/" + id)).andExpect(status().isNoContent());

		assertSingleInfoLine(output, "Customer deleted id=" + id);
		assertNoPii(output, PII_CUSTOMER);
	}

	@Test
	void emailUniqueConstraintRaceLogsNoPii(CapturedOutput output) throws Exception {
		createCustomer(PII_CUSTOMER);
		doReturn(false).when(repository).existsByEmail(anyString());

		postCustomer(with(PII_CUSTOMER, "cpf", "11144477735")).andExpect(status().isConflict());

		assertNoPii(output, PII_CUSTOMER);
	}

	@Test
	void cpfUniqueConstraintRaceLogsNoPii(CapturedOutput output) throws Exception {
		createCustomer(PII_CUSTOMER);
		doReturn(false).when(repository).existsByCpf(anyString());

		postCustomer(with(PII_CUSTOMER, "email", "other.pii@example.com")).andExpect(status().isConflict());

		assertNoPii(output, PII_CUSTOMER);
	}

	private static void assertSingleInfoLine(CapturedOutput output, String message) {
		List<String> lines = output.getOut().lines().filter(line -> line.contains(message)).toList();
		assertThat(lines).hasSize(1);
		assertThat(lines.getFirst()).contains(" INFO ").contains("customer.CustomerService");
	}

	private static void assertNoPii(CapturedOutput output, Map<String, Object> customer) {
		String cpf = (String) customer.get("cpf");
		String phone = (String) customer.get("phone");
		assertThat(output.getAll()).doesNotContain((String) customer.get("email"), cpf,
				cpf.replace(".", "").replace("-", ""), phone, phone.replace("+", ""));
	}

}
