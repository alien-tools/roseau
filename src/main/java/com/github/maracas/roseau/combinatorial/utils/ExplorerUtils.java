package com.github.maracas.roseau.combinatorial.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ExplorerUtils {
	public static boolean checkPathExists(Path path) {
		return path.toFile().exists();
	}

	public static boolean removeDirectory(Path path) {
		try {
			var file = path.toFile();
			FileUtils.cleanDirectory(file);
			FileUtils.deleteDirectory(file);

			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	public static boolean cleanOrCreateDirectory(Path path) {
		if (path.toString().isBlank()) return false;

		try {
			File file = path.toFile();
			if (file.exists()) {
				try {
					FileUtils.cleanDirectory(file);
					return true;
				} catch (IOException ignored) {
					return false;
				}
			}

			return file.mkdirs();
		} catch (SecurityException e) {
			return false;
		}
	}

	public static List<File> getFilesInPath(Path path, String extension) {
		return FileUtils.listFiles(path.toFile(), new String[]{extension}, true).stream().toList();
	}
}
