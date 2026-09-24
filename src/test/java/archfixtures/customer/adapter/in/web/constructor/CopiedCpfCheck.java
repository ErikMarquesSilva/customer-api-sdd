package archfixtures.customer.adapter.in.web.constructor;

/** Helper that carries a copied CPF check in its constructor. */
class CopiedCpfCheck {

	final boolean valid;

	CopiedCpfCheck(String value) {
		this.valid = value != null && value.length() == 11;
	}

}
