#!/bin/bash

# sigma-config.sh - Switch between full and fast SigmaKEE KB configurations
#
# Problem: Loading the full SUMO ontology (all .kif files) provides complete
# term coverage for browsing and coding in jEdit/VSCode, but makes Vampire
# inference queries very slow or timeout. Loading only Merge.kif makes
# inference fast but limits available terms.
#
# Solution: This script allows quick switching between two config modes:
#   - full: All KB files loaded (for jEdit browsing, coding, term lookup)
#   - fast: Minimal KB files (for quick Vampire/E-Prover inference testing)
#
# Usage:
#   sigma-config.sh full    # Switch to full KB (coding mode)
#   sigma-config.sh fast    # Switch to fast KB (inference mode)
#   sigma-config.sh status  # Show current mode
#   sigma-config.sh init    # Create initial config files if missing
#
# After switching, restart jEdit or Sigma to reload the KB.

set -e

# Determine SIGMA_HOME
if [[ -z "$SIGMA_HOME" ]]; then
    SIGMA_HOME="$HOME/.sigmakee"
fi

CONFIG_DIR="$SIGMA_HOME/KBs"
CURRENT="$CONFIG_DIR/config.xml"
FULL="$CONFIG_DIR/config-full.xml"
FAST="$CONFIG_DIR/config-fast.xml"

# Colors for output (if terminal supports it)
if [[ -t 1 ]]; then
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    NC='\033[0m' # No Color
else
    GREEN=''
    YELLOW=''
    BLUE=''
    NC=''
fi

usage() {
    cat << EOF
sigma-config.sh - Switch between full and fast SigmaKEE KB configurations

Usage: $0 [command]

Commands:
  full    Switch to full KB mode (all ontology files for coding/browsing)
  fast    Switch to fast KB mode (minimal files for quick inference)
  status  Show current configuration mode (default if no command given)
  init    Initialize config files (creates full and fast variants)

After switching modes, restart jEdit or Sigma to reload the KB.

Examples:
  $0 full     # Load all terms for jEdit coding
  $0 fast     # Quick Vampire testing with minimal KB
  $0          # Check current mode

EOF
}

detect_mode() {
    if grep -q "Mid-level-ontology.kif" "$CURRENT" 2>/dev/null; then
        echo "full"
    else
        echo "fast"
    fi
}

init_configs() {
    if [[ ! -f "$CURRENT" ]]; then
        echo -e "${YELLOW}Error: No config.xml found at $CURRENT${NC}"
        echo "Please run 'ant install' first to set up SigmaKEE."
        exit 1
    fi

    # Create full config if missing
    if [[ ! -f "$FULL" ]]; then
        echo "Creating config-full.xml..."
        cp "$CURRENT" "$FULL"
    fi

    # Create fast config if missing
    if [[ ! -f "$FAST" ]]; then
        echo "Creating config-fast.xml..."
        # Start with current config and reduce to minimal KB
        cp "$CURRENT" "$FAST"

        # Replace the kb block with minimal constituents
        # This is a simple approach - keeps Merge.kif only
        sed -i '/<kb name="SUMO"/,/<\/kb>/c\
  <kb name="SUMO" >\
    <constituent filename="Merge.kif" />\
  </kb>' "$FAST"
    fi

    echo -e "${GREEN}Config files initialized.${NC}"
    echo "  Full config: $FULL"
    echo "  Fast config: $FAST"
}

switch_full() {
    if [[ ! -f "$FULL" ]]; then
        echo -e "${YELLOW}Full config not found. Running init...${NC}"
        init_configs
    fi

    cp "$FULL" "$CURRENT"
    echo -e "${GREEN}Switched to FULL config mode${NC}"
    echo ""
    echo "  Mode: Full KB (coding/browsing)"
    echo "  - All ontology files loaded"
    echo "  - Complete term coverage for jEdit/VSCode"
    echo "  - Inference queries may be slow"
    echo ""
    echo -e "${BLUE}Restart jEdit or Sigma to reload the KB.${NC}"
}

switch_fast() {
    if [[ ! -f "$FAST" ]]; then
        echo -e "${YELLOW}Fast config not found. Running init...${NC}"
        init_configs
    fi

    cp "$FAST" "$CURRENT"
    echo -e "${GREEN}Switched to FAST config mode${NC}"
    echo ""
    echo "  Mode: Minimal KB (inference testing)"
    echo "  - Only Merge.kif loaded"
    echo "  - Fast Vampire/E-Prover queries"
    echo "  - Limited terms available"
    echo ""
    echo -e "${BLUE}Restart jEdit or Sigma to reload the KB.${NC}"
}

show_status() {
    local mode=$(detect_mode)
    echo -e "Current mode: ${GREEN}$mode${NC}"
    echo ""
    echo "Config files:"
    echo "  Current: $CURRENT"
    [[ -f "$FULL" ]] && echo "  Full:    $FULL" || echo "  Full:    (not created)"
    [[ -f "$FAST" ]] && echo "  Fast:    $FAST" || echo "  Fast:    (not created)"
    echo ""
    echo "Use '$0 full' or '$0 fast' to switch modes."
}

# Main
case "${1:-status}" in
    full)
        switch_full
        ;;
    fast)
        switch_fast
        ;;
    status)
        show_status
        ;;
    init)
        init_configs
        ;;
    -h|--help|help)
        usage
        ;;
    *)
        echo "Unknown command: $1"
        echo ""
        usage
        exit 1
        ;;
esac
