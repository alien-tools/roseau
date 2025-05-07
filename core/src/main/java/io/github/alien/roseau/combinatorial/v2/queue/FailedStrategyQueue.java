package io.github.alien.roseau.combinatorial.v2.queue;

import java.util.concurrent.LinkedBlockingQueue;

public final class FailedStrategyQueue extends AbstractQueue<String> {
	public FailedStrategyQueue() {
		super(new LinkedBlockingQueue<>());
	}
}
