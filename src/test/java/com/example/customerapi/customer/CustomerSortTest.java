package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

class CustomerSortTest {

	@ParameterizedTest
	@NullAndEmptySource
	void absentSortDefaultsToNameAscendingWithIdTiebreaker(String sort) {
		assertThat(CustomerSort.parse(sort)).isEqualTo(Sort.by(Direction.ASC, "name").and(Sort.by(Direction.ASC, "id")));
	}

	@ParameterizedTest
	@CsvSource({ "name,asc,ASC", "name,desc,DESC", "email,asc,ASC", "email,desc,DESC", "createdAt,asc,ASC",
			"createdAt,desc,DESC", "updatedAt,asc,ASC", "updatedAt,desc,DESC" })
	void allowedPropertyAndDirectionAreUsedWithIdTiebreaker(String property, String direction, Direction expected) {
		assertThat(CustomerSort.parse(property + "," + direction))
			.isEqualTo(Sort.by(expected, property).and(Sort.by(Direction.ASC, "id")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "name", "email", "createdAt", "updatedAt" })
	void allowedPropertyWithoutDirectionIsAscendingWithIdTiebreaker(String property) {
		assertThat(CustomerSort.parse(property))
			.isEqualTo(Sort.by(Direction.ASC, property).and(Sort.by(Direction.ASC, "id")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "cpf,asc", "id,desc", "phone,asc", "version,asc", "cpf", "id" })
	void propertyOutsideWhitelistIsRejected(String sort) {
		assertThatThrownBy(() -> CustomerSort.parse(sort)).isInstanceOf(InvalidSortException.class);
	}

	@Test
	void unknownDirectionIsRejected() {
		assertThatThrownBy(() -> CustomerSort.parse("name,up")).isInstanceOf(InvalidSortException.class);
	}

}
