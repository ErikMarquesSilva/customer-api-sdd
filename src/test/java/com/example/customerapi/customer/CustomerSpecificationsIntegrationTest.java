package com.example.customerapi.customer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.example.customerapi.TestcontainersConfiguration;
import com.example.customerapi.customer.domain.CustomerFilter;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CustomerSpecificationsIntegrationTest {

	private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

	@Autowired
	private CustomerRepository repository;

	@Autowired
	private CustomerService service;

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
		PageResponse<CustomerResponse> result = service.list(new CustomerFilter("n_", null), 0, 20, null);

		assertThat(result.content()).isEmpty();
		assertThat(result.page().totalElements()).isZero();
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
		PageResponse<CustomerResponse> first = service.list(new CustomerFilter(null, null), 0, 3, null);
		PageResponse<CustomerResponse> second = service.list(new CustomerFilter(null, null), 1, 3, null);

		assertThat(first.content()).extracting(CustomerResponse::name)
			.containsExactly("Ana Souza", "Bruno Reis", "Mariana Lima");
		assertThat(first.page()).isEqualTo(new PageResponse.PageMetadata(0, 3, 4, 2));
		assertThat(second.content()).extracting(CustomerResponse::name).containsExactly("Promo a%b");
		assertThat(second.page()).isEqualTo(new PageResponse.PageMetadata(1, 3, 4, 2));
	}

	@Test
	void sortByEmailDescendingOrdersResults() {
		PageResponse<CustomerResponse> result = service.list(new CustomerFilter(null, null), 0, 20, "email,desc");

		assertThat(result.content()).extracting(CustomerResponse::email)
			.containsExactly("promo@x.com", "mariana@x.com", "bruno@x.com", "ana@x.com");
	}

	private List<String> names(CustomerFilter filter) {
		return service.list(filter, 0, 20, null).content().stream().map(CustomerResponse::name).toList();
	}

	private void save(String name, String email, String cpf) {
		repository.saveAndFlush(new Customer(new CustomerRequest(name, email, cpf, null, null, "São Paulo", "SP"), NOW));
	}

}
