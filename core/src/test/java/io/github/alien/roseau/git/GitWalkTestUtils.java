package io.github.alien.roseau.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class GitWalkTestUtils {
	private GitWalkTestUtils() {
	}

	record CommitCsvRow(
		String commit,
		String message,
		int exportedTypesCount,
		int exportedMethodsCount,
		int exportedFieldsCount,
		long checkoutTimeMs,
		long apiTimeMs,
		long diffTimeMs,
		long statsTimeMs,
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

	static Map<String, CommitCsvRow> readCommitCsvRows(Path commitsCsv) throws IOException {
		List<Map<String, String>> rows = readCsv(commitsCsv);
		Map<String, CommitCsvRow> result = new LinkedHashMap<>();
		for (Map<String, String> row : rows) {
			String sha = row.get("commit_sha");
			result.put(sha, new CommitCsvRow(
				sha,
				row.get("commit_short_msg"),
				Integer.parseInt(row.get("exported_types_count")),
				Integer.parseInt(row.get("exported_methods_count")),
				Integer.parseInt(row.get("exported_fields_count")),
				Long.parseLong(row.get("checkout_time_ms")),
				Long.parseLong(row.get("api_time_ms")),
				Long.parseLong(row.get("diff_time_ms")),
				Long.parseLong(row.get("stats_time_ms")),
				Integer.parseInt(row.get("breaking_changes_count"))
			));
		}
		return result;
	}

	static Map<String, Integer> readBreakingChangesCountByCommit(Path bcsCsv) throws IOException {
		List<Map<String, String>> rows = readCsv(bcsCsv);
		Map<String, Integer> result = new LinkedHashMap<>();
		for (Map<String, String> row : rows) {
			result.merge(row.get("commit"), 1, Integer::sum);
		}
		return result;
	}

	static Status status(Git git) throws Exception {
		return git.status().call();
	}

	static List<Map<String, String>> readCsvRows(Path csv) throws IOException {
		return readCsv(csv);
	}

	private static List<Map<String, String>> readCsv(Path csv) throws IOException {
		List<String> lines = Files.readAllLines(csv);
		if (lines.isEmpty()) {
			return List.of();
		}
		List<String> header = parseCsvLine(lines.getFirst());
		List<Map<String, String>> rows = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.isBlank()) {
				continue;
			}
			List<String> values = parseCsvLine(line);
			Map<String, String> row = new LinkedHashMap<>();
			for (int col = 0; col < header.size(); col++) {
				String value = col < values.size() ? values.get(col) : "";
				row.put(header.get(col), value);
			}
			rows.add(row);
		}
		return rows;
	}

	private static List<String> parseCsvLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				values.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		values.add(current.toString());
		return values;
	}
}
