package com.github.maracas.roseau.spoon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.CompilationProgress;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.Messages;
import spoon.support.compiler.jdt.JDTConstants;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

class FastJDTCompiler extends Compiler {
	private static final Logger LOGGER = LogManager.getLogger();

	FastJDTCompiler(INameEnvironment environment, IErrorHandlingPolicy policy, CompilerOptions options,
	                ICompilerRequestor requestor, IProblemFactory problemFactory, PrintWriter out,
	                CompilationProgress progress) {
		super(environment, policy, options, requestor, problemFactory, out, progress);
	}

	private void sortModuleDeclarationsFirst(ICompilationUnit[] sourceUnits) {
		Arrays.sort(sourceUnits, (u1, u2) -> {
			char[] fn1 = u1.getFileName();
			char[] fn2 = u2.getFileName();
			boolean isMod1 = CharOperation.endsWith(fn1, JDTConstants.MODULE_INFO_FILE_NAME) || CharOperation.endsWith(fn1, JDTConstants.MODULE_INFO_CLASS_NAME);
			boolean isMod2 = CharOperation.endsWith(fn2, JDTConstants.MODULE_INFO_FILE_NAME) || CharOperation.endsWith(fn2, JDTConstants.MODULE_INFO_CLASS_NAME);
			if (isMod1 == isMod2) {
				return 0;
			} else {
				return isMod1 ? -1 : 1;
			}
		});
	}

	protected CompilationUnitDeclaration[] buildUnits(CompilationUnit[] sourceUnits) {
		this.reportProgress(Messages.compilation_beginningToCompile);
		this.sortModuleDeclarationsFirst(sourceUnits);
		CompilationUnit[] filteredSourceUnits = this.ignoreSyntaxErrors(sourceUnits);
		this.beginToCompile(filteredSourceUnits);

		for(int i = 0; i < this.totalUnits; ++i) {
			CompilationUnitDeclaration unit = this.unitsToProcess[i];
			this.reportProgress(Messages.bind(Messages.compilation_processing, new String(unit.getFileName())));
			if (unit.scope != null) {
				unit.scope.faultInTypes();
			}

			unit.resolve();
			unit.ignoreFurtherInvestigation = false;
			this.requestor.acceptResult(unit.compilationResult);
			this.reportWorked(1, i);
		}

		ArrayList<CompilationUnitDeclaration> unitsToReturn = new ArrayList();

		for(CompilationUnitDeclaration cud : this.unitsToProcess) {
			if (cud != null) {
				unitsToReturn.add(cud);
			}
		}

		return unitsToReturn.toArray(new CompilationUnitDeclaration[0]);
	}

	private CompilationUnit[] ignoreSyntaxErrors(CompilationUnit[] sourceUnits) {
		ArrayList<CompilationUnit> sourceUnitList = new ArrayList();
		int maxUnits = sourceUnits.length;

		for(int i = 0; i < maxUnits; ++i) {
			CompilationResult unitResult = new CompilationResult(sourceUnits[i], i, maxUnits, this.options.maxProblemsPerUnit);
			CompilationUnitDeclaration parsedUnit = this.parser.parse(sourceUnits[i], unitResult);
			if (parsedUnit.hasErrors()) {
				LOGGER.warn("Syntax error detected in: " + String.valueOf(sourceUnits[i].getFileName()));
			} else {
				sourceUnitList.add(sourceUnits[i]);
			}
		}

		this.initializeParser();
		return sourceUnitList.toArray(new CompilationUnit[0]);
	}
}
