package com.example.customerapi.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.customerapi.customer.application.port.out.LocationCountPort;
import com.example.customerapi.customer.domain.CityLocation;
import com.example.customerapi.customer.domain.LocationCount;
import com.example.customerapi.customer.domain.StateLocation;

@ExtendWith(MockitoExtension.class)
class CustomerLocationServiceTest {

	@Mock
	private LocationCountPort locationCounts;

	private CustomerLocationService service;

	@BeforeEach
	void setUp() {
		service = new CustomerLocationService(locationCounts);
	}

	// --- GEO-002, GEO-003, GEO-004, GEO-005

	@Test
	void rowsBecomeStatesSortedByUfWithCitiesAndSummedTotals() {
		rows(row("SP", "Limeira", 50), row("SP", "Campinas", 100), row("MG", "Uberlândia", 30));

		assertThat(service.groupByLocation()).isEqualTo(List.of(
				new StateLocation("MG", 30, List.of(new CityLocation("Uberlândia", 30))),
				new StateLocation("SP", 150,
						List.of(new CityLocation("Campinas", 100), new CityLocation("Limeira", 50)))));
	}

	// --- GEO-005

	@Test
	void statesAreSortedAlphabeticallyByUf() {
		rows(row("SP", "Campinas", 1), row("RJ", "Niterói", 1), row("AC", "Rio Branco", 1), row("MG", "Uberaba", 1),
				row("BA", "Salvador", 1));

		assertThat(service.groupByLocation()).extracting(StateLocation::state)
			.containsExactly("AC", "BA", "MG", "RJ", "SP");
	}

	// --- GEO-006

	@Test
	void citiesAreSortedByPtBrCollationIgnoringCaseAndAccents() {
		rows(row("SP", "campos do Jordão", 1), row("SP", "Bauru", 2), row("SP", "Águas de Lindóia", 3));

		assertThat(service.groupByLocation()).singleElement()
			.satisfies(sp -> assertThat(sp.cities()).extracting(CityLocation::city)
				.containsExactly("Águas de Lindóia", "Bauru", "campos do Jordão"));
	}

	// --- GEO-019 (GEO-006 with GEO-012): accent-only variants are separate entries in plain character order

	@Test
	void citiesEqualIgnoringAccentsKeepTheSameOrderWhateverTheRowOrder() {
		rows(row("MG", "Uberlândia", 1), row("MG", "Uberlandia", 2));
		List<CityLocation> first = service.groupByLocation().getFirst().cities();

		rows(row("MG", "Uberlandia", 2), row("MG", "Uberlândia", 1));
		List<CityLocation> second = service.groupByLocation().getFirst().cities();

		assertThat(first).containsExactly(new CityLocation("Uberlandia", 2), new CityLocation("Uberlândia", 1));
		assertThat(second).isEqualTo(first);
	}

	// --- GEO-007

	@Test
	void noRowsGiveEmptyStates() {
		rows();

		assertThat(service.groupByLocation()).isEqualTo(List.of());
	}

	// --- Edge case: exactly one customer (GEO-002, GEO-003)

	@Test
	void oneCustomerGivesOneStateWithOneCityBothWithTotalOne() {
		rows(row("PR", "Curitiba", 1));

		assertThat(service.groupByLocation())
			.isEqualTo(List.of(new StateLocation("PR", 1, List.of(new CityLocation("Curitiba", 1)))));
	}

	// --- Edge case: all customers in one city (GEO-004)

	@Test
	void allCustomersInOneCityGiveStateTotalEqualToCityTotal() {
		rows(row("RJ", "Niterói", 7));

		assertThat(service.groupByLocation())
			.isEqualTo(List.of(new StateLocation("RJ", 7, List.of(new CityLocation("Niterói", 7)))));
	}

	private void rows(LocationCount... rows) {
		when(locationCounts.countByLocation()).thenReturn(List.of(rows));
	}

	private static LocationCount row(String state, String city, long total) {
		return new LocationCount(state, city, total);
	}

}
