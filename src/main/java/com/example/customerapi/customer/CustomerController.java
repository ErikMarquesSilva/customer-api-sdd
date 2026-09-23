package com.example.customerapi.customer;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * HTTP binding only; rules live in {@link CustomerService}, errors in the shared exception handler.
 */
@RestController
@RequestMapping(CustomerController.BASE_PATH)
public class CustomerController {

	static final String BASE_PATH = "/api/v1/customers";

	private final CustomerService service;

	public CustomerController(CustomerService service) {
		this.service = service;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
		CustomerResponse created = service.create(request);
		return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
	}

	@GetMapping("/{id}")
	public CustomerResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

}
