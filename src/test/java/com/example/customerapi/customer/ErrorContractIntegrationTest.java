package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.example.customerapi.HttpIntegrationTestSupport;

class ErrorContractIntegrationTest extends HttpIntegrationTestSupport {

	/** CUST-35: problem+json with type, title, status, detail and instance (the request path). */
	private static void assertProblem(ResultActions result, int status, String path) throws Exception {
		result.andExpect(status().is(status))
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value(not(emptyOrNullString())))
			.andExpect(jsonPath("$.title").value(not(emptyOrNullString())))
			.andExpect(jsonPath("$.status").value(status))
			.andExpect(jsonPath("$.detail").value(not(emptyOrNullString())))
			.andExpect(jsonPath("$.instance").value(path));
	}

	@Test
	void validationErrorIsProblemWithAllContractFields() throws Exception {
		assertProblem(postCustomer(without(validCustomer(), "name")), 400, CUSTOMERS);
	}

	@Test
	void blankNameAndCityYieldExactlyOneErrorEntryPerField() throws Exception {
		postCustomer(with(with(validCustomer(), "name", " "), "city", " ")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(2)))
			.andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "city")))
			.andExpect(jsonPath("$.errors[0].message").isNotEmpty())
			.andExpect(jsonPath("$.errors[1].message").isNotEmpty());
	}

	@Test
	void notFoundIsProblemWithAllContractFields() throws Exception {
		String path = CUSTOMERS + "/" + UUID.randomUUID();

		assertProblem(mockMvc.perform(get(path)), 404, path);
	}

	@Test
	void conflictIsProblemWithAllContractFields() throws Exception {
		createCustomer(validCustomer());

		assertProblem(postCustomer(with(validCustomer(), "cpf", "11144477735")), 409, CUSTOMERS);
	}

	@Test
	void invalidUuidIsProblemWithAllContractFields() throws Exception {
		String path = CUSTOMERS + "/123";

		assertProblem(mockMvc.perform(get(path)), 400, path);
	}

	@Test
	void malformedJsonReturns400Problem() throws Exception {
		ResultActions result = mockMvc
			.perform(post(CUSTOMERS).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Ana\", "));

		assertProblem(result, 400, CUSTOMERS);
		assertThat(repository.count()).isZero();
	}

	@Test
	void textPlainBodyReturns415Problem() throws Exception {
		ResultActions result = mockMvc
			.perform(post(CUSTOMERS).contentType(MediaType.TEXT_PLAIN).content(json(validCustomer())));

		assertProblem(result, 415, CUSTOMERS);
		assertThat(repository.count()).isZero();
	}

	@Test
	void unexpectedExceptionReturns500WithFixedDetailAndNoExceptionDetails() throws Exception {
		doThrow(new IllegalStateException("secret-internal-failure")).when(repository).findById(any());
		String path = CUSTOMERS + "/" + UUID.randomUUID();

		ResultActions result = mockMvc.perform(get(path));

		assertProblem(result, 500, path);
		result.andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
			.andExpect(jsonPath("$.trace").doesNotExist())
			.andExpect(jsonPath("$.exception").doesNotExist())
			.andExpect(jsonPath("$.message").doesNotExist());
		String body = result.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain("secret-internal-failure", "IllegalStateException", "at com.example");
	}

}
