package io.github.alien.roseau.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.Set;

/**
 * A module declaration within the library ({@code module-info.java}).
 */
public sealed class ModuleDecl {
	/**
	 * The fully qualified name of the module.
	 */
	private final String qualifiedName;
	/**
	 * The set of packages exported by the module.
	 */
	private final Set<String> exports;
	/**
	 * The (unique) unnamed module.
	 */
	public static final ModuleDecl UNNAMED_MODULE = new UnnamedModule();
	private static final String UNNAMED_MODULE_NAME = "<unnamed module>";

	@JsonIgnore
	public ModuleDecl(String qualifiedName, Set<String> exports) {
		Preconditions.checkNotNull(qualifiedName);
		Preconditions.checkNotNull(exports);
		this.qualifiedName = qualifiedName;
		this.exports = Set.copyOf(exports);
	}

	// We really want our UNNAMED_MODULE when it is one
	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public static ModuleDecl create(String qualifiedName, Set<String> exports) {
		if (UNNAMED_MODULE_NAME.equals(qualifiedName)) {
			return UNNAMED_MODULE;
		}
		Set<String> ex = (exports == null) ? Set.of() : Set.copyOf(exports);
		return new ModuleDecl(qualifiedName, ex);
	}

	/**
	 * Returns the qualified name of the module.
	 *
	 * @return the qualified name of the module
	 */
	public String getQualifiedName() {
		return qualifiedName;
	}

	/**
	 * Returns the set of exported package names.
	 *
	 * @return the set of exported package names
	 */
	public Set<String> getExports() {
		return exports;
	}

	/**
	 * Returns true if the module exports the given package name.
	 *
	 * @param packageName the package name to check
	 * @return true if the module exports the given package name
	 */
	public boolean isExporting(String packageName) {
		return exports.contains(packageName);
	}

	/**
	 * Checks whether this module is the unnamed module.
	 *
	 * @return true if this module is the unnamed module
	 */
	public boolean isUnnamed() {
		return false;
	}

	private static final class UnnamedModule extends ModuleDecl {
		private UnnamedModule() {
			super(UNNAMED_MODULE_NAME, Set.of());
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return obj instanceof ModuleDecl other
			&& Objects.equals(qualifiedName, other.qualifiedName)
			&& Objects.equals(exports, other.exports);
	}

	@Override
	public int hashCode() {
		return Objects.hash(qualifiedName, exports);
	}
}
