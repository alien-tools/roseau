package io.github.alien.roseau.combinatorial.v2.queue;

import java.util.concurrent.LinkedBlockingQueue;

public final class FailedStrategyQueue extends AbstractQueue<String> {
	private static FailedStrategyQueue _instance = null;

	public FailedStrategyQueue() {
		super(new LinkedBlockingQueue<>());
	}

	public static FailedStrategyQueue getInstance() {
		if (_instance == null) {
			_instance = new FailedStrategyQueue();
		}
		return _instance;
	}
}
