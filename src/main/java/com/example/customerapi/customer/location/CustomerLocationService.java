package com.example.customerapi.customer.location;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.customerapi.customer.CustomerRepository;

/**
 * Builds the customer counts per state and city from one aggregate query; no customer entity is loaded.
 */
@Service
public class CustomerLocationService {

	/** pt-BR order ignoring case and accents; plain string order breaks ties so the result is deterministic. */
	private static final Comparator<CityGroup> CITY_ORDER;

	static {
		Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
		collator.setStrength(Collator.PRIMARY);
		CITY_ORDER = Comparator.comparing(CityGroup::city, collator).thenComparing(CityGroup::city);
	}

	private final CustomerRepository repository;

	public CustomerLocationService(CustomerRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public LocationGroupingResponse groupByLocation() {
		Map<String, List<CityGroup>> citiesByState = new TreeMap<>();
		for (LocationCount row : repository.countByLocation()) {
			citiesByState.computeIfAbsent(row.getState(), state -> new ArrayList<>())
				.add(new CityGroup(row.getCity(), row.getTotal()));
		}
		List<StateGroup> states = citiesByState.entrySet().stream().map(entry -> {
			List<CityGroup> cities = entry.getValue().stream().sorted(CITY_ORDER).toList();
			long total = cities.stream().mapToLong(CityGroup::totalCustomers).sum();
			return new StateGroup(entry.getKey(), total, cities);
		}).toList();
		return new LocationGroupingResponse(states);
	}

}
