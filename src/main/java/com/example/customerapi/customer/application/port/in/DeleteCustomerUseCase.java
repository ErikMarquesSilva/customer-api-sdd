package com.example.customerapi.customer.application.port.in;

import java.util.UUID;

public interface DeleteCustomerUseCase {

	/** Fails with {@code CustomerNotFoundException} for an unknown id. */
	void delete(UUID id);

}
