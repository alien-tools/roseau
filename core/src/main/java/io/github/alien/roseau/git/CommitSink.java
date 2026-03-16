package io.github.alien.roseau.git;

/**
 * Receives {@link CommitAnalysis} results during a {@link GitWalker#walk(CommitSink)}.
 */
@FunctionalInterface
public interface CommitSink {
	void accept(CommitAnalysis analysis);
}
