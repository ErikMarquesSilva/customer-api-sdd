package com.example.customerapi.customer.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.customerapi.TestcontainersConfiguration;
import com.example.customerapi.customer.Customer;
import com.example.customerapi.customer.CustomerData;
import com.example.customerapi.customer.CustomerRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CustomerLocationQueryIntegrationTest {

	private static final Instant T1 = Instant.parse("2026-01-15T10:00:00Z");

	private static final Instant T2 = Instant.parse("2026-01-15T11:00:00Z");

	private static final Instant T3 = Instant.parse("2026-01-15T12:00:00Z");

	@Autowired
	private CustomerRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private int seed;

	@BeforeEach
	void cleanDatabase() {
		repository.deleteAllInBatch();
	}

	// --- GEO-003, GEO-009, GEO-011

	@Test
	void casesOfOneCityFormOneRowShowingTheEarliestCreatedSpelling() {
		// Saved out of creation order, so neither insertion order nor min/max of the spellings picks the answer.
		save("Campinas", "SP", T2);
		save("campinas", "SP", T1);
		save("belo horizonte", "MG", T3);
		save("Belo Horizonte", "MG", T1);
		save("BELO HORIZONTE", "MG", T2);

		assertThat(repository.countByLocation())
			.extracting(LocationCount::getState, LocationCount::getCity, LocationCount::getTotal)
			.containsExactlyInAnyOrder(tuple("SP", "campinas", 2L), tuple("MG", "Belo Horizonte", 3L));
	}

	// --- GEO-011 (ties on created_at broken by id)

	@Test
	void sameCreationInstantPicksTheSpellingOfTheLowestId() {
		insert("00000000-0000-0000-0000-000000000002", "Campinas", "SP", T1);
		insert("00000000-0000-0000-0000-000000000001", "campinas", "SP", T1);
		insert("00000000-0000-0000-0000-000000000004", "belo horizonte", "MG", T1);
		insert("00000000-0000-0000-0000-000000000003", "Belo Horizonte", "MG", T1);

		assertThat(repository.countByLocation())
			.extracting(LocationCount::getState, LocationCount::getCity, LocationCount::getTotal)
			.containsExactlyInAnyOrder(tuple("SP", "campinas", 2L), tuple("MG", "Belo Horizonte", 2L));
	}

	// --- GEO-012

	@Test
	void citiesDifferingByAccentsAreSeparateRows() {
		save("Uberlândia", "MG", T1);
		save("Uberlandia", "MG", T2);
		save("Uberlândia", "MG", T3);

		assertThat(repository.countByLocation())
			.extracting(LocationCount::getState, LocationCount::getCity, LocationCount::getTotal)
			.containsExactlyInAnyOrder(tuple("MG", "Uberlândia", 2L), tuple("MG", "Uberlandia", 1L));
	}

	// --- GEO-018: case folding of non-ASCII letters (needs a UTF-8 LC_CTYPE database)

	@Test
	void casesDifferingInNonAsciiLettersFormOneRow() {
		save("SÃO CARLOS", "SP", T2);
		save("São Carlos", "SP", T1);

		assertThat(repository.countByLocation())
			.extracting(LocationCount::getState, LocationCount::getCity, LocationCount::getTotal)
			.containsExactly(tuple("SP", "São Carlos", 2L));
	}

	// --- GEO-010

	@Test
	void sameCityNameInDifferentStatesIsOneRowPerState() {
		save("Santa Rita", "SP", T1);
		save("Santa Rita", "MG", T2);
		save("Santa Rita", "MG", T3);

		assertThat(repository.countByLocation())
			.extracting(LocationCount::getState, LocationCount::getCity, LocationCount::getTotal)
			.containsExactlyInAnyOrder(tuple("SP", "Santa Rita", 1L), tuple("MG", "Santa Rita", 2L));
	}

	// --- GEO-007 at the query level

	@Test
	void emptyTableGivesNoRows() {
		assertThat(repository.countByLocation()).isEmpty();
	}

	private void save(String city, String state, Instant createdAt) {
		seed++;
		repository.saveAndFlush(new Customer(new TestCustomerData("Customer " + seed, "customer" + seed + "@example.com",
				String.format("%011d", seed), null, LocalDate.of(1990, 5, 20), city, state), createdAt));
	}

	private void insert(String id, String city, String state, Instant createdAt) {
		seed++;
		jdbcTemplate.update("""
				insert into customer (id, name, email, cpf, city, state, created_at, updated_at, version)
				values (?, ?, ?, ?, ?, ?, ?, ?, 0)
				""", UUID.fromString(id), "Customer " + seed, "customer" + seed + "@example.com",
				String.format("%011d", seed), city, state, Timestamp.from(createdAt), Timestamp.from(createdAt));
	}

	private record TestCustomerData(String name, String email, String cpf, String phone, LocalDate birthDate,
			String city, String state) implements CustomerData {
	}

}
