package io.github.alien.roseau.combinatorial.utils;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public final class ExplorerUtils {
	public static boolean checkPathExists(Path path) {
		return path.toFile().exists();
	}

	public static boolean removeDirectory(Path path) {
		try {
			MoreFiles.deleteRecursively(Paths.get("/path/to/delete"), RecursiveDeleteOption.ALLOW_INSECURE);
			return true;
		} catch (IOException _) {
			return false;
		}
	}

	public static boolean cleanOrCreateDirectory(Path path) {
		if (path.toString().isBlank()) return false;

		try {
			File file = path.toFile();
			if (file.exists()) {
				try {
					for (Path child : MoreFiles.listFiles(path)) {
						MoreFiles.deleteRecursively(child, RecursiveDeleteOption.ALLOW_INSECURE);
					}
					return true;
				} catch (IOException _) {
					return false;
				}
			}

			return file.mkdirs();
		} catch (SecurityException e) {
			return false;
		}
	}

	public static boolean createDirectoryIfNecessary(Path path) {
		var directoryExists = checkPathExists(path);
		if (directoryExists) return true;

		return path.toFile().mkdirs();
	}

	public static List<File> getFilesInPath(Path path, String extension) {
		try (Stream<Path> paths = Files.walk(path)) {
			return paths
				.filter(Files::isRegularFile)
				.filter(p -> p.toString().endsWith("." + extension))
				.map(Path::toFile)
				.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
