package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.example.customerapi.HttpIntegrationTestSupport;

class CustomerDeleteIntegrationTest extends HttpIntegrationTestSupport {

	// --- CUST-25

	@Test
	void deleteExistingCustomerReturns204WithEmptyBodyAndRemovesIt() throws Exception {
		UUID id = createCustomer(validCustomer());
		UUID other = createCustomer(validCustomer("Bruno Reis", 1));

		mockMvc.perform(delete(CUSTOMERS + "/" + id))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.existsById(other)).isTrue();
	}

	// --- CUST-26

	@Test
	void getAfterDeleteReturns404() throws Exception {
		UUID id = createCustomer(validCustomer());
		mockMvc.perform(delete(CUSTOMERS + "/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(get(CUSTOMERS + "/" + id))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

	// --- CUST-27

	@Test
	void emailAndCpfOfDeletedCustomerCanBeUsedByANewCustomer() throws Exception {
		UUID id = createCustomer(validCustomer());
		mockMvc.perform(delete(CUSTOMERS + "/" + id)).andExpect(status().isNoContent());

		postCustomer(with(validCustomer(), "name", "Ana Nova")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("ana@example.com"))
			.andExpect(jsonPath("$.cpf").value("52998224725"));
		assertThat(repository.count()).isEqualTo(1);
	}

	// --- CUST-28

	@Test
	void deleteUnknownIdReturns404AndDeletesNothing() throws Exception {
		UUID existing = createCustomer(validCustomer());

		mockMvc.perform(delete(CUSTOMERS + "/" + UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));

		assertThat(repository.existsById(existing)).isTrue();
	}

}
