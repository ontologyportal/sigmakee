#!/bin/bash -e

sed -i 's/.*$to_real.*//g' $1
sed -i 's/.*$remainder_t.*//g' $1
sed -i 's/.*$quotient_e.*//g' $1
