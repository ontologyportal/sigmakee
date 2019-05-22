#!/bin/bash

echo "Building SigmaKEE..."
docker build -t sigma:kee .

echo "Coping config.xml..."
docker run --rm -itd --name sigmakee sigma:kee
docker cp sigmakee:/sigma/sigmakee/KBs/config.xml .
docker stop sigmakee

echo "Cloning SUMO..."
git clone https://github.com/ontologyportal/sumo SUMO

echo "Downloading WordNet..."
wget -q 'http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz'
tar xf WordNet-3.0.tar.gz
mkdir WordNetMappings
mv WordNet-3.0/dict/* WordNetMappings
sudo rm -r WordNet-3.0*
