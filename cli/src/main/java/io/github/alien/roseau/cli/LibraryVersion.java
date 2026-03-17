package io.github.alien.roseau.cli;

import java.nio.file.Path;

sealed interface LibraryVersion {
	record LocalPath(Path path) implements LibraryVersion {}

	record MavenCoordinates(ArtifactCoordinates coordinates) implements LibraryVersion {}
}
