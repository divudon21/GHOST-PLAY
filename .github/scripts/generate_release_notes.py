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


def section_for(files: list[str]) -> str:
    joined = " ".join(files)
    for name, pattern in PATH_RULES:
        if re.search(pattern, joined, re.IGNORECASE):
            return name
    return "other"


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
