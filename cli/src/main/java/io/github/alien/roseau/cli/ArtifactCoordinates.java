package io.github.alien.roseau.cli;

import com.google.common.base.Preconditions;

record ArtifactCoordinates(
	String groupId,
	String artifactId,
	String version,
	String extension,
	String classifier
) {
	ArtifactCoordinates {
		Preconditions.checkNotNull(groupId);
		Preconditions.checkNotNull(artifactId);
		Preconditions.checkNotNull(version);
	}

	ArtifactCoordinates(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version, "jar", null);
	}

	static ArtifactCoordinates parse(String coordinates) {
		String[] parts = coordinates.split(":");
		return switch (parts.length) {
			case 3 -> new ArtifactCoordinates(parts[0], parts[1], parts[2]);
			case 4 -> new ArtifactCoordinates(parts[0], parts[1], parts[3], parts[2], null);
			case 5 -> new ArtifactCoordinates(parts[0], parts[1], parts[4], parts[2], parts[3]);
			default -> throw new IllegalArgumentException(
				"Invalid Maven coordinates '%s': expected groupId:artifactId:version".formatted(coordinates));
		};
	}
}
