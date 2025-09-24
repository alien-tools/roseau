package io.github.alien.roseau.api.analysis;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.resolution.TypeResolver;

public class APIAnalyzer
	implements ErasureProvider, HierarchyProvider, PropertiesProvider, SubtypingResolver, TypeParameterResolver {
	private final TypeResolver resolver;

	protected APIAnalyzer(TypeResolver resolver) {
		Preconditions.checkNotNull(resolver);
		this.resolver = resolver;
	}

	@Override
	public TypeResolver resolver() {
		return resolver;
	}

	@Override
	public ErasureProvider erasure() {
		return this;
	}

	@Override
	public HierarchyProvider hierarchy() {
		return this;
	}

	@Override
	public SubtypingResolver subtyping() {
		return this;
	}

	@Override
	public TypeParameterResolver typeParameter() {
		return this;
	}
}
