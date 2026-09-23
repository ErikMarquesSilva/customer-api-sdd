package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.customerapi.HttpIntegrationTestSupport;

/**
 * Create and update run inside one read-write transaction. CUST-24 relies on it: Hibernate's versioned UPDATE
 * detects a concurrent change only against the version read in the same transaction. Two checks: a probe at a
 * persistence call made by the use case catches a lost or read-only transaction (class or method level, any
 * non-starting propagation), and a transaction count catches a split one (e.g. the write in REQUIRES_NEW).
 */
class TransactionBoundaryIntegrationTest extends HttpIntegrationTestSupport {

	private record Observed(boolean active, boolean readOnly) {
	}

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void createCompletesExactlyOneTransaction() throws Exception {
		Statistics statistics = statistics();
		statistics.clear();

		postCustomer(validCustomer()).andExpect(status().isCreated());

		assertThat(statistics.getTransactionCount()).isEqualTo(1);
		assertThat(statistics.getSuccessfulTransactionCount()).isEqualTo(1);
	}

	@Test
	void updateCompletesExactlyOneTransaction() throws Exception {
		UUID id = createCustomer(validCustomer());
		Statistics statistics = statistics();
		statistics.clear();

		putEmail(id, "ana.new@example.com").andExpect(status().isOk());

		assertThat(statistics.getTransactionCount()).isEqualTo(1);
		assertThat(statistics.getSuccessfulTransactionCount()).isEqualTo(1);
	}

	@Test
	void createRunsInsideAReadWriteTransaction() throws Exception {
		AtomicReference<Observed> observed = new AtomicReference<>();
		doAnswer(invocation -> {
			observed.set(currentTransaction());
			return false; // true answer: the table is empty
		}).when(repository).existsByEmail(anyString());

		postCustomer(validCustomer()).andExpect(status().isCreated());

		assertThat(observed.get()).isEqualTo(new Observed(true, false));
	}

	@Test
	void updateRunsInsideAReadWriteTransaction() throws Exception {
		UUID id = createCustomer(validCustomer());
		AtomicReference<Observed> observed = new AtomicReference<>();
		doAnswer(invocation -> {
			observed.set(currentTransaction());
			return false; // true answer: no other customer uses the new email
		}).when(repository).existsByEmailAndIdNot(anyString(), eq(id));

		putEmail(id, "ana.new@example.com").andExpect(status().isOk());

		assertThat(observed.get()).isEqualTo(new Observed(true, false));
	}

	private ResultActions putEmail(UUID id, String email) throws Exception {
		return mockMvc.perform(put(CUSTOMERS + "/" + id).contentType(MediaType.APPLICATION_JSON)
			.content(json(with(validCustomer(), "email", email))));
	}

	private Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}

	private static Observed currentTransaction() {
		return new Observed(TransactionSynchronizationManager.isActualTransactionActive(),
				TransactionSynchronizationManager.isCurrentTransactionReadOnly());
	}

}
