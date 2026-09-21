package io.github.alien.roseau.diff.formatter;

import com.google.common.base.Suppliers;
import io.github.alien.roseau.Roseau;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.MethodDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.diff.RoseauReport;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.options.RoseauOptions;
import io.github.alien.roseau.utils.TestUtils;

import java.util.List;
import java.util.function.Supplier;

final class ReportFixtures {
	private static final String MIXED_V1 = """
		package pkg;

		public class A<T> {
			public void removed() {}
			public void mutated() {}
			public java.util.List<java.lang.String> generic(java.util.List<java.lang.String> p) { return p; }
		}""";

	private static final String MIXED_V2 = """
		package pkg;

		public class A {
			public static void mutated() {}
			public java.util.List<java.lang.Integer> generic(java.util.List<java.lang.Integer> p) { return p; }
		}""";

	private static final Supplier<RoseauReport> MIXED = memoize(MIXED_V1, MIXED_V2);

	private static final Supplier<RoseauReport> EMPTY = memoize(MIXED_V1, MIXED_V1);

	private static final Supplier<RoseauReport> INHERITED = memoize("""
		package pkg;

		public class Base {
			public void removed() {}
		}

		public class Child extends Base {
		}""", """
		package pkg;

		public class Base {
		}

		public class Child extends Base {
		}""");

	private static final Supplier<RoseauReport> EXCLUDED = Suppliers.memoize(() -> {
		RoseauOptions.Exclude exclude = new RoseauOptions.Exclude(List.of("pkg\\.A\\.removed\\(\\)"), List.of());
		return Roseau.diff(TestUtils.buildSourcesAPI(MIXED_V1, exclude), TestUtils.buildSourcesAPI(MIXED_V2, exclude));
	});

	private static final Supplier<API> SINGLE_METHOD = Suppliers.memoize(() -> TestUtils.buildSourcesAPI("""
		package pkg;

		public class A {
			public void m() {}
		}"""));

	private ReportFixtures() {
	}

	static RoseauReport mixed() {
		return MIXED.get();
	}

	static RoseauReport empty() {
		return EMPTY.get();
	}

	static RoseauReport inherited() {
		return INHERITED.get();
	}

	static RoseauReport excluded() {
		return EXCLUDED.get();
	}

	static RoseauReport reportedAt(SourceLocation location) {
		var api = SINGLE_METHOD.get();
		var a = api.getLibraryTypes().findType("pkg.A").orElseThrow();
		var m = a.getDeclaredMethods().iterator().next();
		var bc = new BreakingChange(BreakingChangeKind.EXECUTABLE_REMOVED, a, m, null,
			new BreakingChangeDetails.None(), location);
		return new RoseauReport(api, api, List.of(bc));
	}

	static RoseauReport reversed(RoseauReport report) {
		return new RoseauReport(report.v1(), report.v2(), report.getAllBreakingChanges().reversed());
	}

	static String normalize(String output) {
		return output.replace("\r\n", "\n")
			.replace("&#92;", "/")
			.replace('\\', '/');
	}

	private static Supplier<RoseauReport> memoize(String v1, String v2) {
		return Suppliers.memoize(() -> Roseau.diff(TestUtils.buildSourcesAPI(v1), TestUtils.buildSourcesAPI(v2)));
	}
}
