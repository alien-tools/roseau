package com.github.maracas.roseau.diff.changes;

import java.util.List;

public interface BreakinChangesFormatter {
    String format(List<BreakingChange> changes);
}
