package com.example.customerapi.customer.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerFilter;

/**
 * Customer storage. Writes translate storage failures into domain exceptions: a unique email or cpf conflict becomes
 * {@code DuplicateFieldException}, a stale version becomes {@code ConcurrentCustomerUpdateException}.
 */
public interface CustomerPersistencePort {

	Optional<Customer> findById(UUID id);

	Customer insert(Customer customer);

	/** Stores new details for an existing customer if its version is still the stored one. */
	Customer update(Customer customer);

	void delete(UUID id);

	boolean existsByEmail(String email);

	boolean existsByCpf(String cpf);

	boolean existsByEmailAndIdNot(String email, UUID id);

	boolean existsByCpfAndIdNot(String cpf, UUID id);

	Page<Customer> search(CustomerFilter filter, Pageable pageable);

}
