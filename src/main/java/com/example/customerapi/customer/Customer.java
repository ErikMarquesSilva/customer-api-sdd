package com.example.customerapi.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "customer")
public class Customer {

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

	protected Customer() {
	}

	public Customer(CustomerData data, Instant now) {
		this.id = UUID.randomUUID();
		apply(data);
		this.createdAt = now;
		this.updatedAt = now;
	}

	public void replaceWith(CustomerData data, Instant now) {
		apply(data);
		this.updatedAt = now;
	}

	private void apply(CustomerData data) {
		this.name = data.name();
		this.email = data.email();
		this.cpf = data.cpf();
		this.phone = data.phone();
		this.birthDate = data.birthDate();
		this.city = data.city();
		this.state = data.state();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getCpf() {
		return cpf;
	}

	public String getPhone() {
		return phone;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public String getCity() {
		return city;
	}

	public String getState() {
		return state;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
