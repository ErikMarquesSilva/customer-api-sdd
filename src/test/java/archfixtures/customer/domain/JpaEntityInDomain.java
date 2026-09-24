package archfixtures.customer.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Violates ARCH-02/ARCH-10: a JPA entity outside the persistence adapter. */
@Entity
public class JpaEntityInDomain {

	@Id
	private Long id;

}
