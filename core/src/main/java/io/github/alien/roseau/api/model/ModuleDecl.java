package io.github.alien.roseau.api.model;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Set;

public sealed class ModuleDecl {
	private final String qualifiedName;
	private final Set<String> exports;

	public ModuleDecl(String qualifiedName, Set<String> exports) {
		Preconditions.checkNotNull(qualifiedName);
		Preconditions.checkNotNull(exports);
		this.qualifiedName = qualifiedName;
		this.exports = Set.copyOf(exports);
	}

	public String getQualifiedName() {
		return qualifiedName;
	}

	public Set<String> getExports() {
		return exports;
	}

	public boolean isExporting(String packageName) {
		return exports.contains(packageName);
	}

	public boolean isUnnamed() {
		return false;
	}

	private static final class UnnamedModule extends ModuleDecl {
		private UnnamedModule(String qualifiedName, Set<String> exports) {
			super(qualifiedName, exports);
		}

		@Override
		public boolean isExporting(String packageName) {
			return true;
		}

		@Override
		public boolean isUnnamed() {
			return true;
		}
	}

	public static final ModuleDecl UNNAMED_MODULE = new UnnamedModule("<unnamed module>", Set.of(""));

	@Override
	public String toString() {
		return "module %s [exports %s]".formatted(qualifiedName, String.join(", ", exports));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ModuleDecl other = (ModuleDecl) obj;
		return Objects.equals(qualifiedName, other.qualifiedName) && Objects.equals(exports, other.exports);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifiedName, exports);
	}
}
