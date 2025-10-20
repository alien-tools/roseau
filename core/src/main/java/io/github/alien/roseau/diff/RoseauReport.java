package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class RoseauReport {
	private final API v1;
	private final API v2;
	private final List<BreakingChange> breakingChanges;

	public RoseauReport(API v1, API v2, Collection<BreakingChange> breakingChanges) {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(breakingChanges);
		this.v1 = v1;
		this.v2 = v2;
		this.breakingChanges = List.copyOf(
			breakingChanges.stream()
				.sorted(
					Comparator.comparing((BreakingChange bc) -> bc.impactedType().getQualifiedName())
						.thenComparing(bc -> bc.impactedSymbol().getQualifiedName())
						.thenComparing(BreakingChange::kind))
				.toList());
	}

	public API v1() {
		return v1;
	}

	public API v2() {
		return v2;
	}

	public List<BreakingChange> getBreakingChanges() {
		return breakingChanges.stream()
			.filter(bc -> !v1.isExcluded(bc.impactedSymbol()))
			.toList();
	}

	public List<BreakingChange> getAllBreakingChanges() {
		return breakingChanges;
	}

	public boolean isBinaryBreaking() {
		return getBreakingChanges().stream().anyMatch(bc -> bc.kind().isBinaryBreaking());
	}

	public boolean isSourceBreaking() {
		return getBreakingChanges().stream().anyMatch(bc -> bc.kind().isSourceBreaking());
	}

	public List<TypeDecl> getImpactedTypes() {
		return getBreakingChanges().stream()
			.map(BreakingChange::impactedType)
			.distinct()
			.toList();
	}

	public List<BreakingChange> getBreakingChanges(TypeDecl type) {
		return getBreakingChanges().stream()
			.filter(bc -> bc.impactedType().equals(type))
			.toList();
	}

	public List<BreakingChange> getTypeBreakingChanges(TypeDecl type) {
		return getBreakingChanges().stream()
			.filter(bc -> bc.impactedSymbol().equals(type))
			.toList();
	}

	public boolean isBinaryBreakingType(TypeDecl type) {
		return getBreakingChanges(type).stream().anyMatch(bc -> bc.kind().isBinaryBreaking());
	}

	public boolean isSourceBreakingType(TypeDecl type) {
		return getBreakingChanges(type).stream().anyMatch(bc -> bc.kind().isSourceBreaking());
	}

	public Map<TypeMemberDecl, List<BreakingChange>> getBreakingChangesPerMember(TypeDecl type) {
		return getBreakingChanges().stream()
			.filter(bc -> type.equals(bc.impactedType()))
			.filter(bc -> bc.impactedSymbol() instanceof TypeMemberDecl)
			.collect(Collectors.groupingBy(
				bc -> (TypeMemberDecl) bc.impactedSymbol(),
				() -> new TreeMap<>(Comparator.comparing(TypeMemberDecl::getQualifiedName)),
				Collectors.toList()
			));
	}

	// FIXME: Do the exclusion/java.lang.Object here, through another class
	static final class Builder {
		private final API v1;
		private final API v2;
		private final Set<BreakingChange> bcs = Sets.newConcurrentHashSet();

		Builder(API v1, API v2) {
			Preconditions.checkNotNull(v1);
			Preconditions.checkNotNull(v2);
			this.v1 = v1;
			this.v2 = v2;
		}

		void typeBC(BreakingChangeKind kind, TypeDecl impactedType) {
			typeBC(kind, impactedType, new BreakingChangeDetails.None());
		}

		void typeBC(BreakingChangeKind kind, TypeDecl impactedType, BreakingChangeDetails details) {
			bcs.add(new BreakingChange(kind, impactedType, impactedType, null, details));
		}

		void memberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl impactedMember) {
			memberBC(kind, impactedType, impactedMember, null, new BreakingChangeDetails.None());
		}

		void memberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl impactedMember,
		              TypeMemberDecl newMember) {
			memberBC(kind, impactedType, impactedMember, newMember, new BreakingChangeDetails.None());
		}

		void memberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl impactedMember,
		              TypeMemberDecl newMember, BreakingChangeDetails details) {
			// java.lang.Object methods are an absolute pain to handle. Many rules
			// do not apply to them as they're implicitly provided to any class.
			if (impactedMember.getContainingType().equals(TypeReference.OBJECT)) {
				return;
			}
			bcs.add(new BreakingChange(kind, impactedType, impactedMember, newMember, details));
		}

		RoseauReport build() {
			return new RoseauReport(v1, v2, bcs.stream().toList());
		}
	}
}
