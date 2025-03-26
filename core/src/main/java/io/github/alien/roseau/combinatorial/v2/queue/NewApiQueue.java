package io.github.alien.roseau.combinatorial.v2.queue;

import io.github.alien.roseau.api.model.API;

import java.util.concurrent.ArrayBlockingQueue;

public final class NewApiQueue extends AbstractQueue<API> {
	public NewApiQueue(int maxSize) {
		super(new ArrayBlockingQueue<>(maxSize));
	}
}
