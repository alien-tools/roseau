package io.github.alien.roseau.diff;

import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.formatter.CsvFormatter;
import io.github.alien.roseau.options.RoseauOptions;
import io.github.alien.roseau.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A qualified name does not tell overloads apart ({@code p.api.C.m}); a qualified signature does
 * ({@code p.api.C.m(int)}). Everything Roseau shows users, and everything users match against, must use the latter,
 * otherwise a rule aimed at one overload silently hits them all.
 *
 * @see io.github.alien.roseau.api.model.Symbol#getUniqueId()
 */
class OverloadIdentityTest {
	private static final String V1 = """
		package p.api;
		public class C {
			public void m(int i) {}
			public void m(java.lang.String s) {}
		}""";
	private static final String V2 = """
		package p.api;
		public class C {}""";

	@Test
	void removed_overloads_are_reported_as_distinct_symbols() {
		var report = Roseau.diff(TestUtils.buildSourcesAPI(V1), TestUtils.buildSourcesAPI(V2));

		assertThat(report.getBreakingChanges())
			.hasSize(2)
			.extracting(bc -> bc.impactedSymbol().getUniqueId())
			.containsExactlyInAnyOrder("p.api.C.m(int)", "p.api.C.m(java.lang.String)");
	}

	@Test
	void removed_overloads_are_grouped_per_member_without_merging() {
		var report = Roseau.diff(TestUtils.buildSourcesAPI(V1), TestUtils.buildSourcesAPI(V2));
		var type = report.getImpactedTypes().getFirst();

		// A TreeMap keyed on a comparator that ties both overloads would collapse them into a single entry
		assertThat(report.getBreakingChangesPerMember(type))
			.hasSize(2)
			.containsOnlyKeys(report.getBreakingChanges().stream()
				.map(bc -> (TypeMemberDecl) bc.impactedSymbol())
				.toArray(TypeMemberDecl[]::new));
	}

	@Test
	void csv_report_names_each_overload_separately() {
		var report = Roseau.diff(TestUtils.buildSourcesAPI(V1), TestUtils.buildSourcesAPI(V2));

		assertThat(new CsvFormatter().format(report).lines().skip(1))
			.hasSize(2)
			.anyMatch(line -> line.contains("p.api.C.m(int)"))
			.anyMatch(line -> line.contains("p.api.C.m(java.lang.String)"));
	}

	@Test
	void ignoring_one_overload_leaves_the_other_reported(@TempDir Path tempDir) throws IOException {
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind
			p.api.C;p.api.C.m(int);EXECUTABLE_REMOVED""");

		var report = Roseau.diff(TestUtils.buildSourcesAPI(V1), TestUtils.buildSourcesAPI(V2))
			.filterReport(new RoseauOptions.Diff(ignored, false, false, false));

		assertThat(report.getBreakingChanges())
			.singleElement()
			.extracting(bc -> bc.impactedSymbol().getUniqueId())
			.isEqualTo("p.api.C.m(java.lang.String)");
	}

	@Test
	void an_ignored_entry_without_parameters_matches_no_overload(@TempDir Path tempDir) throws IOException {
		// 'p.api.C.m' is the qualified name both overloads share; it must not be accepted as a wildcard over them
		var ignored = tempDir.resolve("ignored.csv");
		Files.writeString(ignored, """
			type;symbol;kind
			p.api.C;p.api.C.m;EXECUTABLE_REMOVED""");

		var report = Roseau.diff(TestUtils.buildSourcesAPI(V1), TestUtils.buildSourcesAPI(V2))
			.filterReport(new RoseauOptions.Diff(ignored, false, false, false));

		assertThat(report.getBreakingChanges()).hasSize(2);
	}

	@Test
	void an_exclusion_pattern_can_target_a_single_overload() {
		var exclude = new RoseauOptions.Exclude(List.of("p\\.api\\.C\\.m\\(int\\)"), List.of());
		var report = Roseau.diff(TestUtils.buildSourcesAPI(V1, exclude), TestUtils.buildSourcesAPI(V2, exclude));

		assertThat(report.getAllBreakingChanges()).hasSize(2);
		assertThat(report.getBreakingChanges())
			.singleElement()
			.extracting(bc -> bc.impactedSymbol().getUniqueId())
			.isEqualTo("p.api.C.m(java.lang.String)");
	}

	@Test
	void qualified_name_and_qualified_signature_are_distinct() {
		var api = TestUtils.buildSourcesAPI(V1);
		var c = TestUtils.assertClass(api, "p.api.C");
		MethodDecl m = TestUtils.assertMethod(api, c, "m(int)");

		assertThat(m.getQualifiedName()).isEqualTo("p.api.C.m");
		assertThat(m.getSignature()).isEqualTo("m(int)");
		assertThat(m.getQualifiedSignature()).isEqualTo("p.api.C.m(int)");
	}
}
