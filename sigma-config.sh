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
#   sigma-config.sh full            # Switch to full KB (coding mode)
#   sigma-config.sh fast            # Switch to fast KB (inference mode)
#   sigma-config.sh add [file.kif]  # Add a .kif file to the current KB config
#   sigma-config.sh remove [name]   # Remove a .kif file from the current KB config
#   sigma-config.sh status          # Show current mode and loaded files
#   sigma-config.sh init            # Create initial config files if missing
#
# After switching or adding files, restart jEdit or Sigma to reload the KB.

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

Usage: $0 [command] [args]

Commands:
  full              Switch to full KB mode (all ontology files for coding/browsing)
  fast              Switch to fast KB mode (minimal files for quick inference)
  add [file.kif]    Add a dev .kif file to the KB (symlinks + adds to config)
  remove [name]     Remove a .kif file from the KB config (and its symlink)
  list              Show all constituent .kif files in the current config
  status            Show current configuration mode (default if no command given)
  init              Initialize config files (creates full and fast variants)

After switching modes or adding/removing files, restart jEdit or Sigma to reload the KB.

Examples:
  $0 full                                         # Load all terms for jEdit coding
  $0 fast                                         # Quick Vampire testing with minimal KB
  $0 add ~/workspace/sumo/development/Cyber.kif   # Add your dev file to the KB
  $0 add                                          # Interactive: prompts for file path
  $0 remove Cyber.kif                             # Remove a file from the KB
  $0 list                                         # Show what's currently loaded
  $0                                              # Check current mode

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

add_file() {
    local filepath="$1"

    # Interactive prompt if no file path provided
    if [[ -z "$filepath" ]]; then
        echo -e "${BLUE}Add a .kif file to the Sigma KB${NC}"
        echo ""
        echo "Enter the full path to the .kif file you are working on:"
        echo "  (e.g., ~/workspace/sumo/development/Cyber.kif)"
        echo ""
        read -rp "> " filepath
        echo ""
    fi

    # Expand ~ and resolve to absolute path
    filepath="${filepath/#\~/$HOME}"
    filepath="$(realpath "$filepath" 2>/dev/null || echo "$filepath")"

    # Validate the file exists and is a .kif file
    if [[ ! -f "$filepath" ]]; then
        echo -e "${YELLOW}Error: File not found: $filepath${NC}"
        exit 1
    fi

    if [[ "$filepath" != *.kif ]]; then
        echo -e "${YELLOW}Error: File must be a .kif file: $filepath${NC}"
        exit 1
    fi

    local basename
    basename="$(basename "$filepath")"

    # Check if already in config
    if grep -q "\"$basename\"" "$CURRENT" 2>/dev/null; then
        echo -e "${YELLOW}$basename is already in the current config.${NC}"

        # Check if symlink exists
        if [[ -L "$CONFIG_DIR/$basename" ]]; then
            echo "  Symlink: $CONFIG_DIR/$basename -> $(readlink "$CONFIG_DIR/$basename")"
        elif [[ -f "$CONFIG_DIR/$basename" ]]; then
            echo "  File exists at: $CONFIG_DIR/$basename (not a symlink)"
        fi
        return 0
    fi

    # Create symlink into KBs directory (if file isn't already there)
    if [[ "$(dirname "$filepath")" != "$CONFIG_DIR" ]]; then
        if [[ -e "$CONFIG_DIR/$basename" ]]; then
            echo -e "${YELLOW}Warning: $CONFIG_DIR/$basename already exists.${NC}"
            read -rp "Overwrite with symlink to $filepath? [y/N] " confirm
            if [[ "$confirm" != [yY] ]]; then
                echo "Aborted."
                exit 1
            fi
            rm "$CONFIG_DIR/$basename"
        fi
        ln -s "$filepath" "$CONFIG_DIR/$basename"
        echo -e "  Symlink: ${GREEN}$CONFIG_DIR/$basename -> $filepath${NC}"
    fi

    # Add constituent line to config.xml (before </kb>)
    sed -i "/<\/kb>/i\\    <constituent filename=\"$basename\" />" "$CURRENT"

    # Also add to full and fast configs if they exist
    if [[ -f "$FULL" ]] && ! grep -q "\"$basename\"" "$FULL"; then
        sed -i "/<\/kb>/i\\    <constituent filename=\"$basename\" />" "$FULL"
        echo "  Added to: config-full.xml"
    fi
    if [[ -f "$FAST" ]] && ! grep -q "\"$basename\"" "$FAST"; then
        sed -i "/<\/kb>/i\\    <constituent filename=\"$basename\" />" "$FAST"
        echo "  Added to: config-fast.xml"
    fi

    echo ""
    echo -e "${GREEN}Added $basename to the KB configuration.${NC}"
    echo -e "${BLUE}Restart jEdit or Sigma to reload the KB.${NC}"
}

remove_file() {
    local name="$1"

    if [[ -z "$name" ]]; then
        echo "Usage: $0 remove <filename.kif>"
        echo ""
        echo "Currently loaded files:"
        list_files
        exit 1
    fi

    # Normalize: just the basename
    name="$(basename "$name")"

    # Protect core files from removal
    case "$name" in
        Merge.kif|english_format.kif|domainEnglishFormat.kif)
            echo -e "${YELLOW}Error: Cannot remove core file $name${NC}"
            exit 1
            ;;
    esac

    if ! grep -q "\"$name\"" "$CURRENT" 2>/dev/null; then
        echo -e "${YELLOW}$name is not in the current config.${NC}"
        exit 1
    fi

    # Remove from all config files
    sed -i "/\"$name\"/d" "$CURRENT"
    [[ -f "$FULL" ]] && sed -i "/\"$name\"/d" "$FULL"
    [[ -f "$FAST" ]] && sed -i "/\"$name\"/d" "$FAST"

    # Remove symlink if it is one (don't delete actual files)
    if [[ -L "$CONFIG_DIR/$name" ]]; then
        rm "$CONFIG_DIR/$name"
        echo "  Removed symlink: $CONFIG_DIR/$name"
    fi

    echo -e "${GREEN}Removed $name from the KB configuration.${NC}"
    echo -e "${BLUE}Restart jEdit or Sigma to reload the KB.${NC}"
}

list_files() {
    echo "Constituent files in current config:"
    grep 'constituent filename' "$CURRENT" | sed 's/.*filename="\([^"]*\)".*/  \1/' | while read -r f; do
        if [[ -L "$CONFIG_DIR/$f" ]]; then
            echo "$f -> $(readlink "$CONFIG_DIR/$f")"
        else
            echo "$f"
        fi
    done
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
    add)
        add_file "$2"
        ;;
    remove|rm)
        remove_file "$2"
        ;;
    list|ls)
        list_files
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
