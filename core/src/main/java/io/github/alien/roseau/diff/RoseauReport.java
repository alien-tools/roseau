package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.ExecutableDecl;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.diff.changes.BreakingChange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record RoseauReport(
	API v1,
	API v2,
	List<BreakingChange> breakingChanges
) {
	public RoseauReport {
		Preconditions.checkNotNull(v1);
		Preconditions.checkNotNull(v2);
		Preconditions.checkNotNull(breakingChanges);
		breakingChanges = List.copyOf(
			breakingChanges.stream()
				.sorted(Comparator.comparing(bc -> bc.impactedSymbol().getQualifiedName()))
				.toList());
	}

	public boolean isBinaryBreaking() {
		return breakingChanges.stream().anyMatch(bc -> bc.kind().isBinaryBreaking());
	}

	public boolean isSourceBreaking() {
		return breakingChanges.stream().anyMatch(bc -> bc.kind().isSourceBreaking());
	}

	public List<TypeDecl> impactedTypes() {
		return impactedTypes(null);
	}

	public List<TypeDecl> impactedTypes(String pkg) {
		return breakingChanges.stream()
			.map(BreakingChange::impactedSymbol)
			.filter(symbol -> pkg == null || pkg.startsWith(symbol.getQualifiedName()))
			.map(symbol -> switch (symbol) {
				case TypeDecl type -> type;
				case TypeMemberDecl member ->
					// This one should be safe
					v1.resolver().resolve(member.getContainingType()).orElseThrow();
			})
			.distinct()
			.sorted(Comparator.comparing(TypeDecl::getQualifiedName))
			.toList();
	}

	public List<BreakingChange> breakingChangesOnType(TypeDecl type) {
		return breakingChanges.stream()
			.filter(bc -> bc.impactedSymbol().equals(type))
			.toList();
	}

	public List<BreakingChange> breakingChangesOnTypeAndMembers(TypeDecl type) {
		return breakingChanges.stream()
			.filter(bc -> switch (bc.impactedSymbol()) {
				case TypeDecl typeDecl -> type.getQualifiedName().equals(typeDecl.getQualifiedName());
				case TypeMemberDecl member -> type.getQualifiedName().equals(member.getContainingType().getQualifiedName());
			})
			.toList();
	}

	public boolean isBinaryBreakingType(TypeDecl type) {
		return breakingChangesOnTypeAndMembers(type).stream().anyMatch(bc -> bc.kind().isBinaryBreaking());
	}

	public boolean isSourceBreakingType(TypeDecl type) {
		return breakingChangesOnTypeAndMembers(type).stream().anyMatch(bc -> bc.kind().isSourceBreaking());
	}

	/**
	 * Returns member-level breaking changes grouped by member display name for the given type. The display name is the
	 * signature for methods/constructors and simple name for fields.
	 */
	public Map<String, List<BreakingChange>> memberChanges(String typeQualifiedName) {
		Map<String, List<BreakingChange>> grouped = new LinkedHashMap<>();
		for (BreakingChange bc : breakingChanges) {
			if (bc.impactedSymbol() instanceof TypeMemberDecl tmd) {
				String owner = v1.resolver().resolve(tmd.getContainingType())
					.map(TypeDecl::getQualifiedName)
					.orElseGet(() -> tmd.getContainingType().getQualifiedName());
				if (Objects.equals(owner, typeQualifiedName)) {
					String key = memberDisplayName(tmd);
					grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(bc);
				}
			}
		}
		// preserve insertion but present keys sorted for determinism
		Map<String, List<BreakingChange>> sorted = new TreeMap<>(grouped);
		sorted.putAll(grouped);
		return sorted;
	}

	/**
	 * Best-effort location of the type in the baseline API (if available).
	 */
	public SourceLocation typeLocation(String typeQualifiedName) {
		return v1.findExportedType(typeQualifiedName)
			.map(TypeDecl::getLocation)
			.orElse(null);
	}

	private static String memberDisplayName(TypeMemberDecl m) {
		if (m instanceof ExecutableDecl e) return e.getSignature();
		return m.getSimpleName();
	}
}
