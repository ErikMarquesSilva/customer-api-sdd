package com.example.customerapi;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

	// Hibernate statistics prove the grouping endpoint runs one statement and loads no entity (GEO-016, GEO-017).
	@Bean
	HibernatePropertiesCustomizer hibernateStatistics() {
		return properties -> properties.put("hibernate.generate_statistics", true);
	}

}
