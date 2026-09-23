package com.example.customerapi.customer;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer business rules. Logs only the operation and the id, never email, cpf or phone.
 */
@Service
@Transactional
public class CustomerService {

	private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

	private final CustomerRepository repository;

	private final Clock clock;

	public CustomerService(CustomerRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public CustomerResponse create(CustomerRequest request) {
		if (repository.existsByEmail(request.email())) {
			throw new DuplicateFieldException("email");
		}
		if (repository.existsByCpf(request.cpf())) {
			throw new DuplicateFieldException("cpf");
		}
		Customer customer = repository.saveAndFlush(new Customer(request, clock.instant()));
		log.info("Customer created id={}", customer.getId());
		return CustomerResponse.from(customer);
	}

	@Transactional(readOnly = true)
	public CustomerResponse get(UUID id) {
		return CustomerResponse.from(find(id));
	}

	public CustomerResponse update(UUID id, CustomerRequest request) {
		Customer customer = find(id);
		if (repository.existsByEmailAndIdNot(request.email(), id)) {
			throw new DuplicateFieldException("email");
		}
		if (repository.existsByCpfAndIdNot(request.cpf(), id)) {
			throw new DuplicateFieldException("cpf");
		}
		customer.replaceWith(request, clock.instant());
		Customer updated = repository.saveAndFlush(customer);
		log.info("Customer updated id={}", id);
		return CustomerResponse.from(updated);
	}

	public void delete(UUID id) {
		repository.delete(find(id));
		log.info("Customer deleted id={}", id);
	}

	private Customer find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
	}

}
