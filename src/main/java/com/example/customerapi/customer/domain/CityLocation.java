package com.example.customerapi.customer.domain;

/**
 * Customers of one city group within a state.
 */
public record CityLocation(String city, long totalCustomers) {
}
