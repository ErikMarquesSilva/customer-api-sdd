package com.example.customerapi.customer;

import java.time.LocalDate;

/**
 * The client-editable fields of a customer, already normalized.
 */
public interface CustomerData {

	String name();

	String email();

	String cpf();

	String phone();

	LocalDate birthDate();

	String city();

	String state();

}
