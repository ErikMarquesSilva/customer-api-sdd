package com.example.customerapi.customer;

public class InvalidSortException extends RuntimeException {

	public InvalidSortException(String sort) {
		super("Invalid sort '" + sort + "'. Use <property>,<asc|desc> with property name, email, createdAt or updatedAt.");
	}

}
