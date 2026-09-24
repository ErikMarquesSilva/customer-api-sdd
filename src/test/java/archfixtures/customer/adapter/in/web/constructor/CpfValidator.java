package archfixtures.customer.adapter.in.web.constructor;

import com.example.customerapi.customer.domain.Cpf;

/** Violates ARCH-03: calls the domain rule but decides with an algorithm hidden in another class's constructor. */
public class CpfValidator {

	public boolean isValid(String value) {
		Cpf.isValid(value);
		return new CopiedCpfCheck(value).valid;
	}

}
