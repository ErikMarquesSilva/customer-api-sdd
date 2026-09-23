package com.example.customerapi.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Runs the hexagonal rules against the production classes (AD-005). Empty rule subjects fail, so a rule cannot pass
 * by matching nothing.
 */
@AnalyzeClasses(packages = "com.example.customerapi", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	@ArchTest
	static final ArchRule domainIsFrameworkFree = HexagonalRules.DOMAIN_IS_FRAMEWORK_FREE;

	@ArchTest
	static final ArchRule applicationUsesNoAdapterOrPersistenceTechnology =
			HexagonalRules.APPLICATION_USES_NO_ADAPTER_OR_PERSISTENCE_TECHNOLOGY;

	@ArchTest
	static final ArchRule portsAreInterfaces = HexagonalRules.PORTS_ARE_INTERFACES;

	@ArchTest
	static final ArchRule servicesUsePortsNotRepositories = HexagonalRules.SERVICES_USE_PORTS_NOT_REPOSITORIES;

	@ArchTest
	static final ArchRule inboundAdaptersUseOnlyInputPorts = HexagonalRules.INBOUND_ADAPTERS_USE_ONLY_INPUT_PORTS;

	@ArchTest
	static final ArchRule outboundAdaptersDoNotReachInboundSide =
			HexagonalRules.OUTBOUND_ADAPTERS_DO_NOT_REACH_INBOUND_SIDE;

	@ArchTest
	static final ArchRule outputPortsAreImplementedByOutboundAdapters =
			HexagonalRules.OUTPUT_PORTS_ARE_IMPLEMENTED_BY_OUTBOUND_ADAPTERS;

	@ArchTest
	static final ArchRule entitiesLiveInPersistenceAdapter = HexagonalRules.ENTITIES_LIVE_IN_PERSISTENCE_ADAPTER;

	@ArchTest
	static final ArchRule repositoriesLiveInPersistenceAdapter = HexagonalRules.REPOSITORIES_LIVE_IN_PERSISTENCE_ADAPTER;

	@ArchTest
	static final ArchRule controllersLiveInWebAdapter = HexagonalRules.CONTROLLERS_LIVE_IN_WEB_ADAPTER;

	@ArchTest
	static final ArchRule featureClassesBelongToALayer = HexagonalRules.FEATURE_CLASSES_BELONG_TO_A_LAYER;

}
