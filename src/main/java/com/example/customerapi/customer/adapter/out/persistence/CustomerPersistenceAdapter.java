package com.example.customerapi.customer.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.example.customerapi.customer.application.port.out.CustomerPersistencePort;
import com.example.customerapi.customer.application.port.out.LocationCountPort;
import com.example.customerapi.customer.domain.ConcurrentCustomerUpdateException;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerFilter;
import com.example.customerapi.customer.domain.CustomerNotFoundException;
import com.example.customerapi.customer.domain.DuplicateFieldException;
import com.example.customerapi.customer.domain.LocationCount;

/**
 * PostgreSQL storage through Spring Data JPA. Translates storage failures into domain exceptions so no caller sees
 * JPA, Hibernate or Spring DAO types.
 */
@Component
class CustomerPersistenceAdapter implements CustomerPersistencePort, LocationCountPort {

	private static final Map<String, String> UNIQUE_CONSTRAINT_FIELDS = Map.of("uk_customer_email", "email",
			"uk_customer_cpf", "cpf");

	private final SpringDataCustomerRepository repository;

	CustomerPersistenceAdapter(SpringDataCustomerRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		return repository.findById(id).map(CustomerJpaEntity::toDomain);
	}

	@Override
	public Customer insert(Customer customer) {
		return write(customer.getId(), () -> repository.saveAndFlush(CustomerJpaEntity.from(customer)).toDomain());
	}

	/**
	 * Applies the new details to the stored row only if nobody changed it since {@code customer} was read. A version
	 * mismatch seen here, or detected by Hibernate's versioned UPDATE, means another write won (CUST-24).
	 */
	@Override
	public Customer update(Customer customer) {
		UUID id = customer.getId();
		CustomerJpaEntity entity = repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
		if (entity.getVersion() != customer.getVersion()) {
			throw new ConcurrentCustomerUpdateException(id);
		}
		entity.apply(customer);
		return write(id, () -> repository.saveAndFlush(entity).toDomain());
	}

	/** Flushes inside the adapter so a conflicting concurrent change is translated here, not at commit. */
	@Override
	public void delete(UUID id) {
		write(id, () -> {
			repository.deleteById(id);
			repository.flush();
			return null;
		});
	}

	@Override
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}

	@Override
	public boolean existsByCpf(String cpf) {
		return repository.existsByCpf(cpf);
	}

	@Override
	public boolean existsByEmailAndIdNot(String email, UUID id) {
		return repository.existsByEmailAndIdNot(email, id);
	}

	@Override
	public boolean existsByCpfAndIdNot(String cpf, UUID id) {
		return repository.existsByCpfAndIdNot(cpf, id);
	}

	@Override
	public Page<Customer> search(CustomerFilter filter, Pageable pageable) {
		return repository.findAll(CustomerSpecifications.matching(filter), pageable).map(CustomerJpaEntity::toDomain);
	}

	@Override
	public List<LocationCount> countByLocation() {
		return repository.countByLocation()
			.stream()
			.map(row -> new LocationCount(row.getState(), row.getCity(), row.getTotal()))
			.toList();
	}

	/**
	 * Runs a flushed write and translates storage failures: a stale version means another request changed the
	 * customer first (CUST-24); a unique constraint means a write passed the pre-check, e.g. two concurrent creates
	 * (CUST-12).
	 */
	private static <T> T write(UUID id, Supplier<T> write) {
		try {
			return write.get();
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new ConcurrentCustomerUpdateException(id);
		}
		catch (DataIntegrityViolationException ex) {
			String field = uniqueField(ex);
			if (field == null) {
				throw ex;
			}
			throw new DuplicateFieldException(field);
		}
	}

	private static String uniqueField(DataIntegrityViolationException ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
				return UNIQUE_CONSTRAINT_FIELDS.get(violation.getConstraintName());
			}
		}
		return null;
	}

}
