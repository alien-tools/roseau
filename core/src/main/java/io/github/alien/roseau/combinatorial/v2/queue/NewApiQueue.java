package io.github.alien.roseau.combinatorial.v2.queue;

import io.github.alien.roseau.api.model.API;
import org.javatuples.Pair;

import java.util.concurrent.ArrayBlockingQueue;

public final class NewApiQueue extends AbstractQueue<Pair<String, API>> {
	public NewApiQueue(int maxSize) {
		super(new ArrayBlockingQueue<>(maxSize));
	}
}
