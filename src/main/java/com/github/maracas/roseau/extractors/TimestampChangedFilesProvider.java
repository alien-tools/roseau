package com.github.maracas.roseau.extractors;

import com.github.maracas.roseau.api.model.API;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TimestampChangedFilesProvider implements ChangedFilesProvider {
    private API oldApi;
    private Path sources;
    private long timestamp;

    public TimestampChangedFilesProvider(Path sources) {
        this.sources = sources;
        this.timestamp = timestamp;
    }

    public void refresh(API oldApi, long timestamp) {
            this.oldApi = oldApi;
            this.timestamp = timestamp;
    }

    @Override
    public ChangedFiles getChangedFiles() {

        try {
            if (oldApi == null) {
                Set<Path> currentFiles = Files.walk(sources)
                        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                        .collect(Collectors.toSet());
                return new ChangedFiles(Set.of(), Set.of(), currentFiles);
            }

            Set<Path> currentFiles = Files.walk(sources)
                    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .collect(Collectors.toSet());
            Set<Path> lastFiles = oldApi.getAllTypes().map(d -> d.getLocation().file()).collect(Collectors.toSet());
            Set<Path> removedfiles = new HashSet<>(lastFiles);
            removedfiles.removeAll(currentFiles);
            Set<Path> createdFiles = new HashSet<>(currentFiles);
            createdFiles.removeAll(lastFiles);
            Set<Path> updatedFiles = currentFiles.stream()
                    .filter(f -> lastFiles.contains(f) && f.toFile().lastModified() > timestamp).collect(Collectors.toSet());
            return new ChangedFiles(updatedFiles, removedfiles, createdFiles);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
