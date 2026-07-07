#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

usage() {
    cat <<EOF2
Usage: bash scripts/installation/install.sh [options]

Detects the current operating system and dispatches to the matching installer:
  macOS              scripts/installation/macos.sh
  Ubuntu/Debian      scripts/installation/ubuntu.sh
  RHEL/CentOS/Fedora scripts/installation/rhel.sh

Common options:
  --skip-verify       Do not run verification scripts.
  --no-pull           Do not pull updates for dependency repositories.
  -h, --help          Show this help.
EOF2
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    usage
    exit 0
fi

case "$(uname -s)" in
    Darwin)
        exec bash "$SCRIPT_DIR/macos.sh" "$@"
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
                exec bash "$SCRIPT_DIR/ubuntu.sh" "$@"
                ;;
            *" rhel "*|*" fedora "*|*" centos "*|*" rocky "*|*" alma "*)
                exec bash "$SCRIPT_DIR/rhel.sh" "$@"
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
