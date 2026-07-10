#!/usr/bin/env bash
# Shared installer functions for SigmaKEE.
# This file is intended to be sourced by OS-specific installer scripts.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

: "${SIGMAKEE_BRANCH:=development}"
: "${SIGMAKEE_REPO:=https://github.com/ontologyportal/sigmakee.git}"

: "${PROGRAMS_DIR:=$HOME/Programs}"
: "${ONTOLOGYPORTAL_GIT:=$HOME/workspace}"
: "${SIGMA_HOME:=$HOME/.sigmakee}"
: "${SIGMA_SRC:=$ONTOLOGYPORTAL_GIT/sigmakee}"

: "${TOMCAT_VERSION:=9.0.107}"
: "${CATALINA_HOME:=$PROGRAMS_DIR/apache-tomcat-$TOMCAT_VERSION}"
: "${CATALINA_OPTS:=-Xmx10g -Xss1m}"

: "${VAMPIRE_VERSION:=v5.0.1}"
: "${VAMPIRE_INSTALL_DIR:=$PROGRAMS_DIR/vampire}"
: "${VAMPIRE_HOME:=$VAMPIRE_INSTALL_DIR/build}"
: "${VAMPIRE_EXEC:=$VAMPIRE_HOME/vampire}"

: "${E_INSTALL_DIR:=$PROGRAMS_DIR/E}"
: "${E_HOME:=$E_INSTALL_DIR/PROVER}"
: "${EPROVER_EXEC:=$E_HOME/eprover}"

: "${LEO_VERSION:=1.7.18}"
: "${LEO_INSTALL_DIR:=$PROGRAMS_DIR/Leo-III}"
: "${LEO_BIN_DIR:=$LEO_INSTALL_DIR/bin}"
: "${LEO_EXEC:=$LEO_BIN_DIR/leo3}"

: "${WORDNET_VERSION:=3.0}"
: "${WORDNET_DIR:=$PROGRAMS_DIR/WordNet-$WORDNET_VERSION}"

: "${SUMOJEDIT_SRC:=$ONTOLOGYPORTAL_GIT/SUMOjEdit}"
: "${SIGMA_CP:=$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*}"

: "${SKIP_VERIFY:=false}"
: "${NO_PULL:=false}"

export SIGMAKEE_BRANCH SIGMAKEE_REPO
export PROGRAMS_DIR ONTOLOGYPORTAL_GIT SIGMA_HOME SIGMA_SRC
export TOMCAT_VERSION CATALINA_HOME CATALINA_OPTS
export VAMPIRE_VERSION VAMPIRE_INSTALL_DIR VAMPIRE_HOME VAMPIRE_EXEC
export E_INSTALL_DIR E_HOME EPROVER_EXEC
export LEO_VERSION LEO_INSTALL_DIR LEO_BIN_DIR LEO_EXEC
export WORDNET_VERSION WORDNET_DIR
export SUMOJEDIT_SRC SIGMA_CP
export PATH="$HOME/.local/bin:$VAMPIRE_HOME:$E_HOME:$CATALINA_HOME/bin:$PATH"

INSTALL_ERRORS=()

log() {
    printf '%s\n' "$*"
}

warn() {
    printf 'WARNING: %s\n' "$*" >&2
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

print_header() {
    printf '\n==================================================================\n'
    printf '>>> %s\n' "$1"
    printf '==================================================================\n'
}

record_error() {
    INSTALL_ERRORS+=("$1")
    warn "$1"
}

run_required() {
    local name="$1"
    shift
    print_header "$name"
    if ! "$@"; then
        fail "$name failed."
    fi
}

run_optional() {
    local name="$1"
    shift
    print_header "$name"
    set +e
    "$@"
    local status=$?
    set -e
    if [ "$status" -ne 0 ]; then
        record_error "$name failed; continuing."
        return 0
    fi
}

run_as_root() {
    if [ "$(id -u)" -eq 0 ]; then
        "$@"
    elif command -v sudo >/dev/null 2>&1; then
        sudo "$@"
    else
        fail "This command requires root privileges: $*"
    fi
}

safe_reinstall_dir() {
    local dir="$1"
    if [ -e "$dir" ]; then
        rm -rf "$dir"
    fi
    mkdir -p "$dir"
}

safe_reinstall_file_path() {
    local path="$1"
    rm -rf "$path"
    mkdir -p "$(dirname "$path")"
}

download_file() {
    local url="$1"
    local dest="$2"
    mkdir -p "$(dirname "$dest")"
    curl -fsSL "$url" -o "$dest"
}

usage_common() {
    cat <<EOF
Usage: bash scripts/installation/install.sh [options]

Options:
  --branch <name>     Branch to install from. Default: $SIGMAKEE_BRANCH
  --skip-verify       Do not run prerequisite/install verification scripts.
  --no-pull           Do not pull updates for existing repositories.
  -h, --help          Show help.

Environment overrides:
  SIGMA_HOME          Default: \$HOME/.sigmakee
  ONTOLOGYPORTAL_GIT  Default: \$HOME/workspace
  SIGMA_SRC           Default: \$ONTOLOGYPORTAL_GIT/sigmakee
  PROGRAMS_DIR        Default: \$HOME/Programs
  CATALINA_HOME       Default: \$HOME/Programs/apache-tomcat-9.0.107
  TOMCAT_VERSION      Default: 9.0.107
EOF
}

parse_common_args() {
    while (($#)); do
        case "$1" in
            --skip-verify)
                SKIP_VERIFY=true
                ;;

            --no-pull)
                NO_PULL=true
                ;;

            -h|--help)
                usage_common
                exit 0
                ;;

            *)
                fail "Unknown common installer option: $1"
                ;;
        esac
        shift
    done
    export SKIP_VERIFY NO_PULL
}

welcome() {
    log "Welcome to the installation of:"
    log '  _________.___  ________    _____      _____'
    log ' /   _____/|   |/  _____/   /     \    /  _  \'
    log ' \_____  \ |   /   \  ___  /  \ /  \  /  /_\  \'
    log ' /        \|   \    \_\  \/    V    \/    |    \'
    log '/_______  /|___|\______  /\____|__  /\____|__  /'
    log '        \/             \/         \/         \/'
    log "Home: http://ontologyportal.github.io/sigmakee/"
}

ensure_install_directories() {
    mkdir -p "$PROGRAMS_DIR" "$ONTOLOGYPORTAL_GIT" "$SIGMA_HOME"
    log "Programs:   $PROGRAMS_DIR"
    log "Workspace:  $ONTOLOGYPORTAL_GIT"
    log "Sigma home: $SIGMA_HOME"
}

clone_or_update_repo_to_dir() {
    local repo="$1"
    local target_dir="$2"
    local branch="${3:-}"
    if [ ! -e "$target_dir" ]; then
        log "Cloning $repo into $target_dir"
        if [ -n "$branch" ]; then
            git clone --branch "$branch" "$repo" "$target_dir"
        else
            git clone "$repo" "$target_dir"
        fi
        return
    fi
    if [ ! -d "$target_dir/.git" ]; then
        fail "$target_dir exists, but it is not a Git repository. Move it or remove it, then rerun."
    fi
    if [ "$NO_PULL" = true ]; then
        log "Using existing checkout without pulling: $target_dir"
        return
    fi
    log "Updating $target_dir"
    if [ -n "$branch" ]; then
        git -C "$target_dir" fetch origin "$branch"
        git -C "$target_dir" checkout "$branch"
        git -C "$target_dir" pull --ff-only origin "$branch"
    else
        git -C "$target_dir" pull --ff-only
    fi
}

clone_or_update_sigmakee_repo() {
    local target_dir="$ONTOLOGYPORTAL_GIT/sigmakee"
    clone_or_update_repo_to_dir "$SIGMAKEE_REPO" "$target_dir" "$SIGMAKEE_BRANCH"
    SIGMA_SRC="$target_dir"
    SIGMA_CP="$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*"
    export SIGMA_SRC SIGMA_CP
}

clone_or_update_workspace_repositories() {
    clone_or_update_repo_to_dir "https://github.com/ontologyportal/sumo" \
        "$ONTOLOGYPORTAL_GIT/sumo"
    clone_or_update_repo_to_dir "https://github.com/ontologyportal/SigmaUtils" \
        "$ONTOLOGYPORTAL_GIT/SigmaUtils"
    clone_or_update_repo_to_dir "https://github.com/ontologyportal/sigmaAntlr" \
        "$ONTOLOGYPORTAL_GIT/sigmaAntlr"
    clone_or_update_repo_to_dir "https://github.com/ontologyportal/TPTP-ANTLR" \
        "$ONTOLOGYPORTAL_GIT/TPTP-ANTLR"
    clone_or_update_repo_to_dir "https://github.com/ontologyportal/SUMOjEdit" \
        "$ONTOLOGYPORTAL_GIT/SUMOjEdit"
    clone_or_update_repo_to_dir "https://github.com/TPTPWorld/JJParser.git" \
        "$ONTOLOGYPORTAL_GIT/JJParser"
    clone_or_update_repo_to_dir "https://github.com/TPTPWorld/TPTP4X.git" \
        "$ONTOLOGYPORTAL_GIT/TPTP4X"
}

clone_or_update_optional_workspace_repositories() {
    clone_or_update_repo_to_dir "https://github.com/ontologyportal/sigmanlp" \
        "$ONTOLOGYPORTAL_GIT/sigmanlp"
}

install_workspace_repos() {
    clone_or_update_sigmakee_repo
    clone_or_update_workspace_repositories
}

ensure_sigmakee_checkout() {
    [ -f "$SIGMA_SRC/build.xml" ] || fail "SIGMA_SRC does not look like a sigmakee checkout: $SIGMA_SRC"
    log "Using SigmaKEE source checkout: $SIGMA_SRC"
}

ensure_sumo_checkout() {
    [ -d "$ONTOLOGYPORTAL_GIT/sumo" ] || fail "SUMO repo missing: $ONTOLOGYPORTAL_GIT/sumo"
    [ -f "$ONTOLOGYPORTAL_GIT/sumo/Merge.kif" ] || fail "SUMO files missing under: $ONTOLOGYPORTAL_GIT/sumo"
}

install_tomcat() {
    if [ -x "$CATALINA_HOME/bin/startup.sh" ] && [ -x "$CATALINA_HOME/bin/shutdown.sh" ]; then
        log "Tomcat already exists at $CATALINA_HOME"
        return
    fi
    local archive="$PROGRAMS_DIR/apache-tomcat-$TOMCAT_VERSION.tar.gz"
    local url="https://archive.apache.org/dist/tomcat/tomcat-9/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"
    log "Installing Apache Tomcat $TOMCAT_VERSION into $PROGRAMS_DIR"
    mkdir -p "$PROGRAMS_DIR"
    download_file "$url" "$archive"
    tar -xzf "$archive" -C "$PROGRAMS_DIR"
    rm -f "$archive"
    chmod +x "$CATALINA_HOME/bin/"*.sh
}

install_tomcat_wrappers() {
    mkdir -p "$HOME/.local/bin"
    cat > "$HOME/.local/bin/startup.sh" <<EOF
#!/usr/bin/env bash
exec "$CATALINA_HOME/bin/startup.sh" "\$@"
EOF
    cat > "$HOME/.local/bin/shutdown.sh" <<EOF
#!/usr/bin/env bash
exec "$CATALINA_HOME/bin/shutdown.sh" "\$@"
EOF
    chmod +x "$HOME/.local/bin/startup.sh" "$HOME/.local/bin/shutdown.sh"
    log "Installed Tomcat wrappers into $HOME/.local/bin"
}

install_wordnet() {
    if [ -d "$WORDNET_DIR/dict" ]; then
        log "WordNet already exists at $WORDNET_DIR"
        return
    fi
    local archive="$PROGRAMS_DIR/WordNet-$WORDNET_VERSION.tar.gz"
    local url="https://wordnetcode.princeton.edu/$WORDNET_VERSION/WordNet-$WORDNET_VERSION.tar.gz"
    log "Installing WordNet $WORDNET_VERSION into $PROGRAMS_DIR"
    mkdir -p "$PROGRAMS_DIR"
    download_file "$url" "$archive"
    tar -xzf "$archive" -C "$PROGRAMS_DIR"
    rm -f "$archive"
    [ -d "$WORDNET_DIR/dict" ] || fail "WordNet install failed. Expected directory: $WORDNET_DIR/dict"
}

detect_vampire_asset() {
    local os
    local arch
    os="$(uname -s)"
    arch="$(uname -m)"
    case "$os:$arch" in
        Linux:x86_64|Linux:amd64)
            printf '%s\n' "vampire-Linux-X64.zip"
            ;;
        Linux:aarch64|Linux:arm64)
            printf '%s\n' "vampire-Linux-ARM64.zip"
            ;;
        Darwin:x86_64|Darwin:amd64)
            printf '%s\n' "vampire-macOS-X64.zip"
            ;;
        Darwin:aarch64|Darwin:arm64)
            printf '%s\n' "vampire-macOS-ARM64.zip"
            ;;
        *)
            fail "Unsupported OS/architecture for Vampire binary: $os $arch"
            ;;
    esac
}

install_vampire_binary() {
    if [ -x "$VAMPIRE_EXEC" ] && "$VAMPIRE_EXEC" --version >/dev/null 2>&1; then
        log "Vampire already installed: $VAMPIRE_EXEC"
        "$VAMPIRE_EXEC" --version || true
        return
    fi
    local asset
    local url
    local zip_file
    local unpack_dir
    local found_vampire
    asset="$(detect_vampire_asset)"
    url="https://github.com/vprover/vampire/releases/download/$VAMPIRE_VERSION/$asset"
    zip_file="$SIGMA_HOME/downloads/$asset"
    unpack_dir="$SIGMA_HOME/downloads/vampire-release"
    log "Installing Vampire $VAMPIRE_VERSION from $asset"
    mkdir -p "$SIGMA_HOME/downloads" "$VAMPIRE_HOME"
    rm -rf "$unpack_dir"
    mkdir -p "$unpack_dir"
    download_file "$url" "$zip_file"
    unzip -q -o "$zip_file" -d "$unpack_dir"
    found_vampire="$(find "$unpack_dir" -type f -name vampire -print -quit 2>/dev/null || true)"
    [ -n "$found_vampire" ] || fail "Could not find vampire executable inside $zip_file"
    cp "$found_vampire" "$VAMPIRE_EXEC"
    chmod +x "$VAMPIRE_EXEC"
    "$VAMPIRE_EXEC" --version >/dev/null 2>&1 || fail "Vampire executable exists but does not run: $VAMPIRE_EXEC"
    rm -rf "$unpack_dir"
    log "Installed Vampire: $VAMPIRE_EXEC"
}

install_eprover() {
    if [ -x "$EPROVER_EXEC" ]; then
        log "E prover already installed: $EPROVER_EXEC"
        return
    fi
    local archive="$PROGRAMS_DIR/E.tgz"
    local url="http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.6/E.tgz"
    log "Installing E prover into $E_INSTALL_DIR"
    mkdir -p "$PROGRAMS_DIR"
    rm -rf "$E_INSTALL_DIR"
    download_file "$url" "$archive"
    tar -xzf "$archive" -C "$PROGRAMS_DIR"
    rm -f "$archive"
    [ -d "$E_INSTALL_DIR" ] || fail "E extraction failed. Expected directory: $E_INSTALL_DIR"
    chmod +x "$E_INSTALL_DIR"/configure 2>/dev/null || true
    find "$E_INSTALL_DIR" -name '*.sh' -exec chmod +x {} \; 2>/dev/null || true
    (
        cd "$E_INSTALL_DIR"
        ./configure
        make
    )
    [ -x "$EPROVER_EXEC" ] || fail "E build failed. Expected executable: $EPROVER_EXEC"
    log "Installed E prover: $EPROVER_EXEC"
}

download_leo_jar() {
    local dest="$1"
    local url="https://github.com/leoprover/Leo-III/releases/download/v$LEO_VERSION/leo3-v$LEO_VERSION.jar"
    download_file "$url" "$dest"
}

install_leo() {
    if [ -x "$LEO_EXEC" ] && [ -f "$LEO_BIN_DIR/leo3.jar" ]; then
        log "Leo-III already installed: $LEO_EXEC"
        return
    fi
    local jar_file="$SIGMA_HOME/downloads/leo3-v$LEO_VERSION.jar"
    log "Installing Leo-III $LEO_VERSION into $LEO_INSTALL_DIR"
    mkdir -p "$SIGMA_HOME/downloads"
    rm -rf "$LEO_INSTALL_DIR"
    mkdir -p "$LEO_BIN_DIR"
    download_leo_jar "$jar_file"
    [ -f "$jar_file" ] || fail "Leo-III download failed. Expected jar: $jar_file"
    cp "$jar_file" "$LEO_BIN_DIR/leo3.jar"
    cat > "$LEO_EXEC" <<'EOF'
#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -jar "$DIR/leo3.jar" "$@"
EOF
    chmod +x "$LEO_EXEC"
    [ -x "$LEO_EXEC" ] || fail "Leo-III launcher was not created: $LEO_EXEC"
    [ -f "$LEO_BIN_DIR/leo3.jar" ] || fail "Leo-III jar was not installed: $LEO_BIN_DIR/leo3.jar"
    log "Installed Leo-III: $LEO_EXEC"
}

install_programs() {
    run_required "Installing Tomcat" install_tomcat
    run_required "Installing Tomcat wrappers" install_tomcat_wrappers
    run_required "Installing WordNet" install_wordnet
    run_required "Installing Vampire binary" install_vampire_binary
    run_required "Installing E prover" install_eprover
    run_optional "Installing Leo-III" install_leo
}

run_ant_project() {
    local project_dir="$1"
    local target="${2:-}"
    local description="$3"
    local output_file
    local status
    print_header "$description"
    [ -f "$project_dir/build.xml" ] || fail "Missing build.xml in $project_dir"
    output_file="$(mktemp)"
    set +e
    if [ -z "$target" ]; then
        (
            cd "$project_dir"
            ant
        ) 2>&1 | tee "$output_file"
        status=${PIPESTATUS[0]}
    else
        (
            cd "$project_dir"
            ant "$target"
        ) 2>&1 | tee "$output_file"
        status=${PIPESTATUS[0]}
    fi
    set -e
    if [ "$status" -ne 0 ] || grep -q "BUILD FAILED" "$output_file"; then
        rm -f "$output_file"
        fail "$description failed."
    fi
    rm -f "$output_file"
}

run_ant_target() {
    local target="$1"
    local description="$2"
    local output_file
    local status
    print_header "$description"
    [ -f "$SIGMA_SRC/build.xml" ] || fail "Missing SigmaKEE build.xml: $SIGMA_SRC/build.xml"
    output_file="$(mktemp)"
    set +e
    if [ -z "$target" ]; then
        (
            cd "$SIGMA_SRC"
            env \
                SIGMA_HOME="$SIGMA_HOME" \
                ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
                SIGMA_SRC="$SIGMA_SRC" \
                CATALINA_OPTS="$CATALINA_OPTS" \
                CATALINA_HOME="$CATALINA_HOME" \
                SIGMA_CP="$SIGMA_CP" \
                ant
        ) 2>&1 | tee "$output_file"
        status=${PIPESTATUS[0]}
    else
        (
            cd "$SIGMA_SRC"
            env \
                SIGMA_HOME="$SIGMA_HOME" \
                ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
                SIGMA_SRC="$SIGMA_SRC" \
                CATALINA_OPTS="$CATALINA_OPTS" \
                CATALINA_HOME="$CATALINA_HOME" \
                SIGMA_CP="$SIGMA_CP" \
                ant "$target"
        ) 2>&1 | tee "$output_file"
        status=${PIPESTATUS[0]}
    fi
    set -e
    if [ "$status" -ne 0 ] || grep -q "BUILD FAILED" "$output_file"; then
        rm -f "$output_file"
        fail "$description failed."
    fi
    rm -f "$output_file"
}

build_sigmautils() {
    run_ant_project "$ONTOLOGYPORTAL_GIT/SigmaUtils" "" "Building SigmaUtils"
}

build_tptp_antlr() {
    run_ant_project "$ONTOLOGYPORTAL_GIT/TPTP-ANTLR" "all" "Building TPTP-ANTLR"
}

build_sigma_antlr() {
    run_ant_project "$ONTOLOGYPORTAL_GIT/sigmaAntlr" "all" "Building sigmaAntlr"
}

install_sigmakee_runtime_data() {
    run_ant_target "install" "Installing SigmaKEE runtime data"
}

build_sigmakee() {
    run_ant_target "" "Building SigmaKEE"
}

build_tptp4x() {
    print_header "Building TPTP4X"
    [ -d "$ONTOLOGYPORTAL_GIT/JJParser" ] || fail "JJParser checkout missing: $ONTOLOGYPORTAL_GIT/JJParser"
    [ -d "$ONTOLOGYPORTAL_GIT/TPTP4X" ] || fail "TPTP4X checkout missing: $ONTOLOGYPORTAL_GIT/TPTP4X"
    (
        cd "$ONTOLOGYPORTAL_GIT/TPTP4X"
        make
    )
    [ -x "$ONTOLOGYPORTAL_GIT/TPTP4X/tptp4X" ] || fail "TPTP4X build failed. Expected executable: $ONTOLOGYPORTAL_GIT/TPTP4X/tptp4X"
}

install_sumojedit() {
    print_header "Installing SUMOjEdit"
    : "${JEDIT_HOME:?JEDIT_HOME must be set by the OS-specific installer before run_common_install.}"
    : "${JEDIT_JAR:?JEDIT_JAR must be set by the OS-specific installer before run_common_install.}"
    mkdir -p "$JEDIT_HOME/jars" "$JEDIT_HOME/modes"
    [ -f "$JEDIT_JAR" ] || fail "jEdit jar not found: $JEDIT_JAR"
    [ -f "$SUMOJEDIT_SRC/build.xml" ] || fail "SUMOjEdit checkout not found: $SUMOJEDIT_SRC"
    (
        cd "$SUMOJEDIT_SRC"
        env \
            SIGMA_HOME="$SIGMA_HOME" \
            SIGMA_SRC="$SIGMA_SRC" \
            SIGMA_CP="$SIGMA_CP" \
            JEDIT_HOME="$JEDIT_HOME" \
            JEDIT_JAR="$JEDIT_JAR" \
            ant
    )
    if [ -f "$SUMOJEDIT_SRC/SUMOjEdit.jar" ]; then
        cp "$SUMOJEDIT_SRC/SUMOjEdit.jar" "$JEDIT_HOME/jars/"
    elif [ -f "$SUMOJEDIT_SRC/dist/SUMOjEdit.jar" ]; then
        cp "$SUMOJEDIT_SRC/dist/SUMOjEdit.jar" "$JEDIT_HOME/jars/"
    else
        fail "SUMOjEdit.jar was not created by ant."
    fi
    cp -f "$SUMOJEDIT_SRC/kif.xml" "$SUMOJEDIT_SRC/TPTP.xml" "$JEDIT_HOME/modes/" 2>/dev/null || true
}

build_all() {
    run_required "Building SigmaUtils" build_sigmautils
    run_required "Building TPTP-ANTLR" build_tptp_antlr
    run_required "Building sigmaAntlr" build_sigma_antlr
    run_required "Installing SigmaKEE runtime data" install_sigmakee_runtime_data
    run_required "Building SigmaKEE" build_sigmakee
    run_required "Building TPTP4X" build_tptp4x
    run_optional "Installing SUMOjEdit" install_sumojedit
}

run_prerequisite_verification() {
    local verifier="$SCRIPT_DIR/3_verify-install.sh"
    local output_file
    [ -f "$verifier" ] || fail "Install verifier not found: $verifier"
    output_file="$(mktemp)"
    bash "$verifier" 2>&1 | tee "$output_file"
    if grep -q "MISSING PREREQUISITES" "$output_file"; then
        rm -f "$output_file"
        fail "Missing prerequisites detected."
    fi
    rm -f "$output_file"
}

run_install_verification() {
    local verifier="$SCRIPT_DIR/3_verify-install.sh"
    [ -f "$verifier" ] || fail "Install verifier not found: $verifier"
    bash "$verifier"
}

verify_installation() {
    if [ "$SKIP_VERIFY" = true ]; then
        warn "Skipping verification because --skip-verify was requested."
        return
    fi
    run_required "Running prerequisite verification" run_prerequisite_verification
    run_required "Running install verification" run_install_verification
}

print_success_message() {
    cat <<EOF
SigmaKEE installation finished.

To load your shell environment in the current terminal, run:
  source ~/.bashrc     # Linux
  source ~/.zshrc      # macOS

To start Tomcat:
  startup.sh

Then open:
  http://localhost:8080/sigma/login.jsp

Default credentials:
  username: admin
  password: admin

To stop Tomcat:
  shutdown.sh
EOF
}

print_error_summary_if_needed() {
    if [ "${#INSTALL_ERRORS[@]}" -eq 0 ]; then
        return
    fi
    warn "Installation completed with non-fatal errors:"
    printf '  - %s\n' "${INSTALL_ERRORS[@]}"
}

run_common_install() {
    parse_common_args "$@"
    welcome
    run_required "Creating install directories" ensure_install_directories
    run_required "Cloning or updating workspace repositories" install_workspace_repos
    run_optional "Cloning or updating optional workspace repositories" clone_or_update_optional_workspace_repositories
    run_required "Checking SigmaKEE checkout" ensure_sigmakee_checkout
    run_required "Checking SUMO checkout" ensure_sumo_checkout
    run_required "Installing external programs" install_programs
    run_required "Building and installing SigmaKEE components" build_all
    verify_installation
    print_error_summary_if_needed
    print_success_message
}