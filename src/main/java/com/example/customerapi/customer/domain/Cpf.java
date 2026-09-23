package com.example.customerapi.customer.domain;

/**
 * The Brazilian CPF rule: 11 digits, not all equal, with both check digits valid.
 */
public final class Cpf {

	private Cpf() {
	}

	public static boolean isValid(String cpf) {
		if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
			return false;
		}
		return checkDigit(cpf, 9) == cpf.charAt(9) - '0' && checkDigit(cpf, 10) == cpf.charAt(10) - '0';
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
