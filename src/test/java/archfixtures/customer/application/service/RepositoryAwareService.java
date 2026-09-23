package archfixtures.customer.application.service;

import org.springframework.data.repository.CrudRepository;

/** Violates ARCH-06: a use case talks to a Spring Data repository instead of an output port. */
public class RepositoryAwareService {

	private CrudRepository<Object, Long> repository;

}
