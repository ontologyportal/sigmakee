#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# Logging / UI
###############################################################################

log() {
    echo "$@"
}

print_header() {
    echo
    echo "=================================================================="
    echo ">>> $1"
    echo "=================================================================="
}

welcome() {
    log "Welcome to the installation of:"
    log '  _________.___  ________    _____      _____'
    log ' /   _____/|   |/  _____/   /     \    /  _  \'
    log ' \_____  \ |   /   \  ___  /  \ /  \  /  /_\  \'
    log ' /        \|   \    \_\  \/    V    \/    |    \'
    log '/_______  /|___|\______  /\____|__  /\____|__  /'
    log '        \/             \/         \/         \/'
    log "Home: http://ontologyportal.github.io/sigmakee/"
}

###############################################################################
# System prerequisites
###############################################################################

install_prerequisites() {
    print_header "Downloading prerequisites"
    sudo apt-get update
    sudo apt-get install -y \
        curl \
        software-properties-common \
        unzip \
        git \
        ant \
        make \
        cmake \
        gcc \
        graphviz \
        build-essential \
        default-jdk
    sudo add-apt-repository -y universe
    sudo apt-get update
    echo "Pre-requisites have been installed."
}

###############################################################################
# Bashrc / environment setup
###############################################################################

ensure_bashrc_exists() {
    if [ ! -f "$HOME/.bashrc" ]; then
        echo ".bashrc does not exist. Creating it now..."
        touch "$HOME/.bashrc"
    else
        echo ".bashrc already exists."
    fi
}

add_to_bashrc() {
    local line="$1"
    if ! grep -Fxq "$line" "$HOME/.bashrc"; then
        echo "Adding: $line to .bashrc..."
        echo "$line" >> "$HOME/.bashrc"
    else
        echo "$line already exists in .bashrc."
    fi
}

set_and_persist() {
    local var_decl="$1"
    eval "$var_decl"
    add_to_bashrc "$var_decl"
}

configure_environment() {
    print_header "Configuring environment variables"
    ensure_bashrc_exists
    add_to_bashrc "alias dir='ls --color=auto --format=vertical -la'"
    add_to_bashrc "export HISTSIZE=10000 HISTFILESIZE=100000"
    set_and_persist "export SIGMA_HOME=\"\$HOME/.sigmakee\""
    set_and_persist "export ONTOLOGYPORTAL_GIT=\"\$HOME/workspace\""
    set_and_persist "export SIGMA_SRC=\"\$ONTOLOGYPORTAL_GIT/sigmakee\""
    set_and_persist "export CATALINA_OPTS=\"-Xmx10g -Xss1m\""
    set_and_persist "export CATALINA_HOME=\"\$HOME/Programs/apache-tomcat-9.0.107\""
    set_and_persist "export VAMPIRE_HOME=\"\$HOME/Programs/vampire/build\""
    set_and_persist "export E_HOME=\"\$HOME/Programs/E/PROVER\""
    set_and_persist "export PATH=\"\$VAMPIRE_HOME:\$E_HOME:\$CATALINA_HOME/bin:\$PATH\""
    set_and_persist "export SIGMA_CP=\"\$SIGMA_SRC/build/sigmakee.jar:\$SIGMA_SRC/lib/*\""
    echo "Environment configured for current script and persisted to ~/.bashrc."
}

###############################################################################
# Directory setup
###############################################################################

create_install_directories() {
    print_header "Creating install directories"
    if [ ! -d "$HOME/Programs" ]; then
        echo "Creating Programs directory in $HOME..."
        mkdir -p "$HOME/Programs"
    else
        echo "Programs directory already exists in $HOME."
    fi
    if [ ! -d "$HOME/workspace" ]; then
        echo "Creating workspace directory in $HOME..."
        mkdir -p "$HOME/workspace"
    else
        echo "Workspace directory already exists in $HOME."
    fi
}

###############################################################################
# Repository setup
###############################################################################

clone_or_update_repo() {
    local repo="$1"
    local dir_name
    local target_dir
    dir_name="$(basename "$repo" .git)"
    target_dir="$HOME/workspace/$dir_name"
    if [ ! -e "$target_dir" ]; then
        echo "Cloning $repo..."
        git clone "$repo" "$target_dir"
        return
    fi
    if [ -d "$target_dir/.git" ]; then
        echo "$dir_name repository already exists. Pulling latest changes..."
        git -C "$target_dir" pull --ff-only
        return
    fi
    echo "The folder $target_dir exists, but it is not a Git repository."
    echo "Move or delete it, then rerun this installer."
    exit 1
}

clone_or_update_repositories() {
    print_header "Cloning or updating repositories"
    local repos=(
        "https://github.com/ontologyportal/sigmakee"
        "https://github.com/ontologyportal/sumo"
        "https://github.com/ontologyportal/TPTP-ANTLR"
        "https://github.com/ontologyportal/sigmaAntlr"
        "https://github.com/ontologyportal/SigmaUtils"
    )
    for repo in "${repos[@]}"; do
        clone_or_update_repo "$repo"
    done
}

###############################################################################
# Verification helpers
###############################################################################

run_prerequisite_verification() {
    print_header "Verifying prerequisites"
    if [ ! -f "$SIGMA_SRC/VerifyInstallationPrerequisites.sh" ]; then
        echo "Missing prerequisite verifier: $SIGMA_SRC/VerifyInstallationPrerequisites.sh"
        exit 1
    fi
    local output
    output=$(bash "$SIGMA_SRC/VerifyInstallationPrerequisites.sh" | tee /dev/tty)
    if echo "$output" | grep -q "MISSING PREREQUISITES"; then
        echo "Error: Missing prerequisites detected. Exiting script."
        exit 1
    fi
    echo "All prerequisites met. Proceeding with the script."
}

run_install_verification() {
    print_header "Running install verification"
    cd "$SIGMA_SRC"
    env SIGMA_HOME="$SIGMA_HOME" \
        ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
        SIGMA_SRC="$SIGMA_SRC" \
        CATALINA_OPTS="$CATALINA_OPTS" \
        CATALINA_HOME="$CATALINA_HOME" \
        SIGMA_CP="$SIGMA_CP" \
        bash VerifyInstall.sh
}

###############################################################################
# Ant build/install
###############################################################################

run_ant_target() {
    local target="$1"
    local description="$2"
    local output
    cd "$SIGMA_SRC"
    print_header "$description"
    if [ -z "$target" ]; then
        output=$(env SIGMA_HOME="$SIGMA_HOME" \
                     ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
                     SIGMA_SRC="$SIGMA_SRC" \
                     CATALINA_OPTS="$CATALINA_OPTS" \
                     CATALINA_HOME="$CATALINA_HOME" \
                     SIGMA_CP="$SIGMA_CP" \
                     ant 2>&1 | tee /dev/tty)
    else
        output=$(env SIGMA_HOME="$SIGMA_HOME" \
                     ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
                     SIGMA_SRC="$SIGMA_SRC" \
                     CATALINA_OPTS="$CATALINA_OPTS" \
                     CATALINA_HOME="$CATALINA_HOME" \
                     SIGMA_CP="$SIGMA_CP" \
                     ant "$target" 2>&1 | tee /dev/tty)
    fi
    if echo "$output" | grep -q "BUILD FAILED"; then
        echo "BUILD FAILED detected. Exiting the script."
        exit 1
    fi
}

install_sigmakee() {
    run_ant_target "install" "Running ant install"
    echo "Install completed successfully."
}

compile_sigmakee() {
    run_ant_target "" "Running ant to compile"
    echo "Ant sigmakee compile completed successfully."
}

###############################################################################
# Final message
###############################################################################

print_success_message() {
    echo
    echo
    echo "SIGMA has been installed! Close and re-open your command line interface, or run:"
    echo "source ~/.bashrc"
    echo
    echo "To start the server:"
    echo "startup.sh"
    echo
    echo "Then point your browser to:"
    echo "http://localhost:8080/sigma/login.html"
    echo
    echo "username: admin     password: admin"
    echo
    echo "The first time logging in can take several minutes while the system is indexing."
    echo "For low memory machines, restrict Knowledge Bases loaded in:"
    echo "\$HOME/.sigmakee/KBs/config.xml"
    echo
    echo "To shutdown the server:"
    echo "shutdown.sh"
}

###############################################################################
# Main
###############################################################################

main() {
    welcome
    install_prerequisites
    configure_environment
    create_install_directories
    clone_or_update_repositories
    run_prerequisite_verification
    install_sigmakee
    compile_sigmakee
    run_install_verification
    print_success_message
}

main "$@"