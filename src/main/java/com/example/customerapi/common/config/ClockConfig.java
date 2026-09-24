package com.example.customerapi.common.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One UTC clock for timestamps and for time-based Bean Validation constraints such as {@code @Past}. It ticks in
 * microseconds, the precision of PostgreSQL {@code timestamptz}, so returned timestamps equal the stored ones.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	public Clock clock() {
		return microsecondPrecision(Clock.systemUTC());
	}

	static Clock microsecondPrecision(Clock base) {
		return Clock.tick(base, Duration.ofNanos(1_000));
	}

	@Bean
	public ValidationConfigurationCustomizer clockProviderCustomizer(Clock clock) {
		return configuration -> configuration.clockProvider(() -> clock);
	}

}
