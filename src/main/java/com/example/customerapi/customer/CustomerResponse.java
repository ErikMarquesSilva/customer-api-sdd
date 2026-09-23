package com.example.customerapi.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

public record CustomerResponse(UUID id, String name, String email, String cpf, String phone, LocalDate birthDate,
		String city, String state, Instant createdAt, Instant updatedAt) {

	static CustomerResponse from(Customer customer) {
		CustomerDetails details = customer.getDetails();
		return new CustomerResponse(customer.getId(), details.name(), details.email(), details.cpf(), details.phone(),
				details.birthDate(), details.city(), details.state(), customer.getCreatedAt(), customer.getUpdatedAt());
	}

}
