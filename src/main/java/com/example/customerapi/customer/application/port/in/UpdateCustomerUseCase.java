package com.example.customerapi.customer.application.port.in;

import java.util.UUID;

import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;

public interface UpdateCustomerUseCase {

	/**
	 * Replaces the customer's details. Fails with {@code CustomerNotFoundException}, {@code DuplicateFieldException}
	 * or {@code ConcurrentCustomerUpdateException}.
	 */
	Customer update(UUID id, CustomerDetails details);

}
