#!/usr/bin/env bash

sudo apt-get update
sudo apt-get -y upgrade
sudo apt-get -y install cvs

sudo apt-get -y install cvs ant openjdk-7-jdk tomcat7

# this should have been made already
if [ ! -d /vagrant/sigma ]
then
  mkdir /vagrant/sigma
fi

cd /vagrant/sigma

# KBs dir req'd for Sigma build
if [ ! -d /vagrant/KBs ]
then
  mkdir /vagrant/KBs
fi

ant createDEB

sudo dpkg -i native-installers/debian/temp/repository/sigma_1.0-1_amd64.deb
#sudo apt-get -f install

#sigma start
#sleep 5
#response=$(curl --write-out %{http_code} --silent --output /dev/null localhost:8080)
#[ $response == 200 ]; echo $?


# test if Sigma is running on guest
response=$(curl --write-out %{http_code} --silent --output /dev/null localhost:8080)
if [ $response == 200 ]
then
  echo Sigma build and installation successful! To use Sigma, navigate to: http://localhost:8888/sigma/login.html
else
  echo Sigma startup failed
fi


