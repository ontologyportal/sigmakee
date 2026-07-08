#!/usr/bin/env bash
set -euo pipefail

echo "Verifying that all prerequisite programs have been installed and all environment variables are set."

os_name="$(uname -s)"

case "$os_name" in
  Darwin)
    applications=("unzip" "git" "ant" "make" "cmake" "dot" "java" "javac")
    ;;
  Linux)
    applications=("unzip" "git" "ant" "make" "cmake" "gcc" "dot" "g++" "java" "javac")
    ;;
  *)
    applications=("unzip" "git" "ant" "make" "cmake" "dot" "java" "javac")
    ;;
esac

env_variables=("SIGMA_HOME" "ONTOLOGYPORTAL_GIT" "SIGMA_SRC" "CATALINA_OPTS" "CATALINA_HOME" "SIGMA_CP")
installation_failure=false

display_name() {
  case "$1" in
    dot) echo "graphviz" ;;
    g++) echo "g++" ;;
    gcc) echo "gcc" ;;
    *) echo "$1" ;;
  esac
}

check_app_installed() {
    local app="$1"
    local app_display_name
    app_display_name="$(display_name "$app")"

    printf "Checking if %-15s is installed ...... " "$app_display_name"
    if command -v "$app" >/dev/null 2>&1; then
        printf "installed: %s\n" "$(command -v "$app")"
    else
        printf "NOT installed\n"
        installation_failure=true
    fi
}

check_env_variable() {
    local var="$1"
    printf "Checking if environment variable %-20s is set ...... " "$var"
    if [ -z "${!var:-}" ]; then
        printf "NOT set\n"
        installation_failure=true
    else
        printf "set: %s\n" "${!var}"
    fi
}

check_dir_exists() {
    local name="$1"
    local dir="$2"

    printf "Checking if %-20s exists ...... " "$name"
    if [ -d "$dir" ]; then
        printf "exists: %s\n" "$dir"
    else
        printf "MISSING: %s\n" "$dir"
        installation_failure=true
    fi
}

check_file_exists() {
    local name="$1"
    local file="$2"

    printf "Checking if %-20s exists ...... " "$name"
    if [ -f "$file" ]; then
        printf "exists: %s\n" "$file"
    else
        printf "MISSING: %s\n" "$file"
        installation_failure=true
    fi
}

check_executable_exists() {
    local name="$1"
    local file="$2"

    printf "Checking if %-20s is executable ...... " "$name"
    if [ -x "$file" ]; then
        printf "executable: %s\n" "$file"
    else
        printf "MISSING or not executable: %s\n" "$file"
        installation_failure=true
    fi
}

for app in "${applications[@]}"; do
    check_app_installed "$app"
done

for var in "${env_variables[@]}"; do
    check_env_variable "$var"
done

check_dir_exists "SIGMA_SRC" "${SIGMA_SRC:-}"
check_dir_exists "SIGMA_HOME" "${SIGMA_HOME:-}"
check_dir_exists "CATALINA_HOME" "${CATALINA_HOME:-}"
check_dir_exists "Tomcat bin" "${CATALINA_HOME:-}/bin"
check_dir_exists "SUMOjEdit" "${SUMOJEDIT_SRC:-}"
check_file_exists "jedit.jar" "${JEDIT_JAR:-}"
check_file_exists "SUMOjEdit plugin" "${JEDIT_HOME:-}/jars/SUMOjEdit.jar"
check_file_exists "SUMO jEdit mode" "${JEDIT_HOME:-}/modes/kif.xml"
check_file_exists "TPTP jEdit mode" "${JEDIT_HOME:-}/modes/TPTP.xml"

check_file_exists "build.xml" "${SIGMA_SRC:-}/build.xml"
check_executable_exists "startup.sh" "${CATALINA_HOME:-}/bin/startup.sh"
check_executable_exists "shutdown.sh" "${CATALINA_HOME:-}/bin/shutdown.sh"

printf "Checking if CATALINA_HOME/bin is on PATH ...... "
if echo "$PATH" | tr ':' '\n' | grep -Fxq "${CATALINA_HOME:-}/bin"; then
    printf "yes\n"
else
    printf "no\n"
    echo "Current PATH: $PATH"
    installation_failure=true
fi

printf "Checking Java version ...... "
java_version="$(java -version 2>&1 | head -n 1 || true)"
echo "$java_version"
if ! java -version 2>&1 | grep -Eq 'version "2[1-9]|version "[3-9][0-9]'; then
    echo "Java 21 or greater not detected."
    installation_failure=true
fi

if $installation_failure; then
    echo "MISSING PREREQUISITES"
    exit 1
fi

echo "All prerequisites are present."