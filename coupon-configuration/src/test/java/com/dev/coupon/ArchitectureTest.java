package com.dev.coupon;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
		packages = "com.dev.coupon",
		importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

	@ArchTest
	static final ArchRule domainIsSelfContained = noClasses()
			.that().resideInAPackage("com.dev.coupon.domain..")
			.should().dependOnClassesThat().resideOutsideOfPackages(
					"java..",
					"com.dev.coupon.domain.."
			);

	@ArchTest
	static final ArchRule applicationDoesNotDependOnAdapters = noClasses()
			.that().resideInAPackage("com.dev.coupon.application..")
			.should().dependOnClassesThat().resideInAPackage("com.dev.coupon.adapter..");

	@ArchTest
	static final ArchRule applicationUsesOnlyAllowedLibraries = noClasses()
			.that().resideInAPackage("com.dev.coupon.application..")
			.should().dependOnClassesThat().resideOutsideOfPackages(
					"java..",
					"com.dev.coupon.application..",
					"com.dev.coupon.domain..",
					"org.springframework.transaction..",
					"org.springframework.dao.."
			);

	@ArchTest
	static final ArchRule inboundAdaptersDoNotDependOnOutboundAdapters = noClasses()
			.that().resideInAPackage("com.dev.coupon.adapter.in..")
			.should().dependOnClassesThat().resideInAPackage("com.dev.coupon.adapter.out..");

	@ArchTest
	static final ArchRule outboundAdaptersDoNotDependOnInboundAdapters = noClasses()
			.that().resideInAPackage("com.dev.coupon.adapter.out..")
			.should().dependOnClassesThat().resideInAPackage("com.dev.coupon.adapter.in..");

	@ArchTest
	static final ArchRule applicationPortsAreInterfaces = classes()
			.that().resideInAPackage("com.dev.coupon.application.port..")
			.should().beInterfaces();

	@ArchTest
	static final ArchRule packagesAreFreeOfCycles = slices()
			.matching("com.dev.coupon.(*)..")
			.should().beFreeOfCycles();
}
