package io.github.alien.roseau.api.analysis;

import io.github.alien.roseau.api.model.FormalTypeParameter;
import io.github.alien.roseau.api.model.TypeParameterScope;
import io.github.alien.roseau.api.model.reference.ArrayTypeReference;
import io.github.alien.roseau.api.model.reference.ITypeReference;
import io.github.alien.roseau.api.model.reference.TypeParameterReference;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.api.model.reference.WildcardTypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds substitution mappings for type parameter references across API versions. When comparing types from v1 and v2,
 * type parameter references may differ due to renaming ({@code T} → {@code U}) or generization (adding new type
 * parameters). This normalizes type references into a common namespace so that equality and compatibility checks
 * work correctly across versions.
 */
public final class TypeParameterMapping {
	private TypeParameterMapping() {
	}

	/**
	 * Builds a mapping that renames old type parameter references to match v2's namespace. Positionally corresponding
	 * parameters are mapped by name. Old parameters beyond the new list's size (removed in v2) are mapped to their
	 * first bound (erasure).
	 *
	 * @param oldParams the type parameters from the v1 declaration
	 * @param newParams the type parameters from the v2 declaration
	 * @return a substitution map from old parameter names to v2-compatible references
	 */
	public static Map<String, ITypeReference> forwardMapping(List<FormalTypeParameter> oldParams,
	                                                         List<FormalTypeParameter> newParams) {
		Map<String, ITypeReference> mapping = new HashMap<>();
		for (int i = 0; i < oldParams.size(); i++) {
			FormalTypeParameter oldParam = oldParams.get(i);
			if (i < newParams.size()) {
				String newName = newParams.get(i).name();
				if (!oldParam.name().equals(newName)) {
					mapping.put(oldParam.name(), new TypeParameterReference(newName));
				}
			} else {
				mapping.put(oldParam.name(), oldParam.bounds().getFirst());
			}
		}
		return mapping;
	}

	/**
	 * Builds a mapping that replaces newly added type parameter references with their erasure (first bound). Type
	 * parameters at positions that existed in the old declaration are left unmapped. This reflects raw-type semantics:
	 * existing clients that don't supply the new type arguments see erased types.
	 *
	 * @param oldParams the type parameters from the v1 declaration
	 * @param newParams the type parameters from the v2 declaration
	 * @return a substitution map from new-only parameter names to their erasure
	 */
	public static Map<String, ITypeReference> eraseAddedMapping(List<FormalTypeParameter> oldParams,
	                                                            List<FormalTypeParameter> newParams) {
		Map<String, ITypeReference> mapping = new HashMap<>();
		for (int i = oldParams.size(); i < newParams.size(); i++) {
			FormalTypeParameter newParam = newParams.get(i);
			mapping.put(newParam.name(), newParam.bounds().getFirst());
		}
		return mapping;
	}

	/**
	 * Merges two substitution maps into one. If both maps contain the same key, {@code secondary} is overridden by
	 * {@code primary}.
	 */
	public static Map<String, ITypeReference> merge(Map<String, ITypeReference> primary,
	                                                Map<String, ITypeReference> secondary) {
		Map<String, ITypeReference> merged = new HashMap<>(secondary);
		merged.putAll(primary);
		return merged;
	}

	/**
	 * Applies a substitution to a type reference, recursively replacing type parameter references according to the
	 * supplied mapping.
	 *
	 * @param ref     the type reference to transform
	 * @param mapping the substitution map
	 * @return the transformed type reference, or the original if no substitutions apply
	 */
	public static ITypeReference substitute(ITypeReference ref, Map<String, ITypeReference> mapping) {
		if (mapping.isEmpty()) {
			return ref;
		}
		return switch (ref) {
			case TypeParameterReference tpr -> mapping.getOrDefault(tpr.name(), tpr);
			case TypeReference<?> tr -> {
				if (tr.typeArguments().isEmpty()) {
					yield tr;
				}
				List<ITypeReference> newArgs = tr.typeArguments().stream()
					.map(arg -> substitute(arg, mapping))
					.toList();
				yield tr.typeArguments().equals(newArgs) ? tr : new TypeReference<>(tr.qualifiedName(), newArgs);
			}
			case ArrayTypeReference arr -> {
				ITypeReference newComponent = substitute(arr.componentType(), mapping);
				yield newComponent.equals(arr.componentType()) ? arr : new ArrayTypeReference(newComponent, arr.dimension());
			}
			case WildcardTypeReference wc -> {
				List<ITypeReference> newBounds = wc.bounds().stream()
					.map(b -> substitute(b, mapping))
					.toList();
				yield wc.bounds().equals(newBounds) ? wc : new WildcardTypeReference(newBounds, wc.upper());
			}
			default -> ref;
		};
	}

	public static final class Normalizer {
		private final Map<String, ITypeReference> forwardMap;
		private final Map<String, ITypeReference> eraseAddedMap;

		private Normalizer(Map<String, ITypeReference> forwardMap, Map<String, ITypeReference> eraseAddedMap) {
			this.forwardMap = forwardMap;
			this.eraseAddedMap = eraseAddedMap;
		}

		/**
		 * Renames old type parameter references to match v2's namespace.
		 */
		public ITypeReference normalizeOld(ITypeReference ref) {
			return substitute(ref, forwardMap);
		}

		/**
		 * Replaces newly-added type parameters with their erasure (raw-type semantics).
		 */
		public ITypeReference normalizeNew(ITypeReference ref) {
			return substitute(ref, eraseAddedMap);
		}

		/**
		 * Creates a normalizer for type-level type parameters only (for fields, type-level bounds).
		 *
		 * @param oldType the v1 type declaration
		 * @param newType the v2 type declaration
		 * @return a normalizer that handles type-level type parameter renaming and erasure
		 */
		public static Normalizer forType(TypeParameterScope oldType, TypeParameterScope newType) {
			Map<String, ITypeReference> forwardMap = forwardMapping(
				oldType.getFormalTypeParameters(), newType.getFormalTypeParameters());
			Map<String, ITypeReference> eraseAddedMap = eraseAddedMapping(
				oldType.getFormalTypeParameters(), newType.getFormalTypeParameters());
			return new Normalizer(forwardMap, eraseAddedMap);
		}

		/**
		 * Creates a normalizer for executable-level type parameters, with executable params shadowing type params.
		 *
		 * @param oldType the v1 type declaration
		 * @param newType the v2 type declaration
		 * @param oldExec the v1 executable declaration
		 * @param newExec the v2 executable declaration
		 * @return a normalizer that handles both type-level and executable-level type parameter normalization
		 */
		public static Normalizer forExecutable(TypeParameterScope oldType, TypeParameterScope newType,
		                                       TypeParameterScope oldExec, TypeParameterScope newExec) {
			Map<String, ITypeReference> typeForward = forwardMapping(
				oldType.getFormalTypeParameters(), newType.getFormalTypeParameters());
			Map<String, ITypeReference> execForward = forwardMapping(
				oldExec.getFormalTypeParameters(), newExec.getFormalTypeParameters());
			Map<String, ITypeReference> forwardMap = merge(execForward, typeForward);

			Map<String, ITypeReference> typeErase = eraseAddedMapping(
				oldType.getFormalTypeParameters(), newType.getFormalTypeParameters());
			Map<String, ITypeReference> execErase = eraseAddedMapping(
				oldExec.getFormalTypeParameters(), newExec.getFormalTypeParameters());
			Map<String, ITypeReference> eraseAddedMap = merge(execErase, typeErase);

			return new Normalizer(forwardMap, eraseAddedMap);
		}
	}
}
