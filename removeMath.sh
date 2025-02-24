#!/bin/bash -e

# Prepending the string expression with an empty string: '' lets it work on
# macOS as well as other *nix O/S's
sed -i '' 's/.*$to_real.*//g' $1
sed -i '' 's/.*$remainder_t.*//g' $1
sed -i '' 's/.*$quotient_e.*//g' $1
