package com.example.customerapi.customer.domain;

import java.util.List;

/**
 * Customers of one state, in total and per city.
 */
public record StateLocation(String state, long totalCustomers, List<CityLocation> cities) {
}
