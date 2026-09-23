package com.example.customerapi.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Hexagonal dependency rules (AD-005). Adapters depend inward on the application and the domain; the domain depends
 * on nothing outside itself.
 */
@AnalyzeClasses(packages = "com.example.customerapi", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

	static final String DOMAIN = "..customer.domain..";

	static final String APPLICATION = "..customer.application..";

	static final String PORT_IN = "..customer.application.port.in..";

	static final String PORT_OUT = "..customer.application.port.out..";

	static final String SERVICE = "..customer.application.service..";

	static final String ADAPTER_IN = "..customer.adapter.in..";

	static final String ADAPTER_OUT = "..customer.adapter.out..";

	static final String COMMON_WEB = "..common.web..";

	/** ARCH-01 */
	@ArchTest
	static final ArchRule domainIsFrameworkFree = noClasses().that()
		.resideInAPackage(DOMAIN)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..", "..customer.adapter..",
				APPLICATION)
		.allowEmptyShould(true);

	/** ARCH-04 */
	@ArchTest
	static final ArchRule applicationUsesNoAdapterOrPersistenceTechnology = noClasses().that()
		.resideInAPackage(APPLICATION)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("..customer.adapter..", "jakarta.persistence..", "org.hibernate..",
				"org.springframework.data.jpa..", "org.springframework.web..")
		.allowEmptyShould(true);

	/** ARCH-05, ARCH-06: ports are interfaces (the list use case also declares its exception). */
	@ArchTest
	static final ArchRule portsAreInterfaces = classes().that()
		.resideInAnyPackage(PORT_IN, PORT_OUT)
		.and()
		.areNotAssignableTo(Throwable.class)
		.should()
		.beInterfaces()
		.allowEmptyShould(true);

	/** ARCH-06 */
	@ArchTest
	static final ArchRule servicesDependOnPortsNotRepositories = noClasses().that()
		.resideInAPackage(SERVICE)
		.should()
		.dependOnClassesThat()
		.areAssignableTo(org.springframework.data.repository.Repository.class)
		.allowEmptyShould(true);

	/** ARCH-07 */
	@ArchTest
	static final ArchRule inboundAdaptersDoNotReachOutboundSide = noClasses().that()
		.resideInAnyPackage(ADAPTER_IN, COMMON_WEB)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(ADAPTER_OUT, PORT_OUT)
		.allowEmptyShould(true);

	/** ARCH-08 */
	@ArchTest
	static final ArchRule outboundAdaptersDoNotReachInboundSide = noClasses().that()
		.resideInAPackage(ADAPTER_OUT)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(ADAPTER_IN, COMMON_WEB)
		.allowEmptyShould(true);

}
