package io.github.alien.roseau.api.analysis;

public interface ApiAnalyzer extends AssignabilityProvider, ErasureProvider, HierarchyProvider,
	PropertiesProvider, SubtypingProvider, TypeParameterProvider {
	@Override
	default ErasureProvider erasure() {
		return this;
	}

	@Override
	default HierarchyProvider hierarchy() {
		return this;
	}

	@Override
	default SubtypingProvider subtyping() {
		return this;
	}

	@Override
	default TypeParameterProvider typeParameter() {
		return this;
	}

	@Override
	default PropertiesProvider properties() {
		return this;
	}
}
