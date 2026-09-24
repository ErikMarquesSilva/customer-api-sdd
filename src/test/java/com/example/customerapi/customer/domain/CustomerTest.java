package com.example.customerapi.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CustomerTest {

	private static final Instant CREATED = Instant.parse("2026-09-22T10:00:00Z");

	private static final Instant LATER = Instant.parse("2026-09-22T11:30:00Z");

	private static final CustomerDetails ANA = new CustomerDetails("Ana Souza", "ana@example.com", "52998224725",
			"11987654321", LocalDate.of(1990, 5, 20), "Campinas", "SP");

	private static final CustomerDetails ANA_MOVED = new CustomerDetails("Ana Lima", "ana.lima@example.com",
			"10000000108", null, null, "Curitiba", "PR");

	// CUST-01: a new customer gets an id and server timestamps.
	@Test
	void registerAssignsAnIdAndSetsBothTimestampsToNow() {
		Customer customer = Customer.register(ANA, CREATED);

		assertThat(customer.getId()).isNotNull();
		assertThat(customer.getDetails()).isEqualTo(ANA);
		assertThat(customer.getCreatedAt()).isEqualTo(CREATED);
		assertThat(customer.getUpdatedAt()).isEqualTo(CREATED);
		assertThat(customer.getVersion()).isZero();
	}

	@Test
	void registerAssignsADifferentIdEachTime() {
		assertThat(Customer.register(ANA, CREATED).getId()).isNotEqualTo(Customer.register(ANA, CREATED).getId());
	}

	// CUST-17, CUST-18, CUST-19: update replaces the details, keeps id, createdAt and version, and sets updatedAt.
	@Test
	void updateReplacesDetailsKeepsIdentityAndSetsUpdatedAt() {
		UUID id = UUID.randomUUID();
		Customer stored = Customer.restore(id, ANA, CREATED, CREATED, 3);

		Customer updated = stored.update(ANA_MOVED, LATER);

		assertThat(updated.getId()).isEqualTo(id);
		assertThat(updated.getDetails()).isEqualTo(ANA_MOVED);
		assertThat(updated.getDetails().phone()).isNull();
		assertThat(updated.getDetails().birthDate()).isNull();
		assertThat(updated.getCreatedAt()).isEqualTo(CREATED);
		assertThat(updated.getUpdatedAt()).isEqualTo(LATER);
		assertThat(updated.getVersion()).isEqualTo(3);
	}

	@Test
	void updateLeavesTheOriginalUnchanged() {
		Customer stored = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 0);

		stored.update(ANA_MOVED, LATER);

		assertThat(stored.getDetails()).isEqualTo(ANA);
		assertThat(stored.getUpdatedAt()).isEqualTo(CREATED);
	}

}
