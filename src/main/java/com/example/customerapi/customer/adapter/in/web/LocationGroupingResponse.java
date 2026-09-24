package com.example.customerapi.customer.adapter.in.web;

import java.util.List;

import com.example.customerapi.customer.domain.StateLocation;

/** Body of {@code GET /api/v1/customers/grouped-by-location}. Holds counts only, no customer data. */
public record LocationGroupingResponse(List<StateGroup> states) {

	static LocationGroupingResponse from(List<StateLocation> states) {
		return new LocationGroupingResponse(states.stream().map(StateGroup::from).toList());
	}

}
