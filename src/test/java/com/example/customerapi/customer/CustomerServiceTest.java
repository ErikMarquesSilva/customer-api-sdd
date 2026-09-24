package com.example.customerapi.customer;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

	private static final Instant CREATED = Instant.parse("2026-01-01T09:00:00Z");

	private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

	private static final CustomerRequest ANA = new CustomerRequest("Ana Souza", "ana@example.com", "52998224725",
			"11987654321", LocalDate.of(1990, 5, 20), "São Paulo", "SP");

	private static final CustomerRequest NEW_DATA = new CustomerRequest("Ana Lima", "ana.lima@example.com",
			"11144477735", "21912345678", LocalDate.of(1991, 6, 21), "Rio de Janeiro", "RJ");

	@Mock
	private CustomerRepository repository;

	private CustomerService service;

	private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

	@BeforeEach
	void setUp() {
		service = new CustomerService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
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
		when(repository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerResponse response = service.create(ANA);

		ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
		verify(repository).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getId()).isNotNull();
		assertThat(response.id()).isEqualTo(saved.getValue().getId());
		assertThat(response).extracting(CustomerResponse::name, CustomerResponse::email, CustomerResponse::cpf,
				CustomerResponse::phone, CustomerResponse::birthDate, CustomerResponse::city, CustomerResponse::state,
				CustomerResponse::createdAt, CustomerResponse::updatedAt)
			.containsExactly("Ana Souza", "ana@example.com", "52998224725", "11987654321", LocalDate.of(1990, 5, 20),
					"São Paulo", "SP", NOW, NOW);
	}

	@Test
	void createWithTakenEmailThrowsDuplicateEmailAndSavesNothing() {
		when(repository.existsByEmail("ana@example.com")).thenReturn(true);

		assertThatThrownBy(() -> service.create(ANA)).isInstanceOfSatisfying(DuplicateFieldException.class,
				ex -> assertThat(ex.getField()).isEqualTo("email"));
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void createWithTakenCpfThrowsDuplicateCpfAndSavesNothing() {
		when(repository.existsByCpf("52998224725")).thenReturn(true);

		assertThatThrownBy(() -> service.create(ANA)).isInstanceOfSatisfying(DuplicateFieldException.class,
				ex -> assertThat(ex.getField()).isEqualTo("cpf"));
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void createLogsOneInfoLineWithIdAndNoPii() {
		when(repository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CustomerResponse response = service.create(ANA);

		assertSingleInfoLineWithoutPii("created", response.id(), ANA);
	}

	// --- get

	@Test
	void getReturnsExistingCustomer() {
		Customer existing = new Customer(ANA, CREATED);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

		CustomerResponse response = service.get(existing.getId());

		assertThat(response).isEqualTo(new CustomerResponse(existing.getId(), "Ana Souza", "ana@example.com",
				"52998224725", "11987654321", LocalDate.of(1990, 5, 20), "São Paulo", "SP", CREATED, CREATED));
	}

	@Test
	void getUnknownIdThrowsNotFound() {
		UUID unknown = UUID.randomUUID();
		when(repository.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(CustomerNotFoundException.class);
	}

	// --- update

	@Test
	void updateReplacesAllFieldsKeepsIdAndCreatedAtAndSetsUpdatedAtFromClock() {
		Customer existing = existing();

		CustomerResponse response = service.update(existing.getId(), NEW_DATA);

		assertThat(response).isEqualTo(new CustomerResponse(existing.getId(), "Ana Lima", "ana.lima@example.com",
				"11144477735", "21912345678", LocalDate.of(1991, 6, 21), "Rio de Janeiro", "RJ", CREATED, NOW));
		verify(repository).saveAndFlush(existing);
	}

	@Test
	void updateWithoutPhoneAndBirthDateStoresThemAsNull() {
		Customer existing = existing();
		CustomerRequest withoutOptionals = new CustomerRequest("Ana Souza", "ana@example.com", "52998224725", null,
				null, "São Paulo", "SP");

		CustomerResponse response = service.update(existing.getId(), withoutOptionals);

		assertThat(existing.getPhone()).isNull();
		assertThat(existing.getBirthDate()).isNull();
		assertThat(response.phone()).isNull();
		assertThat(response.birthDate()).isNull();
	}

	@Test
	void updateWithEmailOfAnotherCustomerThrowsDuplicateEmailAndLeavesCustomerUnchanged() {
		Customer existing = new Customer(ANA, CREATED);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(repository.existsByEmailAndIdNot("ana.lima@example.com", existing.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.update(existing.getId(), NEW_DATA))
			.isInstanceOfSatisfying(DuplicateFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("email"));
		assertUnchanged(existing);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateWithCpfOfAnotherCustomerThrowsDuplicateCpfAndLeavesCustomerUnchanged() {
		Customer existing = new Customer(ANA, CREATED);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		when(repository.existsByCpfAndIdNot("11144477735", existing.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.update(existing.getId(), NEW_DATA))
			.isInstanceOfSatisfying(DuplicateFieldException.class, ex -> assertThat(ex.getField()).isEqualTo("cpf"));
		assertUnchanged(existing);
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void updateKeepingOwnEmailAndCpfIsNotAConflict() {
		Customer existing = existing();
		// The customer's own values exist in the table; only a check that excludes this id is correct.
		lenient().when(repository.existsByEmail("ana@example.com")).thenReturn(true);
		lenient().when(repository.existsByCpf("52998224725")).thenReturn(true);

		CustomerResponse response = service.update(existing.getId(), ANA);

		assertThat(response.email()).isEqualTo("ana@example.com");
		assertThat(response.cpf()).isEqualTo("52998224725");
		assertThat(response.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void updateUnknownIdThrowsNotFoundAndSavesNothing() {
		UUID unknown = UUID.randomUUID();
		when(repository.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(unknown, ANA)).isInstanceOf(CustomerNotFoundException.class);
		verify(repository, never()).saveAndFlush(any());
		verify(repository, never()).save(any());
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
		Customer existing = new Customer(ANA, CREATED);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

		service.delete(existing.getId());

		verify(repository).delete(existing);
	}

	@Test
	void deleteUnknownIdThrowsNotFoundAndDeletesNothing() {
		UUID unknown = UUID.randomUUID();
		when(repository.findById(unknown)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(unknown)).isInstanceOf(CustomerNotFoundException.class);
		verify(repository, never()).delete(any(Customer.class));
		verify(repository, never()).deleteById(any());
	}

	@Test
	void deleteLogsOneInfoLineWithIdAndNoPii() {
		Customer existing = new Customer(ANA, CREATED);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

		service.delete(existing.getId());

		assertSingleInfoLineWithoutPii("deleted", existing.getId(), ANA);
	}

	// --- helpers

	private Customer existing() {
		Customer existing = new Customer(ANA, CREATED);
		when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
		lenient().when(repository.existsByEmailAndIdNot(anyString(), any())).thenReturn(false);
		lenient().when(repository.existsByCpfAndIdNot(anyString(), any())).thenReturn(false);
		when(repository.saveAndFlush(existing)).thenReturn(existing);
		return existing;
	}

	private static void assertUnchanged(Customer customer) {
		assertThat(customer).extracting(Customer::getName, Customer::getEmail, Customer::getCpf, Customer::getPhone,
				Customer::getBirthDate, Customer::getCity, Customer::getState, Customer::getCreatedAt,
				Customer::getUpdatedAt)
			.containsExactly("Ana Souza", "ana@example.com", "52998224725", "11987654321", LocalDate.of(1990, 5, 20),
					"São Paulo", "SP", CREATED, CREATED);
	}

	private void assertSingleInfoLineWithoutPii(String operation, UUID id, CustomerRequest data) {
		List<String> infoLines = logs.list.stream()
			.filter(event -> event.getLevel() == Level.INFO)
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
		assertThat(infoLines).hasSize(1);
		assertThat(infoLines.getFirst()).contains(operation).contains(id.toString());
		assertNoPii(data);
	}

	private void assertNoPii(CustomerRequest data) {
		assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
			.allSatisfy(message -> assertThat(message).doesNotContain(data.email(), data.cpf(), data.phone()));
	}

	private static Logger serviceLogger() {
		return (Logger) LoggerFactory.getLogger(CustomerService.class);
	}

}
