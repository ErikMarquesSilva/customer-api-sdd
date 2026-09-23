package archfixtures.customer.adapter.in.web.validation;

/** Violates ARCH-03: re-implements the CPF rule instead of delegating to the domain. */
public class CpfValidator {

	public boolean isValid(String value) {
		return value != null && value.matches("\\d{11}");
	}

}
