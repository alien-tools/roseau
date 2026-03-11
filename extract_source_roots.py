#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path, PurePosixPath
from typing import Iterable
from xml.etree import ElementTree as ET

from ruamel.yaml import YAML
from ruamel.yaml.comments import CommentedMap, CommentedSeq


BUILD_FILES = (
    "pom.xml",
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
    "build.xml",
    "project.xml",
    "project.properties",
    "gradle.properties",
    "maven.xml",
)

COMMON_ROOTS = (
    "src/main/java",
    "src/java",
    "src/main",
    "src",
    "main/java",
    "java",
)

PACKAGE_EXCLUSION_PATTERNS = {
    "impl": r".*(\.)?impl(\.)?.*",
    "implementation": r".*(\.)?implementation(\.)?.*",
    "experimental": r".*(\.)?experimental(\.)?.*",
    "incubating": r".*(\.)?incubating(\.)?.*",
    "private": r".*(\.)?private(\.)?.*",
}

ANNOTATION_CANDIDATES = {
    "ApiStatus",
    "Beta",
    "DoNotCall",
    "Experimental",
    "Incubating",
    "Internal",
    "IntellijInternalApi",
    "PackagePrivate",
    "Preview",
    "PreviewFeature",
    "RestrictedApi",
    "VisibleForTesting",
    "treatAsPrivate",
}

ANNOTATION_USE_RE = re.compile(r"@([A-Za-z_][\w.]*)")
IMPORT_RE = re.compile(r"^\s*import\s+([A-Za-z_][\w.]*);", re.MULTILINE)
PACKAGE_RE = re.compile(r"^\s*package\s+([A-Za-z_][\w.]*);", re.MULTILINE)
ANNOTATION_DEF_RE = re.compile(r"@interface\s+([A-Za-z_]\w*)")
API_STATUS_RE = re.compile(r"status\s*=\s*(?:[A-Za-z_][\w.]*\.)?(INTERNAL|EXPERIMENTAL)")


def normalize_token(value: str | None) -> str:
    if not value:
        return ""
    return re.sub(r"[^a-z0-9]+", "", value.lower())


def short_commit(commit: str) -> str:
    return commit[:10]


@dataclass
class Inference:
    root: str | None
    module: str | None
    method: str
    note: str | None = None


@dataclass
class RootSegment:
    root: str
    module: str
    introduced_commit: str | None
    introduced_subject: str | None


@dataclass
class RepoAnalysis:
    segments: list[RootSegment] = field(default_factory=list)
    unresolved: list[str] = field(default_factory=list)


@dataclass
class ExclusionAnnotation:
    name: str
    args: dict[str, str]
    occurrences: int


@dataclass
class ExclusionAnalysis:
    names: list[str] = field(default_factory=list)
    annotations: list[ExclusionAnnotation] = field(default_factory=list)


class GitRepo:
    def __init__(self, work_tree: Path) -> None:
        self.work_tree = work_tree
        self._file_cache: dict[tuple[str, str], str | None] = {}
        self._exists_cache: dict[tuple[str, str], bool] = {}
        self._tree_cache: dict[tuple[str, str], list[str]] = {}
        self._subject_cache: dict[str, str] = {}

    def run(self, *args: str, check: bool = True) -> str:
        proc = subprocess.run(
            ["git", "-C", str(self.work_tree), *args],
            text=True,
            capture_output=True,
        )
        if check and proc.returncode != 0:
            raise RuntimeError(f"git {' '.join(args)} failed: {proc.stderr.strip()}")
        return proc.stdout

    def default_ref(self) -> str:
        try:
            ref = self.run("symbolic-ref", "refs/remotes/origin/HEAD").strip()
            if ref:
                return ref
        except RuntimeError:
            pass
        return "HEAD"

    def first_parent_commits(self, ref: str) -> list[str]:
        out = self.run("rev-list", "--first-parent", ref)
        return [line for line in out.splitlines() if line]

    def commit_subject(self, commit: str) -> str:
        if commit not in self._subject_cache:
            self._subject_cache[commit] = self.run("show", "-s", "--format=%s", commit).strip()
        return self._subject_cache[commit]

    def changed_entries(self, parent: str, commit: str) -> list[tuple[str, list[str]]]:
        out = self.run(
            "diff-tree",
            "--name-status",
            "--find-renames",
            "--find-copies",
            "--no-commit-id",
            "-r",
            parent,
            commit,
        )
        entries: list[tuple[str, list[str]]] = []
        for line in out.splitlines():
            parts = line.split("\t")
            if len(parts) >= 2:
                entries.append((parts[0], parts[1:]))
        return entries

    def show_file(self, commit: str, path: str) -> str | None:
        key = (commit, path)
        if key in self._file_cache:
            return self._file_cache[key]
        proc = subprocess.run(
            ["git", "-C", str(self.work_tree), "show", f"{commit}:{path}"],
            text=True,
            capture_output=True,
        )
        if proc.returncode != 0:
            self._file_cache[key] = None
        else:
            self._file_cache[key] = proc.stdout
        return self._file_cache[key]

    def path_exists(self, commit: str, path: str) -> bool:
        key = (commit, path)
        if key in self._exists_cache:
            return self._exists_cache[key]
        proc = subprocess.run(
            ["git", "-C", str(self.work_tree), "ls-tree", "--name-only", commit, "--", path],
            text=True,
            capture_output=True,
        )
        exists = proc.returncode == 0 and bool(proc.stdout.strip())
        self._exists_cache[key] = exists
        return exists

    def files_under(self, commit: str, path: str) -> list[str]:
        key = (commit, path)
        if key in self._tree_cache:
            return self._tree_cache[key]
        proc = subprocess.run(
            ["git", "-C", str(self.work_tree), "ls-tree", "-r", "--name-only", commit, "--", path],
            text=True,
            capture_output=True,
        )
        if proc.returncode != 0:
            files: list[str] = []
        else:
            files = [line for line in proc.stdout.splitlines() if line]
        self._tree_cache[key] = files
        return files

    def path_has_java(self, commit: str, path: str) -> bool:
        return any(name.endswith(".java") for name in self.files_under(commit, path))


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def child_text(element: ET.Element, name: str) -> str | None:
    for child in list(element):
        if local_name(child.tag) == name and child.text is not None:
            return child.text.strip()
    return None


def child(element: ET.Element, name: str) -> ET.Element | None:
    for item in list(element):
        if local_name(item.tag) == name:
            return item
    return None


@dataclass
class PomModel:
    packaging: str
    artifact_id: str | None
    name: str | None
    source_directory: str | None
    modules: list[str]


def parse_pom(text: str) -> PomModel | None:
    try:
        root = ET.fromstring(text)
    except ET.ParseError:
        return None
    build = child(root, "build")
    modules_el = child(root, "modules")
    modules = []
    if modules_el is not None:
        for module_el in list(modules_el):
            if local_name(module_el.tag) == "module" and module_el.text:
                modules.append(module_el.text.strip())
    source_directory = None
    if build is not None:
        source_directory = child_text(build, "sourceDirectory")
    return PomModel(
        packaging=child_text(root, "packaging") or "jar",
        artifact_id=child_text(root, "artifactId"),
        name=child_text(root, "name"),
        source_directory=source_directory,
        modules=modules,
    )


class RootInferer:
    def __init__(self, repo: GitRepo, entry: CommentedMap) -> None:
        self.repo = repo
        self.entry = entry
        git_dir = Path(str(entry["gitDir"]))
        self.repo_root = git_dir.parent
        self.relative_roots = [
            PurePosixPath(str(root)).relative_to(self.repo_root.as_posix()).as_posix()
            for root in entry.get("sourceRoots", [])
        ]
        self.preferred_modules = self._preferred_modules()
        self.head_anchor = self._head_anchor()

    def _preferred_modules(self) -> list[str]:
        modules: list[str] = []
        for root in self.relative_roots:
            parts = PurePosixPath(root).parts
            if not parts:
                continue
            first = parts[0]
            if first == "src" or first == "java" or first == "main":
                continue
            if first not in modules:
                modules.append(first)
        return modules

    def _head_anchor(self) -> tuple[str | None, str | None]:
        if not self.preferred_modules:
            return (None, None)
        module = self.preferred_modules[0]
        pom = self.repo.show_file("HEAD", f"{module}/pom.xml")
        if not pom:
            return (None, None)
        model = parse_pom(pom)
        if not model:
            return (None, None)
        return (model.artifact_id, model.name)

    def infer(self, commit: str, previous_root: str | None) -> Inference:
        pom = self.repo.show_file(commit, "pom.xml")
        if pom:
            inference = self._infer_from_maven(commit, pom)
            if inference.root:
                return inference

        inference = self._infer_from_gradle(commit)
        if inference.root:
            return inference

        inference = self._infer_from_ant(commit)
        if inference.root:
            return inference

        return self._infer_from_fallback(commit, previous_root)

    def _infer_from_maven(self, commit: str, pom_text: str) -> Inference:
        model = parse_pom(pom_text)
        if not model:
            return Inference(None, None, "maven", "unable to parse pom.xml")

        root_candidate = self._validate_root(commit, model.source_directory or "src/main/java")
        module_candidates = []
        for module in model.modules:
            module_pom = self.repo.show_file(commit, f"{module}/pom.xml")
            if not module_pom:
                continue
            module_model = parse_pom(module_pom)
            if not module_model:
                continue
            root_rel = self._module_root(commit, module, module_model)
            if root_rel:
                module_candidates.append((module, module_model, root_rel))

        if module_candidates:
            chosen = self._choose_module(module_candidates)
            if chosen is not None:
                return Inference(chosen[2], chosen[0], "maven")
            valid_roots = sorted({item[2] for item in module_candidates})
            return Inference(
                None,
                None,
                "maven",
                "ambiguous Maven modules with valid roots: " + ", ".join(valid_roots),
            )

        if root_candidate:
            return Inference(root_candidate, self._module_name_for_root(root_candidate), "maven")
        return Inference(None, None, "maven", "no valid Maven source root found")

    def _module_root(self, commit: str, module: str, model: PomModel) -> str | None:
        source_dir = model.source_directory or "src/main/java"
        module_source = PurePosixPath(module, source_dir).as_posix()
        validated = self._validate_root(commit, module_source)
        if validated:
            return validated

        fallback = PurePosixPath(module, "src/main").as_posix()
        if self.repo.path_exists(commit, fallback) and self.repo.path_has_java(commit, fallback):
            return fallback
        return None

    def _choose_module(
        self, candidates: list[tuple[str, PomModel, str]]
    ) -> tuple[str, PomModel, str] | None:
        scored: list[tuple[int, tuple[str, PomModel, str]]] = []
        anchor_artifact = normalize_token(self.head_anchor[0])
        anchor_name = normalize_token(self.head_anchor[1])
        preferred_normalized = [normalize_token(module) for module in self.preferred_modules]

        for index, candidate in enumerate(candidates):
            module, model, _root = candidate
            score = 0
            if module in self.preferred_modules:
                score = max(score, 100 - self.preferred_modules.index(module))
            module_norm = normalize_token(Path(module).name)
            if module_norm in preferred_normalized:
                score = max(score, 80 - preferred_normalized.index(module_norm))
            if anchor_artifact and normalize_token(model.artifact_id) == anchor_artifact:
                score = max(score, 70)
            if anchor_name and normalize_token(model.name) == anchor_name:
                score = max(score, 60)
            if model.packaging != "pom":
                score = max(score, 20)
            scored.append((score, candidate))

        scored.sort(key=lambda item: item[0], reverse=True)
        if not scored:
            return None
        best_score = scored[0][0]
        if best_score <= 0:
            if len(candidates) == 1:
                return candidates[0]
            return None
        winners = [candidate for score, candidate in scored if score == best_score]
        if len(winners) == 1:
            return winners[0]
        return None

    def _infer_from_gradle(self, commit: str) -> Inference:
        settings_text = self.repo.show_file(commit, "settings.gradle.kts") or self.repo.show_file(
            commit, "settings.gradle"
        )
        module_names = self._parse_gradle_modules(settings_text or "")

        current_build = self.repo.show_file(commit, "build.gradle.kts") or self.repo.show_file(
            commit, "build.gradle"
        )
        explicit_dirs = self._parse_gradle_source_dirs(current_build or "")
        if explicit_dirs:
            for source_dir in explicit_dirs:
                validated = self._validate_root(commit, source_dir, allow_existing_directory=True)
                if validated:
                    return Inference(validated, self._module_name_for_root(validated), "gradle")

        valid_modules: list[str] = []
        for module in module_names:
            module_build = self.repo.show_file(
                commit, f"{module}/build.gradle.kts"
            ) or self.repo.show_file(commit, f"{module}/build.gradle")
            module_dirs = self._parse_gradle_source_dirs(module_build or "")
            roots = module_dirs or ["src/main/java"]
            for root in roots:
                module_root = PurePosixPath(module, root).as_posix()
                validated = self._validate_root(commit, module_root, allow_existing_directory=True)
                if validated:
                    valid_modules.append(validated)
                    break

        if len(valid_modules) == 1:
            return Inference(valid_modules[0], self._module_name_for_root(valid_modules[0]), "gradle")

        root_default = self._validate_root(commit, "src/main/java")
        if root_default:
            return Inference(root_default, self._module_name_for_root(root_default), "gradle")
        return Inference(None, None, "gradle", "no valid Gradle source root found")

    def _infer_from_ant(self, commit: str) -> Inference:
        build_text = self.repo.show_file(commit, "build.xml") or ""
        project_text = self.repo.show_file(commit, "project.xml") or ""
        project_props = self.repo.show_file(commit, "project.properties") or ""
        combined = "\n".join([build_text, project_text, project_props])

        explicit = self._extract_ant_source_dir(combined)
        if explicit:
            validated = self._validate_root(commit, explicit, allow_existing_directory=True)
            if validated:
                return Inference(validated, self._module_name_for_root(validated), "ant")

        if project_text:
            legacy = self._validate_root(commit, "src/java")
            if legacy:
                return Inference(legacy, self._module_name_for_root(legacy), "ant")
        return Inference(None, None, "ant", "no valid Ant or Maven 1 source root found")

    def _infer_from_fallback(self, commit: str, previous_root: str | None) -> Inference:
        candidates: list[str] = []
        seen: set[str] = set()

        def add(path: str) -> None:
            if path and path not in seen:
                candidates.append(path)
                seen.add(path)

        if previous_root:
            add(previous_root)
        for root in self.relative_roots:
            add(root)
        for module in self.preferred_modules:
            for suffix in COMMON_ROOTS:
                add(PurePosixPath(module, suffix).as_posix())
        for suffix in COMMON_ROOTS:
            add(suffix)

        valid = [candidate for candidate in candidates if self.repo.path_has_java(commit, candidate)]
        if not valid:
            return Inference(
                None, None, "fallback", "no candidate source root contains Java sources"
            )

        top = valid[0]
        same_priority = [candidate for candidate in valid if candidate == top]
        if len(same_priority) == 1:
            return Inference(
                top,
                self._module_name_for_root(top),
                "fallback",
                "derived from known conventions",
            )
        return Inference(
            None, None, "fallback", "ambiguous fallback source roots: " + ", ".join(valid)
        )

    def _validate_root(
        self, commit: str, root: str, allow_existing_directory: bool = False
    ) -> str | None:
        root = str(PurePosixPath(root))
        if self.repo.path_has_java(commit, root):
            return root
        if allow_existing_directory and self.repo.path_exists(commit, root):
            return root
        return None

    @staticmethod
    def _parse_gradle_modules(text: str) -> list[str]:
        modules: list[str] = []
        for match in re.finditer(r"include\s*\(([^)]*)\)", text, re.MULTILINE | re.DOTALL):
            for token in re.findall(r"['\"]:?([^,'\"\s)]+)['\"]", match.group(1)):
                if token not in modules:
                    modules.append(token)
        for match in re.finditer(r"include\s+(.+)", text):
            for token in re.findall(r"['\"]:?([^,'\"\s]+)['\"]", match.group(1)):
                if token not in modules:
                    modules.append(token)
        return modules

    @staticmethod
    def _parse_gradle_source_dirs(text: str) -> list[str]:
        results: list[str] = []
        for match in re.finditer(r"srcDirs?\s*(?:=)?\s*\[([^\]]+)\]", text):
            for token in re.findall(r"['\"]([^'\"]+)['\"]", match.group(1)):
                results.append(token)
        for match in re.finditer(r"srcDir\s+['\"]([^'\"]+)['\"]", text):
            results.append(match.group(1))
        return results

    @staticmethod
    def _extract_ant_source_dir(text: str) -> str | None:
        patterns = (
            r'name="src\.dir"\s+value="([^"]+)"',
            r'name="maven\.build\.src"\s+value="([^"]+)"',
            r"<sourceDirectory>([^<]+)</sourceDirectory>",
        )
        for pattern in patterns:
            match = re.search(pattern, text)
            if match:
                return match.group(1).strip()
        return None

    @staticmethod
    def _module_name_for_root(root: str) -> str:
        parts = PurePosixPath(root).parts
        if not parts:
            return "."
        first = parts[0]
        if first in {"src", "java", "main"}:
            return "."
        return first


def progress_line(message: str, enabled: bool = True) -> None:
    if enabled:
        print(message, file=sys.stderr, flush=True)


def analyze_repo(
    entry: CommentedMap, verbose: bool = False, progress: bool = True
) -> RepoAnalysis:
    git_dir = Path(str(entry["gitDir"]))
    repo = GitRepo(git_dir.parent)
    inferer = RootInferer(repo, entry)
    ref = repo.default_ref()
    commits = repo.first_parent_commits(ref)
    if not commits:
        return RepoAnalysis(unresolved=["no commits found on default branch"])

    segments: list[RootSegment] = []
    unresolved: list[str] = []
    previous_root: str | None = None

    ordered_commits = list(reversed(commits))
    total = len(ordered_commits)
    report_step = max(100, total // 20)

    for index, commit in enumerate(ordered_commits):
        if progress and (index == 0 or index + 1 == total or (index + 1) % report_step == 0):
            progress_line(
                f"  {entry['id']}: commit {index + 1}/{total} ({short_commit(commit)})",
                enabled=progress,
            )
        if index == 0:
            inference = inferer.infer(commit, previous_root)
        else:
            parent = ordered_commits[index - 1]
            changes = repo.changed_entries(parent, commit)
            if should_recompute(inferer, previous_root, changes):
                inference = inferer.infer(commit, previous_root)
            else:
                inference = Inference(
                    previous_root,
                    inferer._module_name_for_root(previous_root) if previous_root else None,
                    "carry-forward",
                )

        if inference.root is None:
            if len(unresolved) < 20:
                unresolved.append(
                    f"{short_commit(commit)} {repo.commit_subject(commit)} [{inference.method}: {inference.note}]"
                )
            continue

        if verbose:
            progress_line(
                f"{entry['id']}: {short_commit(commit)} -> {inference.root} [{inference.module}] ({inference.method})"
            )

        if not segments or segments[-1].root != inference.root or segments[-1].module != inference.module:
            segments.append(
                RootSegment(
                    root=inference.root,
                    module=inference.module or ".",
                    introduced_commit=None if not segments else commit,
                    introduced_subject=None if not segments else repo.commit_subject(commit),
                )
            )
        previous_root = inference.root

    return RepoAnalysis(segments=segments, unresolved=unresolved)


def should_recompute(
    inferer: RootInferer,
    previous_root: str | None,
    changes: list[tuple[str, list[str]]],
) -> bool:
    if previous_root is None:
        return True

    prefixes = [previous_root.rstrip("/") + "/"]
    prefixes.extend(root.rstrip("/") + "/" for root in inferer.relative_roots)
    prefixes.extend(module.rstrip("/") + "/" for module in inferer.preferred_modules)
    prefixes.extend(["src/", "java/"])

    for status, paths in changes:
        kind = status[0]
        for path in paths:
            if Path(path).name in BUILD_FILES:
                return True
            if kind in {"A", "D", "R", "C"} and any(path.startswith(prefix) for prefix in prefixes):
                return True
    return False


def make_sequence(repo_root: Path, segments: Iterable[RootSegment]) -> CommentedSeq:
    seq = CommentedSeq()
    ordered = list(segments)[::-1]
    for index, segment in enumerate(ordered):
        absolute_root = str(repo_root / PurePosixPath(segment.root))
        seq.append(absolute_root)
        if segment.introduced_commit:
            comment = f"{short_commit(segment.introduced_commit)} {segment.introduced_subject}"
        elif index == len(ordered) - 1:
            comment = "initial root on first-parent history"
        else:
            comment = "present at HEAD"
        seq.yaml_add_eol_comment(comment, index)
    return seq


def make_module_sequence(segments: Iterable[RootSegment]) -> CommentedSeq:
    seq = CommentedSeq()
    ordered = list(segments)[::-1]
    for index, segment in enumerate(ordered):
        seq.append(segment.module)
        if segment.introduced_commit:
            comment = f"{short_commit(segment.introduced_commit)} {segment.introduced_subject}"
        elif index == len(ordered) - 1:
            comment = "initial tracked module on first-parent history"
        else:
            comment = "tracked at HEAD"
        seq.yaml_add_eol_comment(comment, index)
    return seq


def unresolved_comment(lines: list[str]) -> str:
    header = "sourceRoots inference gaps on first-parent history:"
    body = "\n".join(f"- {line}" for line in lines)
    return f"{header}\n{body}"


def canonical_annotation_key(name: str, args: dict[str, str]) -> tuple[str, tuple[tuple[str, str], ...]]:
    return (name, tuple(sorted(args.items())))


def extract_exclusions(
    entry: CommentedMap,
    analysis: RepoAnalysis,
    default_exclusions: CommentedMap | dict | None,
    progress: bool,
) -> CommentedMap | None:
    existing = entry.get("exclusions")
    latest_segment = analysis.segments[-1] if analysis.segments else None
    inferred = infer_current_exclusions(entry, latest_segment, progress=progress)

    default_names = set()
    default_annotations = set()
    if default_exclusions:
        for pattern in default_exclusions.get("names", []) or []:
            default_names.add(str(pattern))
        for annotation in default_exclusions.get("annotations", []) or []:
            default_annotations.add(
                canonical_annotation_key(
                    str(annotation.get("name")),
                    {str(k): str(v) for k, v in (annotation.get("args") or {}).items()},
                )
            )

    names: list[str] = []
    existing_names = []
    if existing and existing.get("names"):
        existing_names = [str(value) for value in existing["names"]]
    for pattern in existing_names + inferred.names:
        if pattern in default_names or pattern in names:
            continue
        names.append(pattern)

    annotations: list[ExclusionAnnotation] = []
    seen_annotations = set()
    if existing and existing.get("annotations"):
        for item in existing["annotations"]:
            args = {str(k): str(v) for k, v in (item.get("args") or {}).items()}
            key = canonical_annotation_key(str(item.get("name")), args)
            if key not in default_annotations and key not in seen_annotations:
                annotations.append(ExclusionAnnotation(str(item.get("name")), args, 0))
                seen_annotations.add(key)
    for item in inferred.annotations:
        key = canonical_annotation_key(item.name, item.args)
        if key in default_annotations or key in seen_annotations:
            continue
        annotations.append(item)
        seen_annotations.add(key)

    if not names and not annotations:
        return existing if existing else None

    exclusions = CommentedMap()
    if names:
        name_seq = CommentedSeq()
        for pattern in names:
            name_seq.append(pattern)
        exclusions["names"] = name_seq
    if annotations:
        ann_seq = CommentedSeq()
        for annotation in annotations:
            ann = CommentedMap()
            ann["name"] = annotation.name
            args = CommentedMap()
            for key, value in annotation.args.items():
                args[key] = value
            ann["args"] = args
            ann_seq.append(ann)
            if annotation.occurrences:
                ann_seq.yaml_add_eol_comment(f"inferred from HEAD ({annotation.occurrences} hits)", len(ann_seq) - 1)
        exclusions["annotations"] = ann_seq
    return exclusions


def infer_current_exclusions(
    entry: CommentedMap, latest_segment: RootSegment | None, progress: bool
) -> ExclusionAnalysis:
    if latest_segment is None:
        return ExclusionAnalysis()

    repo_root = Path(str(entry["gitDir"])).parent
    source_root = repo_root / PurePosixPath(latest_segment.root)
    if not source_root.exists():
        return ExclusionAnalysis()

    progress_line(f"  {entry['id']}: scanning exclusions under {source_root}", enabled=progress)

    java_files = [path for path in source_root.rglob("*.java") if path.is_file()]
    if not java_files:
        return ExclusionAnalysis()

    progress_line(f"  {entry['id']}: found {len(java_files)} Java files for exclusion scan", enabled=progress)

    package_token_hits: dict[str, set[str]] = {token: set() for token in PACKAGE_EXCLUSION_PATTERNS}
    annotation_definitions: dict[str, set[str]] = {}
    candidate_files: list[Path] = []
    marker_strings = tuple(f"@{name}" for name in ANNOTATION_CANDIDATES)

    for path in java_files:
        rel_parent = path.parent.relative_to(source_root)
        rel_parts = rel_parent.parts
        package_key = ".".join(rel_parts)
        for token in PACKAGE_EXCLUSION_PATTERNS:
            if token in rel_parts:
                package_token_hits[token].add(package_key)

        text = path.read_text(errors="ignore")
        package_name = package_from_text(text) or package_key.replace("/", ".")
        for match in ANNOTATION_DEF_RE.finditer(text):
            simple = match.group(1)
            fq_name = f"{package_name}.{simple}" if package_name else simple
            annotation_definitions.setdefault(simple, set()).add(fq_name)

        if "@API(" in text or any(marker in text for marker in marker_strings):
            candidate_files.append(path)

    names = [
        pattern
        for token, pattern in PACKAGE_EXCLUSION_PATTERNS.items()
        if package_token_hits[token]
    ]

    annotation_occurrences: dict[tuple[str, tuple[tuple[str, str], ...]], ExclusionAnnotation] = {}
    for path in candidate_files:
        text = path.read_text(errors="ignore")
        package_name = package_from_text(text)
        imports = import_map_from_text(text)
        for raw_name in ANNOTATION_USE_RE.findall(text):
            if raw_name == "interface":
                continue
            simple_name = raw_name.split(".")[-1]
            if simple_name not in ANNOTATION_CANDIDATES:
                continue
            if simple_name == "ApiStatus":
                for status in infer_api_statuses(text):
                    name = resolve_annotation_name(raw_name, imports, package_name, annotation_definitions)
                    args = {"status": status}
                    key = canonical_annotation_key(name, args)
                    if key not in annotation_occurrences:
                        annotation_occurrences[key] = ExclusionAnnotation(name, args, 0)
                    annotation_occurrences[key].occurrences += 1
                continue

            name = resolve_annotation_name(raw_name, imports, package_name, annotation_definitions)
            key = canonical_annotation_key(name, {})
            if key not in annotation_occurrences:
                annotation_occurrences[key] = ExclusionAnnotation(name, {}, 0)
            annotation_occurrences[key].occurrences += 1

    annotations = sorted(
        annotation_occurrences.values(),
        key=lambda item: (-item.occurrences, item.name, tuple(sorted(item.args.items()))),
    )
    return ExclusionAnalysis(names=names, annotations=annotations)


def package_from_text(text: str) -> str | None:
    match = PACKAGE_RE.search(text)
    return match.group(1) if match else None


def import_map_from_text(text: str) -> dict[str, str]:
    imports: dict[str, str] = {}
    for imported in IMPORT_RE.findall(text):
        simple = imported.split(".")[-1]
        imports[simple] = imported
    return imports


def infer_api_statuses(text: str) -> set[str]:
    statuses = set()
    for match in re.finditer(r"@ApiStatus\s*\((.*?)\)", text, re.DOTALL):
        args = match.group(1)
        status_match = API_STATUS_RE.search(args)
        if status_match:
            statuses.add(status_match.group(1))
    return statuses


def resolve_annotation_name(
    raw_name: str,
    imports: dict[str, str],
    package_name: str | None,
    definitions: dict[str, set[str]],
) -> str:
    if "." in raw_name:
        first, rest = raw_name.split(".", 1)
        if first and first[0].islower():
            return raw_name
        if first in imports:
            return imports[first] + "." + rest
        if package_name:
            return package_name + "." + raw_name
        return raw_name

    if raw_name in imports:
        return imports[raw_name]

    candidates = definitions.get(raw_name, set())
    if package_name:
        local = f"{package_name}.{raw_name}"
        if local in candidates:
            return local
    if len(candidates) == 1:
        return next(iter(candidates))
    return raw_name


def update_yaml(
    walk_path: Path, repo_ids: set[str] | None, inplace: bool, verbose: bool, progress: bool
) -> int:
    yaml = YAML()
    yaml.preserve_quotes = True
    yaml.width = 4096

    data = yaml.load(walk_path.read_text())
    repositories = data.get("repositories", [])
    default_exclusions = (data.get("defaults") or {}).get("exclusions")
    selected = [entry for entry in repositories if not repo_ids or entry.get("id") in repo_ids]
    updated = 0

    for repo_index, entry in enumerate(selected, start=1):
        progress_line(
            f"[{repo_index}/{len(selected)}] analyzing {entry['id']}",
            enabled=progress,
        )
        analysis = analyze_repo(entry, verbose=verbose, progress=progress)
        git_dir = Path(str(entry["gitDir"]))
        set_or_insert(entry, "trackedModules", make_module_sequence(analysis.segments), before="sourceRoots")
        entry["sourceRoots"] = make_sequence(git_dir.parent, analysis.segments)
        exclusions = extract_exclusions(entry, analysis, default_exclusions, progress)
        if exclusions:
            set_or_insert(entry, "exclusions", exclusions, before="outputDir")
        if analysis.unresolved:
            entry.yaml_set_comment_before_after_key(
                "outputDir", before=unresolved_comment(analysis.unresolved)
            )
        progress_line(
            f"[{repo_index}/{len(selected)}] finished {entry['id']} with {len(analysis.segments)} history segments",
            enabled=progress,
        )
        updated += 1

    if inplace:
        with walk_path.open("w") as handle:
            yaml.dump(data, handle)
    else:
        yaml.dump(data, sys.stdout)
    return updated


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Extract source root history from first-parent git history and update walk.yaml."
    )
    parser.add_argument("walk_yaml", nargs="?", default="walk.yaml")
    parser.add_argument("--repo", action="append", dest="repos", help="Repository id to update")
    parser.add_argument("--in-place", action="store_true", help="Rewrite walk.yaml")
    parser.add_argument("--verbose", action="store_true", help="Print commit-level inference details")
    parser.add_argument(
        "--no-progress", action="store_true", help="Disable progress output on stderr"
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    walk_path = Path(args.walk_yaml)
    repo_ids = set(args.repos) if args.repos else None
    updated = update_yaml(
        walk_path,
        repo_ids,
        inplace=args.in_place,
        verbose=args.verbose,
        progress=not args.no_progress,
    )
    if args.in_place:
        print(f"updated {updated} repository entries in {walk_path}")
    return 0


def set_or_insert(entry: CommentedMap, key: str, value: object, before: str) -> None:
    if key in entry:
        del entry[key]
    if before in entry:
        position = list(entry.keys()).index(before)
        entry.insert(position, key, value)
        return
    entry[key] = value


if __name__ == "__main__":
    raise SystemExit(main())
