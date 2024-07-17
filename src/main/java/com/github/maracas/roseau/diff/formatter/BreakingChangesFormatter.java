package com.github.maracas.roseau.diff.formatter;

import com.github.maracas.roseau.diff.changes.BreakingChange;

import java.util.List;

public interface BreakingChangesFormatter {
    String format(List<BreakingChange> changes);
    String getFileExtension();
}
