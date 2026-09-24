package archfixtures.customer.application.service;

import org.springframework.stereotype.Service;

/** Violates the transactional use-case rule: a use-case service without a transaction boundary. */
@Service
public class NonTransactionalService {
}
