package io.github.alien.roseau;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Post-diff policy controlling which breaking changes are visible in the final report.
 */
public final class DiffPolicy {
	public enum Scope {
		ALL {
			@Override
			boolean includes(BreakingChangeKind kind) {
				return true;
			}
		},
		SOURCE_ONLY {
			@Override
			boolean includes(BreakingChangeKind kind) {
				return kind.isSourceBreaking();
			}
		},
		BINARY_ONLY {
			@Override
			boolean includes(BreakingChangeKind kind) {
				return kind.isBinaryBreaking();
			}
		};

		abstract boolean includes(BreakingChangeKind kind);
	}

	private record SymbolExclusions(List<Pattern> namePatterns, List<AnnotationExclusion> annotations) {
		private SymbolExclusions {
			namePatterns = List.copyOf(namePatterns);
			annotations = List.copyOf(annotations);
		}

		boolean excludes(Symbol symbol, API baselineApi) {
			boolean isAnnotationExcluded = annotations.stream()
				.anyMatch(ann -> symbol.hasAnnotation(new TypeReference<>(ann.name()), ann.args()));
			boolean isNameExcluded = namePatterns.stream()
				.anyMatch(pattern -> pattern.matcher(symbol.getQualifiedName()).matches());

			return switch (symbol) {
				case TypeDecl type -> isAnnotationExcluded || isNameExcluded ||
					type.getEnclosingType()
						.map(t -> baselineApi.resolver().resolve(t).map(resolved -> excludes(resolved, baselineApi)).orElse(false))
						.orElse(false);
				case TypeMemberDecl member -> isAnnotationExcluded || isNameExcluded ||
					baselineApi.resolver().resolve(member.getContainingType())
						.map(resolved -> excludes(resolved, baselineApi))
						.orElse(false);
			};
		}

		boolean isEmpty() {
			return namePatterns.isEmpty() && annotations.isEmpty();
		}
	}

	public record AnnotationExclusion(String name, Map<String, String> args) {
		public AnnotationExclusion {
			Preconditions.checkArgument(name != null && !name.isBlank(), "Annotation exclusion name must not be blank");
			args = args != null ? Map.copyOf(args) : Map.of();
		}
	}

	public record IgnoredBreakingChange(String type, String symbol, BreakingChangeKind kind) {
		public IgnoredBreakingChange {
			Preconditions.checkArgument(type != null && !type.isBlank(), "Ignored type must not be blank");
			Preconditions.checkArgument(symbol != null && !symbol.isBlank(), "Ignored symbol must not be blank");
			Preconditions.checkNotNull(kind);
		}

		public boolean matches(BreakingChange bc) {
			return bc.impactedType().getQualifiedName().equals(type)
				&& bc.impactedSymbol().getQualifiedName().equals(symbol)
				&& bc.kind() == kind;
		}
	}

	private static final DiffPolicy NONE = new DiffPolicy(
		Scope.ALL,
		new SymbolExclusions(List.of(), List.of()),
		Set.of()
	);

	private final Scope scope;
	private final SymbolExclusions symbolExclusions;
	private final Set<IgnoredBreakingChange> ignoredBreakingChanges;

	private DiffPolicy(Scope scope, SymbolExclusions symbolExclusions, Set<IgnoredBreakingChange> ignored) {
		this.scope = Preconditions.checkNotNull(scope);
		this.symbolExclusions = Preconditions.checkNotNull(symbolExclusions);
		this.ignoredBreakingChanges = Set.copyOf(ignored);
	}

	public static DiffPolicy empty() {
		return NONE;
	}

	public static Builder builder() {
		return new Builder();
	}

	public List<BreakingChange> filter(List<BreakingChange> breakingChanges, API baselineApi) {
		Preconditions.checkNotNull(breakingChanges);
		Preconditions.checkNotNull(baselineApi);
		return breakingChanges.stream()
			.filter(bc -> includes(bc, baselineApi))
			.toList();
	}

	public boolean includes(BreakingChange breakingChange, API baselineApi) {
		Preconditions.checkNotNull(breakingChange);
		Preconditions.checkNotNull(baselineApi);
		return scope.includes(breakingChange.kind())
			&& ignoredBreakingChanges.stream().noneMatch(ignored -> ignored.matches(breakingChange))
			&& !symbolExclusions.excludes(breakingChange.impactedSymbol(), baselineApi)
			&& !symbolExclusions.excludes(breakingChange.impactedType(), baselineApi);
	}

	public Scope scope() {
		return scope;
	}

	public static final class Builder {
		private Scope scope = Scope.ALL;
		private final List<Pattern> excludedNamePatterns = new ArrayList<>(10);
		private final List<AnnotationExclusion> excludedAnnotations = new ArrayList<>(10);
		private final Set<IgnoredBreakingChange> ignoredBreakingChanges = HashSet.newHashSet(10);

		private Builder() {
		}

		public Builder scope(Scope scope) {
			this.scope = Preconditions.checkNotNull(scope);
			return this;
		}

		public Builder excludeNames(List<Pattern> patterns) {
			if (patterns != null) {
				patterns.stream()
					.filter(Objects::nonNull)
					.forEach(excludedNamePatterns::add);
			}
			return this;
		}

		public Builder excludeAnnotations(List<AnnotationExclusion> annotations) {
			if (annotations != null) {
				annotations.stream()
					.filter(Objects::nonNull)
					.forEach(excludedAnnotations::add);
			}
			return this;
		}

		public Builder ignoreBreakingChanges(List<IgnoredBreakingChange> breakingChanges) {
			if (breakingChanges != null) {
				breakingChanges.stream()
					.filter(Objects::nonNull)
					.forEach(ignoredBreakingChanges::add);
			}
			return this;
		}

		public DiffPolicy build() {
			SymbolExclusions symbolExclusions = new SymbolExclusions(excludedNamePatterns, excludedAnnotations);
			return new DiffPolicy(scope, symbolExclusions, ignoredBreakingChanges);
		}
	}
}
