package com.example.customerapi.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Proves each hexagonal rule can fail: run against the violation fixtures, it must report the fixture that breaks it.
 * The fixtures live in the top-level {@code archfixtures} package so Spring's component and entity scanning, rooted at
 * {@code com.example.customerapi}, never picks them up.
 */
class HexagonalRulesDiscriminationTest {

	private static final JavaClasses FIXTURES = new ClassFileImporter()
		.importPackages("archfixtures");

	static Stream<Arguments> rulesAndTheirViolations() {
		return Stream.of(Arguments.of(HexagonalRules.DOMAIN_IS_FRAMEWORK_FREE, "SpringAwareDomainObject"),
				Arguments.of(HexagonalRules.APPLICATION_USES_NO_ADAPTER_OR_PERSISTENCE_TECHNOLOGY, "JpaAwareService"),
				Arguments.of(HexagonalRules.PORTS_ARE_INTERFACES, "ConcretePort"),
				Arguments.of(HexagonalRules.SERVICES_USE_PORTS_NOT_REPOSITORIES, "RepositoryAwareService"),
				Arguments.of(HexagonalRules.INBOUND_ADAPTERS_USE_ONLY_INPUT_PORTS, "ControllerUsingOutputPort"),
				Arguments.of(HexagonalRules.INBOUND_ADAPTERS_USE_ONLY_INPUT_PORTS, "ControllerUsingService"),
				Arguments.of(HexagonalRules.OUTBOUND_ADAPTERS_DO_NOT_REACH_INBOUND_SIDE, "PersistenceUsingController"),
				Arguments.of(HexagonalRules.OUTPUT_PORTS_ARE_IMPLEMENTED_BY_OUTBOUND_ADAPTERS,
						"PortImplementedInService"),
				Arguments.of(HexagonalRules.ENTITIES_LIVE_IN_PERSISTENCE_ADAPTER, "JpaEntityInDomain"),
				Arguments.of(HexagonalRules.REPOSITORIES_LIVE_IN_PERSISTENCE_ADAPTER, "RepositoryInWebAdapter"),
				Arguments.of(HexagonalRules.CONTROLLERS_LIVE_IN_WEB_ADAPTER, "RestControllerInService"),
				Arguments.of(HexagonalRules.USE_CASE_SERVICES_ARE_TRANSACTIONAL, "NonTransactionalService"),
				Arguments.of(HexagonalRules.USE_CASE_SERVICES_ARE_TRANSACTIONAL, "SupportsOnlyService"),
				Arguments.of(HexagonalRules.CPF_VALIDATION_DELEGATES_TO_DOMAIN,
						"archfixtures.customer.adapter.in.web.validation.CpfValidator"),
				Arguments.of(HexagonalRules.CPF_VALIDATION_DELEGATES_TO_DOMAIN,
						"archfixtures.customer.adapter.in.web.ignoring.CpfValidator"),
				Arguments.of(HexagonalRules.CPF_VALIDATION_DELEGATES_TO_DOMAIN,
						"archfixtures.customer.adapter.in.web.constructor.CpfValidator"),
				Arguments.of(HexagonalRules.FEATURE_CLASSES_BELONG_TO_A_LAYER, "StrayClass"));
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("rulesAndTheirViolations")
	void ruleReportsItsViolationFixture(ArchRule rule, String violatingClass) {
		assertThatThrownBy(() -> rule.check(FIXTURES)).isInstanceOf(AssertionError.class)
			.hasMessageContaining(violatingClass);
	}

}
