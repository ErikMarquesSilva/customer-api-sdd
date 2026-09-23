package com.example.customerapi.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaMember.Predicates.declaredIn;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.Set;

import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;

import com.example.customerapi.customer.domain.Cpf;

/**
 * Hexagonal dependency and placement rules (AD-005). Adapters depend inward on the application and the domain; the
 * domain depends on nothing outside itself. Patterns match any package segment named {@code customer}, so the same
 * rules also run against the violation fixtures.
 */
final class HexagonalRules {

	static final String CUSTOMER = "..customer..";

	static final String DOMAIN = "..customer.domain..";

	static final String APPLICATION = "..customer.application..";

	static final String PORT_IN = "..customer.application.port.in..";

	static final String PORT_OUT = "..customer.application.port.out..";

	static final String SERVICE = "..customer.application.service..";

	static final String ADAPTER = "..customer.adapter..";

	static final String ADAPTER_IN = "..customer.adapter.in..";

	static final String WEB_ADAPTER = "..customer.adapter.in.web..";

	static final String ADAPTER_OUT = "..customer.adapter.out..";

	static final String PERSISTENCE_ADAPTER = "..customer.adapter.out.persistence..";

	static final String COMMON_WEB = "..common.web..";

	private static final Set<String> STARTS_A_TRANSACTION = Set.of("REQUIRED", "REQUIRES_NEW", "NESTED");

	/** {@code @Transactional} whose propagation opens a transaction; SUPPORTS, NEVER etc. would not. */
	private static final DescribedPredicate<JavaAnnotation<?>> TRANSACTIONAL_STARTING_A_TRANSACTION =
			new DescribedPredicate<>("@Transactional starting a transaction") {

		@Override
		public boolean test(JavaAnnotation<?> annotation) {
			if (!annotation.getRawType().isEquivalentTo(Transactional.class)) {
				return false;
			}
			String propagation = annotation.get("propagation")
				.map(value -> value instanceof JavaEnumConstant constant ? constant.name() : String.valueOf(value))
				.orElse("REQUIRED");
			return STARTS_A_TRANSACTION.contains(propagation);
		}

	};

	/** ARCH-01 */
	static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses().that()
		.resideInAPackage(DOMAIN)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("org.springframework..", "jakarta..", "org.hibernate..", ADAPTER, APPLICATION)
		.as("ARCH-01 the domain depends on no framework, adapter or application class");

	/** ARCH-04 */
	static final ArchRule APPLICATION_USES_NO_ADAPTER_OR_PERSISTENCE_TECHNOLOGY = noClasses().that()
		.resideInAPackage(APPLICATION)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(ADAPTER, "jakarta.persistence..", "org.hibernate..", "org.springframework.data.jpa..",
				"org.springframework.web..")
		.as("ARCH-04 the application depends on no adapter, JPA, Hibernate or web class");

	/** ARCH-05, ARCH-06: ports are interfaces (the list use case also declares its exception). */
	static final ArchRule PORTS_ARE_INTERFACES = classes().that()
		.resideInAnyPackage(PORT_IN, PORT_OUT)
		.and()
		.areNotAssignableTo(Throwable.class)
		.should()
		.beInterfaces()
		.as("ARCH-05/06 ports are interfaces");

	/** ARCH-06 */
	static final ArchRule SERVICES_USE_PORTS_NOT_REPOSITORIES = noClasses().that()
		.resideInAPackage(SERVICE)
		.should()
		.dependOnClassesThat()
		.areAssignableTo(Repository.class)
		.as("ARCH-06 use cases reach storage only through output ports");

	/** ARCH-07: inbound adapters use the use-case interfaces only. */
	static final ArchRule INBOUND_ADAPTERS_USE_ONLY_INPUT_PORTS = noClasses().that()
		.resideInAnyPackage(ADAPTER_IN, COMMON_WEB)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(ADAPTER_OUT, PORT_OUT, SERVICE)
		.as("ARCH-07 inbound adapters depend on no outbound adapter, output port or service class");

	/** ARCH-08 */
	static final ArchRule OUTBOUND_ADAPTERS_DO_NOT_REACH_INBOUND_SIDE = noClasses().that()
		.resideInAPackage(ADAPTER_OUT)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(ADAPTER_IN, COMMON_WEB)
		.as("ARCH-08 outbound adapters depend on no inbound adapter");

	/** ARCH-09 */
	static final ArchRule OUTPUT_PORTS_ARE_IMPLEMENTED_BY_OUTBOUND_ADAPTERS = classes().that()
		.implement(resideInAPackage(PORT_OUT))
		.should()
		.resideInAPackage(ADAPTER_OUT)
		.as("ARCH-09 output ports are implemented in adapter.out");

	/** ARCH-02, ARCH-10 */
	static final ArchRule ENTITIES_LIVE_IN_PERSISTENCE_ADAPTER = classes().that()
		.areAnnotatedWith(Entity.class)
		.should()
		.resideInAPackage(PERSISTENCE_ADAPTER)
		.as("ARCH-02/10 JPA entities live in adapter.out.persistence");

	/** ARCH-10 */
	static final ArchRule REPOSITORIES_LIVE_IN_PERSISTENCE_ADAPTER = classes().that()
		.areAssignableTo(Repository.class)
		.should()
		.resideInAPackage(PERSISTENCE_ADAPTER)
		.as("ARCH-10 Spring Data repositories live in adapter.out.persistence");

	/** ARCH-10 */
	static final ArchRule CONTROLLERS_LIVE_IN_WEB_ADAPTER = classes().that()
		.areAnnotatedWith(RestController.class)
		.should()
		.resideInAPackage(WEB_ADAPTER)
		.as("ARCH-10 REST controllers live in adapter.in.web");

	/**
	 * Use cases run in one transaction. In production the CUST-24 conflict is detected by Hibernate's versioned
	 * UPDATE against the version read in that same transaction.
	 */
	static final ArchRule USE_CASE_SERVICES_ARE_TRANSACTIONAL = classes().that()
		.resideInAPackage(SERVICE)
		.and()
		.areAnnotatedWith(Service.class)
		.should()
		.beAnnotatedWith(TRANSACTIONAL_STARTING_A_TRANSACTION)
		.as("use-case services are @Transactional with a propagation that starts a transaction");

	/** ARCH-03: the Bean Validation adapter delegates to the one CPF rule in the domain. */
	static final ArchRule CPF_VALIDATION_DELEGATES_TO_DOMAIN = classes().that()
		.haveSimpleName("CpfValidator")
		.should()
		.callMethod(Cpf.class, "isValid", String.class)
		.andShould()
		// Its own methods are allowed: javac adds a bridge isValid(Object, ...) for the generic ConstraintValidator.
		.onlyCallMethodsThat(are(declaredIn(Cpf.class)).or(declaredIn(simpleName("CpfValidator"))))
		.as("ARCH-03 the web CPF validator delegates to the domain rule and computes nothing itself");

	/** Every feature class belongs to one hexagon layer. */
	static final ArchRule FEATURE_CLASSES_BELONG_TO_A_LAYER = classes().that()
		.resideInAPackage(CUSTOMER)
		.should()
		.resideInAnyPackage(DOMAIN, APPLICATION, ADAPTER)
		.as("feature classes live in domain, application or adapter");

	private HexagonalRules() {
	}

}
