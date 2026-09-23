package com.example.customerapi.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import com.example.customerapi.TestcontainersConfiguration;
import com.example.customerapi.customer.application.port.in.ListCustomersUseCase;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;
import com.example.customerapi.customer.domain.CustomerFilter;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CustomerSpecificationsIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

	@Autowired
	private SpringDataCustomerRepository repository;

	@Autowired
	private ListCustomersUseCase listCustomers;

	@BeforeEach
	void seed() {
		repository.deleteAllInBatch();
		save("Mariana Lima", "mariana@x.com", "11144477735");
		save("Bruno Reis", "bruno@x.com", "39053344705");
		save("Ana Souza", "ana@x.com", "52998224725");
		save("Promo a%b", "promo@x.com", "15350946056");
	}

	@Test
	void nameFilterMatchesContainedValueIgnoringCase() {
		assertThat(names(new CustomerFilter("ana", null))).containsExactly("Ana Souza", "Mariana Lima");
	}

	@Test
	void percentInNameFilterIsLiteralNotWildcard() {
		assertThat(names(new CustomerFilter("a%", null))).containsExactly("Promo a%b");
	}

	@Test
	void underscoreInNameFilterIsLiteralNotWildcard() {
		Page<Customer> result = listCustomers.list(new CustomerFilter("n_", null), 0, 20, null);

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}

	@Test
	void emailFilterMatchesTrimmedValueIgnoringCase() {
		assertThat(names(new CustomerFilter(null, " ANA@X.COM "))).containsExactly("Ana Souza");
	}

	@Test
	void nameAndEmailFiltersAreCombinedWithAnd() {
		assertThat(names(new CustomerFilter("ana", "mariana@x.com"))).containsExactly("Mariana Lima");
		assertThat(names(new CustomerFilter("bruno", "ana@x.com"))).isEmpty();
	}

	@Test
	void defaultListIsSortedByNameAndPagedWithMetadata() {
		Page<Customer> first = listCustomers.list(new CustomerFilter(null, null), 0, 3, null);
		Page<Customer> second = listCustomers.list(new CustomerFilter(null, null), 1, 3, null);

		assertThat(first.getContent()).extracting(customer -> customer.getDetails().name())
			.containsExactly("Ana Souza", "Bruno Reis", "Mariana Lima");
		assertThat(List.of(first.getNumber(), first.getSize(), first.getTotalElements(), first.getTotalPages()))
			.containsExactly(0, 3, 4L, 2);
		assertThat(second.getContent()).extracting(customer -> customer.getDetails().name()).containsExactly("Promo a%b");
		assertThat(List.of(second.getNumber(), second.getSize(), second.getTotalElements(), second.getTotalPages()))
			.containsExactly(1, 3, 4L, 2);
	}

	@Test
	void sortByEmailDescendingOrdersResults() {
		Page<Customer> result = listCustomers.list(new CustomerFilter(null, null), 0, 20, "email,desc");

		assertThat(result.getContent()).extracting(customer -> customer.getDetails().email())
			.containsExactly("promo@x.com", "mariana@x.com", "bruno@x.com", "ana@x.com");
	}

	private List<String> names(CustomerFilter filter) {
		return listCustomers.list(filter, 0, 20, null)
			.getContent()
			.stream()
			.map(customer -> customer.getDetails().name())
			.toList();
	}

	private void save(String name, String email, String cpf) {
		repository.saveAndFlush(newEntity(new CustomerDetails(name, email, cpf, null, null, "São Paulo", "SP"), NOW));
	}

	private static CustomerJpaEntity newEntity(CustomerDetails details, Instant createdAt) {
		return CustomerJpaEntity.from(Customer.register(details, createdAt));
	}

}
