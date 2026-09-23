package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.customerapi.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CustomerRepositoryIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

	@Autowired
	private CustomerRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanDatabase() {
		repository.deleteAllInBatch();
	}

	@Test
	void flywayCreatesCustomerTableOnEmptyDatabase() {
		Integer appliedV1 = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
		Integer customerTables = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'customer'",
				Integer.class);

		assertThat(appliedV1).isEqualTo(1);
		assertThat(customerTables).isEqualTo(1);
	}

	@Test
	void duplicateEmailViolatesEmailUniqueConstraint() {
		repository.saveAndFlush(new Customer(data("ana@example.com", "52998224725"), NOW));
		Customer duplicate = new Customer(data("ana@example.com", "11144477735"), NOW);

		assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class)
			.cause()
			.isInstanceOfSatisfying(ConstraintViolationException.class,
					cve -> assertThat(cve.getConstraintName()).isEqualTo("uk_customer_email"));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void duplicateCpfViolatesCpfUniqueConstraint() {
		repository.saveAndFlush(new Customer(data("ana@example.com", "52998224725"), NOW));
		Customer duplicate = new Customer(data("bruno@example.com", "52998224725"), NOW);

		assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class)
			.cause()
			.isInstanceOfSatisfying(ConstraintViolationException.class,
					cve -> assertThat(cve.getConstraintName()).isEqualTo("uk_customer_cpf"));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void existsByEmailAndCpfFindStoredValuesOnly() {
		repository.saveAndFlush(new Customer(data("ana@example.com", "52998224725"), NOW));

		assertThat(repository.existsByEmail("ana@example.com")).isTrue();
		assertThat(repository.existsByEmail("bruno@example.com")).isFalse();
		assertThat(repository.existsByCpf("52998224725")).isTrue();
		assertThat(repository.existsByCpf("11144477735")).isFalse();
	}

	@Test
	void existsByEmailAndIdNotIgnoresTheCustomerItself() {
		Customer ana = repository.saveAndFlush(new Customer(data("ana@example.com", "52998224725"), NOW));
		Customer bruno = repository.saveAndFlush(new Customer(data("bruno@example.com", "11144477735"), NOW));

		assertThat(repository.existsByEmailAndIdNot("ana@example.com", ana.getId())).isFalse();
		assertThat(repository.existsByEmailAndIdNot("ana@example.com", bruno.getId())).isTrue();
	}

	@Test
	void existsByCpfAndIdNotIgnoresTheCustomerItself() {
		Customer ana = repository.saveAndFlush(new Customer(data("ana@example.com", "52998224725"), NOW));
		Customer bruno = repository.saveAndFlush(new Customer(data("bruno@example.com", "11144477735"), NOW));

		assertThat(repository.existsByCpfAndIdNot("52998224725", ana.getId())).isFalse();
		assertThat(repository.existsByCpfAndIdNot("52998224725", bruno.getId())).isTrue();
	}

	private static CustomerData data(String email, String cpf) {
		return new TestCustomerData("Customer", email, cpf, "11987654321", LocalDate.of(1990, 5, 20), "São Paulo",
				"SP");
	}

	private record TestCustomerData(String name, String email, String cpf, String phone, LocalDate birthDate,
			String city, String state) implements CustomerData {
	}

}
