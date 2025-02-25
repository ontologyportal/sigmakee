#!/bin/bash -e

UNAME=$( command -v uname)

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
    printf 'unknown o/s\n'
    ;;
esac

nix() {
    sed -i 's/.*$to_real.*//g' $1
    sed -i 's/.*$remainder_t.*//g' $1
    sed -i 's/.*$quotient_e.*//g' $1
}

mac() {
    # Prepending the string expression with an empty string: '' lets it work on
    # macOS by mitigating unknown label errors
    sed -i '' 's/.*$to_real.*//g' $1
    sed -i '' 's/.*$remainder_t.*//g' $1
    sed -i '' 's/.*$quotient_e.*//g' $1
}
