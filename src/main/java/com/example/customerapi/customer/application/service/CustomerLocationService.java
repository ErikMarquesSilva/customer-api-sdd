package com.example.customerapi.customer.application.service;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.customerapi.customer.application.port.in.GroupCustomersByLocationUseCase;
import com.example.customerapi.customer.application.port.out.LocationCountPort;
import com.example.customerapi.customer.domain.CityLocation;
import com.example.customerapi.customer.domain.LocationCount;
import com.example.customerapi.customer.domain.StateLocation;

/**
 * Builds the customer counts per state and city from one aggregate query; no customer is loaded.
 */
@Service
public class CustomerLocationService implements GroupCustomersByLocationUseCase {

	/** pt-BR order ignoring case and accents; plain string order breaks ties so the result is deterministic. */
	private static final Comparator<CityLocation> CITY_ORDER;

	static {
		Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
		collator.setStrength(Collator.PRIMARY);
		CITY_ORDER = Comparator.comparing(CityLocation::city, collator).thenComparing(CityLocation::city);
	}

	private final LocationCountPort locationCounts;

	public CustomerLocationService(LocationCountPort locationCounts) {
		this.locationCounts = locationCounts;
	}

	@Override
	@Transactional(readOnly = true)
	public List<StateLocation> groupByLocation() {
		Map<String, List<CityLocation>> citiesByState = new TreeMap<>();
		for (LocationCount row : locationCounts.countByLocation()) {
			citiesByState.computeIfAbsent(row.state(), state -> new ArrayList<>())
				.add(new CityLocation(row.city(), row.total()));
		}
		return citiesByState.entrySet().stream().map(entry -> {
			List<CityLocation> cities = entry.getValue().stream().sorted(CITY_ORDER).toList();
			long total = cities.stream().mapToLong(CityLocation::totalCustomers).sum();
			return new StateLocation(entry.getKey(), total, cities);
		}).toList();
	}

}
