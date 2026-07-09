#!/usr/bin/env bash
set -euo pipefail

# Branch used when bootstrapping from GitHub.
# This defaults to development because this installer is intended to be runnable from:
# https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/development/scripts/installation/install.sh
: "${SIGMAKEE_BRANCH:=development}"
: "${SIGMAKEE_REPO:=https://github.com/ontologyportal/sigmakee.git}"

usage() {
    cat <<EOF
Usage: bash scripts/installation/install.sh [options]

Options:
  --branch <name>     Branch to install from. Default: $SIGMAKEE_BRANCH
  --skip-verify       Do not run verification scripts.
  --no-pull           Do not pull updates for dependency repositories.
  -h, --help          Show this help.

Examples:
  bash scripts/installation/install.sh
  bash scripts/installation/install.sh --branch development

Curl bootstrap:
  curl -fsSL https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/development/scripts/installation/install.sh | bash

EOF
}

while (($#)); do
    case "$1" in
        --branch)
            shift
            [ $# -gt 0 ] || {
                echo "ERROR: --branch requires a value." >&2
                exit 1
            }
            SIGMAKEE_BRANCH="$1"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "ERROR: Unknown option: $1" >&2
            usage
            exit 1
            ;;
    esac
    shift
done

export SIGMAKEE_BRANCH SIGMAKEE_REPO

# Detect whether this script is being run from a real file or piped through bash.
# When piped, BASH_SOURCE[0] may be unset or not point to an actual script file.
SCRIPT_PATH="${BASH_SOURCE[0]:-}"
if [ -n "$SCRIPT_PATH" ] && [ -f "$SCRIPT_PATH" ]; then
    SCRIPT_DIR="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"
else
    SCRIPT_DIR=""
fi

bootstrap_from_github() {
    local tmpdir
    tmpdir="$(mktemp -d)"
    echo "Bootstrapping SigmaKEE installer from branch: $SIGMAKEE_BRANCH"
    echo "Installer temp directory: $tmpdir"
    local base_url
    base_url="https://raw.githubusercontent.com/ontologyportal/sigmakee/refs/heads/$SIGMAKEE_BRANCH/scripts/installation"
    for script in common.sh ubuntu.sh rhel.sh macos.sh verify-prerequisites.sh verify-install.sh; do
        curl -fsSL "$base_url/$script" -o "$tmpdir/$script"
        chmod +x "$tmpdir/$script"
    done
    SCRIPT_DIR="$tmpdir"
}

# If this script was piped through bash, download the sibling installer scripts.
if [ -z "$SCRIPT_DIR" ] || [ ! -f "$SCRIPT_DIR/common.sh" ]; then
    bootstrap_from_github
fi

case "$(uname -s)" in
    Darwin)
        exec bash "$SCRIPT_DIR/macos.sh"
        ;;
    Linux)
        if [ ! -r /etc/os-release ]; then
            echo "ERROR: Cannot detect Linux distribution because /etc/os-release is missing." >&2
            exit 1
        fi
        # shellcheck disable=SC1091
        source /etc/os-release
        os_id="${ID:-}"
        os_like="${ID_LIKE:-}"
        case " $os_id $os_like " in
            *" ubuntu "*|*" debian "*)
                exec bash "$SCRIPT_DIR/ubuntu.sh"
                ;;
            *" rhel "*|*" fedora "*|*" centos "*|*" rocky "*|*" alma "*)
                exec bash "$SCRIPT_DIR/rhel.sh"
                ;;
            *)
                echo "ERROR: Unsupported Linux distribution: ${PRETTY_NAME:-unknown}" >&2
                echo "Supported families: Ubuntu/Debian and RHEL/CentOS/Fedora/Rocky/Alma." >&2
                exit 1
                ;;
        esac
        ;;
    *)
        echo "ERROR: Unsupported OS: $(uname -s)" >&2
        exit 1
        ;;
esac