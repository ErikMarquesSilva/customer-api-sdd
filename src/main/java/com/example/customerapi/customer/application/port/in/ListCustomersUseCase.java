package com.example.customerapi.customer.application.port.in;

import org.springframework.data.domain.Page;

import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerFilter;

public interface ListCustomersUseCase {

	/**
	 * One page of customers matching the filter. {@code sort} is {@code <property>[,<asc|desc>]} over name, email,
	 * createdAt or updatedAt; anything else fails with {@link InvalidSortException}.
	 */
	Page<Customer> list(CustomerFilter filter, int page, int size, String sort);

}
