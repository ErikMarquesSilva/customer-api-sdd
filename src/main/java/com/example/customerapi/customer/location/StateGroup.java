package com.example.customerapi.customer.location;

import java.util.List;

public record StateGroup(String state, long totalCustomers, List<CityGroup> cities) {
}
