package io.github.alien.roseau.diff;

import io.github.alien.roseau.api.model.API;

public interface ApiComparator<T> {
	T compare(API v1, API v2);
}
