#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# If this script lives in sigmakee/scripts, then ../.. is the workspace containing all repos.
ROOT="${ONTOLOGYPORTAL_GIT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"

export ONTOLOGYPORTAL_GIT="${ONTOLOGYPORTAL_GIT:-$ROOT}"
export SIGMA_SRC="${SIGMA_SRC:-$ROOT/sigmakee}"
export SIGMA_HOME="${SIGMA_HOME:-$ROOT/sigmakee-runtime}"
export CORPORA="${CORPORA:-$ROOT/sigmanlp/corpora}"
export CATALINA_HOME="${CATALINA_HOME:-$ROOT/tomcat}"
export SIGMA_CP="${SIGMA_CP:-$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*}"

echo "ONTOLOGYPORTAL_GIT=$ONTOLOGYPORTAL_GIT"
echo "SIGMA_SRC=$SIGMA_SRC"
echo "SIGMA_HOME=$SIGMA_HOME"
echo "CORPORA=$CORPORA"
echo "CATALINA_HOME=$CATALINA_HOME"
echo "SIGMA_CP=$SIGMA_CP"

build_repo() {
  local repo="$1"
  local dir="$ROOT/$repo"

  echo
  echo "================ Building $repo ================"

  if [ ! -d "$dir" ]; then
    echo "ERROR: expected repo directory does not exist: $dir"
    exit 1
  fi

  if [ ! -f "$dir/build.xml" ]; then
    echo "ERROR: expected Ant build file does not exist: $dir/build.xml"
    exit 1
  fi

  cd "$dir"
  ant
}

build_repo "TPTP-ANTLR"
build_repo "SigmaUtils"
build_repo "sigmaAntlr"
build_repo "sigmakee"
build_repo "SUMOjEdit"
build_repo "sigmanlp"

echo
echo "All related repo builds passed."