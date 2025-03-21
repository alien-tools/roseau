package io.github.alien.roseau.api.model;

import io.github.alien.roseau.api.model.reference.ReflectiveTypeFactory;

/**
 * An interface representing the capability for deep copying an object.
 * All elements in an {@link API} are deep-copyable to facilitate partial updates of APIs.
 *
 * @param <T> The type of object that is deep-copied.
 */
public interface DeepCopyable<T> {
	T deepCopy();
	T deepCopy(ReflectiveTypeFactory factory);
}
