package archfixtures.customer.adapter.in.web;

import archfixtures.customer.application.port.out.FixturePort;

/** Violates ARCH-07: the web adapter reaches an output port. */
public class ControllerUsingOutputPort {

	private FixturePort port;

}
