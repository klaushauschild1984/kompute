#!/usr/bin/env bash
# Pushes the current HEAD (on "main") to origin, retrying on non-fast-forward
# rejection by rebasing onto the latest origin/main.
#
# The release workflow commits directly to main outside of a PR, which races
# against other automation pushing to main in parallel (e.g. the CI job's
# coverage badge commits). A plain `git push` can lose that race; this makes
# the push self-heal instead of leaving main and a just-pushed tag
# inconsistent.
#
# Usage: git-push-retry.sh [tag-name]
#   With a tag name, the tag is (re-)created on the current HEAD and pushed
#   together with main atomically on every attempt. Without one, only main
#   is pushed.

set -euo pipefail

readonly TAG="${1:-}"
readonly MAX_ATTEMPTS=5

for attempt in $(seq 1 "${MAX_ATTEMPTS}"); do
  if [ -n "${TAG}" ]; then
    git tag -f -a "${TAG}" -m "Release ${TAG}"
    if git push --atomic origin main "refs/tags/${TAG}"; then
      exit 0
    fi
    git tag -d "${TAG}"
  else
    if git push origin main; then
      exit 0
    fi
  fi

  if [ "${attempt}" -eq "${MAX_ATTEMPTS}" ]; then
    echo "::error::Failed to push to main after ${MAX_ATTEMPTS} attempts (concurrent update kept winning)."
    exit 1
  fi

  echo "Push rejected (attempt ${attempt}/${MAX_ATTEMPTS}) — main advanced concurrently, rebasing and retrying..."
  git fetch origin main
  git rebase origin/main
done
