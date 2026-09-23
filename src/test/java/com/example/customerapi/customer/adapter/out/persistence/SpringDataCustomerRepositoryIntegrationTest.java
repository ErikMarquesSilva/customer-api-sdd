package com.example.customerapi.customer.adapter.out.persistence;

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
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringDataCustomerRepositoryIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

	@Autowired
	private SpringDataCustomerRepository repository;

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
		repository.saveAndFlush(newEntity(data("ana@example.com", "52998224725"), NOW));
		CustomerJpaEntity duplicate = newEntity(data("ana@example.com", "11144477735"), NOW);

		assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class)
			.cause()
			.isInstanceOfSatisfying(ConstraintViolationException.class,
					cve -> assertThat(cve.getConstraintName()).isEqualTo("uk_customer_email"));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void duplicateCpfViolatesCpfUniqueConstraint() {
		repository.saveAndFlush(newEntity(data("ana@example.com", "52998224725"), NOW));
		CustomerJpaEntity duplicate = newEntity(data("bruno@example.com", "52998224725"), NOW);

		assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class)
			.cause()
			.isInstanceOfSatisfying(ConstraintViolationException.class,
					cve -> assertThat(cve.getConstraintName()).isEqualTo("uk_customer_cpf"));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void existsByEmailAndCpfFindStoredValuesOnly() {
		repository.saveAndFlush(newEntity(data("ana@example.com", "52998224725"), NOW));

		assertThat(repository.existsByEmail("ana@example.com")).isTrue();
		assertThat(repository.existsByEmail("bruno@example.com")).isFalse();
		assertThat(repository.existsByCpf("52998224725")).isTrue();
		assertThat(repository.existsByCpf("11144477735")).isFalse();
	}

	@Test
	void existsByEmailAndIdNotIgnoresTheCustomerItself() {
		CustomerJpaEntity ana = repository.saveAndFlush(newEntity(data("ana@example.com", "52998224725"), NOW));
		CustomerJpaEntity bruno = repository.saveAndFlush(newEntity(data("bruno@example.com", "11144477735"), NOW));

		assertThat(repository.existsByEmailAndIdNot("ana@example.com", ana.getId())).isFalse();
		assertThat(repository.existsByEmailAndIdNot("ana@example.com", bruno.getId())).isTrue();
	}

	@Test
	void existsByCpfAndIdNotIgnoresTheCustomerItself() {
		CustomerJpaEntity ana = repository.saveAndFlush(newEntity(data("ana@example.com", "52998224725"), NOW));
		CustomerJpaEntity bruno = repository.saveAndFlush(newEntity(data("bruno@example.com", "11144477735"), NOW));

		assertThat(repository.existsByCpfAndIdNot("52998224725", ana.getId())).isFalse();
		assertThat(repository.existsByCpfAndIdNot("52998224725", bruno.getId())).isTrue();
	}

	private static CustomerDetails data(String email, String cpf) {
		return new CustomerDetails("Customer", email, cpf, "11987654321", LocalDate.of(1990, 5, 20), "São Paulo",
				"SP");
	}

	private static CustomerJpaEntity newEntity(CustomerDetails details, Instant createdAt) {
		return CustomerJpaEntity.from(Customer.register(details, createdAt));
	}

}
