package io.github.alien.roseau.combinatorial.v2.benchmark.result;

public record ToolResult(String toolName, long executionTime, boolean isBinaryBreaking, boolean isSourceBreaking) {}
