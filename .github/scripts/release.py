#!/usr/bin/env python3
"""
Changelog-driven release helper. The top CHANGELOG entry names the next
version without a date; this script validates it, stamps the release date,
extracts release notes, and prepares the next development cycle.

Subcommands:
  validate  Check the top entry (version without date, non-empty, newer than
            the last tag) and print `version=vX.Y.Z` (to GITHUB_OUTPUT if set).
  finalize  Stamp today's date on the top entry, add its compare link, and set
            the release version in build.gradle.kts.
  notes     Print the body of the top entry (the release notes).
  prepare   Insert an empty next-minor entry on top and set the next
            SNAPSHOT version in build.gradle.kts.
"""

import datetime
import os
import pathlib
import re
import subprocess
import sys

CHANGELOG = pathlib.Path("CHANGELOG.md")
BUILD_GRADLE = pathlib.Path("build.gradle.kts")
REPO_URL = "https://github.com/klaushauschild1984/kompute"

UNRELEASED_HEADING = re.compile(r"^## \[v(\d+)\.(\d+)\.(\d+)\]$", re.MULTILINE)
ANY_HEADING = re.compile(r"^## \[", re.MULTILINE)
GRADLE_VERSION = re.compile(r'version = "[^"]+"')


def fail(message: str) -> None:
    print(f"::error::{message}")
    sys.exit(1)


def last_tag() -> str | None:
    tags = subprocess.run(
        ["git", "tag", "--list", "v*", "--sort=-version:refname"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout.split()
    return tags[0] if tags else None


def parse_version(tag: str) -> tuple[int, int, int]:
    major, minor, patch = tag.lstrip("v").split(".")
    return int(major), int(minor), int(patch)


def unreleased_entry() -> tuple[tuple[int, int, int], re.Match]:
    match = UNRELEASED_HEADING.search(CHANGELOG.read_text())
    if not match:
        fail("CHANGELOG.md has no top entry of the form `## [vX.Y.Z]` without a date.")
    return (int(match.group(1)), int(match.group(2)), int(match.group(3))), match


def entry_body(text: str, heading: re.Match) -> str:
    rest = text[heading.end():]
    next_heading = ANY_HEADING.search(rest)
    body = rest[: next_heading.start()] if next_heading else rest
    return re.sub(r"^\[v.*", "", body, flags=re.MULTILINE | re.DOTALL).strip()


def set_gradle_version(version: str) -> None:
    text = BUILD_GRADLE.read_text()
    replaced, count = GRADLE_VERSION.subn(f'version = "{version}"', text, count=1)
    if count != 1:
        fail(f'No `version = "..."` assignment found in {BUILD_GRADLE}.')
    BUILD_GRADLE.write_text(replaced)


def validate() -> None:
    version, heading = unreleased_entry()
    if not entry_body(CHANGELOG.read_text(), heading):
        fail("The unreleased CHANGELOG entry is empty — nothing to release.")
    previous = last_tag()
    if previous and version <= parse_version(previous):
        fail(f"CHANGELOG version v{'.'.join(map(str, version))} is not newer than the last tag {previous}.")
    tag = f"v{version[0]}.{version[1]}.{version[2]}"
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a") as out:
            out.write(f"version={tag}\n")
    print(f"version={tag}")


def finalize() -> None:
    version, heading = unreleased_entry()
    tag = f"v{version[0]}.{version[1]}.{version[2]}"
    today = datetime.date.today().isoformat()
    text = CHANGELOG.read_text()
    text = text.replace(heading.group(0), f"## [{tag}] — {today}", 1)

    if f"[{tag}]: " not in text:
        previous = last_tag()
        if previous:
            link = f"[{tag}]: {REPO_URL}/compare/{previous}...{tag}"
        else:
            link = f"[{tag}]: {REPO_URL}/releases/tag/{tag}"
        first_link = re.search(r"^\[v.*$", text, re.MULTILINE)
        if first_link:
            text = text[: first_link.start()] + link + "\n" + text[first_link.start():]
        else:
            text = text.rstrip() + "\n\n" + link + "\n"
    CHANGELOG.write_text(text)

    set_gradle_version(f"{version[0]}.{version[1]}.{version[2]}")


def notes() -> None:
    text = CHANGELOG.read_text()
    heading = re.search(r"^## \[v\d+\.\d+\.\d+\].*$", text, re.MULTILINE)
    if not heading:
        fail("CHANGELOG.md has no entries.")
    print(entry_body(text, heading))


def prepare() -> None:
    previous = last_tag()
    if not previous:
        fail("No release tag found — nothing to prepare from.")
    major, minor, _ = parse_version(previous)
    next_version = f"{major}.{minor + 1}.0"

    text = CHANGELOG.read_text()
    first_heading = ANY_HEADING.search(text)
    if not first_heading:
        fail("CHANGELOG.md has no entries.")
    text = text[: first_heading.start()] + f"## [v{next_version}]\n\n" + text[first_heading.start():]
    CHANGELOG.write_text(text)

    set_gradle_version(f"{next_version}-SNAPSHOT")


COMMANDS = {"validate": validate, "finalize": finalize, "notes": notes, "prepare": prepare}

if len(sys.argv) != 2 or sys.argv[1] not in COMMANDS:
    print(f"usage: release.py {{{'|'.join(COMMANDS)}}}", file=sys.stderr)
    sys.exit(2)
COMMANDS[sys.argv[1]]()
