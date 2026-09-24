package com.example.customerapi.customer.domain;

import java.util.UUID;

/**
 * Another request changed the customer after it was read; the earlier write wins.
 */
public class ConcurrentCustomerUpdateException extends RuntimeException {

	public ConcurrentCustomerUpdateException(UUID id) {
		super("Customer " + id + " was changed by another request.");
	}

}
