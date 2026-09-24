package com.example.customerapi.customer;

import java.util.Set;

import org.springframework.data.domain.Sort;

/**
 * Parses {@code sort=<property>[,<asc|desc>]} against a whitelist. A missing direction means ascending. Default
 * {@code name,asc}; {@code id} is always appended as a tiebreaker so pages are stable.
 */
final class CustomerSort {

	private static final Set<String> ALLOWED = Set.of("name", "email", "createdAt", "updatedAt");

	private static final Sort TIEBREAKER = Sort.by(Sort.Direction.ASC, "id");

	private CustomerSort() {
	}

	static Sort parse(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.ASC, "name").and(TIEBREAKER);
		}
		String[] parts = sort.split(",", -1);
		if (parts.length > 2 || !ALLOWED.contains(parts[0])) {
			throw new InvalidSortException(sort);
		}
		String direction = parts.length == 2 ? parts[1] : "asc";
		Sort.Direction sortDirection = switch (direction) {
			case "asc" -> Sort.Direction.ASC;
			case "desc" -> Sort.Direction.DESC;
			default -> throw new InvalidSortException(sort);
		};
		return Sort.by(sortDirection, parts[0]).and(TIEBREAKER);
	}

}
