package com.example.customerapi.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

final class CustomerSpecifications {

	private static final char ESCAPE = '\\';

	private CustomerSpecifications() {
	}

	/**
	 * Name contains the value ignoring case (LIKE wildcards in the value are literal), email equals the trimmed value
	 * ignoring case, both combined with AND.
	 */
	static Specification<Customer> matching(CustomerFilter filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (hasText(filter.name())) {
				String pattern = "%" + escapeLike(filter.name().toLowerCase(Locale.ROOT)) + "%";
				predicates.add(cb.like(cb.lower(root.get("name")), pattern, ESCAPE));
			}
			if (hasText(filter.email())) {
				predicates.add(cb.equal(root.get("email"), filter.email().trim().toLowerCase(Locale.ROOT)));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
