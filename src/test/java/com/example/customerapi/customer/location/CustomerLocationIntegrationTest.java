package com.example.customerapi.customer.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.ResultActions;

import com.example.customerapi.HttpIntegrationTestSupport;

import jakarta.persistence.EntityManagerFactory;

class CustomerLocationIntegrationTest extends HttpIntegrationTestSupport {

	private static final String GROUPED = CUSTOMERS + "/grouped-by-location";

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private final Map<UUID, Map<String, Object>> bodies = new HashMap<>();

	private int seed;

	// --- GEO-001 to GEO-006, GEO-008 (spec independent test)

	@Test
	void independentTestDataGivesExactGroupingWithoutCustomerFields() throws Exception {
		customerIn("Limeira", "SP");
		customerIn("Campinas", "SP");
		customerIn("Campinas", "SP");
		customerIn("Uberlândia", "MG");

		String body = grouped().andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"states": [
					  {"state": "MG", "totalCustomers": 1, "cities": [{"city": "Uberlândia", "totalCustomers": 1}]},
					  {"state": "SP", "totalCustomers": 3, "cities": [
					    {"city": "Campinas", "totalCustomers": 2},
					    {"city": "Limeira", "totalCustomers": 1}]}
					]}
					""", JsonCompareMode.STRICT))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).doesNotContain("\"id\"", "\"name\"", "\"email\"", "\"cpf\"", "\"phone\"", "\"birthDate\"",
				"\"createdAt\"", "\"updatedAt\"");
	}

	// --- GEO-007

	@Test
	void noCustomersGiveEmptyStates() throws Exception {
		grouped().andExpect(status().isOk()).andExpect(content().json("{\"states\": []}", JsonCompareMode.STRICT));
	}

	// --- GEO-009 to GEO-012 end to end

	@Test
	void citiesGroupIgnoringCaseButNotAccentsAndPerState() throws Exception {
		customerIn("Campinas", "SP");
		customerIn("campinas", "SP");
		customerIn("Uberlândia", "MG");
		customerIn("Uberlandia", "MG");
		customerIn("Santa Rita", "SP");
		customerIn("Santa Rita", "MG");

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [
				  {"state": "MG", "totalCustomers": 3, "cities": [
				    {"city": "Santa Rita", "totalCustomers": 1},
				    {"city": "Uberlandia", "totalCustomers": 1},
				    {"city": "Uberlândia", "totalCustomers": 1}]},
				  {"state": "SP", "totalCustomers": 3, "cities": [
				    {"city": "Campinas", "totalCustomers": 2},
				    {"city": "Santa Rita", "totalCustomers": 1}]}
				]}
				""", JsonCompareMode.STRICT));
	}

	// --- GEO-013

	@Test
	void deletedCustomerIsExcludedFromTheNextResponse() throws Exception {
		UUID deleted = customerIn("Campinas", "SP");
		customerIn("Campinas", "SP");
		customerIn("Limeira", "SP");

		mockMvc.perform(delete(CUSTOMERS + "/" + deleted)).andExpect(status().isNoContent());

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [{"state": "SP", "totalCustomers": 2, "cities": [
				  {"city": "Campinas", "totalCustomers": 1},
				  {"city": "Limeira", "totalCustomers": 1}]}]}
				""", JsonCompareMode.STRICT));
	}

	// --- GEO-014

	@Test
	void movedCustomerCountsOnlyUnderTheNewLocation() throws Exception {
		UUID movedToOtherState = customerIn("Campinas", "SP");
		customerIn("Campinas", "SP");
		UUID movedToOtherCity = customerIn("Campinas", "SP");

		move(movedToOtherState, "Curitiba", "PR");
		move(movedToOtherCity, "Limeira", "SP");

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [
				  {"state": "PR", "totalCustomers": 1, "cities": [{"city": "Curitiba", "totalCustomers": 1}]},
				  {"state": "SP", "totalCustomers": 2, "cities": [
				    {"city": "Campinas", "totalCustomers": 1},
				    {"city": "Limeira", "totalCustomers": 1}]}
				]}
				""", JsonCompareMode.STRICT));
	}

	// --- GEO-015 (spec independent test: move, then delete the last customer of the city and state)

	@Test
	void stateAndCityWithoutCustomersLeftAreOmitted() throws Exception {
		UUID moved = customerIn("Campinas", "SP");
		UUID deleted = customerIn("Campinas", "SP");

		move(moved, "Curitiba", "PR");
		mockMvc.perform(delete(CUSTOMERS + "/" + deleted)).andExpect(status().isNoContent());

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [{"state": "PR", "totalCustomers": 1, "cities": [{"city": "Curitiba", "totalCustomers": 1}]}]}
				""", JsonCompareMode.STRICT));
	}

	// --- GEO-015 (last of a city deleted, last of a state moved away)

	@Test
	void cityEmptiedByDeleteAndStateEmptiedByMoveAreOmitted() throws Exception {
		customerIn("Campinas", "SP");
		UUID lastInLimeira = customerIn("Limeira", "SP");
		UUID lastInMg = customerIn("Uberlândia", "MG");

		mockMvc.perform(delete(CUSTOMERS + "/" + lastInLimeira)).andExpect(status().isNoContent());
		move(lastInMg, "Campinas", "SP");

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [{"state": "SP", "totalCustomers": 2, "cities": [{"city": "Campinas", "totalCustomers": 2}]}]}
				""", JsonCompareMode.STRICT));
	}

	// --- GEO-016, GEO-017

	@Test
	void oneCallRunsOneStatementAndLoadsNoEntity() throws Exception {
		customerIn("Campinas", "SP");
		customerIn("Limeira", "SP");
		customerIn("Uberlândia", "MG");
		customerIn("Belo Horizonte", "MG");
		customerIn("Curitiba", "PR");
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		grouped().andExpect(status().isOk());

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
		assertThat(statistics.getEntityLoadCount()).isZero();
	}

	// --- Edge case: exactly one customer

	@Test
	void oneCustomerGivesOneStateWithOneCity() throws Exception {
		customerIn("Curitiba", "PR");

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [{"state": "PR", "totalCustomers": 1, "cities": [{"city": "Curitiba", "totalCustomers": 1}]}]}
				""", JsonCompareMode.STRICT));
	}

	// --- Edge case: all customers in one city

	@Test
	void allCustomersInOneCityGiveStateTotalEqualToCityTotal() throws Exception {
		customerIn("Campinas", "SP");
		customerIn("Campinas", "SP");
		customerIn("Campinas", "SP");

		grouped().andExpect(status().isOk()).andExpect(content().json("""
				{"states": [{"state": "SP", "totalCustomers": 3, "cities": [{"city": "Campinas", "totalCustomers": 3}]}]}
				""", JsonCompareMode.STRICT));
	}

	// --- Edge case: query parameters are ignored

	@Test
	void queryParametersAreIgnored() throws Exception {
		customerIn("Campinas", "SP");
		customerIn("Uberlândia", "MG");

		mockMvc.perform(get(GROUPED).param("state", "SP").param("city", "Campinas"))
			.andExpect(status().isOk())
			.andExpect(content().json("""
					{"states": [
					  {"state": "MG", "totalCustomers": 1, "cities": [{"city": "Uberlândia", "totalCustomers": 1}]},
					  {"state": "SP", "totalCustomers": 1, "cities": [{"city": "Campinas", "totalCustomers": 1}]}
					]}
					""", JsonCompareMode.STRICT));
	}

	private ResultActions grouped() throws Exception {
		return mockMvc.perform(get(GROUPED));
	}

	private UUID customerIn(String city, String state) throws Exception {
		seed++;
		Map<String, Object> body = location(validCustomer("Customer " + seed, seed), city, state);
		UUID id = createCustomer(body);
		bodies.put(id, body);
		return id;
	}

	/** Changes only the location through PUT, keeping every other field as created. */
	private void move(UUID id, String city, String state) throws Exception {
		mockMvc
			.perform(put(CUSTOMERS + "/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(json(location(bodies.get(id), city, state))))
			.andExpect(status().isOk());
	}

	private static Map<String, Object> location(Map<String, Object> body, String city, String state) {
		return with(with(body, "city", city), "state", state);
	}

}
