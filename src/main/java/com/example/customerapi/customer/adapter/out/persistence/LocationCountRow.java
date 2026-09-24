package com.example.customerapi.customer.adapter.out.persistence;

/**
 * Projection of one row of the native location query.
 */
interface LocationCountRow {

	String getState();

	String getCity();

	long getTotal();

}
