package com.example.customerapi.customer.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A registered customer. Immutable: {@link #update} returns a new instance. {@code version} is the optimistic
 * concurrency token; it changes only when storage accepts a write.
 */
public final class Customer {

	private final UUID id;

	private final CustomerDetails details;

	private final Instant createdAt;

	private final Instant updatedAt;

	private final long version;

	private Customer(UUID id, CustomerDetails details, Instant createdAt, Instant updatedAt, long version) {
		this.id = Objects.requireNonNull(id, "id");
		this.details = Objects.requireNonNull(details, "details");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
		this.version = version;
	}

	/** A new customer with a fresh id; both timestamps are {@code now}. */
	public static Customer register(CustomerDetails details, Instant now) {
		return new Customer(UUID.randomUUID(), details, now, now, 0);
	}

	/** Rebuilds a customer read from storage. */
	public static Customer restore(UUID id, CustomerDetails details, Instant createdAt, Instant updatedAt,
			long version) {
		return new Customer(id, details, createdAt, updatedAt, version);
	}

	/** The same customer with new details: id, creation time and version are kept, {@code updatedAt} is {@code now}. */
	public Customer update(CustomerDetails newDetails, Instant now) {
		return new Customer(id, newDetails, createdAt, now, version);
	}

	public UUID getId() {
		return id;
	}

	public CustomerDetails getDetails() {
		return details;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public long getVersion() {
		return version;
	}

}
