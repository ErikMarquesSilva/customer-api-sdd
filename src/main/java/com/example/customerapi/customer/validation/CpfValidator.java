package com.example.customerapi.customer.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		if (!value.matches("\\d{11}") || value.chars().distinct().count() == 1) {
			return false;
		}
		return checkDigit(value, 9) == value.charAt(9) - '0' && checkDigit(value, 10) == value.charAt(10) - '0';
	}

	private static int checkDigit(String cpf, int length) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += (cpf.charAt(i) - '0') * (length + 1 - i);
		}
		int remainder = sum * 10 % 11;
		return remainder == 10 ? 0 : remainder;
	}

}
