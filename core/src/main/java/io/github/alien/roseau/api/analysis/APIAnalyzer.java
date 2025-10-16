package io.github.alien.roseau.api.analysis;

public interface APIAnalyzer
	extends ErasureProvider, HierarchyProvider, PropertiesProvider, SubtypingResolver, TypeParameterResolver {
	@Override
	default ErasureProvider erasure() {
		return this;
	}

	@Override
	default HierarchyProvider hierarchy() {
		return this;
	}

	@Override
	default SubtypingResolver subtyping() {
		return this;
	}

	@Override
	default TypeParameterResolver typeParameter() {
		return this;
	}

	@Override
	default PropertiesProvider properties() {
		return this;
	}
}
