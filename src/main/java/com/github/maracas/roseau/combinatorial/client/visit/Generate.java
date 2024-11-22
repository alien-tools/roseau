package com.github.maracas.roseau.combinatorial.client.visit;

import java.util.Map;

@FunctionalInterface
public interface Generate {
	Map<String, String> generate();
}
