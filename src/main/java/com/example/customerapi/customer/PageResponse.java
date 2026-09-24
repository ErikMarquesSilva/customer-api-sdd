package com.example.customerapi.customer;

import java.util.List;

import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> content, PageMetadata page) {

	public record PageMetadata(int number, int size, long totalElements, int totalPages) {
	}

	static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(),
				new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
	}

}
