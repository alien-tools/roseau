package io.github.alien.roseau.extractors.asm;

import io.github.alien.roseau.api.model.ModuleDecl;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;

import java.util.HashSet;
import java.util.Set;

final class AsmModuleVisitor extends ClassVisitor {
	private ModuleDecl module;

	AsmModuleVisitor(int api) {
		super(api);
	}

	@Override
	public ModuleVisitor visitModule(String name, int access, String version) {
		Set<String> exports = new HashSet<>();
		return new ModuleVisitor(api) {
			@Override
			public void visitExport(String packaze, int access, String... modules) {
				if (modules == null || modules.length == 0) {
					exports.add(packaze.replace('/', '.'));
				}
			}

			@Override
			public void visitEnd() {
				module = new ModuleDecl(name, exports);
			}
		};
	}

	ModuleDecl getModuleDecl() {
		return module;
	}
}
