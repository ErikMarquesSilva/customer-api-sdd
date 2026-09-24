package com.example.customerapi.customer.application.port.out;

import java.util.List;

import com.example.customerapi.customer.domain.LocationCount;

public interface LocationCountPort {

	/**
	 * Customer counts per state and city group, computed by storage in one statement. Cities are grouped ignoring
	 * case but not accents, and each group shows the spelling of its earliest-created customer. Rows are unordered.
	 */
	List<LocationCount> countByLocation();

}
