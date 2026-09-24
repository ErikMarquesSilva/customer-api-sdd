package archfixtures.customer.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Violates the transactional use-case rule: SUPPORTS does not start a transaction. */
@Service
@Transactional(propagation = Propagation.SUPPORTS)
public class SupportsOnlyService {
}
