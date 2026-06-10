#!/usr/bin/env bash
set -Eeuo pipefail

: "${ONTOLOGYPORTAL_GIT:?ONTOLOGYPORTAL_GIT is not set}"
: "${SIGMA_HOME:?SIGMA_HOME is not set}"
: "${CATALINA_HOME:?CATALINA_HOME is not set}"

pull_repo() {
    local repo="$1"

    if [ ! -d "$repo/.git" ]; then
        echo "WARN: repo missing, skipping: $repo"
        return 0
    fi

    echo "Updating $repo"
    git -C "$repo" pull --ff-only
}

pull_repo "$ONTOLOGYPORTAL_GIT/sumo"

if [ -d "$ONTOLOGYPORTAL_GIT/sumo" ]; then
    mkdir -p "$SIGMA_HOME/KBs"
    cp "$ONTOLOGYPORTAL_GIT/sumo"/*.kif "$SIGMA_HOME/KBs/" 2>/dev/null || true
    cp -R "$ONTOLOGYPORTAL_GIT/sumo/Translations" "$SIGMA_HOME/KBs/" 2>/dev/null || true
    cp -R "$ONTOLOGYPORTAL_GIT/sumo/WordNetMappings" "$SIGMA_HOME/KBs/" 2>/dev/null || true
    cp -R "$ONTOLOGYPORTAL_GIT/sumo/development" "$SIGMA_HOME/KBs/" 2>/dev/null || true
fi

pull_repo "$ONTOLOGYPORTAL_GIT/SigmaUtils"
pull_repo "$ONTOLOGYPORTAL_GIT/sigma-rs"
pull_repo "$ONTOLOGYPORTAL_GIT/sigmakee"
pull_repo "$ONTOLOGYPORTAL_GIT/TPTP-ANTLR"
pull_repo "$ONTOLOGYPORTAL_GIT/SUMOjEdit"
pull_repo "$ONTOLOGYPORTAL_GIT/sumonlp"
pull_repo "$ONTOLOGYPORTAL_GIT/sigmanlp"
pull_repo "$ONTOLOGYPORTAL_GIT/sigmaAntlr"

rm -f "$ONTOLOGYPORTAL_GIT/SUMOjEdit/build.properties"
rm -f "$ONTOLOGYPORTAL_GIT/SUMOjEdit/build.number"

rm -f "$CATALINA_HOME"/logs/*.*
rm -f "$CATALINA_HOME"/webapps/sigma/graph/*.*
rm -f "$SIGMA_HOME"/KBs/*.ser
rm -f "$SIGMA_HOME"/KBs/*.tptp
rm -f "$SIGMA_HOME"/KBs/*.tff
rm -f "$SIGMA_HOME"/KBs/*.thf
rm -f "$SIGMA_HOME"/KBs/WordNetMappings/*.ser

echo "updateFiles.sh completed."
