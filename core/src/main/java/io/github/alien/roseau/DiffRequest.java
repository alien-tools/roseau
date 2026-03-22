package io.github.alien.roseau;

import com.google.common.base.Preconditions;

/**
 * End-to-end request for comparing two libraries under a given {@link DiffPolicy}.
 *
 * @param v1     the baseline library
 * @param v2     the target library
 * @param policy the policy applied to the diff result
 */
public record DiffRequest(Library v1, Library v2, DiffPolicy policy) {
	public DiffRequest {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		policy = policy == null ? DiffPolicy.none() : policy;
	}

	public DiffRequest(Library v1, Library v2) {
		this(v1, v2, DiffPolicy.none());
	}
}
