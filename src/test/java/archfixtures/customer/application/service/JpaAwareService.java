package archfixtures.customer.application.service;

import jakarta.persistence.EntityManager;

/** Violates ARCH-04: the application uses JPA. */
public class JpaAwareService {

	private EntityManager entityManager;

}
