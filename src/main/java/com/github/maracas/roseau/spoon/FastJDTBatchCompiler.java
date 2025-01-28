package com.github.maracas.roseau.spoon;

import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.problem.ProblemReporter;
import org.eclipse.jdt.internal.core.util.CommentRecorderParser;
import spoon.compiler.Environment;
import spoon.support.compiler.SpoonProgress;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.compiler.jdt.JDTBatchCompiler;
import spoon.support.compiler.jdt.TreeBuilderRequestor;

import java.util.Locale;

class FastJDTBatchCompiler extends JDTBatchCompiler {
	private INameEnvironment nameEnvironment;
	private TreeBuilderRequestor requestor;
	private Environment spoonEnvironment;

	FastJDTBatchCompiler(JDTBasedSpoonCompiler jdtCompiler, INameEnvironment nameEnvironment,
	                            TreeBuilderRequestor requestor, Environment spoonEnvironment) {
		super(jdtCompiler);
		this.nameEnvironment = nameEnvironment;
		this.requestor = requestor;
		this.spoonEnvironment = spoonEnvironment;
	}

	@Override
	public CompilationUnitDeclaration[] getUnits() {
		startTime = System.currentTimeMillis();
		if (nameEnvironment == null) {
			nameEnvironment = getLibraryAccess();
		}
		CompilerOptions compilerOptions = new CompilerOptions(this.options);
		//compilerOptions.ignoreMethodBodies = true;
		compilerOptions.parseLiteralExpressionsAsConstants = false;

		IErrorHandlingPolicy errorHandlingPolicy;

		if (spoonEnvironment.getNoClasspath()) {

			// in no classpath, we should proceed on error,
			// as we will encounter some
			errorHandlingPolicy = new IErrorHandlingPolicy() {
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
		} else {

			// when there is a classpath, we should not have any error
			errorHandlingPolicy = new IErrorHandlingPolicy() {
				@Override
				public boolean proceedOnErrors() {
					return false;
				}

				// we wait for all errors to be gathered before stopping
				@Override
				public boolean stopOnFirstError() {
					return false;
				}

				@Override
				public boolean ignoreAllErrors() {
					return false;
				}
			};
		}

		IProblemFactory problemFactory = getProblemFactory();
		FastJDTCompiler treeBuilderCompiler = new FastJDTCompiler(
			nameEnvironment, errorHandlingPolicy, compilerOptions, requestor, problemFactory,
			this.out, spoonEnvironment.getIgnoreSyntaxErrors(), spoonEnvironment.getLevel(),
			new CompilationProgress() {

				private String currentElement = null;
				private int totalTask = -1;

				@Override
				public void begin(int i) { }

				@Override
				public void done() { }

				@Override
				public boolean isCanceled() {
					return false;
				}

				@Override
				public void setTaskName(String s) {
					String strToFind = "Processing ";
					int processingPosition = s.indexOf(strToFind);
					if (processingPosition != -1) {
						currentElement = s.substring(processingPosition + strToFind.length());
					}
				}

				@Override
				public void worked(int increment, int remaining) {
					if (totalTask == -1) {
						totalTask = remaining + 1;
					}
					spoonEnvironment.getSpoonProgress().step(SpoonProgress.Process.COMPILE, currentElement, totalTask - remaining, totalTask);
				}
			});
		if (spoonEnvironment.getNoClasspath()) {
			treeBuilderCompiler.lookupEnvironment.problemReporter = new ProblemReporter(errorHandlingPolicy, compilerOptions, problemFactory) {
				@Override
				public int computeSeverity(int problemID) {
					// ignore all the problem and continue the build creation
					return 256;
				}
			};
			treeBuilderCompiler.lookupEnvironment.mayTolerateMissingType = true;
		}
		spoonEnvironment.getSpoonProgress().start(SpoonProgress.Process.COMPILE);
		// they have to be done all at once
		final CompilationUnitDeclaration[] result = treeBuilderCompiler.buildUnits(getCompilationUnits());
		spoonEnvironment.getSpoonProgress().end(SpoonProgress.Process.COMPILE);
		// now adding the doc
		if (spoonEnvironment.isCommentsEnabled()) {
			spoonEnvironment.getSpoonProgress().start(SpoonProgress.Process.COMMENT);
			//compile comments only if they are needed
			for (int i = 0; i < result.length; i++) {
				CompilationUnitDeclaration unit = result[i];
				CommentRecorderParser parser =
					new CommentRecorderParser(
						new ProblemReporter(
							DefaultErrorHandlingPolicies.proceedWithAllProblems(),
							compilerOptions,
							new DefaultProblemFactory(Locale.getDefault())),
						false);

				//reuse the source compilation unit
				ICompilationUnit sourceUnit = unit.compilationResult.compilationUnit;

				final CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, compilerOptions.maxProblemsPerUnit);
				CompilationUnitDeclaration tmpDeclForComment = parser.dietParse(sourceUnit, compilationResult);
				unit.comments = tmpDeclForComment.comments;
				spoonEnvironment.getSpoonProgress().step(SpoonProgress.Process.COMMENT, new String(unit.getFileName()), i + 1, result.length);
			}
			spoonEnvironment.getSpoonProgress().end(SpoonProgress.Process.COMMENT);
		}
		return result;
	}
}
