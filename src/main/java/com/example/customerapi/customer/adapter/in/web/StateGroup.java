package com.example.customerapi.customer.adapter.in.web;

import java.util.List;

import com.example.customerapi.customer.domain.StateLocation;

public record StateGroup(String state, long totalCustomers, List<CityGroup> cities) {

	static StateGroup from(StateLocation state) {
		return new StateGroup(state.state(), state.totalCustomers(), state.cities().stream().map(CityGroup::from).toList());
	}

}
