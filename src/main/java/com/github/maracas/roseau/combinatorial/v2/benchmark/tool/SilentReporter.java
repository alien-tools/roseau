package com.github.maracas.roseau.combinatorial.v2.benchmark.tool;

import org.jetbrains.annotations.Nullable;
import org.revapi.AnalysisContext;
import org.revapi.Report;
import org.revapi.ReportComparator;
import org.revapi.Reporter;

import javax.annotation.Nonnull;
import java.io.Reader;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public final class SilentReporter implements Reporter {
	private static final String CONFIG_ROOT_PATH = "revapi.reporter.roseau.silent-reporter";

	private SortedSet<Report> reports;

	public boolean hasReports() {
		return !reports.isEmpty();
	}

	@Override
	public void initialize(@Nonnull AnalysisContext analysis) {
		this.reports = new TreeSet<>(getReportsByElementOrderComparator());
	}

	@Override
	public void report(Report report) {
		if (!report.getDifferences().isEmpty()) {
			reports.add(report);
		}
	}

	@Override
	public String getExtensionId() { return CONFIG_ROOT_PATH; }

	@Nullable
	@Override
	public Reader getJSONSchema() { return null; }

	@Override
	public void close() throws Exception {}

	private static Comparator<Report> getReportsByElementOrderComparator() {
		return (new ReportComparator.Builder()).withComparisonStrategy(ReportComparator.Strategy.HIERARCHICAL).build();
	}
}
