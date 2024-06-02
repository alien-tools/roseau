package com.github.maracas.roseau.diff.formatter;

import java.util.List;

import com.github.maracas.roseau.diff.changes.BreakingChange;

public interface BreakingChangesFormatter {
    String format(List<BreakingChange> changes);
    String getFileExtension();
}
