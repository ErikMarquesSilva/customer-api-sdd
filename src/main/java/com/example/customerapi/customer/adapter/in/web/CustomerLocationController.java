package com.example.customerapi.customer.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.customerapi.customer.application.port.in.GroupCustomersByLocationUseCase;

/**
 * HTTP binding only; the grouping rules live behind {@link GroupCustomersByLocationUseCase}. The literal path is more
 * specific than {@code /api/v1/customers/{id}}, so it is matched before any UUID conversion.
 */
@RestController
public class CustomerLocationController {

	private final GroupCustomersByLocationUseCase groupByLocation;

	public CustomerLocationController(GroupCustomersByLocationUseCase groupByLocation) {
		this.groupByLocation = groupByLocation;
	}

	@GetMapping("/api/v1/customers/grouped-by-location")
	public LocationGroupingResponse groupedByLocation() {
		return new LocationGroupingResponse(groupByLocation.groupByLocation()
			.stream()
			.map(state -> new StateGroup(state.state(), state.totalCustomers(),
					state.cities().stream().map(city -> new CityGroup(city.city(), city.totalCustomers())).toList()))
			.toList());
	}

}
