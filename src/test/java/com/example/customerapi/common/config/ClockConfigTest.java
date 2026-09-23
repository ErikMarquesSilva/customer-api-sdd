package com.example.customerapi.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

	private final Clock clock = new ClockConfig().clock();

	@Test
	void clockIsUtc() {
		assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
	}

	// PostgreSQL timestamptz stores microseconds; an instant with finer precision would differ from what a
	// later read returns (CUST-01, CUST-14, CUST-19).
	@Test
	void nanosecondBaseClockIsTruncatedToMicroseconds() {
		Clock base = Clock.fixed(Instant.parse("2026-09-22T10:15:30.123456789Z"), ZoneOffset.UTC);

		assertThat(ClockConfig.microsecondPrecision(base).instant())
			.isEqualTo(Instant.parse("2026-09-22T10:15:30.123456Z"));
	}

	@Test
	void clockBeanInstantsHaveNoSubMicrosecondPrecision() {
		for (int i = 0; i < 1_000; i++) {
			Instant instant = clock.instant();
			assertThat(instant.getNano() % 1_000).isZero();
		}
	}

}
