#!/usr/bin/env bash
set -euo pipefail

# Branch used when bootstrapping from GitHub.
# This defaults to development because this installer is intended to be runnable from:
# https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/development/scripts/installation/install.sh
: "${SIGMAKEE_BRANCH:=development}"
: "${SIGMAKEE_REPO:=https://github.com/ontologyportal/sigmakee.git}"

SCRIPT_DIR=""
PASSTHROUGH_ARGS=()

INSTALLER_SCRIPTS=(
    1_ubuntu.sh
    1_rhel.sh
    1_macos.sh
    2_common.sh
    3_verify-install.sh
)

log() {
    printf '%s\n' "$*"
}

die() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

usage() {
    cat <<EOF
Usage: bash scripts/installation/install.sh [options]

Options:
  --branch <name>     Branch to install from. Default: $SIGMAKEE_BRANCH
  -h, --help          Show this help.

Examples:
  bash scripts/installation/install.sh
  bash scripts/installation/install.sh --branch development

Curl bootstrap:
  curl -fsSL https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/development/scripts/installation/install.sh | bash

Curl bootstrap with branch:
  curl -fsSL https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/development/scripts/installation/install.sh | bash -s -- --branch development

EOF
}

parse_args() {
    while (($#)); do
        case "$1" in
            --branch)
                shift
                [ $# -gt 0 ] || die "--branch requires a value."
                SIGMAKEE_BRANCH="$1"
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                die "Unknown option: $1"
                ;;
        esac
        shift
    done
    export SIGMAKEE_BRANCH SIGMAKEE_REPO
}

resolve_script_dir() {
    local script_path="${BASH_SOURCE[0]:-}"
    if [ -n "$script_path" ] && [ -f "$script_path" ]; then
        SCRIPT_DIR="$(cd "$(dirname "$script_path")" && pwd)"
    else
        SCRIPT_DIR=""
    fi
}

bootstrap_from_github() {
    local tmpdir
    local base_url
    local script
    tmpdir="$(mktemp -d)"
    base_url="https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/$SIGMAKEE_BRANCH/scripts/installation"
    log "Bootstrapping SigmaKEE installer from branch: $SIGMAKEE_BRANCH"
    log "Installer temp directory: $tmpdir"
    for script in "${INSTALLER_SCRIPTS[@]}"; do
        log "Downloading $script"
        curl -fsSL "$base_url/$script" -o "$tmpdir/$script"
        chmod +x "$tmpdir/$script"
    done
    SCRIPT_DIR="$tmpdir"
}

need_bootstrap() {
    [ -z "$SCRIPT_DIR" ] || [ ! -f "$SCRIPT_DIR/common.sh" ]
}

ensure_installer_scripts_exist() {
    local script
    for script in "${INSTALLER_SCRIPTS[@]}"; do
        [ -f "$SCRIPT_DIR/$script" ] || die "Missing installer script: $SCRIPT_DIR/$script"
    done
}

detect_linux_installer() {
    [ -r /etc/os-release ] || die "Cannot detect Linux distribution because /etc/os-release is missing."
    # shellcheck disable=SC1091
    source /etc/os-release
    local os_id="${ID:-}"
    local os_like="${ID_LIKE:-}"
    local distro_string=" $os_id $os_like "
    case "$distro_string" in
        *" ubuntu "*|*" debian "*)
            printf '%s\n' "$SCRIPT_DIR/1_ubuntu.sh"
            ;;
        *" rhel "*|*" fedora "*|*" centos "*|*" rocky "*|*" alma "*)
            printf '%s\n' "$SCRIPT_DIR/1_rhel.sh"
            ;;
        *)
            die "Unsupported Linux distribution: ${PRETTY_NAME:-unknown}. Supported families: Ubuntu/Debian and RHEL/CentOS/Fedora/Rocky/Alma."
            ;;
    esac
}

detect_os_installer() {
    case "$(uname -s)" in
        Darwin)
            printf '%s\n' "$SCRIPT_DIR/1_macos.sh"
            ;;
        Linux)
            detect_linux_installer
            ;;
        *)
            die "Unsupported OS: $(uname -s)"
            ;;
    esac
}

detect_os_and_dispatch() {
    local installer
    installer="$(detect_os_installer)"
    [ -x "$installer" ] || chmod +x "$installer"
    log "Dispatching to OS installer: $installer"
    exec bash "$installer" "${PASSTHROUGH_ARGS[@]}"
}

main() {
    parse_args "$@"
    resolve_script_dir
    if need_bootstrap; then
        bootstrap_from_github
    fi
    ensure_installer_scripts_exist
    detect_os_and_dispatch
}

main "$@"