package archfixtures.customer.application.service;

import org.springframework.web.bind.annotation.RestController;

/** Violates ARCH-10: a controller outside adapter.in.web. */
@RestController
public class RestControllerInService {
}
