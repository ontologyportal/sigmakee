#!/bin/bash -e

UNAME=$(command -v uname)
file=$1

function nix {
    sed -i 's/.*$to_real.*//g' $file
    sed -i 's/.*$remainder_t.*//g' $file
    sed -i 's/.*$quotient_e.*//g' $file
}

# Prepending the string expression with an empty string: '' lets it work on
# macOS by mitigating unknown label errors
function mac {
    sed -i '' 's/.*$to_real.*//g' $file
    sed -i '' 's/.*$remainder_t.*//g' $file
    sed -i '' 's/.*$quotient_e.*//g' $file
}

case $( "${UNAME}" | tr '[:upper:]' '[:lower:]') in
  linux*)
    nix
    ;;
  darwin*)
    mac
    ;;
  msys*|cygwin*|mingw*)
    # or possible 'bash on windows'
    nix
    ;;
  nt|win*)
    nix
    ;;
  *)
    printf 'unsupported o/s\n'
    ;;
esac
