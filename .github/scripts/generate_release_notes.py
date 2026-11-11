#!/usr/bin/env python3
"""Build the changelog section of a Ghost Play release.

Inputs (environment):
  PREVIOUS_TAG  previous release tag, e.g. v3.13 (may be empty for the first run)
  CURRENT_TAG   tag being released, e.g. v3.14
  REPO          owner/name, used for compare links
  NOTES_FILE    changelog file, default docs/CHANGELOG.md
  OUT_FILE      where to write the generated markdown section

Only commits between the two tags are considered, so the output always
describes what actually changed in *this* build. Each commit subject is mapped
onto a technical section (player, network, interface, tooling); commits that
only touch CI/release plumbing are summarised instead of listed one by one.
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

SECTION_TITLES = {
    "player": "Playback engine",
    "codec": "Codecs & containers",
    "network": "Streaming & network",
    "interface": "Interface",
    "update": "Updater",
    "other": "Other changes",
    "tooling": "Build, CI & release",
}

# Ordered rules: the first pattern that matches the changed paths decides the
# section. Anything unmatched falls into "other".
PATH_RULES = [
    ("codec", r"(nextlib|media3ext|decoder|codec|ffmpeg)"),
    ("player", r"(PlayerScreen|MusicService|AudioPlayer|playback|player)"),
    ("network", r"(datasource|httppipeline|okhttp|network)"),
    ("update", r"(UpdatesService|UpdatesScreen|UpdatesViewModel|release_notes)"),
    ("interface", r"(ui/|screens/|components/|navigation/|res/|theme)"),
    ("tooling", r"(\.github/|gradle|build\.gradle|gradlew|scripts/|docs/)"),
]

# Commits that only touch documentation or repository assets are not part of a
# build; listing them would drown the real changes.
NOISE_PATHS = re.compile(r"^(README|docs/|assets/|\.github/ISSUE|LICENSE|\.gitignore)", re.IGNORECASE)
NOISE_SUBJECTS = re.compile(
    r"^(add files via upload|delete |update .*\.md$|create .*\.md$|rename .*\.md)",
    re.IGNORECASE,
)

CONVENTIONAL = re.compile(
    r"^(feat|fix|perf|refactor|docs|test|build|ci|chore|style)(\(([^)]+)\))?!?:\s*(.+)$"
)


def run(*args: str) -> str:
    return subprocess.run(args, capture_output=True, text=True, check=False).stdout.strip()


def changed_files(prev: str, cur: str) -> dict[str, list[str]]:
    span = f"{prev}..{cur}" if prev else cur
    out = run("git", "log", "--no-merges", "--pretty=format:__COMMIT__%H", "--name-only", span)
    commits: dict[str, list[str]] = {}
    current = None
    for line in out.splitlines():
        if line.startswith("__COMMIT__"):
            current = line.replace("__COMMIT__", "")
            commits[current] = []
        elif line.strip() and current:
            commits[current].append(line.strip())
    return commits


def subject(sha: str) -> str:
    return run("git", "log", "-1", "--pretty=%s", sha)


def is_noise(files: list[str], subject_text: str) -> bool:
    if NOISE_SUBJECTS.match(subject_text):
        return True
    return bool(files) and all(NOISE_PATHS.match(path) for path in files)


def curated_section(notes_file: str, version: str) -> list[str]:
    """Return the hand-written changelog section for [version], if present."""
    if not os.path.exists(notes_file):
        return []
    wanted = version.lstrip("v").strip()
    lines: list[str] = []
    inside = False
    for raw in open(notes_file, encoding="utf-8"):
        line = raw.rstrip()
        if line.startswith("## "):
            inside = line[3:].strip() == wanted
            continue
        if inside:
            lines.append(line)
    return lines + [""] if lines else []


def section_for(files: list[str]) -> str:
    """Classify by the area that most of the changed paths belong to."""
    counts: dict[str, int] = {}
    for path in files:
        for name, pattern in PATH_RULES:
            if re.search(pattern, path, re.IGNORECASE):
                counts[name] = counts.get(name, 0) + 1
                break
    if not counts:
        return "other"
    # Interface changes dominate when a commit touches both UI and player code,
    # which is what a feature commit usually does.
    for preferred in ("interface", "player", "codec", "network", "update", "tooling"):
        if counts.get(preferred, 0) >= max(1, max(counts.values()) - 1):
            return preferred
    return max(counts, key=counts.get)


def humanise(text: str) -> str:
    match = CONVENTIONAL.match(text)
    if match:
        body = match.group(4).strip()
        kind = match.group(1)
        prefix = {"feat": "Added", "fix": "Fixed", "perf": "Performance", "refactor": "Reworked"}.get(kind)
        text = f"{prefix}: {body}" if prefix else body
    return text[:1].upper() + text[1:] if text else text


def main() -> int:
    prev = os.environ.get("PREVIOUS_TAG", "").strip()
    cur = os.environ.get("CURRENT_TAG", "").strip() or "HEAD"
    repo = os.environ.get("REPO", "").strip()
    notes_file = os.environ.get("NOTES_FILE", "docs/CHANGELOG.md")
    out_file = os.environ.get("OUT_FILE", "ci/changelog_section.md")

    curated = curated_section(notes_file, cur)
    if curated:
        lines = curated
        if prev and repo:
            lines.append(f"**Full diff**: https://github.com/{repo}/compare/{prev}...{cur}")
            lines.append("")
        text = "\n".join(lines)
        os.makedirs(os.path.dirname(out_file) or ".", exist_ok=True)
        with open(out_file, "w", encoding="utf-8") as handle:
            handle.write(text)
        print(text)
        return 0

    commits = changed_files(prev, cur)
    grouped: dict[str, list[str]] = {key: [] for key in SECTION_TITLES}
    seen: set[str] = set()

    for sha, files in commits.items():
        raw = subject(sha).strip()
        if not raw or raw in seen:
            continue
        seen.add(raw)
        # Release commits and formatting-only noise are not user-facing changes.
        if re.match(r"^(Merge |Revert )", raw):
            continue
        if is_noise(files, raw):
            continue
        section = section_for(files)
        grouped[section].append(humanise(raw))

    lines: list[str] = []
    for key in ("codec", "player", "network", "update", "interface", "other", "tooling"):
        items = grouped[key]
        if not items:
            continue
        lines.append(f"### {SECTION_TITLES[key]}")
        lines.append("")
        for item in items:
            lines.append(f"- {item}")
        lines.append("")

    if not lines:
        lines = ["### Changes", "", "- Maintenance release.", ""]

    # Point readers at the curated, technical changelog when one exists.
    if os.path.exists(notes_file):
        lines.append(f"Full technical changelog: `{notes_file}`")
        lines.append("")

    if prev and repo:
        lines.append(
            f"**Full diff**: https://github.com/{repo}/compare/{prev}...{cur}"
        )
        lines.append("")

    text = "\n".join(lines)
    os.makedirs(os.path.dirname(out_file) or ".", exist_ok=True)
    with open(out_file, "w", encoding="utf-8") as handle:
        handle.write(text)
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
