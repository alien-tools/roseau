package com.github.maracas.roseau.combinatorial.v2.queue;

import org.javatuples.Pair;

import java.util.concurrent.BlockingQueue;

public abstract sealed class AbstractQueue<T> permits NewApiQueue, ResultsProcessQueue {
	private final BlockingQueue<Pair<String, T>> queue;

	public AbstractQueue(BlockingQueue<Pair<String, T>> queue) {
		this.queue = queue;
	}

	public void put(String strategy, T data) {
		try {
			queue.put(new Pair<>(strategy, data));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public Pair<String, T> take() {
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
