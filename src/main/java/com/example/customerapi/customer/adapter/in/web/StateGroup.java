package com.example.customerapi.customer.adapter.in.web;

import java.util.List;

public record StateGroup(String state, long totalCustomers, List<CityGroup> cities) {
}
