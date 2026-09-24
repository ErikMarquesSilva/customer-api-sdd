package com.example.customerapi.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerResponse(UUID id, String name, String email, String cpf, String phone, LocalDate birthDate,
		String city, String state, Instant createdAt, Instant updatedAt) {

	static CustomerResponse from(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getCpf(),
				customer.getPhone(), customer.getBirthDate(), customer.getCity(), customer.getState(),
				customer.getCreatedAt(), customer.getUpdatedAt());
	}

}
