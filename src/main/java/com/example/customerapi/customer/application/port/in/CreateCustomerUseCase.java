package com.example.customerapi.customer.application.port.in;

import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

public interface CreateCustomerUseCase {

	/** Registers a customer; fails with {@code DuplicateFieldException} when the email or cpf is taken. */
	Customer create(CustomerDetails details);

}
