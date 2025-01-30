package com.github.maracas.roseau.combinatorial.v2;

import com.github.maracas.roseau.api.model.API;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class NewApiQueue {
	private final BlockingQueue<API> queue;

	public NewApiQueue(int maxSize) {
		queue = new ArrayBlockingQueue<>(maxSize);
	}

	public void put(API api) {
		try {
			queue.put(api);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public API take() {
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
