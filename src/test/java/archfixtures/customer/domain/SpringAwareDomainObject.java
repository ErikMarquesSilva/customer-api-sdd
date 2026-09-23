package archfixtures.customer.domain;

import org.springframework.stereotype.Component;

/** Violates ARCH-01: the domain uses Spring. */
@Component
public class SpringAwareDomainObject {
}
