#!/usr/bin/env python3
'''\nGenerate roseau-dataset from diff tests.\n\nThis script scans all JUnit test classes under:\n  core/src/test/java/io/github/alien/roseau/diff\n\nFor each @Test method, it extracts:\n  - test class name (e.g., FieldTypeChangedTest)\n  - test method name (e.g., boxing)\n  - code snippets assigned to local variables `v1` and `v2` inside the test method\n    (supports Java 21 text blocks: var v1 = \"\"\"...\"\"\"; var v2 = \"\"\"...\"\"\";)\n    If v1/v2 are not found or are empty strings, they are treated as empty.\n\nIt then writes the following structure under project root:\n  roseau-dataset/\n    client/src/{testClass}-{testName}/Main.java  (empty file)\n    v1/src/pkg/{testClass}-{testName}/{testName}.java  (package pkg; + code1)\n    v2/src/pkg/{testClass}-{testName}/{testName}.java  (package pkg; + code2)\n\nNotes:\n- The script is idempotent and overwrites existing files for the same cases.\n- Test and class names are sanitized to be filesystem-friendly.\n\nRun from repository root:\n  python3 scripts/generate_roseau_dataset.py\n'''
from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Iterable, Tuple, List, Optional

REPO_ROOT = Path(__file__).resolve().parents[1]
DIFF_TESTS_DIR = REPO_ROOT / "core" / "src" / "test" / "java" / "io" / "github" / "alien" / "roseau" / "diff"
OUTPUT_ROOT = REPO_ROOT / "roseau-dataset"

# Regex helpers
# Matches a JUnit @Test method signature and captures the method name.
TEST_METHOD_PATTERN = re.compile(
    r"@Test\s*(?:\r?\n\s*)*(?:public\s+|protected\s+|private\s+|static\s+|final\s+|\s+)*void\s+(?P<name>[a-zA-Z_$][a-zA-Z0-9_$]*)\s*\(\s*\)\s*\{",
    re.MULTILINE,
)

# Non-greedy extraction of a Java 21 text block assigned to var v1 / v2.
# We intentionally allow optional types/var and whitespace.
V_PATTERN = re.compile(
    r"(?P<var>v1|v2)\s*=\s*(?P<value>\"\"\".*?\"\"\"|\".*?\"|null)\s*;",
    re.DOTALL,
)

# Matches the entire body of a method starting at the opening '{' of the method
# found by TEST_METHOD_PATTERN, accounting for nested braces inside.

def _find_brace_matched_body(text: str, start_index: int) -> Tuple[int, int]:
    """
    Given text and index of the method opening '{', return (start, end) slice
    covering the full method body including braces. Returns (-1, -1) if not found.
    """
    if start_index < 0 or start_index >= len(text) or text[start_index] != '{':
        return -1, -1
    depth = 0
    i = start_index
    while i < len(text):
        ch = text[i]
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return start_index, i + 1
        i += 1
    return -1, -1


def _unindent_java_text_block(s: str) -> str:
    """Strip the surrounding triple quotes and return the raw content of a Java text block."""
    if s.startswith('"""') and s.endswith('"""'):
        inner = s[3:-3]
        # Java text blocks preserve indentation; we return as-is.
        # Leading newline after opening """ is customary; keep content as-is.
        return inner
    if s.startswith('"') and s.endswith('"'):
        # Simple string literal: unescape minimal escapes (\n, \t, \r, \"), then return
        body = s[1:-1]
        body = body.encode('utf-8').decode('unicode_escape')
        return body
    if s == 'null':
        return ''
    return s


def _sanitize(name: str) -> str:
    # Replace spaces and risky path chars. Keep Java identifier chars and dash.
    return re.sub(r"[^a-zA-Z0-9_$-]", "_", name)


def _extract_v1_v2(method_body: str) -> Tuple[str, str]:
    v1 = ''
    v2 = ''
    for m in V_PATTERN.finditer(method_body):
        var = m.group('var')
        raw = m.group('value').strip()
        content = _unindent_java_text_block(raw)
        if var == 'v1':
            v1 = content
        elif var == 'v2':
            v2 = content
    return v1, v2


# Matches @Client annotation with a string or text block payload placed on the test method
CLIENT_PATTERN = re.compile(r"@Client\s*\(\s*(\"\"\".*?\"\"\"|\".*?\"|null)\s*\)", re.DOTALL)

def _iter_test_methods(content: str) -> Iterable[Tuple[str, str, str]]:
    """
    Yield (testName, methodBodyText, clientCode) for each @Test-annotated method.
    clientCode is empty string when no @Client annotation is present.
    """
    for m in TEST_METHOD_PATTERN.finditer(content):
        name = m.group('name')
        # Find the position of the opening brace '{' matched by the pattern end
        brace_pos = content.find('{', m.end() - 1)
        if brace_pos == -1:
            continue
        start, end = _find_brace_matched_body(content, brace_pos)
        if start == -1:
            continue
        # Heuristically search a window before the @Test for an adjacent @Client annotation
        pre_start = max(0, m.start() - 500)
        header_window = content[pre_start:m.start()]
        client_code = ''
        # Find the closest (last) @Client before this @Test to avoid picking one from a previous method
        mc_iter = list(CLIENT_PATTERN.finditer(header_window))
        if mc_iter:
            raw = mc_iter[-1].group(1).strip()
            client_code = _unindent_java_text_block(raw)
        yield name, content[start:end], client_code


def _write_case(test_class: str, test_name: str, code1: str, code2: str, client_code: str) -> None:
    # Use underscore between class and test names for all outputs (client, v1, v2)
    case_dir_us = f"{_sanitize(test_class)}_{_sanitize(test_name)}"

    # client: match v1/v2 directory naming and include package declaration
    client_dir = OUTPUT_ROOT / "client" / "src" / case_dir_us
    client_dir.mkdir(parents=True, exist_ok=True)
    package_line = f"package {case_dir_us};\n"
    main_path = client_dir / "Main.java"
    if client_code.strip():
        main_src = (
            package_line
            + "\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) {\n"
            + "        " + client_code.strip() + "\n"
            + "    }\n"
            + "}\n"
        )
        main_path.write_text(main_src, encoding="utf-8")
    else:
        # Keep minimal file with just package declaration if no @Client provided
        main_path.write_text(package_line, encoding="utf-8")

    # v1 (drop 'pkg' directory level)
    v1_dir = OUTPUT_ROOT / "v1" / "src" / case_dir_us
    v1_dir.mkdir(parents=True, exist_ok=True)
    _write_java_files_for_code(v1_dir, package_line, test_name, code1)

    # v2 (drop 'pkg' directory level)
    v2_dir = OUTPUT_ROOT / "v2" / "src" / case_dir_us
    v2_dir.mkdir(parents=True, exist_ok=True)
    _write_java_files_for_code(v2_dir, package_line, test_name, code2)


# -------- Helpers for extracting top-level type declarations and writing files --------
TYPE_DECL_PATTERN = re.compile(r"(class|interface|enum|record|@interface)\s+([A-Za-z_][A-Za-z0-9_]*)", re.MULTILINE)


def _compute_brace_depths(text: str) -> list[int]:
    depths: List[int] = [0] * len(text)
    depth = 0
    i = 0
    in_sl_comment = False
    in_ml_comment = False
    in_str = False
    str_delim = ''
    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ''
        if in_sl_comment:
            if ch == '\n':
                in_sl_comment = False
        elif in_ml_comment:
            if ch == '*' and nxt == '/':
                in_ml_comment = False
                i += 1
        elif in_str:
            if ch == '\\':
                i += 1  # skip escaped
            elif ch == str_delim:
                in_str = False
        else:
            # not in string/comment
            if ch == '/' and nxt == '/':
                in_sl_comment = True
                i += 1
            elif ch == '/' and nxt == '*':
                in_ml_comment = True
                i += 1
            elif ch in ('"', "'"):
                in_str = True
                str_delim = ch
            elif ch == '{':
                depth += 1
            elif ch == '}':
                depth = max(0, depth - 1)
        depths[i] = depth
        i += 1
    return depths


def _find_matching_brace(text: str, open_index: int) -> Optional[int]:
    depth = 0
    i = open_index
    in_sl_comment = False
    in_ml_comment = False
    in_str = False
    str_delim = ''
    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ''
        if in_sl_comment:
            if ch == '\n':
                in_sl_comment = False
        elif in_ml_comment:
            if ch == '*' and nxt == '/':
                in_ml_comment = False
                i += 1
        elif in_str:
            if ch == '\\':
                i += 1
            elif ch == str_delim:
                in_str = False
        else:
            if ch == '/' and nxt == '/':
                in_sl_comment = True
                i += 1
            elif ch == '/' and nxt == '*':
                in_ml_comment = True
                i += 1
            elif ch in ('"', "'"):
                in_str = True
                str_delim = ch
            elif ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    return None


def _extract_type_declarations(code: str) -> List[Tuple[str, str]]:
    results: List[Tuple[str, str]] = []
    if not code:
        return results
    depths = _compute_brace_depths(code)
    for m in re.finditer(r"(?ms)(^[\t ]*(?:@[A-Za-z0-9_$.]+(?:\([^)]*\))?[\t ]*)*(?:public|protected|private|abstract|final|sealed|non-sealed|nonsealed|static|strictfp|\s)*)\s*(class|interface|enum|record|@interface)\s+([A-Za-z_][A-Za-z0-9_]*)[^\{;]*\{", code):
        annos_and_ws = m.group(1)
        kind = m.group(2)
        name = m.group(3)
        brace_pos = code.find('{', m.end() - 1)
        if brace_pos == -1:
            continue
        # ensure top-level
        if brace_pos < len(depths) and depths[brace_pos] != 1:
            # We want the declaration's opening brace to bring depth to 1 (i.e., top-level)
            continue
        end_brace = _find_matching_brace(code, brace_pos)
        if end_brace is None:
            continue
        # Expand start to include annotations directly above, captured in group(1)
        start = m.start(2) - len(annos_and_ws)
        if start < 0:
            start = m.start(2)
        decl_text = code[start:end_brace + 1]
        results.append((name, decl_text))
    return results


def _write_java_files_for_code(out_dir: Path, package_line: str, fallback_name: str, code: str) -> None:
    if not code:
        # Respect prior behavior: create empty file if code is empty
        (out_dir / f"{fallback_name}.java").write_text("", encoding="utf-8")
        return
    decls = _extract_type_declarations(code)
    if decls:
        for type_name, decl_text in decls:
            (out_dir / f"{type_name}.java").write_text(package_line + decl_text, encoding="utf-8")
        # Remove previously created fallback file if it exists
        fb = out_dir / f"{fallback_name}.java"
        if fb.exists():
            try:
                fb.unlink()
            except Exception:
                pass
    else:
        # Fallback: no type declarations found; still write single file to keep data
        (out_dir / f"{fallback_name}.java").write_text(package_line + code, encoding="utf-8")


# ----------------------------------------------------------------------------

def main(argv: list[str]) -> int:
    if not DIFF_TESTS_DIR.exists():
        print(f"[ERROR] Tests directory not found: {DIFF_TESTS_DIR}", file=sys.stderr)
        return 1

    OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

    total_classes = 0
    total_cases = 0

    for test_file in sorted(DIFF_TESTS_DIR.glob("*.java")):
        content = test_file.read_text(encoding="utf-8")
        test_class = test_file.stem  # e.g., FieldTypeChangedTest
        class_cases = 0
        for test_name, body, client_code in _iter_test_methods(content):
            v1, v2 = _extract_v1_v2(body)
            _write_case(test_class, test_name, v1, v2, client_code)
            total_cases += 1
            class_cases += 1
        if class_cases > 0:
            total_classes += 1
            print(f"Processed {class_cases:3d} test(s) from {test_class}")

    print(f"\nDone. Classes with tests: {total_classes}, total test cases: {total_cases}")
    print(f"Output written under: {OUTPUT_ROOT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
