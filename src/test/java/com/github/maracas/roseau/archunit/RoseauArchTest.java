package com.github.maracas.roseau.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.github.maracas.roseau", importOptions = ImportOption.DoNotIncludeTests.class)
class RoseauArchTest {
	@ArchTest
	static final ArchRule spoon_is_only_used_for_parsing =
		noClasses().that().resideOutsideOfPackage("..roseau.extractors.sources..")
			.should().accessClassesThat().resideInAPackage("spoon..");

	@ArchTest
	static final ArchRule asm_is_only_used_for_parsing =
		noClasses().that().resideOutsideOfPackage("..roseau.extractors.jar..")
			.should().accessClassesThat().resideInAPackage("org.objectweb.asm..");
}
