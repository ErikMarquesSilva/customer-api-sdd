package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.customerapi.HttpIntegrationTestSupport;

/**
 * The write use cases run inside one read-write transaction. CUST-24 relies on it: Hibernate's versioned UPDATE
 * detects a concurrent change only against the version read in the same transaction. The probe runs at a
 * persistence call made by the use case, so it catches any way of losing the transaction (class or method level,
 * a non-starting propagation, readOnly).
 */
class TransactionBoundaryIntegrationTest extends HttpIntegrationTestSupport {

	private record Observed(boolean active, boolean readOnly) {
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

		mockMvc
			.perform(put(CUSTOMERS + "/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(json(with(validCustomer(), "email", "ana.new@example.com"))))
			.andExpect(status().isOk());

		assertThat(observed.get()).isEqualTo(new Observed(true, false));
	}

	private static Observed currentTransaction() {
		return new Observed(TransactionSynchronizationManager.isActualTransactionActive(),
				TransactionSynchronizationManager.isCurrentTransactionReadOnly());
	}

}
