package com.example.customerapi.customer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.example.customerapi.HttpIntegrationTestSupport;

class CustomerListIntegrationTest extends HttpIntegrationTestSupport {

	/** Creates "Customer 01".."Customer 25" (emails c01..c25@example.com) in a shuffled order. */
	private void create25Customers() throws Exception {
		for (int i = 0; i < 25; i++) {
			int n = (i * 7) % 25 + 1;
			createCustomer(with(validCustomer(name(n), n), "email", email(n)));
		}
	}

	private static String name(int n) {
		return String.format("Customer %02d", n);
	}

	private static String email(int n) {
		return String.format("c%02d@example.com", n);
	}

	private static String[] names(int fromInclusive, int toInclusive) {
		return IntStream.rangeClosed(fromInclusive, toInclusive).mapToObj(CustomerListIntegrationTest::name)
			.toArray(String[]::new);
	}

	private ResultActions list(String... params) throws Exception {
		var request = get(CUSTOMERS);
		for (int i = 0; i < params.length; i += 2) {
			request.param(params[i], params[i + 1]);
		}
		return mockMvc.perform(request);
	}

	private void createAnaMarianaAndBruno() throws Exception {
		createCustomer(with(validCustomer("Ana Souza", 1), "email", "ana@x.com"));
		createCustomer(with(validCustomer("Mariana Lima", 2), "email", "mariana@x.com"));
		createCustomer(with(validCustomer("Bruno Reis", 3), "email", "bruno@x.com"));
	}

	// --- CUST-29

	@Test
	void defaultListReturnsFirst20SortedByNameWithPageMetadata() throws Exception {
		create25Customers();

		list().andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name").value(contains(names(1, 20))))
			.andExpect(jsonPath("$.page.number").value(0))
			.andExpect(jsonPath("$.page.size").value(20))
			.andExpect(jsonPath("$.page.totalElements").value(25))
			.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	// --- CUST-30

	@Test
	void secondPageReturnsRemaining5() throws Exception {
		create25Customers();

		list("page", "1").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name").value(contains(names(21, 25))))
			.andExpect(jsonPath("$.page.number").value(1))
			.andExpect(jsonPath("$.page.size").value(20))
			.andExpect(jsonPath("$.page.totalElements").value(25))
			.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@Test
	void pageAndSizeSelectThatPageWithThatSize() throws Exception {
		create25Customers();

		list("page", "2", "size", "10").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name").value(contains(names(21, 25))))
			.andExpect(jsonPath("$.page.number").value(2))
			.andExpect(jsonPath("$.page.size").value(10))
			.andExpect(jsonPath("$.page.totalPages").value(3));
	}

	@Test
	void size100IsAccepted() throws Exception {
		create25Customers();

		list("size", "100").andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(25)))
			.andExpect(jsonPath("$.page.size").value(100))
			.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	// --- CUST-31

	@Test
	void sortByEmailDescendingOrdersByEmailDescending() throws Exception {
		create25Customers();

		list("sort", "email,desc", "size", "5").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].email")
				.value(contains(email(25), email(24), email(23), email(22), email(21))));
	}

	// --- CUST-52

	@Test
	void sortWithoutDirectionOrdersAscendingByThatProperty() throws Exception {
		create25Customers();

		list("sort", "email", "size", "5").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].email")
				.value(contains(email(1), email(2), email(3), email(4), email(5))));
	}

	// --- CUST-32, CUST-33, CUST-53

	@ParameterizedTest(name = "{0}={1}")
	@CsvSource({ "page, -1", "size, 0", "size, 101", "sort, 'cpf,asc'", "sort, cpf", "sort, 'name,up'" })
	void outOfRangePagingOrNonWhitelistedSortReturns400(String param, String value) throws Exception {
		createAnaMarianaAndBruno();

		ResultActions result = list(param, value);

		assertProblem(result, 400, CUSTOMERS);
		result.andExpect(jsonPath("$.content").doesNotExist());
	}

	// --- CUST-34

	@Test
	void noMatchReturnsEmptyContentAndTotalElements0() throws Exception {
		createAnaMarianaAndBruno();

		list("name", "zzz").andExpect(status().isOk())
			.andExpect(jsonPath("$.content", empty()))
			.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	// --- CUST-40, CUST-41, CUST-42

	@Test
	void nameFilterReturnsCustomersWhoseNameContainsTheValueIgnoringCase() throws Exception {
		createAnaMarianaAndBruno();

		list("name", "ANA").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name").value(contains("Ana Souza", "Mariana Lima")))
			.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void emailFilterReturnsTheCustomerWithTheTrimmedEmailIgnoringCase() throws Exception {
		createAnaMarianaAndBruno();

		list("email", " ANA@X.COM ").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name").value(contains("Ana Souza")))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void nameAndEmailFiltersMustBothMatch() throws Exception {
		createAnaMarianaAndBruno();

		list("name", "ana", "email", "mariana@x.com").andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name").value(contains("Mariana Lima")));
		list("name", "bruno", "email", "ana@x.com").andExpect(status().isOk())
			.andExpect(jsonPath("$.content", empty()))
			.andExpect(jsonPath("$.page.totalElements").value(0));
	}

}
