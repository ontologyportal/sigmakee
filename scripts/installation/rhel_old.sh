#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "$SCRIPT_DIR/common.sh"

package_manager() {
    if command -v dnf >/dev/null 2>&1; then
        echo dnf
    elif command -v yum >/dev/null 2>&1; then
        echo yum
    else
        fail "Neither dnf nor yum was found."
    fi
}

install_rhel_prerequisites() {
    print_header "Installing RHEL-family prerequisites"

    local pm
    pm="$(package_manager)"

    sudo "$pm" -y update
    sudo "$pm" -y install \
        ca-certificates \
        curl \
        tar \
        gzip \
        unzip \
        git \
        make \
        cmake \
        gcc \
        gcc-c++ \
        graphviz \
        java-21-openjdk \
        java-21-openjdk-devel || fail "Failed to install required RHEL packages."

    sudo "$pm" -y groupinstall "Development Tools" || \
        sudo "$pm" -y group install "Development Tools" || \
        warn "Could not install Development Tools group. Continuing because gcc/gcc-c++ were installed directly."

    sudo "$pm" -y install ant || true
    if ! command -v ant >/dev/null 2>&1; then
        install_ant_manually
    fi
}

install_ant_manually() {
    print_header "Installing Apache Ant manually"

    local ant_version="1.10.15"
    local ant_dir="$PROGRAMS_DIR/apache-ant-$ant_version"
    local ant_zip="$PROGRAMS_DIR/apache-ant-$ant_version-bin.zip"
    local ant_url="https://archive.apache.org/dist/ant/binaries/apache-ant-$ant_version-bin.zip"

    mkdir -p "$PROGRAMS_DIR"

    if [ ! -x "$ant_dir/bin/ant" ]; then
        curl -fsSL "$ant_url" -o "$ant_zip"
        unzip -q -o "$ant_zip" -d "$PROGRAMS_DIR"
    fi

    export ANT_HOME="$ant_dir"
    export PATH="$ANT_HOME/bin:$PATH"

    if ! command -v ant >/dev/null 2>&1; then
        fail "Ant manual install failed."
    fi
}

configure_rhel_java() {
    print_header "Configuring Java 21"

    local javac_path
    javac_path="$(readlink -f "$(command -v javac)")"

    export JAVA_HOME
    JAVA_HOME="$(cd "$(dirname "$javac_path")/.." && pwd)"
    export PATH="$JAVA_HOME/bin:$PATH"

    java -version
    javac -version
}

main() {
    install_rhel_prerequisites
    configure_rhel_java
    run_common_install "$@"
}

main "$@"
