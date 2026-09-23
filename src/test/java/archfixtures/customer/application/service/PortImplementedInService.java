package archfixtures.customer.application.service;

import archfixtures.customer.application.port.out.FixturePort;

/** Violates ARCH-09: an output port implemented outside adapter.out. */
public class PortImplementedInService implements FixturePort {

	@Override
	public void call() {
	}

}
