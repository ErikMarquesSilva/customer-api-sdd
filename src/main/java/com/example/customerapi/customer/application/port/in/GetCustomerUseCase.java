package com.example.customerapi.customer.application.port.in;

import java.util.UUID;

import com.example.customerapi.customer.domain.Customer;

public interface GetCustomerUseCase {

	/** Fails with {@code CustomerNotFoundException} for an unknown id. */
	Customer get(UUID id);

}
