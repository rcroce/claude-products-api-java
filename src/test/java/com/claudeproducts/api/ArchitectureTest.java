package com.claudeproducts.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.claudeproducts.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule apiDoesNotUsePersistence = noClasses().that()
		.resideInAPackage("..product.api..")
		.should()
		.dependOnClassesThat()
		.resideInAPackage("..persistence..")
		.because("o controller fala com o serviço, nunca direto com o repositório");

	@ArchTest
	static final ArchRule domainDoesNotUseWeb = noClasses().that()
		.resideInAPackage("..domain..")
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage("org.springframework.web..", "org.springframework.http..", "..product.api..")
		.because("regras de negócio não dependem de HTTP");

}
