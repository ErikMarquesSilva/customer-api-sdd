package com.example.customerapi.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.example.customerapi.customer.application.port.out.CustomerPersistencePort;
import com.example.customerapi.customer.domain.Customer;
import com.example.customerapi.customer.domain.CustomerDetails;
import com.example.customerapi.customer.domain.CustomerFilter;
import com.example.customerapi.customer.domain.CustomerNotFoundException;
import com.example.customerapi.customer.domain.DuplicateFieldException;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T09:00:00Z");

	private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

	private static final CustomerDetails ANA = new CustomerDetails("Ana Souza", "ana@example.com", "52998224725",
			"11987654321", LocalDate.of(1990, 5, 20), "São Paulo", "SP");

	private static final CustomerDetails NEW_DATA = new CustomerDetails("Ana Lima", "ana.lima@example.com",
			"11144477735", "21912345678", LocalDate.of(1991, 6, 21), "Rio de Janeiro", "RJ");

	@Mock
	private CustomerPersistencePort persistence;

	private CustomerService service;

	private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

	@BeforeEach
	void setUp() {
		service = new CustomerService(persistence, Clock.fixed(NOW, ZoneOffset.UTC));
		logs.start();
		serviceLogger().addAppender(logs);
	}

	@AfterEach
	void detachLogs() {
		serviceLogger().detachAppender(logs);
	}

	// --- create

	@Test
	void createPersistsCustomerAndReturnsIt() {
		when(persistence.insert(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Customer created = service.create(ANA);

		ArgumentCaptor<Customer> inserted = ArgumentCaptor.forClass(Customer.class);
		verify(persistence).insert(inserted.capture());
		assertThat(inserted.getValue().getId()).isNotNull();
		assertThat(created.getId()).isEqualTo(inserted.getValue().getId());
		assertThat(created.getDetails()).isEqualTo(ANA);
		assertThat(created.getCreatedAt()).isEqualTo(NOW);
		assertThat(created.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void createWithTakenEmailThrowsDuplicateEmailAndSavesNothing() {
		when(persistence.existsByEmail("ana@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.create(ANA)).isInstanceOfSatisfying(DuplicateFieldException.class,
				ex -> assertThat(ex.getField()).isEqualTo("email"));
		verify(persistence, never()).insert(any());
	}

	@Test
	void createWithTakenCpfThrowsDuplicateCpfAndSavesNothing() {
		when(persistence.existsByCpf("52998224725")).thenReturn(true);

		assertThatThrownBy(() -> service.create(ANA)).isInstanceOfSatisfying(DuplicateFieldException.class,
				ex -> assertThat(ex.getField()).isEqualTo("cpf"));
		verify(persistence, never()).insert(any());
	}

	@Test
	void createLogsOneInfoLineWithIdAndNoPii() {
		when(persistence.insert(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Customer created = service.create(ANA);

		assertSingleInfoLineWithoutPii("created", created.getId(), ANA);
	}

	// --- get

	@Test
	void getReturnsExistingCustomer() {
		Customer existing = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 0);
		when(persistence.findById(existing.getId())).thenReturn(Optional.of(existing));

		assertThat(service.get(existing.getId())).isSameAs(existing);
	}

	@Test
	void getUnknownIdThrowsNotFound() {
		UUID unknown = UUID.randomUUID();
		when(persistence.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(CustomerNotFoundException.class);
	}

	// --- list

	@Test
	void listSearchesWithTheFilterAndAPageRequestSortedAsRequested() {
		CustomerFilter filter = new CustomerFilter("ana", null);
		Page<Customer> page = new PageImpl<>(List.of());
		when(persistence.search(filter, PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "email").and(Sort.by("id")))))
			.thenReturn(page);

		assertThat(service.list(filter, 1, 5, "email,desc")).isSameAs(page);
	}

	// --- update

	@Test
	void updateReplacesAllFieldsKeepsIdCreatedAtAndVersionAndSetsUpdatedAtFromClock() {
		Customer existing = existing();

		Customer updated = service.update(existing.getId(), NEW_DATA);

		assertThat(updated.getId()).isEqualTo(existing.getId());
		assertThat(updated.getDetails()).isEqualTo(NEW_DATA);
		assertThat(updated.getCreatedAt()).isEqualTo(CREATED);
		assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
		assertThat(updated.getVersion()).isEqualTo(existing.getVersion());
	}

	@Test
	void updateWithoutPhoneAndBirthDateStoresThemAsNull() {
		Customer existing = existing();
		CustomerDetails withoutOptionals = new CustomerDetails("Ana Souza", "ana@example.com", "52998224725", null,
				null, "São Paulo", "SP");

		service.update(existing.getId(), withoutOptionals);

		ArgumentCaptor<Customer> stored = ArgumentCaptor.forClass(Customer.class);
		verify(persistence).update(stored.capture());
		assertThat(stored.getValue().getDetails().phone()).isNull();
		assertThat(stored.getValue().getDetails().birthDate()).isNull();
	}

	@Test
	void updateWithEmailOfAnotherCustomerThrowsDuplicateEmailAndStoresNothing() {
		Customer existing = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 0);
		when(persistence.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(persistence.existsByEmailAndIdNot("ana.lima@example.com", existing.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.update(existing.getId(), NEW_DATA))
			.isInstanceOfSatisfying(DuplicateFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("email"));
		verify(persistence, never()).update(any());
	}

	@Test
	void updateWithCpfOfAnotherCustomerThrowsDuplicateCpfAndStoresNothing() {
		Customer existing = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 0);
		when(persistence.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(persistence.existsByCpfAndIdNot("11144477735", existing.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.update(existing.getId(), NEW_DATA))
			.isInstanceOfSatisfying(DuplicateFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("cpf"));
		verify(persistence, never()).update(any());
	}

	@Test
	void updateKeepingOwnEmailAndCpfIsNotAConflict() {
		Customer existing = existing();
		// The customer's own values exist in the table; only a check that excludes this id is correct.
		lenient().when(persistence.existsByEmail("ana@example.com")).thenReturn(true);
		lenient().when(persistence.existsByCpf("52998224725")).thenReturn(true);

		Customer updated = service.update(existing.getId(), ANA);

		assertThat(updated.getDetails().email()).isEqualTo("ana@example.com");
		assertThat(updated.getDetails().cpf()).isEqualTo("52998224725");
		assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void updateUnknownIdThrowsNotFoundAndStoresNothing() {
		UUID unknown = UUID.randomUUID();
		when(persistence.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(unknown, ANA)).isInstanceOf(CustomerNotFoundException.class);
		verify(persistence, never()).update(any());
		verify(persistence, never()).insert(any());
	}

	@Test
	void updateLogsOneInfoLineWithIdAndNoPii() {
		Customer existing = existing();

		service.update(existing.getId(), NEW_DATA);

		assertSingleInfoLineWithoutPii("updated", existing.getId(), NEW_DATA);
		assertNoPii(ANA);
	}

	// --- delete

	@Test
	void deleteRemovesExistingCustomer() {
		Customer existing = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 0);
		when(persistence.findById(existing.getId())).thenReturn(Optional.of(existing));

		service.delete(existing.getId());

		verify(persistence).delete(existing.getId());
	}

	@Test
	void deleteUnknownIdThrowsNotFoundAndDeletesNothing() {
		UUID unknown = UUID.randomUUID();
		when(persistence.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(unknown)).isInstanceOf(CustomerNotFoundException.class);
		verify(persistence, never()).delete(any());
	}

	@Test
	void deleteLogsOneInfoLineWithIdAndNoPii() {
		Customer existing = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 0);
		when(persistence.findById(existing.getId())).thenReturn(Optional.of(existing));

		service.delete(existing.getId());

		assertSingleInfoLineWithoutPii("deleted", existing.getId(), ANA);
	}

	// --- helpers

	private Customer existing() {
		Customer existing = Customer.restore(UUID.randomUUID(), ANA, CREATED, CREATED, 4);
		when(persistence.findById(existing.getId())).thenReturn(Optional.of(existing));
		lenient().when(persistence.existsByEmailAndIdNot(anyString(), any())).thenReturn(false);
		lenient().when(persistence.existsByCpfAndIdNot(anyString(), any())).thenReturn(false);
		when(persistence.update(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
		return existing;
	}

	private void assertSingleInfoLineWithoutPii(String operation, UUID id, CustomerDetails data) {
		List<String> infoLines = logs.list.stream()
			.filter(event -> event.getLevel() == Level.INFO)
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
		assertThat(infoLines).hasSize(1);
		assertThat(infoLines.getFirst()).contains(operation).contains(id.toString());
		assertNoPii(data);
	}

	private void assertNoPii(CustomerDetails data) {
		assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
			.allSatisfy(message -> assertThat(message).doesNotContain(data.email(), data.cpf(), data.phone()));
	}

	private static Logger serviceLogger() {
		return (Logger) LoggerFactory.getLogger(CustomerService.class);
	}

}
