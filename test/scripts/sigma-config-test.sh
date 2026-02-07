#!/bin/bash

# sigma-config-test.sh - Unit tests for sigma-config.sh
#
# Run with: ./test/scripts/sigma-config-test.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SIGMA_CONFIG="$REPO_ROOT/sigma-config.sh"

# Create temp directory for testing
TEST_DIR=$(mktemp -d)
export SIGMA_HOME="$TEST_DIR"
mkdir -p "$TEST_DIR/KBs"

# Track test results
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

cleanup() {
    rm -rf "$TEST_DIR"
}
trap cleanup EXIT

pass() {
    ((TESTS_PASSED++))
    echo -e "${GREEN}PASS${NC}: $1"
}

fail() {
    ((TESTS_FAILED++))
    echo -e "${RED}FAIL${NC}: $1"
}

run_test() {
    ((TESTS_RUN++))
    local test_name="$1"
    shift
    if "$@"; then
        pass "$test_name"
    else
        fail "$test_name"
    fi
}

# Create a minimal config.xml for testing
create_test_config() {
    cat > "$TEST_DIR/KBs/config.xml" << 'EOF'
<configuration>
  <preference name="baseDir" value="/test/.sigmakee" />
  <kb name="SUMO" >
    <constituent filename="Merge.kif" />
    <constituent filename="Mid-level-ontology.kif" />
  </kb>
</configuration>
EOF
}

# ============================================================
# Test Cases
# ============================================================

test_script_exists() {
    [[ -x "$SIGMA_CONFIG" ]]
}

test_help_works() {
    "$SIGMA_CONFIG" --help >/dev/null 2>&1
}

test_status_without_config() {
    rm -f "$TEST_DIR/KBs/config.xml"
    # Should not crash, but may show error
    "$SIGMA_CONFIG" status 2>&1 | grep -q "mode\|Error\|not found"
}

test_init_creates_configs() {
    create_test_config
    "$SIGMA_CONFIG" init >/dev/null 2>&1
    [[ -f "$TEST_DIR/KBs/config-full.xml" ]] && [[ -f "$TEST_DIR/KBs/config-fast.xml" ]]
}

test_detect_full_mode() {
    create_test_config
    local output=$("$SIGMA_CONFIG" status 2>&1)
    echo "$output" | grep -q "full"
}

test_switch_to_fast() {
    create_test_config
    "$SIGMA_CONFIG" init >/dev/null 2>&1
    "$SIGMA_CONFIG" fast >/dev/null 2>&1
    # Fast config should NOT have Mid-level-ontology.kif
    ! grep -q "Mid-level-ontology.kif" "$TEST_DIR/KBs/config.xml"
}

test_switch_to_full() {
    create_test_config
    "$SIGMA_CONFIG" init >/dev/null 2>&1
    "$SIGMA_CONFIG" fast >/dev/null 2>&1
    "$SIGMA_CONFIG" full >/dev/null 2>&1
    # Full config should have Mid-level-ontology.kif
    grep -q "Mid-level-ontology.kif" "$TEST_DIR/KBs/config.xml"
}

test_fast_has_merge_kif() {
    create_test_config
    "$SIGMA_CONFIG" init >/dev/null 2>&1
    "$SIGMA_CONFIG" fast >/dev/null 2>&1
    # Fast config should still have Merge.kif
    grep -q "Merge.kif" "$TEST_DIR/KBs/config.xml"
}

test_invalid_command() {
    # Should exit with error for invalid command
    ! "$SIGMA_CONFIG" invalid_command_xyz >/dev/null 2>&1
}

# ============================================================
# Run Tests
# ============================================================

echo "Running sigma-config.sh unit tests..."
echo "Test directory: $TEST_DIR"
echo ""

run_test "Script exists and is executable" test_script_exists
run_test "Help command works" test_help_works
run_test "Status handles missing config" test_status_without_config
run_test "Init creates config files" test_init_creates_configs
run_test "Detects full mode correctly" test_detect_full_mode
run_test "Switch to fast mode" test_switch_to_fast
run_test "Switch to full mode" test_switch_to_full
run_test "Fast mode still has Merge.kif" test_fast_has_merge_kif
run_test "Invalid command returns error" test_invalid_command

echo ""
echo "============================================================"
echo "Results: $TESTS_PASSED/$TESTS_RUN passed"
if [[ $TESTS_FAILED -gt 0 ]]; then
    echo -e "${RED}$TESTS_FAILED tests failed${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
fi
