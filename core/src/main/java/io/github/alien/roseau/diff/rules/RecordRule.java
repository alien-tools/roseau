package io.github.alien.roseau.diff.rules;

import io.github.alien.roseau.api.model.RecordDecl;

public interface RecordRule {
	default void onAddedRecord(RecordDecl rcrd, TypeRuleContext ctx) {}
	default void onRemovedRecord(RecordDecl rcrd, TypeRuleContext ctx) {}
	default void onMatchedRecord(RecordDecl oldRecord, RecordDecl newRecord, TypeRuleContext ctx) {}
}
