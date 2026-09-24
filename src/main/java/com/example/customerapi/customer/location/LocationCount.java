package com.example.customerapi.customer.location;

/**
 * One aggregate row of the location grouping: the customers of one city group within one state.
 */
public interface LocationCount {

	String getState();

	String getCity();

	long getTotal();

}
