package com.example.customerapi.customer.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.customerapi.customer.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerLocationServiceTest {

	@Mock
	private CustomerRepository repository;

	private CustomerLocationService service;

	@BeforeEach
	void setUp() {
		service = new CustomerLocationService(repository);
	}

	// --- GEO-002, GEO-003, GEO-004, GEO-005

	@Test
	void rowsBecomeStatesSortedByUfWithCitiesAndSummedTotals() {
		rows(row("SP", "Limeira", 50), row("SP", "Campinas", 100), row("MG", "Uberlândia", 30));

		assertThat(service.groupByLocation()).isEqualTo(new LocationGroupingResponse(List.of(
				new StateGroup("MG", 30, List.of(new CityGroup("Uberlândia", 30))),
				new StateGroup("SP", 150, List.of(new CityGroup("Campinas", 100), new CityGroup("Limeira", 50))))));
	}

	// --- GEO-005

	@Test
	void statesAreSortedAlphabeticallyByUf() {
		rows(row("SP", "Campinas", 1), row("RJ", "Niterói", 1), row("AC", "Rio Branco", 1), row("MG", "Uberaba", 1),
				row("BA", "Salvador", 1));

		assertThat(service.groupByLocation().states()).extracting(StateGroup::state)
			.containsExactly("AC", "BA", "MG", "RJ", "SP");
	}

	// --- GEO-006

	@Test
	void citiesAreSortedByPtBrCollationIgnoringCaseAndAccents() {
		rows(row("SP", "campos do Jordão", 1), row("SP", "Bauru", 2), row("SP", "Águas de Lindóia", 3));

		assertThat(service.groupByLocation().states()).singleElement()
			.satisfies(sp -> assertThat(sp.cities()).extracting(CityGroup::city)
				.containsExactly("Águas de Lindóia", "Bauru", "campos do Jordão"));
	}

	// --- GEO-019 (GEO-006 with GEO-012): accent-only variants are separate entries in plain character order

	@Test
	void citiesEqualIgnoringAccentsKeepTheSameOrderWhateverTheRowOrder() {
		rows(row("MG", "Uberlândia", 1), row("MG", "Uberlandia", 2));
		List<CityGroup> first = service.groupByLocation().states().getFirst().cities();

		rows(row("MG", "Uberlandia", 2), row("MG", "Uberlândia", 1));
		List<CityGroup> second = service.groupByLocation().states().getFirst().cities();

		assertThat(first).containsExactly(new CityGroup("Uberlandia", 2), new CityGroup("Uberlândia", 1));
		assertThat(second).isEqualTo(first);
	}

	// --- GEO-007

	@Test
	void noRowsGiveEmptyStates() {
		rows();

		assertThat(service.groupByLocation()).isEqualTo(new LocationGroupingResponse(List.of()));
	}

	// --- Edge case: exactly one customer (GEO-002, GEO-003)

	@Test
	void oneCustomerGivesOneStateWithOneCityBothWithTotalOne() {
		rows(row("PR", "Curitiba", 1));

		assertThat(service.groupByLocation()).isEqualTo(new LocationGroupingResponse(
				List.of(new StateGroup("PR", 1, List.of(new CityGroup("Curitiba", 1))))));
	}

	// --- Edge case: all customers in one city (GEO-004)

	@Test
	void allCustomersInOneCityGiveStateTotalEqualToCityTotal() {
		rows(row("RJ", "Niterói", 7));

		assertThat(service.groupByLocation()).isEqualTo(new LocationGroupingResponse(
				List.of(new StateGroup("RJ", 7, List.of(new CityGroup("Niterói", 7))))));
	}

	private void rows(LocationCount... rows) {
		when(repository.countByLocation()).thenReturn(List.of(rows));
	}

	private static LocationCount row(String state, String city, long total) {
		return new Row(state, city, total);
	}

	private record Row(String getState, String getCity, long getTotal) implements LocationCount {
	}

}
