package archfixtures.customer.adapter.in.web.ignoring;

import com.example.customerapi.customer.domain.Cpf;

/** Violates ARCH-03: calls the domain rule but decides with its own algorithm. */
public class CpfValidator {

	public boolean isValid(String value) {
		Cpf.isValid(value);
		return value != null && value.matches("\\d{11}");
	}

}
