package io.github.alien.roseau.combinatorial.v2.queue;

import io.github.alien.roseau.combinatorial.v2.benchmark.result.ToolResult;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public final class ResultsProcessQueue extends AbstractQueue<List<ToolResult>> {
	private static ResultsProcessQueue _instance = null;

	private ResultsProcessQueue() {
		super(new LinkedBlockingQueue<>());
	}

	public static ResultsProcessQueue getInstance() {
		if (_instance == null) {
			_instance = new ResultsProcessQueue();
		}
		return _instance;
	}
}
