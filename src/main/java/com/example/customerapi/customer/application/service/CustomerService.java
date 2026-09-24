package com.example.customerapi.customer.application.service;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.customerapi.customer.application.port.in.CreateCustomerUseCase;
import com.example.customerapi.customer.application.port.in.DeleteCustomerUseCase;
import com.example.customerapi.customer.application.port.in.GetCustomerUseCase;
import com.example.customerapi.customer.application.port.in.ListCustomersUseCase;
import com.example.customerapi.customer.application.port.in.UpdateCustomerUseCase;
import com.example.customerapi.customer.application.port.out.CustomerPersistencePort;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;
import com.example.customerapi.customer.domain.CustomerFilter;
import com.example.customerapi.customer.domain.CustomerNotFoundException;
import com.example.customerapi.customer.domain.DuplicateFieldException;

/**
 * Customer use cases. Logs only the operation and the id, never email, cpf or phone.
 */
@Service
@Transactional
public class CustomerService implements CreateCustomerUseCase, GetCustomerUseCase, UpdateCustomerUseCase,
		DeleteCustomerUseCase, ListCustomersUseCase {

	private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

	private final CustomerPersistencePort persistence;

	private final Clock clock;

	public CustomerService(CustomerPersistencePort persistence, Clock clock) {
		this.persistence = persistence;
		this.clock = clock;
	}

	@Override
	public Customer create(CustomerDetails details) {
		if (persistence.existsByEmail(details.email())) {
			throw new DuplicateFieldException("email");
		}
		if (persistence.existsByCpf(details.cpf())) {
			throw new DuplicateFieldException("cpf");
		}
		Customer created = persistence.insert(Customer.register(details, clock.instant()));
		log.info("Customer created id={}", created.getId());
		return created;
	}

	@Override
	@Transactional(readOnly = true)
	public Customer get(UUID id) {
		return find(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<Customer> list(CustomerFilter filter, int page, int size, String sort) {
		return persistence.search(filter, PageRequest.of(page, size, CustomerSort.parse(sort)));
	}

	@Override
	public Customer update(UUID id, CustomerDetails details) {
		Customer current = find(id);
		if (persistence.existsByEmailAndIdNot(details.email(), id)) {
			throw new DuplicateFieldException("email");
		}
		if (persistence.existsByCpfAndIdNot(details.cpf(), id)) {
			throw new DuplicateFieldException("cpf");
		}
		Customer updated = persistence.update(current.update(details, clock.instant()));
		log.info("Customer updated id={}", id);
		return updated;
	}

	@Override
	public void delete(UUID id) {
		find(id);
		persistence.delete(id);
		log.info("Customer deleted id={}", id);
	}

	private Customer find(UUID id) {
		return persistence.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
	}

}
