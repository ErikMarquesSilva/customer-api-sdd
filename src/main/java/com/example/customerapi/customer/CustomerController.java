package com.example.customerapi.customer;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.example.customerapi.customer.domain.CustomerFilter;

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

	/** Explicit paging parameters instead of {@code Pageable}, so out-of-range values fail with 400 (CUST-32). */
	@GetMapping
	public PageResponse<CustomerResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@RequestParam(required = false) String sort, @RequestParam(required = false) String name,
			@RequestParam(required = false) String email) {
		return service.list(new CustomerFilter(name, email), page, size, sort);
	}

	@GetMapping("/{id}")
	public CustomerResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		service.delete(id);
	}

}
