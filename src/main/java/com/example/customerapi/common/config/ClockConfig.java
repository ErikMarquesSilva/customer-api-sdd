package com.example.customerapi.common.config;

import java.time.Clock;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One UTC clock for timestamps and for time-based Bean Validation constraints such as {@code @Past}.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public ValidationConfigurationCustomizer clockProviderCustomizer(Clock clock) {
		return configuration -> configuration.clockProvider(() -> clock);
	}

}
