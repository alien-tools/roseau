package io.github.alien.roseau;

import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.extractors.ExtractorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoseauOptionsTest {
	@Test
	void merge_with_null_is_this() {
		var base = RoseauOptions.newDefault();
		assertThat(base.mergeWith(null)).isSameAs(base);
		assertThat(base.common().mergeWith(null)).isSameAs(base.common());
		assertThat(base.v1().mergeWith((RoseauOptions.Library) null)).isSameAs(base.v1());
		assertThat(base.v1().mergeWith((RoseauOptions.Common) null)).isSameAs(base.v1());
		assertThat(base.v1().classpath().mergeWith(null)).isSameAs(base.v1().classpath());
		assertThat(base.v1().excludes().mergeWith(null)).isSameAs(base.v1().excludes());
	}

	@Test
	void merge_overrides_existing() {
		var cpBase = new RoseauOptions.Classpath(Path.of("base-pom.xml"), Set.of(Path.of("base.jar")));
		var exBase = new RoseauOptions.Exclude(
			List.of("base.*"), List.of(new RoseauOptions.AnnotationExclusion("BaseAnn", Map.of("baseK", "baseV"))));
		var commonBase = new RoseauOptions.Common(ExtractorType.ASM, cpBase, exBase);
		var libBase = new RoseauOptions.Library(
			Path.of("base-lib"), ExtractorType.ASM, cpBase, exBase, Path.of("base.json"));
		var base = new RoseauOptions(commonBase, libBase, libBase, Path.of("base-ignore.csv"),
			List.of(new RoseauOptions.Report(Path.of("base.csv"), BreakingChangesFormatterFactory.CSV)));

		var cpOther = new RoseauOptions.Classpath(Path.of("other-pom.xml"), Set.of(Path.of("other.jar")));
		var exOther = new RoseauOptions.Exclude(
			List.of("other.*"), List.of(new RoseauOptions.AnnotationExclusion("OtherAnn", Map.of("otherK", "otherV"))));
		var commonOther = new RoseauOptions.Common(ExtractorType.JDT, cpOther, exOther);
		var libOther = new RoseauOptions.Library(
			Path.of("other-lib"), ExtractorType.JDT, cpOther, exOther, Path.of("other.json"));
		var other = new RoseauOptions(commonOther, libOther, libOther, Path.of("other-ignore.csv"),
			List.of(new RoseauOptions.Report(Path.of("other.html"), BreakingChangesFormatterFactory.HTML)));

		var merged = base.mergeWith(other);

		assertThat(merged.ignore()).isEqualTo(Path.of("other-ignore.csv"));
		assertThat(merged.reports()).singleElement().isEqualTo(
			new RoseauOptions.Report(Path.of("other.html"), BreakingChangesFormatterFactory.HTML));

		assertThat(merged.common().extractor()).isEqualTo(ExtractorType.JDT);
		assertThat(merged.common().classpath().pom()).isEqualTo(Path.of("other-pom.xml"));
		assertThat(merged.common().classpath().jars()).containsExactly(Path.of("other.jar"));
		assertThat(merged.common().excludes().names()).containsExactly("other.*");
		assertThat(merged.common().excludes().annotations()).singleElement().isEqualTo(
			new RoseauOptions.AnnotationExclusion("OtherAnn", Map.of("otherK", "otherV")));

		assertThat(merged.v1().location()).isEqualTo(Path.of("other-lib"));
		assertThat(merged.v1().extractor()).isEqualTo(ExtractorType.JDT);
		assertThat(merged.v1().apiReport()).isEqualTo(Path.of("other.json"));
		assertThat(merged.v1().classpath().pom()).isEqualTo(Path.of("other-pom.xml"));
		assertThat(merged.v1().classpath().jars()).containsExactly(Path.of("other.jar"));
		assertThat(merged.v1().excludes().names()).containsExactly("other.*");

		assertThat(merged.v2()).isEqualTo(merged.v1());
	}

	@Test
	void merge_does_not_override_with_null_or_empty() {
		var cpBase = new RoseauOptions.Classpath(Path.of("base-pom.xml"), Set.of(Path.of("base.jar")));
		var exBase = new RoseauOptions.Exclude(
			List.of("base.*"), List.of(new RoseauOptions.AnnotationExclusion("BaseAnn", Map.of("baseK", "baseV"))));
		var commonBase = new RoseauOptions.Common(ExtractorType.ASM, cpBase, exBase);
		var libBase = new RoseauOptions.Library(
			Path.of("base-lib"), ExtractorType.ASM, cpBase, exBase, Path.of("base.json"));
		var base = new RoseauOptions(commonBase, libBase, libBase, Path.of("base-ignore.csv"),
			List.of(new RoseauOptions.Report(Path.of("base.csv"), BreakingChangesFormatterFactory.CSV)));

		var cpOther = new RoseauOptions.Classpath(null, Set.of());
		var exOther = new RoseauOptions.Exclude(List.of(), List.of());
		var commonOther = new RoseauOptions.Common(null, cpOther, exOther);
		var libOther = new RoseauOptions.Library(null, null, cpOther, exOther, null);
		var other = new RoseauOptions(commonOther, libOther, libOther, null, List.of());

		var merged = base.mergeWith(other);
		assertThat(merged).isEqualTo(base);
	}

	@Test
	void merge_with_common_overrides_when_not_set() {
		var cpCommon = new RoseauOptions.Classpath(Path.of("common-pom.xml"), Set.of(Path.of("common.jar")));
		var exCommon = new RoseauOptions.Exclude(
			List.of("common.*"), List.of(new RoseauOptions.AnnotationExclusion("CommonAnn", Map.of("commonK", "commonV"))));
		var common = new RoseauOptions.Common(ExtractorType.JDT, cpCommon, exCommon);

		var cpSet = new RoseauOptions.Classpath(Path.of("set-pom.xml"), Set.of(Path.of("set.jar")));
		var exSet = new RoseauOptions.Exclude(
			List.of("set.*"), List.of(new RoseauOptions.AnnotationExclusion("SetAnn", Map.of("setK", "setV"))));
		var set = new RoseauOptions.Library(Path.of("set"), ExtractorType.ASM, cpSet, exSet, Path.of("a.json"));

		var cpUnset = new RoseauOptions.Classpath(null, Set.of());
		var exUnset = new RoseauOptions.Exclude(List.of(), List.of());
		var unset = new RoseauOptions.Library(Path.of("unset"), null, cpUnset, exUnset, Path.of("a.json"));

		var mergedWithSet = set.mergeWith(common);
		assertThat(mergedWithSet).isEqualTo(set);

		var mergedWithUnset = unset.mergeWith(common);
		assertThat(mergedWithUnset.location()).isEqualTo(Path.of("unset"));
		assertThat(mergedWithUnset.extractor()).isEqualTo(ExtractorType.JDT);
		assertThat(mergedWithUnset.classpath()).isEqualTo(cpCommon);
		assertThat(mergedWithUnset.excludes()).isEqualTo(exCommon);
		assertThat(mergedWithUnset.apiReport()).isEqualTo(Path.of("a.json"));
	}

	@Test
	void load_sets_values(@TempDir Path tmp) throws IOException {
		var yaml = tmp.resolve("opts.yaml");
		var content = """
			common:
			  extractor: JDT
			  classpath:
			    pom: /pom.xml
			    jars: [/cp/a.jar, /cp/b.jar]
			  excludes:
			    names: [com.acme.*]
			    annotations:
			      - name: java.lang.Deprecated
			        args: { since: 1.0 }
			v1:
			  location: /lib/v1
			  extractor: JDT
			  classpath:
			    jars: [/v1.jar]
			  excludes:
			    names: [x]
			  apiReport: /api/v1.json
			v2:
			  location: /lib/v2
			  extractor: ASM
			  classpath:
			    jars: [/v2.jar]
			  excludes:
			    names: [y]
			  apiReport: /api/v2.json
			ignore: /ignore.yaml
			reports:
			  - file: /report.csv
			    format: CSV
			  - file: /report.html
			    format: HTML
			""";
		Files.writeString(yaml, content);

		var options = RoseauOptions.load(yaml);

		assertThat(options.common().extractor()).isEqualTo(ExtractorType.JDT);
		assertThat(options.common().classpath().pom()).isEqualTo(Path.of("/pom.xml"));
		assertThat(options.common().classpath().jars()).contains(Path.of("/cp/a.jar"), Path.of("/cp/b.jar"));
		assertThat(options.common().excludes().names()).containsExactly("com.acme.*");
		assertThat(options.common().excludes().annotations()).hasSize(1);
		assertThat(options.v1().location()).isEqualTo(Path.of("/lib/v1"));
		assertThat(options.v1().apiReport()).isEqualTo(Path.of("/api/v1.json"));
		assertThat(options.v2().location()).isEqualTo(Path.of("/lib/v2"));
		assertThat(options.v2().extractor()).isEqualTo(ExtractorType.ASM);
		assertThat(options.ignore()).isEqualTo(Path.of("/ignore.yaml"));
		assertThat(options.reports()).containsExactly(
			new RoseauOptions.Report(Path.of("/report.csv"), BreakingChangesFormatterFactory.CSV),
			new RoseauOptions.Report(Path.of("/report.html"), BreakingChangesFormatterFactory.HTML));
	}
}
