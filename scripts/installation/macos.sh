#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

require_homebrew() {
    if ! command -v brew >/dev/null 2>&1; then
        fail "Homebrew is required on macOS. Install Homebrew, then rerun this script."
    fi
}

ensure_xcode_tools() {
    if ! xcode-select -p >/dev/null 2>&1; then
        xcode-select --install || true
        fail "Xcode Command Line Tools installation was started. Rerun this script after it finishes."
    fi
}

install_macos_prerequisites() {
    print_header "Installing macOS prerequisites"
    require_homebrew
    ensure_xcode_tools
    brew update
    brew install \
    git \
    ant \
    cmake \
    graphviz \
    curl \
    make \
    openjdk@21 \
    pkg-config
    brew install eprover || warn "Homebrew eprover install failed or formula is unavailable. Continuing."
    brew install --cask --no-quarantine jedit || true
}

configure_macos_java() {
    print_header "Configuring Java 21"
    local openjdk_prefix=""
    openjdk_prefix="$(brew --prefix openjdk@21 2>/dev/null || true)"
    if [ -n "$openjdk_prefix" ] && [ -d "$openjdk_prefix/libexec/openjdk.jdk" ]; then
        sudo mkdir -p /Library/Java/JavaVirtualMachines
        sudo ln -sfn "$openjdk_prefix/libexec/openjdk.jdk" \
            /Library/Java/JavaVirtualMachines/openjdk-21.jdk
    fi
    export JAVA_HOME
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
    [ -n "$JAVA_HOME" ] || fail "Could not find a JDK on macOS."
    export PATH="$JAVA_HOME/bin:$PATH"
    java -version
    javac -version
}

main() {
    install_macos_prerequisites
    configure_macos_java
    run_common_install "$@"
}

main "$@"
