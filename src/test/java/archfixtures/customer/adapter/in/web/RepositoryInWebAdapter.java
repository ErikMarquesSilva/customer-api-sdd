package archfixtures.customer.adapter.in.web;

import org.springframework.data.repository.CrudRepository;

/** Violates ARCH-10: a Spring Data repository outside adapter.out.persistence. */
public interface RepositoryInWebAdapter extends CrudRepository<Object, Long> {
}
