package com.example.customerapi.customer.domain;

import java.time.LocalDate;

/**
 * The client-editable fields of a customer, already normalized and validated at the edge.
 */
public record CustomerDetails(String name, String email, String cpf, String phone, LocalDate birthDate, String city,
		String state) {
}
