package io.github.alien.roseau.diff;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.github.alien.roseau.DiffPolicy;
import io.github.alien.roseau.RoseauException;
import io.github.alien.roseau.api.model.API;
import io.github.alien.roseau.api.model.SourceLocation;
import io.github.alien.roseau.api.model.Symbol;
import io.github.alien.roseau.api.model.TypeDecl;
import io.github.alien.roseau.api.model.TypeMemberDecl;
import io.github.alien.roseau.api.model.reference.TypeReference;
import io.github.alien.roseau.diff.changes.BreakingChange;
import io.github.alien.roseau.diff.changes.BreakingChangeDetails;
import io.github.alien.roseau.diff.changes.BreakingChangeKind;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatter;
import io.github.alien.roseau.diff.formatter.BreakingChangesFormatterFactory;
import io.github.alien.roseau.diff.formatter.CliFormatter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record RoseauReport(API v1, API v2, List<BreakingChange> breakingChanges) {
	public RoseauReport(API v1, API v2, List<BreakingChange> breakingChanges) {
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

	public List<BreakingChange> getBinaryBreakingChanges() {
		return breakingChanges.stream()
			.filter(bc -> bc.kind().isBinaryBreaking())
			.toList();
	}

	public List<BreakingChange> getSourceBreakingChanges() {
		return breakingChanges.stream()
			.filter(bc -> bc.kind().isSourceBreaking())
			.toList();
	}

	public boolean isBinaryBreaking() {
		return !getBinaryBreakingChanges().isEmpty();
	}

	public boolean isSourceBreaking() {
		return !getSourceBreakingChanges().isEmpty();
	}

	public List<TypeDecl> getImpactedTypes() {
		return breakingChanges.stream()
			.map(BreakingChange::impactedType)
			.distinct()
			.toList();
	}

	public List<BreakingChange> getBreakingChanges(TypeDecl type) {
		return breakingChanges.stream()
			.filter(bc -> bc.impactedType().equals(type))
			.toList();
	}

	public List<BreakingChange> getTypeBreakingChanges(TypeDecl type) {
		return breakingChanges.stream()
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
		return breakingChanges.stream()
			.filter(bc -> type.equals(bc.impactedType()))
			.filter(bc -> bc.impactedSymbol() instanceof TypeMemberDecl)
			.collect(Collectors.groupingBy(
				bc -> (TypeMemberDecl) bc.impactedSymbol(),
				() -> new TreeMap<>(Comparator.comparing(TypeMemberDecl::getQualifiedName)),
				Collectors.toList()
			));
	}

	public RoseauReport filter(DiffPolicy policy) {
		Preconditions.checkNotNull(policy);
		return new RoseauReport(v1, v2, policy.filter(breakingChanges, v1));
	}

	public void writeReport(BreakingChangesFormatterFactory format, Path path) {
		Preconditions.checkNotNull(format);
		Preconditions.checkNotNull(path);
		try {
			Path parent = path.toAbsolutePath().normalize().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			BreakingChangesFormatter fmt = format == BreakingChangesFormatterFactory.CLI
				? new CliFormatter(CliFormatter.Mode.PLAIN)
				: BreakingChangesFormatterFactory.newBreakingChangesFormatter(format);
			Files.writeString(path, fmt.format(this), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RoseauException("Error writing report to %s".formatted(path), e);
		}
	}

	public static Builder builder(API v1, API v2) {
		return new Builder(v1, v2);
	}

	public static final class Builder {
		private final API v1;
		private final API v2;
		private final Set<BreakingChange> bcs = Sets.newConcurrentHashSet();

		public Builder(API v1, API v2) {
			Preconditions.checkNotNull(v1);
			Preconditions.checkNotNull(v2);
			this.v1 = v1;
			this.v2 = v2;
		}

		public void typeBC(BreakingChangeKind kind, TypeDecl impactedType) {
			typeBC(kind, impactedType, new BreakingChangeDetails.None());
		}

		public void typeBC(BreakingChangeKind kind, TypeDecl impactedType, BreakingChangeDetails details) {
			bcs.add(new BreakingChange(kind, impactedType, impactedType, null, details, impactedType.getLocation()));
		}

		public void typeBC(BreakingChangeKind kind, TypeDecl impactedType, Symbol newSymbol,
		                   BreakingChangeDetails details) {
			bcs.add(new BreakingChange(kind, impactedType, impactedType, newSymbol, details, impactedType.getLocation()));
		}

		public void memberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl impactedMember) {
			memberBC(kind, impactedType, impactedMember, null, new BreakingChangeDetails.None());
		}

		public void memberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl impactedMember,
		                     TypeMemberDecl newMember) {
			memberBC(kind, impactedType, impactedMember, newMember, new BreakingChangeDetails.None());
		}

		public void memberBC(BreakingChangeKind kind, TypeDecl impactedType, TypeMemberDecl impactedMember,
		                     TypeMemberDecl newMember, BreakingChangeDetails details) {
			// java.lang.Object methods are an absolute pain to handle. Many rules
			// do not apply to them as they're implicitly provided to any class.
			if (impactedMember.getContainingType().equals(TypeReference.OBJECT)) {
				return;
			}
			bcs.add(new BreakingChange(kind, impactedType, impactedMember, newMember, details,
				reportLocation(impactedType, impactedMember)));
		}

		private SourceLocation reportLocation(TypeDecl impactedType, TypeMemberDecl impactedMember) {
			return isLibraryType(impactedMember.getContainingType())
				? impactedMember.getLocation()
				: impactedType.getLocation();
		}

		private boolean isLibraryType(TypeReference<TypeDecl> type) {
			return v1.getLibraryTypes().findType(type.getQualifiedName()).isPresent();
		}

		public RoseauReport build() {
			return new RoseauReport(v1, v2, bcs.stream().toList());
		}
	}
}
