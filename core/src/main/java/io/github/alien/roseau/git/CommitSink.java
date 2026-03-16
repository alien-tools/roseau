package io.github.alien.roseau.git;

/**
 * Receives {@link CommitAnalysis} results during a walk.
 */
@FunctionalInterface
public interface CommitSink {
	void accept(CommitAnalysis analysis) throws Exception;
}
