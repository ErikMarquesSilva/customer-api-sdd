package com.example.customerapi.customer.domain;

/**
 * A unique customer field is already used by another customer. The message never contains the value.
 */
public class DuplicateFieldException extends RuntimeException {

	private final String field;

	public DuplicateFieldException(String field) {
		super("Another customer already uses this " + field + ".");
		this.field = field;
	}

	public String getField() {
		return field;
	}

}
