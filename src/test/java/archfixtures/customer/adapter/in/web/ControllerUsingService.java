package archfixtures.customer.adapter.in.web;

import archfixtures.customer.application.service.JpaAwareService;

/** Violates ARCH-07 (design): the web adapter depends on a service class instead of a use-case interface. */
public class ControllerUsingService {

	private JpaAwareService service;

}
