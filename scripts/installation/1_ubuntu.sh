#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=2_common.sh
source "$SCRIPT_DIR/2_common.sh"

UBUNTU_PROFILE="$HOME/.bashrc"

APT_PACKAGES=(
    ca-certificates
    curl
    libcurl4-openssl-dev
    unzip
    git
    ant
    make
    cmake
    gcc
    g++
    graphviz
    build-essential
    openjdk-21-jdk
    openjdk-21-jre-headless
    jedit
)

require_linux() {
    if [ "$(uname -s)" != "Linux" ]; then
        fail "ubuntu.sh can only be run on Linux."
    fi
}

require_apt_get() {
    if ! command -v apt-get >/dev/null 2>&1; then
        fail "apt-get was not found. This script is intended for Ubuntu/Debian systems."
    fi
}

enable_universe_if_available() {
    if command -v add-apt-repository >/dev/null 2>&1; then
        run_as_root add-apt-repository -y universe || true
    fi
}

install_ubuntu_prerequisites() {
    print_header "Installing Ubuntu/Debian prerequisites"
    require_linux
    require_apt_get
    run_as_root apt-get update
    enable_universe_if_available
    run_as_root apt-get update
    run_as_root env DEBIAN_FRONTEND=noninteractive apt-get install -y "${APT_PACKAGES[@]}"
}

find_java_21_binary() {
    find /usr/lib/jvm -path '*/java-21-openjdk*/bin/java' -type f -print -quit 2>/dev/null || true
}

find_javac_21_binary() {
    find /usr/lib/jvm -path '*/java-21-openjdk*/bin/javac' -type f -print -quit 2>/dev/null || true
}

configure_java() {
    print_header "Configuring Java 21"
    local java_bin=""
    local javac_bin=""
    java_bin="$(find_java_21_binary)"
    javac_bin="$(find_javac_21_binary)"
    [ -n "$java_bin" ] || fail "Could not find Java 21 under /usr/lib/jvm."
    [ -n "$javac_bin" ] || fail "Could not find javac 21 under /usr/lib/jvm."
    export JAVA_HOME
    JAVA_HOME="$(cd "$(dirname "$javac_bin")/.." && pwd)"
    export PATH="$JAVA_HOME/bin:$PATH"
    run_as_root update-alternatives --install /usr/bin/java java "$java_bin" 2100
    run_as_root update-alternatives --install /usr/bin/javac javac "$javac_bin" 2100
    run_as_root update-alternatives --set java "$java_bin"
    run_as_root update-alternatives --set javac "$javac_bin"
    java -version
    javac -version
}

configure_ubuntu_environment() {
    print_header "Configuring Ubuntu SigmaKEE environment"
    export JEDIT_HOME="$HOME/.jedit"
    export JEDIT_JAR="/usr/share/jedit/jedit.jar"
    export SUMOJEDIT_SRC="$ONTOLOGYPORTAL_GIT/SUMOjEdit"
    export SIGMA_SRC="$ONTOLOGYPORTAL_GIT/sigmakee"
    export SIGMA_CP="$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*"
    export PATH="$HOME/.local/bin:$JAVA_HOME/bin:$VAMPIRE_HOME:$E_HOME:$CATALINA_HOME/bin:$PATH"
    log "JAVA_HOME:       $JAVA_HOME"
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

write_bashrc() {
    print_header "Writing ~/.bashrc"
    local profile="$UBUNTU_PROFILE"
    mkdir -p "$(dirname "$profile")"
    touch "$profile"
    remove_existing_sigmakee_profile_block "$profile"
    cat >> "$profile" <<EOF
# >>> SigmaKEE installer >>>
export JAVA_HOME="$JAVA_HOME"
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
export PATH="\$HOME/.local/bin:\$JAVA_HOME/bin:\$VAMPIRE_HOME:\$E_HOME:\$CATALINA_HOME/bin:\$PATH"
alias jedit='java -Xmx10g -Xss1m -jar "$JEDIT_JAR"'
alias dir='ls --color=auto --format=vertical -la'
export HISTSIZE=10000 HISTFILESIZE=100000
# <<< SigmaKEE installer <<<
EOF
    log "Environment block written to $profile"
}

main() {
    local phase
    phase="$(detect_phase_arg "$@")"
    [ -n "$phase" ] && INSTALL_PHASE="$phase"

    if [ "$INSTALL_PHASE" = "build" ]; then
        # System packages, Java, and ~/.bashrc were already set up during
        # the "deps" phase; just make sure this shell has them so `ant`
        # (invoked by build_all) can run.
        configure_java
        configure_ubuntu_environment
    else
        install_ubuntu_prerequisites
        configure_java
        configure_ubuntu_environment
        write_bashrc
    fi
    run_common_install "$@"
}

main "$@"