package com.example.customerapi.customer.adapter.in.web;

import com.example.customerapi.customer.domain.CityLocation;

public record CityGroup(String city, long totalCustomers) {

	static CityGroup from(CityLocation city) {
		return new CityGroup(city.city(), city.totalCustomers());
	}

}
