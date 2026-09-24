package com.example.customerapi.customer.application.port.in;

import java.util.UUID;

public interface DeleteCustomerUseCase {

	/**
	 * Fails with {@code CustomerNotFoundException} for an unknown id, or {@code ConcurrentCustomerUpdateException} when
	 * another request changed the customer after it was read.
	 */
	void delete(UUID id);

}
