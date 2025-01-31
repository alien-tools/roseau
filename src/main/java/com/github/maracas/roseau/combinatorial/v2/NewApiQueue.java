package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.API;
import org.javatuples.Pair;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class NewApiQueue {
	private final BlockingQueue<Pair<String, API>> queue;

	public NewApiQueue(int maxSize) {
		queue = new ArrayBlockingQueue<>(maxSize);
	}

	public void put(Pair<String, API> strategyAndApi) {
		try {
			queue.put(strategyAndApi);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public Pair<String, API> take() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public boolean hasStillWork() {
		return !queue.isEmpty();
	}
}
