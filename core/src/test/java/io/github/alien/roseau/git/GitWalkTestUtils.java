package io.github.alien.roseau.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GitWalkTestUtils {
	private GitWalkTestUtils() {
	}

	record CsvRow(
		String commit,
		String message,
		int typesCount,
		int methodsCount,
		int fieldsCount,
		long checkoutTime,
		long apiTime,
		long diffTime,
		long statsTime,
		int breakingChangesCount
	) {
	}

	static Git initRepo(Path repoDir) throws Exception {
		Files.createDirectories(repoDir);
		return Git.init()
			.setDirectory(repoDir.toFile())
			.call();
	}

	static RevCommit commit(Git git, String message, Map<String, String> filesToWrite, List<String> filesToDelete) throws Exception {
		Path root = git.getRepository().getWorkTree().toPath();

		for (String file : filesToDelete) {
			Path path = root.resolve(file);
			if (Files.exists(path)) {
				Files.delete(path);
				git.rm().addFilepattern(file).call();
			}
		}

		for (Map.Entry<String, String> entry : filesToWrite.entrySet()) {
			Path path = root.resolve(entry.getKey());
			Files.createDirectories(path.getParent());
			Files.writeString(path, entry.getValue());
			git.add().addFilepattern(entry.getKey()).call();
		}

		return git.commit()
			.setMessage(message)
			.setAuthor("roseau-tests", "roseau-tests@example.org")
			.call();
	}

	static Map<String, CsvRow> readCsvRows(Path csv) throws IOException {
		List<String> lines = Files.readAllLines(csv);
		Map<String, CsvRow> rows = new LinkedHashMap<>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.isBlank()) {
				continue;
			}
			String[] fields = line.split("\\|", -1);
			rows.put(fields[0], new CsvRow(
				fields[0],
				fields[2],
				Integer.parseInt(fields[4]),
				Integer.parseInt(fields[5]),
				Integer.parseInt(fields[6]),
				Long.parseLong(fields[9]),
				Long.parseLong(fields[11]),
				Long.parseLong(fields[12]),
				Long.parseLong(fields[13]),
				Integer.parseInt(fields[14])
			));
		}
		return rows;
	}

	static Status status(Git git) throws Exception {
		return git.status().call();
	}
}
