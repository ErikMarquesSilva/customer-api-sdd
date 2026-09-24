package com.example.customerapi.customer.adapter.in.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Bean Validation adapter for {@link Cpf}; the rule itself lives in the domain (ARCH-03).
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		// null is valid here; @NotBlank reports a missing cpf.
		return value == null || com.example.customerapi.customer.domain.Cpf.isValid(value);
	}

}
