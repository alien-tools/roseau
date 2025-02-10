package com.github.maracas.roseau.spoon;

import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.compiler.jdt.JDTBatchCompiler;
import spoon.support.compiler.jdt.TreeBuilderRequestor;

class FastJDTBatchCompiler extends JDTBatchCompiler {
	private INameEnvironment nameEnvironment;
	private TreeBuilderRequestor requestor;

	FastJDTBatchCompiler(JDTBasedSpoonCompiler jdtCompiler, INameEnvironment nameEnvironment,
	                            TreeBuilderRequestor requestor) {
		super(jdtCompiler);
		this.nameEnvironment = nameEnvironment;
		this.requestor = requestor;
	}

	@Override
	public CompilationUnitDeclaration[] getUnits() {
		startTime = System.currentTimeMillis();
		if (nameEnvironment == null) {
			nameEnvironment = getLibraryAccess();
		}
		CompilerOptions compilerOptions = new CompilerOptions(this.options);
		compilerOptions.ignoreMethodBodies = true;
		compilerOptions.parseLiteralExpressionsAsConstants = false;

		IProblemFactory problemFactory = getProblemFactory();
		FastJDTCompiler treeBuilderCompiler = new FastJDTCompiler(nameEnvironment, noErrorPolicy, compilerOptions,
			requestor, problemFactory, this.out, noCompilationProgress);

		treeBuilderCompiler.lookupEnvironment.problemReporter = new ProblemReporter(noErrorPolicy, compilerOptions, problemFactory) {
			@Override
			public int computeSeverity(int problemID) {
				// ignore all the problem and continue the build creation
				return 256;
			}
		};
		treeBuilderCompiler.lookupEnvironment.mayTolerateMissingType = true;

		// they have to be done all at once
		return treeBuilderCompiler.buildUnits(getCompilationUnits());
	}

	private IErrorHandlingPolicy noErrorPolicy = new IErrorHandlingPolicy() {
		@Override
		public boolean proceedOnErrors() {
			return true;
		}

		@Override
		public boolean stopOnFirstError() {
			return false;
		}

		// we cannot ignore them, because JDT will continue its process
		// and it led to NPE in several places
		@Override
		public boolean ignoreAllErrors() {
			return false;
		}
	};

	private CompilationProgress noCompilationProgress = new CompilationProgress() {
		@Override
		public void begin(int remainingWork) {}
		@Override
		public void done() {}
		@Override
		public boolean isCanceled() {
			return false;
		}
		@Override
		public void setTaskName(String name) {}
		@Override
		public void worked(int workIncrement, int remainingWork) {}
	};
}
