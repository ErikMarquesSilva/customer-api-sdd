package archfixtures.customer.adapter.out.persistence;

import archfixtures.customer.adapter.in.web.ControllerUsingOutputPort;

/** Violates ARCH-08: the persistence adapter reaches the web adapter. */
public class PersistenceUsingController {

	private ControllerUsingOutputPort controller;

}
