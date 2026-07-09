#!/usr/bin/env bash
set -euo pipefail

SUDO=""
if command -v sudo >/dev/null 2>&1 && [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

install_ubuntu_prerequisites() {
    print_header "Installing Ubuntu/Debian prerequisites"

    $SUDO apt-get update

    if command -v add-apt-repository >/dev/null 2>&1; then
        $SUDO add-apt-repository -y universe || true
        $SUDO apt-get update
    fi

        run_as_root env DEBIAN_FRONTEND=noninteractive apt-get install -y \
        ca-certificates \
        curl \
        libcurl4-openssl-dev \
        unzip \
        git \
        ant \
        make \
        cmake \
        gcc \
        g++ \
        graphviz \
        build-essential \
        openjdk-21-jdk \
        openjdk-21-jre-headless \
        jedit
}

configure_ubuntu_java() {
    print_header "Configuring Java 21"

    local java_bin=""
    local javac_bin=""

    java_bin="$(find /usr/lib/jvm -path '*/java-21-openjdk*/bin/java' -type f -print -quit 2>/dev/null || true)"
    javac_bin="$(find /usr/lib/jvm -path '*/java-21-openjdk*/bin/javac' -type f -print -quit 2>/dev/null || true)"

    [ -n "$java_bin" ] || fail "Could not find Java 21 under /usr/lib/jvm."
    [ -n "$javac_bin" ] || fail "Could not find javac 21 under /usr/lib/jvm."

    export JAVA_HOME
    JAVA_HOME="$(cd "$(dirname "$javac_bin")/.." && pwd)"
    export PATH="$JAVA_HOME/bin:$PATH"

    $SUDO update-alternatives --install /usr/bin/java java "$java_bin" 2100
    $SUDO update-alternatives --install /usr/bin/javac javac "$javac_bin" 2100
    $SUDO update-alternatives --set java "$java_bin"
    $SUDO update-alternatives --set javac "$javac_bin"

    java -version
    javac -version
}

main() {
    install_ubuntu_prerequisites
    configure_ubuntu_java
    run_common_install "$@"
}

main "$@"
