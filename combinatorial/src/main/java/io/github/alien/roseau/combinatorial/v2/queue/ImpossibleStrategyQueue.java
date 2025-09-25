package io.github.alien.roseau.combinatorial.v2.queue;

import java.util.concurrent.LinkedBlockingQueue;

public final class ImpossibleStrategyQueue extends AbstractQueue<String> {
	private static ImpossibleStrategyQueue _instance = null;

	private ImpossibleStrategyQueue() {
		super(new LinkedBlockingQueue<>());
	}

	public static ImpossibleStrategyQueue getInstance() {
		if (_instance == null) {
			_instance = new ImpossibleStrategyQueue();
		}
		return _instance;
	}
}
