#!/usr/bin/env bash

sudo apt-get update
sudo apt-get -y upgrade
sudo apt-get -y install cvs

cd ~/
#cvs -d :ext:kyjohnsonipsoft@sigmakee.cvs.sourceforge.net:/cvsroot/sigmakee checkout sigma
cvs -d :ext:anonymous@sigmakee.cvs.sourceforge.net:/cvsroot/sigmakee checkout sigma

cd /home/vagrant/sigma
sudo apt-get -y install ant openjdk-7-jdk
mkdir ~/KBs
ant createDEB

sudo dpkg -i native-installers/debian/temp/repository/sigma_1.0-1_amd64.deb
sudo apt-get -y install tomcat7
sudo apt-get -f install
sigma start
sleep 5
response=$(curl --write-out %{http_code} --silent --output /dev/null localhost:8080)
[ $response == 200 ]; echo $?
