package com.example.customerapi.customer.domain;

/**
 * The number of customers in one city group of one state.
 */
public record LocationCount(String state, String city, long total) {
}
