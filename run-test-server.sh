#!/bin/bash -e
cd "$(dirname "$0")"
BIN="./interop_testing/build/install/interop_testing/bin/test-service-server"
if [[ ! -e "$BIN" ]]; then
  cat >&2 <<EOF
Could not find binary. It can be built with:
./gradlew :interop_testing:installDist
EOF
  exit 1
fi
exec "$BIN" "$@"
