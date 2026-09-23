package com.example.customerapi.customer.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A Brazilian CPF: 11 digits, not all equal, with valid check digits. {@code null} is valid.
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface Cpf {

	String message() default "must be a valid CPF";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
