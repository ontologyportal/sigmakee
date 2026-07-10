#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=2_common.sh
source "$SCRIPT_DIR/2_common.sh"

RHEL_PROFILE="$HOME/.bashrc"
ANT_VERSION="1.10.15"
ANT_HOME="$PROGRAMS_DIR/apache-ant-$ANT_VERSION"

PKG_MANAGER=""

RHEL_PACKAGES=(
    ca-certificates
    curl
    unzip
    git
    make
    cmake
    gcc
    gcc-c++
    graphviz
    libcurl-devel
    java-21-openjdk
    java-21-openjdk-devel
)

RHEL_OPTIONAL_PACKAGES=(
    httpd
    jedit
)

require_linux() {
    if [ "$(uname -s)" != "Linux" ]; then
        fail "rhel.sh can only be run on Linux."
    fi
}

detect_package_manager() {
    if command -v dnf >/dev/null 2>&1; then
        PKG_MANAGER="dnf"
    elif command -v yum >/dev/null 2>&1; then
        PKG_MANAGER="yum"
    else
        fail "Neither dnf nor yum was found. This script is intended for RHEL-like systems."
    fi
    log "Using package manager: $PKG_MANAGER"
}

package_update() {
    run_as_root "$PKG_MANAGER" update -y
}

package_install() {
    run_as_root "$PKG_MANAGER" install -y "$@"
}

package_group_install() {
    local group_name="$1"
    if run_as_root "$PKG_MANAGER" groupinstall -y "$group_name"; then
        log "Installed package group: $group_name"
    else
        warn "Could not install package group: $group_name. Continuing."
    fi
}

install_rhel_packages() {
    print_header "Installing RHEL prerequisites"
    package_update
    package_install "${RHEL_PACKAGES[@]}"
    package_group_install "Development Tools"
}

install_optional_rhel_packages() {
    local package
    for package in "${RHEL_OPTIONAL_PACKAGES[@]}"; do
        if package_install "$package"; then
            log "Installed optional package: $package"
        else
            warn "Optional package failed or is unavailable: $package. Continuing."
        fi
    done
}

install_ant_manually_if_needed() {
    print_header "Checking Apache Ant"
    if command -v ant >/dev/null 2>&1; then
        log "Ant already available: $(command -v ant)"
        ant -version
        return
    fi
    local archive="$PROGRAMS_DIR/apache-ant-$ANT_VERSION-bin.zip"
    local url="https://archive.apache.org/dist/ant/binaries/apache-ant-$ANT_VERSION-bin.zip"
    mkdir -p "$PROGRAMS_DIR"
    log "Installing Apache Ant $ANT_VERSION into $PROGRAMS_DIR"
    curl -fsSL "$url" -o "$archive"
    unzip -q -o "$archive" -d "$PROGRAMS_DIR"
    rm -f "$archive"
    [ -x "$ANT_HOME/bin/ant" ] || fail "Ant installation failed. Expected executable: $ANT_HOME/bin/ant"
    export ANT_HOME
    export PATH="$ANT_HOME/bin:$PATH"
    ant -version
}

install_rhel_prerequisites() {
    require_linux
    detect_package_manager
    install_rhel_packages
    install_optional_rhel_packages
    install_ant_manually_if_needed
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
    export PATH="$JAVA_HOME/bin:$ANT_HOME/bin:$PATH"
    if command -v alternatives >/dev/null 2>&1; then
        run_as_root alternatives --install /usr/bin/java java "$java_bin" 2100
        run_as_root alternatives --install /usr/bin/javac javac "$javac_bin" 2100
        run_as_root alternatives --set java "$java_bin"
        run_as_root alternatives --set javac "$javac_bin"
    else
        warn "alternatives command not found. JAVA_HOME and PATH were set for this installer process."
    fi
    java -version
    javac -version
}

find_jedit_jar() {
    local candidate
    for candidate in \
        /usr/share/jedit/jedit.jar \
        /usr/share/java/jedit.jar \
        /usr/share/java/jedit/jedit.jar
    do
        if [ -f "$candidate" ]; then
            printf '%s\n' "$candidate"
            return
        fi
    done
    printf '%s\n' "/usr/share/jedit/jedit.jar"
}

configure_rhel_environment() {
    print_header "Configuring RHEL SigmaKEE environment"
    export ANT_HOME="$ANT_HOME"
    export JEDIT_HOME="$HOME/.jedit"
    export JEDIT_JAR
    JEDIT_JAR="$(find_jedit_jar)"
    if [ ! -f "$JEDIT_JAR" ]; then
        warn "jEdit jar not found at expected location: $JEDIT_JAR"
        warn "SUMOjEdit installation may be skipped unless jEdit is installed."
    fi
    export SUMOJEDIT_SRC="$ONTOLOGYPORTAL_GIT/SUMOjEdit"
    export SIGMA_SRC="$ONTOLOGYPORTAL_GIT/sigmakee"
    export SIGMA_CP="$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*"
    export PATH="$HOME/.local/bin:$JAVA_HOME/bin:$ANT_HOME/bin:$VAMPIRE_HOME:$E_HOME:$CATALINA_HOME/bin:$PATH"
    log "JAVA_HOME:       $JAVA_HOME"
    log "ANT_HOME:        $ANT_HOME"
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
    local profile="$RHEL_PROFILE"
    mkdir -p "$(dirname "$profile")"
    touch "$profile"
    remove_existing_sigmakee_profile_block "$profile"
    cat >> "$profile" <<EOF
# >>> SigmaKEE installer >>>
export JAVA_HOME="$JAVA_HOME"
export ANT_HOME="$ANT_HOME"
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
export PATH="\$HOME/.local/bin:\$JAVA_HOME/bin:\$ANT_HOME/bin:\$VAMPIRE_HOME:\$E_HOME:\$CATALINA_HOME/bin:\$PATH"
alias jedit='java -Xmx10g -Xss1m -jar "$JEDIT_JAR"'
alias dir='ls --color=auto --format=vertical -la'
export HISTSIZE=10000 HISTFILESIZE=100000
# <<< SigmaKEE installer <<<
EOF
    log "Environment block written to $profile"
}

main() {
    install_rhel_prerequisites
    configure_java
    configure_rhel_environment
    write_bashrc
    run_common_install "$@"
}

main "$@"