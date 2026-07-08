#!/usr/bin/env bash
# Shared installer functions for SigmaKEE.
# This file is intended to be sourced by install.sh and OS-specific scripts.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SIGMA_SRC_DEFAULT="$(cd "$SCRIPT_DIR/../.." && pwd)"

: "${SIGMA_SRC:=$SIGMA_SRC_DEFAULT}"
: "${ONTOLOGYPORTAL_GIT:=$(cd "$SIGMA_SRC/.." && pwd)}"
: "${PROGRAMS_DIR:=$HOME/Programs}"
: "${SIGMA_HOME:=$HOME/.sigmakee}"
: "${TOMCAT_VERSION:=9.0.107}"
: "${CATALINA_HOME:=$PROGRAMS_DIR/apache-tomcat-$TOMCAT_VERSION}"
: "${CATALINA_OPTS:=-Xmx10g -Xss1m}"
: "${VAMPIRE_HOME:=$PROGRAMS_DIR/vampire/build}"
: "${E_HOME:=$PROGRAMS_DIR/E/PROVER}"
: "${SIGMA_CP:=$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*}"

export SIGMA_SRC ONTOLOGYPORTAL_GIT PROGRAMS_DIR SIGMA_HOME TOMCAT_VERSION
export CATALINA_HOME CATALINA_OPTS VAMPIRE_HOME E_HOME SIGMA_CP
export PATH="$VAMPIRE_HOME:$E_HOME:$CATALINA_HOME/bin:$PATH"

RUN_VERIFY=1
UPDATE_DEPENDENCIES=1

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

welcome() {
    log "Welcome to the SigmaKEE installer"
    log "Home: http://ontologyportal.github.io/sigmakee/"
}

usage_common() {
    cat <<EOF2
Usage: bash scripts/installation/install.sh [options]

Options:
  --skip-verify       Do not run prerequisite/install verification scripts.
  --no-pull           Do not pull updates for dependency repositories.
  -h, --help          Show this help.

Environment overrides:
  SIGMA_HOME          Default: \$HOME/.sigmakee
  ONTOLOGYPORTAL_GIT  Default: parent directory of this sigmakee checkout
  SIGMA_SRC           Default: this sigmakee checkout
  PROGRAMS_DIR        Default: \$HOME/Programs
  CATALINA_HOME       Default: \$HOME/Programs/apache-tomcat-9.0.107
  TOMCAT_VERSION      Default: 9.0.107
EOF2
}

parse_common_args() {
    while (($#)); do
        case "$1" in
            --skip-verify)
                RUN_VERIFY=0
                ;;
            --no-pull)
                UPDATE_DEPENDENCIES=0
                ;;
            -h|--help)
                usage_common
                exit 0
                ;;
            *)
                fail "Unknown option: $1"
                ;;
        esac
        shift
    done
}

shell_profile_file() {
    if [ -n "${SIGMA_INSTALL_PROFILE:-}" ]; then
        printf '%s\n' "$SIGMA_INSTALL_PROFILE"
        return
    fi

    case "$(uname -s)" in
        Darwin)
            printf '%s\n' "$HOME/.zshrc"
            ;;
        *)
            printf '%s\n' "$HOME/.bashrc"
            ;;
    esac
}

write_profile_block() {
    local profile
    local tmp
    profile="$(shell_profile_file)"
    mkdir -p "$(dirname "$profile")"
    touch "$profile"
    tmp="$(mktemp)"

    awk '
        /# >>> SigmaKEE installer >>>/ { skip = 1; next }
        /# <<< SigmaKEE installer <<</ { skip = 0; next }
        !skip { print }
    ' "$profile" > "$tmp"
    cat "$tmp" > "$profile"
    rm -f "$tmp"

    cat >> "$profile" <<EOF2
# >>> SigmaKEE installer >>>
export SIGMA_HOME="$SIGMA_HOME"
export ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT"
export SIGMA_SRC="$SIGMA_SRC"
export PROGRAMS_DIR="$PROGRAMS_DIR"
export CATALINA_HOME="$CATALINA_HOME"
export CATALINA_OPTS="$CATALINA_OPTS"
export VAMPIRE_HOME="$VAMPIRE_HOME"
export E_HOME="$E_HOME"
export SIGMA_CP="$SIGMA_CP"
export PATH="\$VAMPIRE_HOME:\$E_HOME:\$CATALINA_HOME/bin:\$PATH"
alias dir='ls --color=auto --format=vertical -la'
export HISTSIZE=10000 HISTFILESIZE=100000
# <<< SigmaKEE installer <<<
EOF2

    log "Environment block written to $profile"
}

ensure_install_directories() {
    print_header "Creating install directories"
    mkdir -p "$PROGRAMS_DIR" "$ONTOLOGYPORTAL_GIT" "$SIGMA_HOME"
    log "Programs:  $PROGRAMS_DIR"
    log "Workspace: $ONTOLOGYPORTAL_GIT"
    log "Sigma home: $SIGMA_HOME"
}

ensure_sigmakee_checkout() {
    if [ ! -f "$SIGMA_SRC/build.xml" ]; then
        fail "SIGMA_SRC does not look like a sigmakee checkout: $SIGMA_SRC"
    fi
    log "Using SigmaKEE source checkout: $SIGMA_SRC"
}

clone_or_update_repo() {
    local repo="$1"
    local dir_name
    local target_dir
    dir_name="$(basename "$repo" .git)"
    target_dir="$ONTOLOGYPORTAL_GIT/$dir_name"

    if [ ! -e "$target_dir" ]; then
        log "Cloning $repo into $target_dir"
        git clone "$repo" "$target_dir"
        return
    fi

    if [ ! -d "$target_dir/.git" ]; then
        fail "$target_dir exists, but it is not a Git repository. Move it or remove it, then rerun."
    fi

    if [ "$UPDATE_DEPENDENCIES" -eq 1 ]; then
        log "Updating $dir_name"
        git -C "$target_dir" pull --ff-only
    else
        log "Skipping update for $dir_name (--no-pull)"
    fi
}

clone_or_update_repositories() {
    print_header "Cloning or updating dependency repositories"

    local repos=(
        "https://github.com/ontologyportal/sumo"
        "https://github.com/ontologyportal/TPTP-ANTLR"
        "https://github.com/ontologyportal/sigmaAntlr"
        "https://github.com/ontologyportal/SigmaUtils"
    )

    for repo in "${repos[@]}"; do
        clone_or_update_repo "$repo"
    done
}

clone_or_update_sigmakee_repo() {
    print_header "Cloning or updating SigmaKEE"

    local target_dir="$ONTOLOGYPORTAL_GIT/sigmakee"

    if [ ! -e "$target_dir" ]; then
        echo "Cloning SigmaKEE branch '$SIGMAKEE_BRANCH'..."
        git clone --branch "$SIGMAKEE_BRANCH" "$SIGMAKEE_REPO" "$target_dir"
    elif [ -d "$target_dir/.git" ]; then
        echo "SigmaKEE repository already exists at $target_dir"

        if [ "$UPDATE_DEPENDENCIES" -eq 1 ]; then
            git -C "$target_dir" fetch origin "$SIGMAKEE_BRANCH"
            git -C "$target_dir" checkout "$SIGMAKEE_BRANCH"
            git -C "$target_dir" pull --ff-only origin "$SIGMAKEE_BRANCH"
        else
            echo "Skipping SigmaKEE update because --no-pull was provided."
        fi
    else
        echo "ERROR: $target_dir exists but is not a Git repository." >&2
        exit 1
    fi

    export SIGMA_SRC="$target_dir"
}

install_tomcat_if_missing() {
    print_header "Checking Tomcat"

    if [ -x "$CATALINA_HOME/bin/startup.sh" ] && [ -x "$CATALINA_HOME/bin/shutdown.sh" ]; then
        log "Tomcat already exists at $CATALINA_HOME"
        return
    fi

    local archive="$PROGRAMS_DIR/apache-tomcat-$TOMCAT_VERSION.tar.gz"
    local url="https://archive.apache.org/dist/tomcat/tomcat-9/v$TOMCAT_VERSION/bin/apache-tomcat-$TOMCAT_VERSION.tar.gz"

    log "Installing Apache Tomcat $TOMCAT_VERSION into $PROGRAMS_DIR"
    mkdir -p "$PROGRAMS_DIR"
    curl -fsSL "$url" -o "$archive"
    tar -xzf "$archive" -C "$PROGRAMS_DIR"
    chmod +x "$CATALINA_HOME/bin/"*.sh
}

run_prerequisite_verification() {
    if [ "$RUN_VERIFY" -ne 1 ]; then
        log "Skipping prerequisite verification."
        return
    fi

    print_header "Verifying prerequisites"

    local verifier="$SCRIPT_DIR/verify-prerequisites.sh"

    if [ ! -f "$verifier" ]; then
        fail "Prerequisite verifier not found: $verifier"
    fi
    
    local output_file
    output_file="$(mktemp)"
    bash "$verifier" 2>&1 | tee "$output_file"
    if grep -q "MISSING PREREQUISITES" "$output_file"; then
        rm -f "$output_file"
        fail "Missing prerequisites detected."
    fi
    rm -f "$output_file"
}

run_install_verification() {
    if [ "$RUN_VERIFY" -ne 1 ]; then
        log "Skipping install verification."
        return
    fi

    print_header "Running install verification"

    local verifier=""
    if [ -f "$SCRIPT_DIR/verify-install.sh" ]; then
        verifier="$SCRIPT_DIR/verify-install.sh"
    elif [ -f "$SIGMA_SRC/VerifyInstall.sh" ]; then
        verifier="$SIGMA_SRC/VerifyInstall.sh"
    fi

    if [ -z "$verifier" ]; then
        warn "No install verifier found. Skipping."
        return
    fi

    bash "$verifier"
}

run_ant_target() {
    local target="$1"
    local description="$2"
    local output_file
    local status

    print_header "$description"
    cd "$SIGMA_SRC"
    output_file="$(mktemp)"

    set +e
    if [ -z "$target" ]; then
        env SIGMA_HOME="$SIGMA_HOME" \
            ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
            SIGMA_SRC="$SIGMA_SRC" \
            CATALINA_OPTS="$CATALINA_OPTS" \
            CATALINA_HOME="$CATALINA_HOME" \
            SIGMA_CP="$SIGMA_CP" \
            ant 2>&1 | tee "$output_file"
        status=${PIPESTATUS[0]}
    else
        env SIGMA_HOME="$SIGMA_HOME" \
            ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
            SIGMA_SRC="$SIGMA_SRC" \
            CATALINA_OPTS="$CATALINA_OPTS" \
            CATALINA_HOME="$CATALINA_HOME" \
            SIGMA_CP="$SIGMA_CP" \
            ant "$target" 2>&1 | tee "$output_file"
        status=${PIPESTATUS[0]}
    fi
    set -e

    if [ "$status" -ne 0 ] || grep -q "BUILD FAILED" "$output_file"; then
        rm -f "$output_file"
        fail "$description failed."
    fi

    rm -f "$output_file"
}

install_sigmakee() {
    run_ant_target "install" "Running ant install"
}

compile_sigmakee() {
    run_ant_target "" "Running ant compile"
}

print_success_message() {
    local profile
    profile="$(shell_profile_file)"

    cat <<EOF2

SigmaKEE installation finished.

To load the environment in your current shell:
  source "$profile"

To start Tomcat:
  startup.sh

Then open:
  http://localhost:8080/sigma/login.html

Default credentials:
  username: admin
  password: admin

To stop Tomcat:
  shutdown.sh
EOF2
}

run_common_install() {
    parse_common_args "$@"
    welcome
    ensure_install_directories
    clone_or_update_sigmakee_repo
    ensure_sigmakee_checkout
    write_profile_block
    install_tomcat_if_missing
    run_prerequisite_verification
    install_sigmakee
    compile_sigmakee
    run_install_verification
    print_success_message
}
