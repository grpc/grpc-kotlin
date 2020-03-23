#!/usr/bin/env bash

set -e -o pipefail

function usage() {
  echo $1; echo
  echo "Usage: $(basename $0) [--help] [--push]"
  echo "  Build the API reference docs, and check whether updates need to be published."
  echo "  By default, if there are updates, then the script exits with an error status,"
  echo "  unless the --push flag is used."
  echo
  echo "  --push  Commit and push changes to the gh-pages branch."
  echo
  exit 1;
}

while [[ "$1" == -* ]]; do
  case "$1" in
    --push)     PUSH=1; shift;;
    -h|--help)  usage;;
    *)          echo "Unrecognized option, use -h for help: $1"; exit 1;;
  esac
done

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
elif [[ -z $PUSH ]]; then
  echo
  echo "IMPORTANT:"
  echo "  You're on the gh-pages branch now."
  echo "  Pages of the API reference have changed; for details, run: git status"
  echo "  You need to commit and push changes to the remote gh-pages branch."
  echo "  You can either push the changes manually or rerun this script using"
  echo "  the --push flag."
  exit 1
else
  # Note: we normalize TZ to Linux Foundation HQ timezone.
  (
    set -x;
    git add . && \
    git commit -am "Docs generated on `TZ=US/Pacific date`" && \
    git push
  )
  if [[ -n "$CURRENT_BRANCH" ]]; then
    echo "Switching back to branch $CURRENT_BRANCH"
    (set -x; git checkout $CURRENT_BRANCH)
  fi
fi
