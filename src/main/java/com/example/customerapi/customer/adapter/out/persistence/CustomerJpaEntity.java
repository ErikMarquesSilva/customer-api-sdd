package com.example.customerapi.customer.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

/**
 * Storage shape of a customer. Only the persistence adapter sees it; the rest of the application uses the domain
 * {@link Customer}.
 */
@Entity
@Table(name = "customer")
class CustomerJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(nullable = false, length = 11)
	private String cpf;

	@Column(length = 14)
	private String phone;

	private LocalDate birthDate;

	@Column(nullable = false, length = 100)
	private String city;

	@Column(nullable = false, length = 2)
	private String state;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected CustomerJpaEntity() {
	}

	static CustomerJpaEntity from(Customer customer) {
		CustomerJpaEntity entity = new CustomerJpaEntity();
		entity.id = customer.getId();
		entity.createdAt = customer.getCreatedAt();
		entity.version = customer.getVersion();
		entity.apply(customer);
		return entity;
	}

	/** Copies the editable details and {@code updatedAt}; id, createdAt and version stay as stored. */
	void apply(Customer customer) {
		CustomerDetails details = customer.getDetails();
		this.name = details.name();
		this.email = details.email();
		this.cpf = details.cpf();
		this.phone = details.phone();
		this.birthDate = details.birthDate();
		this.city = details.city();
		this.state = details.state();
		this.updatedAt = customer.getUpdatedAt();
	}

	Customer toDomain() {
		return Customer.restore(id, new CustomerDetails(name, email, cpf, phone, birthDate, city, state), createdAt,
				updatedAt, version);
	}

	UUID getId() {
		return id;
	}

	long getVersion() {
		return version;
	}

}
