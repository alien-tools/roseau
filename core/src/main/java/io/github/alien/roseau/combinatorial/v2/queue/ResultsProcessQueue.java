package io.github.alien.roseau.combinatorial.v2.queue;

import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public final class ResultsProcessQueue extends AbstractQueue<List<ToolResult>> {
	public ResultsProcessQueue() {
		super(new LinkedBlockingQueue<>());
	}
}
