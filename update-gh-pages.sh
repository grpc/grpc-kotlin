#!/usr/bin/env bash
#
# This script will build the API reference docs, copy them to the gh-pages
# branch, and tell you whether you need to commit changes.

set -e -o pipefail

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
: ${CURRENT_BRANCH:=master}

(
  set -x;
  ./gradlew dokka && \
  git checkout gh-pages && \
  git pull && \
  rm -Rf *.css grpc-kotlin && \
  cp -R build/dokka/* .
)

if [[ -z $(git status --porcelain) ]]; then
  echo
  echo "No updates necessary to the API reference."
  if [[ -n "$CURRENT_BRANCH" ]]; then
    echo "Switching back to branch $CURRENT_BRANCH"
    (set -x; git checkout $CURRENT_BRANCH)
  fi
else
  echo
  echo "IMPORTANT:"
  echo "  You're on the gh-pages branch now."
  echo "  Pages of the API reference have changed; for details, run: git status"
  echo "  You need to commit and push the changes to the upstream gh-pages branch."
  exit 1
fi
