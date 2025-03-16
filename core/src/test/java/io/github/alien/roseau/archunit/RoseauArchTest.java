package io.github.alien.roseau.archunit;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.alien.roseau", importOptions = ImportOption.DoNotIncludeTests.class)
class RoseauArchTest {
	@ArchTest
	static final ArchRule spoon_is_only_used_for_parsing =
		noClasses().that().resideOutsideOfPackage("..roseau.extractors.spoon..")
			.should().accessClassesThat().resideInAPackage("spoon..");

	@ArchTest
	static final ArchRule asm_is_only_used_for_parsing =
		noClasses().that().resideOutsideOfPackage("..roseau.extractors.asm..")
			.should().accessClassesThat().resideInAPackage("org.objectweb.asm..");

	@ArchTest
	static final ArchRule jdt_is_only_used_for_parsing =
		noClasses().that().resideOutsideOfPackage("..roseau.extractors.jdt..")
			.should().accessClassesThat().resideInAPackage("org.eclipse.jdt..");
}
