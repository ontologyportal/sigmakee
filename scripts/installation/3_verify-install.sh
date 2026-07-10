#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

: "${PROGRAMS_DIR:=$HOME/Programs}"
: "${ONTOLOGYPORTAL_GIT:=$HOME/workspace}"
: "${SIGMA_HOME:=$HOME/.sigmakee}"
: "${SIGMA_SRC:=$ONTOLOGYPORTAL_GIT/sigmakee}"
: "${TOMCAT_VERSION:=9.0.107}"
: "${CATALINA_HOME:=$PROGRAMS_DIR/apache-tomcat-$TOMCAT_VERSION}"
: "${CATALINA_OPTS:=-Xmx10g -Xss1m}"
: "${SIGMA_CP:=$SIGMA_SRC/build/sigmakee.jar:$SIGMA_SRC/lib/*}"

: "${VAMPIRE_EXEC:=$PROGRAMS_DIR/vampire/build/vampire}"
: "${EPROVER_EXEC:=$PROGRAMS_DIR/E/PROVER/eprover}"
: "${LEO_EXEC:=$PROGRAMS_DIR/Leo-III/bin/leo3}"
: "${TPTP4X_EXEC:=$ONTOLOGYPORTAL_GIT/TPTP4X/tptp4X}"
: "${SIGMA_URL:=http://localhost:8080/sigma/login.jsp}"

VERIFY_FAILURES=()

log() {
    printf '%s\n' "$*"
}

warn() {
    printf 'WARNING: %s\n' "$*" >&2
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

record_failure() {
    VERIFY_FAILURES+=("$1")
    printf '[FAIL] %s\n' "$1" >&2
}

record_success() {
    printf '[ OK ] %s\n' "$1"
}

print_header() {
    printf '\n==================================================================\n'
    printf '>>> %s\n' "$1"
    printf '==================================================================\n'
}

require_env() {
    local var="$1"

    if [ -z "${!var:-}" ]; then
        record_failure "Environment variable is not set: $var"
        return 1
    fi
    record_success "$var=${!var}"
}

check_dir_exists() {
    local name="$1"
    local dir="$2"
    if [ -d "$dir" ]; then
        record_success "$name exists: $dir"
        return 0
    fi
    record_failure "$name missing: $dir"
    return 1
}

check_dir_nonempty() {
    local name="$1"
    local dir="$2"
    if [ ! -d "$dir" ]; then
        record_failure "$name missing: $dir"
        return 1
    fi
    if [ -z "$(ls -A "$dir" 2>/dev/null)" ]; then
        record_failure "$name is empty: $dir"
        return 1
    fi
    record_success "$name exists and is non-empty: $dir"
}

check_file_exists() {
    local name="$1"
    local file="$2"
    if [ -f "$file" ]; then
        record_success "$name exists: $file"
        return 0
    fi
    record_failure "$name missing: $file"
    return 1
}

check_executable_exists() {
    local name="$1"
    local file="$2"
    if [ -x "$file" ]; then
        record_success "$name is executable: $file"
        return 0
    fi
    record_failure "$name missing or not executable: $file"
    return 1
}

run_and_capture() {
    local output_file="$1"
    shift
    set +e
    "$@" >"$output_file" 2>&1
    local status=$?
    set -e
    return "$status"
}

tee_safely() {
    local output_file="$1"
    if [ -t 1 ] && [ -w /dev/tty ]; then
        tee "$output_file"
    else
        tee "$output_file" >/dev/null
    fi
}

verify_environment() {
    print_header "Verifying required environment"
    require_env SIGMA_HOME || true
    require_env ONTOLOGYPORTAL_GIT || true
    require_env SIGMA_SRC || true
    require_env CATALINA_HOME || true
    require_env CATALINA_OPTS || true
    require_env SIGMA_CP || true
}

verify_workspace() {
    print_header "Verifying workspace repositories"
    check_dir_nonempty "Workspace" "$ONTOLOGYPORTAL_GIT" || true
    check_dir_nonempty "SigmaKEE checkout" "$SIGMA_SRC" || true
    check_file_exists "SigmaKEE build.xml" "$SIGMA_SRC/build.xml" || true
    check_dir_nonempty "SUMO checkout" "$ONTOLOGYPORTAL_GIT/sumo" || true
    check_file_exists "SUMO Merge.kif" "$ONTOLOGYPORTAL_GIT/sumo/Merge.kif" || true
    check_dir_nonempty "SigmaUtils checkout" "$ONTOLOGYPORTAL_GIT/SigmaUtils" || true
    check_dir_nonempty "TPTP-ANTLR checkout" "$ONTOLOGYPORTAL_GIT/TPTP-ANTLR" || true
    check_dir_nonempty "sigmaAntlr checkout" "$ONTOLOGYPORTAL_GIT/sigmaAntlr" || true
    check_dir_nonempty "SUMOjEdit checkout" "$ONTOLOGYPORTAL_GIT/SUMOjEdit" || true
    check_dir_nonempty "TPTP4X checkout" "$ONTOLOGYPORTAL_GIT/TPTP4X" || true
    check_dir_nonempty "JJParser checkout" "$ONTOLOGYPORTAL_GIT/JJParser" || true
}

verify_programs() {
    print_header "Verifying installed external programs"
    check_dir_nonempty "Programs directory" "$PROGRAMS_DIR" || true
    check_dir_nonempty "Tomcat directory" "$CATALINA_HOME" || true
    check_dir_nonempty "Tomcat bin directory" "$CATALINA_HOME/bin" || true
    check_dir_nonempty "Tomcat webapps directory" "$CATALINA_HOME/webapps" || true
    check_executable_exists "Tomcat startup.sh" "$CATALINA_HOME/bin/startup.sh" || true
    check_executable_exists "Tomcat shutdown.sh" "$CATALINA_HOME/bin/shutdown.sh" || true
    check_dir_nonempty "WordNet directory" "$PROGRAMS_DIR/WordNet-3.0" || true
    check_dir_nonempty "WordNet dict directory" "$PROGRAMS_DIR/WordNet-3.0/dict" || true
    check_dir_nonempty "E prover directory" "$PROGRAMS_DIR/E" || true
    check_file_exists "E configure script" "$PROGRAMS_DIR/E/configure" || true
    check_executable_exists "E prover executable" "$EPROVER_EXEC" || true
    check_dir_nonempty "Vampire directory" "$PROGRAMS_DIR/vampire" || true
    check_executable_exists "Vampire executable" "$VAMPIRE_EXEC" || true
    if [ -e "$LEO_EXEC" ]; then
        check_executable_exists "Leo-III executable" "$LEO_EXEC" || true
    else
        warn "Leo-III executable not found. This is acceptable if Leo-III is optional: $LEO_EXEC"
    fi
    check_executable_exists "TPTP4X executable" "$TPTP4X_EXEC" || true
}

verify_sigmakee_data() {
    print_header "Verifying SigmaKEE runtime data"
    local config_file="$SIGMA_HOME/KBs/config.xml"
    check_dir_nonempty "SIGMA_HOME" "$SIGMA_HOME" || true
    check_dir_nonempty "SigmaKEE KB directory" "$SIGMA_HOME/KBs" || true
    check_dir_nonempty "WordNetMappings directory" "$SIGMA_HOME/KBs/WordNetMappings" || true
    check_file_exists "config.xml" "$config_file" || true
    check_file_exists "Merge.kif" "$SIGMA_HOME/KBs/Merge.kif" || true
    check_file_exists "Mid-level-ontology.kif" "$SIGMA_HOME/KBs/Mid-level-ontology.kif" || true
    check_file_exists "english_format.kif" "$SIGMA_HOME/KBs/english_format.kif" || true
    check_file_exists "domainEnglishFormat.kif" "$SIGMA_HOME/KBs/domainEnglishFormat.kif" || true
}

verify_config_xml() {
    print_header "Verifying config.xml path replacement"
    local config_file="$SIGMA_HOME/KBs/config.xml"
    check_file_exists "config.xml" "$config_file" || return 1
    if grep -qF "/home/theuser" "$config_file"; then
        record_failure "config.xml still contains /home/theuser"
    else
        record_success "config.xml does not contain /home/theuser"
    fi
    local expected_strings=(
        "$SIGMA_HOME"
        "$SIGMA_HOME/KBs"
        "$ONTOLOGYPORTAL_GIT/sumo"
        "$EPROVER_EXEC"
        "$VAMPIRE_EXEC"
        "$TPTP4X_EXEC"
    )
    local expected
    for expected in "${expected_strings[@]}"; do
        if grep -qF -- "$expected" "$config_file"; then
            record_success "config.xml contains expected path: $expected"
        else
            record_failure "config.xml missing expected path: $expected"
        fi
    done
    if [ -x "$LEO_EXEC" ]; then
        if grep -qF -- "$LEO_EXEC" "$config_file"; then
            record_success "config.xml contains Leo-III path: $LEO_EXEC"
        else
            warn "config.xml does not contain Leo-III path: $LEO_EXEC"
        fi
    fi
}

verify_build_outputs() {
    print_header "Verifying build outputs"
    check_dir_nonempty "SigmaKEE Ivy directory" "$SIGMA_SRC/.ivy" || true
    check_file_exists "SigmaUtils jar" "$ONTOLOGYPORTAL_GIT/SigmaUtils/sigmaUtils.jar" || true
    check_file_exists "TPTP-ANTLR jar" "$ONTOLOGYPORTAL_GIT/TPTP-ANTLR/tptp.jar" || true
    check_file_exists "sigmaAntlr jar" "$ONTOLOGYPORTAL_GIT/sigmaAntlr/sigmaAntlr.jar" || true
    check_file_exists "SigmaKEE jar" "$SIGMA_SRC/build/sigmakee.jar" || true
    check_file_exists "Sigma WAR" "$CATALINA_HOME/webapps/sigma.war" || true
}

verify_provers() {
    print_header "Verifying prover executables"
    if check_executable_exists "Vampire" "$VAMPIRE_EXEC"; then
        if "$VAMPIRE_EXEC" --version >/dev/null 2>&1; then
            record_success "Vampire runs successfully."
        else
            record_failure "Vampire exists but failed to run: $VAMPIRE_EXEC --version"
        fi
    fi
    if check_executable_exists "E prover" "$EPROVER_EXEC"; then
        if "$EPROVER_EXEC" --version >/dev/null 2>&1 || "$EPROVER_EXEC" -h >/dev/null 2>&1; then
            record_success "E prover runs successfully."
        else
            record_failure "E prover exists but failed to run."
        fi
    fi
    if check_executable_exists "TPTP4X" "$TPTP4X_EXEC"; then
        if "$TPTP4X_EXEC" -h >/dev/null 2>&1; then
            record_success "TPTP4X runs successfully."
        else
            record_failure "TPTP4X exists but failed to run."
        fi
    fi
    if [ -x "$LEO_EXEC" ]; then
        if "$LEO_EXEC" --help >/dev/null 2>&1 || "$LEO_EXEC" -h >/dev/null 2>&1; then
            record_success "Leo-III runs successfully."
        else
            warn "Leo-III exists but did not respond to --help/-h. It may still be usable."
        fi
    else
        warn "Skipping Leo-III runtime check because Leo-III is not installed."
    fi
}

verify_kb_stress_test() {
    print_header "Knowledge Base stress test"
    check_file_exists "SigmaKEE jar" "$SIGMA_SRC/build/sigmakee.jar" || return 1
    log "Building and testing knowledge base. This may take several minutes."
    if java -Xmx20g -cp "$SIGMA_CP" com.articulate.sigma.KB -t 2>/dev/null | grep -qF "KB.test()"; then
        record_success "Knowledge Base stress test passed."
    else
        record_failure "Knowledge Base stress test failed: java -Xmx20g -cp \"$SIGMA_CP\" com.articulate.sigma.KB -t"
    fi
}

start_tomcat() {
    print_header "Starting Tomcat"
    check_executable_exists "Tomcat startup.sh" "$CATALINA_HOME/bin/startup.sh" || return 1
    local output
    output="$("$CATALINA_HOME/bin/startup.sh" 2>&1 || true)"
    printf '%s\n' "$output"
    if echo "$output" | grep -qF "Tomcat started."; then
        record_success "Tomcat startup command completed."
        return 0
    fi
    if echo "$output" | grep -qiE "already running|Tomcat may already be running"; then
        record_success "Tomcat appears to already be running."
        return 0
    fi
    warn "Tomcat startup output did not contain the expected success message. Continuing to web check."
}

verify_webapp() {
    print_header "Verifying SigmaKEE web application"
    local timeout=90
    local interval=3
    local elapsed=0
    local output=""
    start_tomcat || true
    while true; do
        output="$(curl -s --fail --max-time 5 "$SIGMA_URL" || true)"
        if echo "$output" | grep -qF "<title>Sigma Login</title>"; then
            record_success "SigmaKEE login page is reachable: $SIGMA_URL"
            return 0
        fi
        log "Waiting for SigmaKEE web app to load..."
        sleep "$interval"
        elapsed=$((elapsed + interval))
        if (( elapsed >= timeout )); then
            record_failure "Timeout waiting for SigmaKEE login page: $SIGMA_URL"
            log "Try checking:"
            log "  curl -i $SIGMA_URL"
            log "  tail -n 100 $CATALINA_HOME/logs/catalina.out"
            log "  ls -lah $CATALINA_HOME/logs"
            return 1
        fi
    done
}

print_summary() {
    print_header "Verification summary"
    if [ "${#VERIFY_FAILURES[@]}" -eq 0 ]; then
        log "Finished verification. Checks indicate a successful installation."
        return 0
    fi
    log "Verification failed with ${#VERIFY_FAILURES[@]} issue(s):"
    local failure
    for failure in "${VERIFY_FAILURES[@]}"; do
        log "  - $failure"
    done
    return 1
}

verify_all() {
    verify_environment || true
    verify_workspace || true
    verify_programs || true
    verify_sigmakee_data || true
    verify_config_xml || true
    verify_build_outputs || true
    verify_provers || true
    verify_kb_stress_test || true
    verify_webapp || true
    print_summary
}

main() {
    verify_all
}

main "$@"