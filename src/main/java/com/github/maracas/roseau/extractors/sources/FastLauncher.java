package com.github.maracas.roseau.extractors.sources;

import spoon.Launcher;
import spoon.SpoonModelBuilder;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;
import spoon.support.compiler.jdt.JDTBatchCompiler;

public class FastLauncher extends Launcher {
	@Override
	protected SpoonModelBuilder getCompilerInstance(Factory factory) {
		return new JDTBasedSpoonCompiler(factory) {
			@Override
			protected JDTBatchCompiler createBatchCompiler() {
				return new FastJDTBatchCompiler(this, environment, requestor);
			}
		};
	}
}
