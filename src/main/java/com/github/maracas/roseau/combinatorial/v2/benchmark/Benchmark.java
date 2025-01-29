package com.github.maracas.roseau.combinatorial.v2.benchmark;

import com.github.maracas.roseau.api.model.API;
import com.github.maracas.roseau.combinatorial.Constants;
import com.github.maracas.roseau.combinatorial.v2.NewApiQueue;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.AbstractTool;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.JapicmpTool;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.RevapiTool;
import com.github.maracas.roseau.combinatorial.v2.benchmark.tool.RoseauTool;
import com.github.maracas.roseau.combinatorial.writer.ApiWriter;

import java.nio.file.Path;
import java.util.List;

public final class Benchmark implements Runnable {
	private final String id;

	private final Path clientsSourcesPath;
	private final Path clientsJarPath;
	private final Path v2SourcesPath;
	private final Path v2JarPath;

	private final NewApiQueue queue;

	private final List<AbstractTool> tools;

	private final ApiWriter apiWriter;

	public Benchmark(
			String id,
			NewApiQueue queue,
			Path clientsSourcesPath,
			Path clientsJarPath,
			Path v1SourcesPath,
			Path v1JarPath,
			Path workingPath
	) {
		System.out.println("Creating Benchmark " + id);
		this.id = id;

		this.clientsSourcesPath = clientsSourcesPath;
		this.clientsJarPath = clientsJarPath;
		this.v2SourcesPath = workingPath.resolve(Path.of(id, Constants.API_FOLDER));
		this.v2JarPath = workingPath.resolve(Path.of(id, Constants.JAR_FOLDER));

		this.queue = queue;

		this.tools = List.of(
				new JapicmpTool(v1JarPath, v2JarPath),
				new RevapiTool(v1JarPath, v2JarPath),
				new RoseauTool(v1SourcesPath, v2SourcesPath)
		);

		this.apiWriter = new ApiWriter(workingPath.resolve(id));
	}

	@Override
	public void run() {
		while (true) {
			var newApi = queue.take();
			if (newApi == null) break;

			try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

			System.out.println("\n--------------------------------------");
			System.out.println("Running Benchmark Thread n°" + id);
			generateNewApiSourcesAndJar(newApi);
			generateGroundTruth();
			runToolsAnalysis();
			System.out.println("Benchmark Thread n°" + id + " finished");
			System.out.println("--------------------------------------\n");
		}
	}

	private void generateNewApiSourcesAndJar(API api) {
		System.out.println("Generating new API Sources");
		apiWriter.createOutputHierarchy();
		apiWriter.write(api);
		System.out.println("\tGenerated to " + v2SourcesPath);

		System.out.println("Generating new API Jar");
		System.out.println("\tGenerated to " + v2JarPath);
	}

	private void generateGroundTruth() {
		System.out.println("Generating Ground Truth");
		System.out.println("\tCompiling new API with Clients");
		System.out.println("\t\tAPI Sources: " + v2SourcesPath);
		System.out.println("\t\tClients Sources: " + clientsSourcesPath);
		System.out.println("\tLinking new API with Clients");
		System.out.println("\t\tAPI Jar: " + v2JarPath);
		System.out.println("\t\tClients Jar: " + clientsJarPath);
	}

	private void runToolsAnalysis() {
		for (var tool : tools) {
			try {
				System.out.println("Running tool: " + tool.getClass().getSimpleName());
				Thread.sleep(1000);
				tool.detectBreakingChanges();
			} catch (InterruptedException ignored) {}
		}
	}
}
