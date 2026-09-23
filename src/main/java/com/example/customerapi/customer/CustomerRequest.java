package com.example.customerapi.customer;

import java.time.LocalDate;
import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.example.customerapi.customer.domain.CustomerDetails;
import com.example.customerapi.customer.validation.Cpf;

/**
 * Body of POST and PUT. Values are normalized in the compact constructor so Bean Validation checks the
 * normalized form.
 */
public record CustomerRequest(
		@NotBlank @Size(min = 2, max = 120) String name,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Cpf String cpf,
		@Pattern(regexp = "^\\+?\\d{10,13}$") String phone,
		@Past LocalDate birthDate,
		@NotBlank @Size(min = 2, max = 100) String city,
		@NotBlank @Pattern(regexp = CustomerRequest.UFS, message = "must be a Brazilian state (UF)") String state) {

	static final String UFS = "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$";

	public CustomerRequest {
		name = name == null ? null : name.trim();
		email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
		cpf = cpf == null ? null : cpf.replace(".", "").replace("-", "");
		city = city == null ? null : city.trim().replaceAll("\\s+", " ");
		state = state == null ? null : state.trim().toUpperCase(Locale.ROOT);
	}

	/** The normalized fields as the domain's input; call only after validation passed. */
	public CustomerDetails toDetails() {
		return new CustomerDetails(name, email, cpf, phone, birthDate, city, state);
	}

}
