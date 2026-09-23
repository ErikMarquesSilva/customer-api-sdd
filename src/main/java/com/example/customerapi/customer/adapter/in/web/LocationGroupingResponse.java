package com.example.customerapi.customer.adapter.in.web;

import java.util.List;

/** Body of {@code GET /api/v1/customers/grouped-by-location}. Holds counts only, no customer data. */
public record LocationGroupingResponse(List<StateGroup> states) {
}
