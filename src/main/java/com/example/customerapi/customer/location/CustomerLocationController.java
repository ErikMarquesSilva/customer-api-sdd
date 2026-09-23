package com.example.customerapi.customer.location;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP binding only; the grouping rules live in {@link CustomerLocationService}. The literal path is more specific
 * than {@code /api/v1/customers/{id}}, so it is matched before any UUID conversion.
 */
@RestController
public class CustomerLocationController {

	private final CustomerLocationService service;

	public CustomerLocationController(CustomerLocationService service) {
		this.service = service;
	}

	@GetMapping("/api/v1/customers/grouped-by-location")
	public LocationGroupingResponse groupedByLocation() {
		return service.groupByLocation();
	}

}
