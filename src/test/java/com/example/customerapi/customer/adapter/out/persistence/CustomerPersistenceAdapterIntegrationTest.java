package com.example.customerapi.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.customerapi.TestcontainersConfiguration;
import com.example.customerapi.customer.domain.ConcurrentCustomerUpdateException;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;
import com.example.customerapi.customer.domain.CustomerNotFoundException;
import com.example.customerapi.customer.domain.DuplicateFieldException;
import com.example.customerapi.customer.domain.LocationCount;

/** The adapter's own duties: mapping, version checks and translation of storage failures (design.md). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CustomerPersistenceAdapterIntegrationTest {

	private static final Instant CREATED = Instant.parse("2026-01-15T10:00:00Z");

	private static final Instant LATER = Instant.parse("2026-01-15T11:00:00Z");

	private static final CustomerDetails ANA = new CustomerDetails("Ana Souza", "ana@example.com", "52998224725",
			"11987654321", LocalDate.of(1990, 5, 20), "Campinas", "SP");

	@Autowired
	private CustomerPersistenceAdapter adapter;

	@Autowired
	private SpringDataCustomerRepository repository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void cleanDatabase() {
		repository.deleteAllInBatch();
	}

	@Test
	void insertedCustomerReadsBackWithEveryField() {
		Customer ana = Customer.register(ANA, CREATED);

		adapter.insert(ana);
		Customer stored = adapter.findById(ana.getId()).orElseThrow();

		assertThat(stored.getId()).isEqualTo(ana.getId());
		assertThat(stored.getDetails()).isEqualTo(ANA);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isEqualTo(CREATED);
		assertThat(stored.getVersion()).isZero();
	}

	// CUST-12: the unique constraint surfaces as the domain's DuplicateFieldException, not a storage exception.
	@Test
	void insertWithTakenEmailThrowsDuplicateEmail() {
		adapter.insert(Customer.register(ANA, CREATED));
		Customer sameEmail = Customer.register(details("ana@example.com", "11144477735"), CREATED);

		assertThatThrownBy(() -> adapter.insert(sameEmail)).isInstanceOfSatisfying(DuplicateFieldException.class,
				ex -> assertThat(ex.getField()).isEqualTo("email"));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void insertWithTakenCpfThrowsDuplicateCpf() {
		adapter.insert(Customer.register(ANA, CREATED));
		Customer sameCpf = Customer.register(details("bruno@example.com", "52998224725"), CREATED);

		assertThatThrownBy(() -> adapter.insert(sameCpf)).isInstanceOfSatisfying(DuplicateFieldException.class,
				ex -> assertThat(ex.getField()).isEqualTo("cpf"));
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void updateStoresNewDetailsKeepsCreatedAtAndAdvancesTheVersion() {
		Customer ana = adapter.insert(Customer.register(ANA, CREATED));
		CustomerDetails moved = details("ana.lima@example.com", "11144477735");

		Customer updated = adapter.update(ana.update(moved, LATER));

		assertThat(updated.getVersion()).isEqualTo(ana.getVersion() + 1);
		Customer stored = adapter.findById(ana.getId()).orElseThrow();
		assertThat(stored.getDetails()).isEqualTo(moved);
		assertThat(stored.getCreatedAt()).isEqualTo(CREATED);
		assertThat(stored.getUpdatedAt()).isEqualTo(LATER);
	}

	// CUST-24: a write based on a version that is no longer stored loses; the stored data is kept.
	@Test
	void updateFromAStaleVersionThrowsConcurrentUpdateAndKeepsTheFirstWrite() {
		Customer read = adapter.insert(Customer.register(ANA, CREATED));
		CustomerDetails firstWrite = details("first@example.com", "11144477735");
		adapter.update(read.update(firstWrite, LATER));

		assertThatThrownBy(() -> adapter.update(read.update(details("second@example.com", "39053344705"), LATER)))
			.isInstanceOf(ConcurrentCustomerUpdateException.class);
		assertThat(adapter.findById(read.getId()).orElseThrow().getDetails()).isEqualTo(firstWrite);
	}

	@Test
	void updateOfAnUnknownCustomerThrowsNotFound() {
		Customer ghost = Customer.register(ANA, CREATED);

		assertThatThrownBy(() -> adapter.update(ghost)).isInstanceOf(CustomerNotFoundException.class);
		assertThat(repository.count()).isZero();
	}

	// Review finding: a new customer is written with one INSERT, not a SELECT (merge) followed by an INSERT.
	@Test
	void insertIssuesOnlyTheInsertStatement() {
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();

		adapter.insert(Customer.register(ANA, CREATED));

		assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
	}

	// Review finding (CUST-24 applied to DELETE): deleting a customer another request changed after it was read
	// is a concurrency conflict (409), not an unexpected error surfacing at commit (500).
	@Test
	void deleteOfACustomerChangedSinceItWasReadThrowsConcurrentUpdateAndKeepsTheOtherWrite() {
		Customer ana = adapter.insert(Customer.register(ANA, CREATED));
		CustomerDetails otherWrite = details("other@example.com", "11144477735");
		TransactionTemplate request = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> request.executeWithoutResult(status -> {
			adapter.findById(ana.getId()).orElseThrow();
			TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
			concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			concurrent.executeWithoutResult(inner -> adapter
				.update(adapter.findById(ana.getId()).orElseThrow().update(otherWrite, LATER)));
			adapter.delete(ana.getId());
		})).isInstanceOf(ConcurrentCustomerUpdateException.class);
		assertThat(adapter.findById(ana.getId()).orElseThrow().getDetails()).isEqualTo(otherWrite);
	}

	@Test
	void deleteRemovesTheRow() {
		Customer ana = adapter.insert(Customer.register(ANA, CREATED));

		adapter.delete(ana.getId());

		assertThat(adapter.findById(ana.getId())).isEmpty();
	}

	@Test
	void findByIdOfAnUnknownIdIsEmpty() {
		assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void locationCountsMapToDomainRecords() {
		adapter.insert(Customer.register(ANA, CREATED));
		adapter.insert(Customer.register(details("bruno@example.com", "11144477735"), LATER));

		assertThat(adapter.countByLocation()).containsExactly(new LocationCount("SP", "Campinas", 2));
	}

	private static CustomerDetails details(String email, String cpf) {
		return new CustomerDetails("Customer", email, cpf, null, null, "Campinas", "SP");
	}

}
