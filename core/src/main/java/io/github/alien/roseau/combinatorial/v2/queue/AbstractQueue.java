package io.github.alien.roseau.combinatorial.v2.queue;

import org.javatuples.Pair;

import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.SECONDS;

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

	public Pair<String, T> poll() {
		try {
			return queue.poll(5, SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public boolean hasStillWork() {
		return !queue.isEmpty();
	}
}
