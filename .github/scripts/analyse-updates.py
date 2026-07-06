#!/usr/bin/env python3
"""
Analyses one or more Ben Manes dependency update reports and writes a GitHub
Actions step summary. Emits ::warning:: annotations for minor updates and
exits with code 1 if any major updates are found across any report.

Usage:
    analyse-updates.py [<label> <report.json>]...

With no arguments, analyses "build/dependencyUpdates/report.json" under the
label "Dependencies".
"""

import json
import os
import sys


def major(version: str) -> str:
    return version.split(".")[0] if version else ""


def classify(current: str, available: str) -> str:
    return "major" if major(current) != major(available) else "minor"


def available_version(dep: dict) -> str:
    avail = dep.get("available", {})
    return avail.get("release") or avail.get("milestone") or avail.get("integration") or ""


def analyse(report_path: str) -> tuple[list[dict], list[dict]]:
    with open(report_path) as f:
        report = json.load(f)

    outdated = report.get("outdated", {}).get("dependencies", [])

    major_updates = []
    minor_updates = []

    for dep in outdated:
        current = dep.get("version", "")
        newest = available_version(dep)
        if not newest:
            continue
        entry = {
            "coordinate": f"{dep['group']}:{dep['name']}",
            "current": current,
            "available": newest,
        }
        if classify(current, newest) == "major":
            major_updates.append(entry)
        else:
            minor_updates.append(entry)

    return major_updates, minor_updates


def write_table(out, heading: str, updates: list[dict], highlight: bool) -> None:
    out.write(f"## {heading}\n\n")
    out.write("| Dependency | Current | Available |\n")
    out.write("|---|---|---|\n")
    for u in updates:
        available = f"**{u['available']}**" if highlight else u["available"]
        out.write(f"| `{u['coordinate']}` | {u['current']} | {available} |\n")
    out.write("\n")


args = sys.argv[1:]
sources = list(zip(args[0::2], args[1::2])) if args else [("Dependencies", "build/dependencyUpdates/report.json")]

all_major = []
all_minor = []
summary_path = os.environ.get("GITHUB_STEP_SUMMARY", "/dev/stdout")
with open(summary_path, "a") as out:
    out.write("# Dependency Update Report\n\n")

    for label, report_path in sources:
        major_updates, minor_updates = analyse(report_path)
        all_major += major_updates
        all_minor += minor_updates

        out.write(f"### {label}\n\n")
        if not major_updates and not minor_updates:
            out.write("All dependencies are up to date.\n\n")
            continue
        if major_updates:
            write_table(out, "Major Updates", major_updates, highlight=True)
        if minor_updates:
            write_table(out, "Minor Updates", minor_updates, highlight=False)

for u in all_minor:
    print(f"::warning::Minor update available: {u['coordinate']} {u['current']} -> {u['available']}")

for u in all_major:
    print(f"::error::Major update available: {u['coordinate']} {u['current']} -> {u['available']}")

if all_major:
    sys.exit(1)
