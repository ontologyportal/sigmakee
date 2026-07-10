#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=2_common.sh
source "$SCRIPT_DIR/2_common.sh"

MACOS_PROFILE="$HOME/.zshrc"
OPENJDK_FORMULA="openjdk@21"

BREW_PACKAGES=(
    git
    ant
    cmake
    graphviz
    curl
    make
    openjdk@21
    pkg-config
)

BREW_OPTIONAL_PACKAGES=(
    eprover
)

BREW_CASKS=(
    jedit
)

require_macos() {
    if [ "$(uname -s)" != "Darwin" ]; then
        fail "macos.sh can only be run on macOS."
    fi
}

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

install_brew_packages() {
    print_header "Installing Homebrew packages"
    brew update
    brew install "${BREW_PACKAGES[@]}"
}

install_optional_brew_packages() {
    local package
    for package in "${BREW_OPTIONAL_PACKAGES[@]}"; do
        if brew install "$package"; then
            log "Installed optional Homebrew package: $package"
        else
            warn "Optional Homebrew package failed or is unavailable: $package. Continuing."
        fi
    done
}

install_brew_casks() {
    local cask
    for cask in "${BREW_CASKS[@]}"; do
        if brew install --cask --no-quarantine "$cask"; then
            log "Installed Homebrew cask: $cask"
        else
            warn "Homebrew cask failed or is already installed: $cask. Continuing."
        fi
    done
}

install_macos_prerequisites() {
    print_header "Installing macOS prerequisites"
    require_macos
    require_homebrew
    ensure_xcode_tools
    install_brew_packages
    install_optional_brew_packages
    install_brew_casks
}

openjdk_prefix() {
    brew --prefix "$OPENJDK_FORMULA" 2>/dev/null || true
}

link_openjdk_for_java_home() {
    local prefix
    prefix="$(openjdk_prefix)"
    if [ -z "$prefix" ]; then
        fail "Could not find Homebrew prefix for $OPENJDK_FORMULA."
    fi
    if [ ! -d "$prefix/libexec/openjdk.jdk" ]; then
        fail "Expected OpenJDK bundle not found: $prefix/libexec/openjdk.jdk"
    fi
    run_as_root mkdir -p /Library/Java/JavaVirtualMachines
    run_as_root ln -sfn "$prefix/libexec/openjdk.jdk" \
        /Library/Java/JavaVirtualMachines/openjdk-21.jdk
}

configure_java() {
    print_header "Configuring Java 21"
    link_openjdk_for_java_home
    export JAVA_HOME
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    [ -n "$JAVA_HOME" ] || fail "Could not find Java 21 on macOS."
    export PATH="$JAVA_HOME/bin:$PATH"
    java -version
    javac -version
}

configure_macos_environment() {
    print_header "Configuring macOS SigmaKEE environment"
    export JEDIT_HOME="$HOME/Library/jEdit"
    export JEDIT_JAR="/Applications/jEdit.app/Contents/Java/jedit.jar"
    export SUMOJEDIT_SRC="$ONTOLOGYPORTAL_GIT/SUMOjEdit"
    export SIGMA_SRC="$ONTOLOGYPORTAL_GIT/sigmakee"
    export SIGMA_CP="$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*"
    export PATH="$HOME/.local/bin:$VAMPIRE_HOME:$E_HOME:$CATALINA_HOME/bin:$PATH"
    log "JEDIT_HOME:      $JEDIT_HOME"
    log "JEDIT_JAR:       $JEDIT_JAR"
    log "SUMOJEDIT_SRC:   $SUMOJEDIT_SRC"
    log "SIGMA_SRC:       $SIGMA_SRC"
    log "CATALINA_HOME:   $CATALINA_HOME"
}

remove_existing_sigmakee_profile_block() {
    local profile="$1"
    local tmp
    tmp="$(mktemp)"
    awk '
        /# >>> SigmaKEE installer >>>/ { skip = 1; next }
        /# <<< SigmaKEE installer <<</ { skip = 0; next }
        !skip { print }
    ' "$profile" > "$tmp"
    cat "$tmp" > "$profile"
    rm -f "$tmp"
}

write_zshrc() {
    print_header "Writing ~/.zshrc"
    local profile="$MACOS_PROFILE"
    local prefix
    prefix="$(openjdk_prefix)"
    mkdir -p "$(dirname "$profile")"
    touch "$profile"
    remove_existing_sigmakee_profile_block "$profile"
    cat >> "$profile" <<EOF
# >>> SigmaKEE installer >>>
export JAVA_HOME="\$(/usr/libexec/java_home -v 21)"
export SIGMA_HOME="$SIGMA_HOME"
export ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT"
export SIGMA_SRC="$SIGMA_SRC"
export PROGRAMS_DIR="$PROGRAMS_DIR"
export CATALINA_HOME="$CATALINA_HOME"
export CATALINA_OPTS="$CATALINA_OPTS"
export VAMPIRE_HOME="$VAMPIRE_HOME"
export E_HOME="$E_HOME"
export SIGMA_CP="$SIGMA_CP"
export SUMOJEDIT_SRC="$SUMOJEDIT_SRC"
export JEDIT_HOME="$JEDIT_HOME"
export JEDIT_JAR="$JEDIT_JAR"
export PATH="\$HOME/.local/bin:\$JAVA_HOME/bin:$prefix/bin:\$VAMPIRE_HOME:\$E_HOME:\$CATALINA_HOME/bin:\$PATH"
alias jedit='java -Xmx10g -Xss1m -jar "$JEDIT_JAR"'
alias dir='ls -la'
export HISTSIZE=10000 HISTFILESIZE=100000
# <<< SigmaKEE installer <<<
EOF
    log "Environment block written to $profile"
}

main() {
    install_macos_prerequisites
    configure_java
    configure_macos_environment
    write_zshrc
    run_common_install "$@"
}

main "$@"