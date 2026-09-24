package com.example.customerapi.customer.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataCustomerRepository
		extends JpaRepository<CustomerJpaEntity, UUID>, JpaSpecificationExecutor<CustomerJpaEntity> {

	boolean existsByEmail(String email);

	boolean existsByCpf(String cpf);

	boolean existsByEmailAndIdNot(String email, UUID id);

	boolean existsByCpfAndIdNot(String cpf, UUID id);

	/**
	 * Counts customers per state and city in one statement. Cities are grouped ignoring case but not accents; each
	 * group shows the spelling of its earliest-created customer (ties broken by id). Rows are unordered.
	 */
	@Query(nativeQuery = true, value = """
			SELECT c.state AS state,
			       (array_agg(c.city ORDER BY c.created_at, c.id))[1] AS city,
			       count(*) AS total
			FROM customer c
			GROUP BY c.state, lower(c.city)
			""")
	List<LocationCountRow> countByLocation();

}
