Introduction
============

Please read these notes thoroughly.  Most installation issues result from not
carefully following the instructions.

You can follow the steps below to do a manual installation on linux or Mac. This
procedure assumes that you start from your home directory and are happy with
having directories created there. The sed command below attempts to modify
~/.sigmakee/KBs/config.xml to conform to your local paths.  If your paths
differ, then you may need to edit your config.xml manually. If you are running
tomcat on vagrant or another VM, you may need to change the port value from 8080.
If you are running on a server, rather than your localhost you'll need to set
the hostname parameter in your config.xml file. E will only work if your $TMPDIR
is set correctly.  No particular version of tomcat is required. If you load a
different version of tomcat, be sure to change $CATALINA_HOME and your paths to
conform to the version. If you use a different mirror or version you'll need to
change the wget commend below and Oracle Java appears to now have a key embedded
in their URL that will change every time. Change "theuser" below to your user name.


System preparation on Linux
==========================

create user theuser
  useradd theuser

add password for theuser
  passwd theuser

add to sudoers file
  usermod -aG sudo theuser

install unzip
  sudo apt-get install unzip
  sudo apt-get update

may need to create a .bashrc
  touch .bashrc

handy to add stuff to .bashrc
  echo "alias dir='ls --color=auto --format=vertical -la'" >> .bashrc
  echo "export HISTSIZE=10000 HISTFILESIZE=100000" >> .bashrc
  echo "export JAVA_HOME=/home/theuser/Programs/jdk1.8.0_112" >> .bashrc
  echo "export PATH=$PATH:$JAVA_HOME/bin" >> .bashrc

You may need to download Java and set your
JAVA_HOME http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html .
The following command line version may work
  wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie"
    http://download.oracle.com/otn-pub/java/jdk/8u112-b15/jdk-8u112-linux-x64.tar.gz

but you also may need to go to the web site, accept the license, then copy the download link
into this command

Linux Installation
==================

mkdir workspace
mkdir Programs
cd Programs
wget 'http://ftp.wayne.edu/apache/tomcat/tomcat-8/v8.5.23/bin/apache-tomcat-8.5.23.zip'
wget 'http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz'
wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.0/E.tgz'
tar -xvzf E.tgz
unzip apache-tomcat-8.5.23.zip
rm apache-tomcat-8.5.23.zip
cd ~/Programs/apache-tomcat-8.5.23/bin
chmod 777 *
cd ~/workspace/
sudo apt-get install git
git clone https://github.com/ontologyportal/sigmakee
git clone https://github.com/ontologyportal/sumo
cd ~
mkdir .sigmakee
cd .sigmakee
mkdir KBs
cp -R ~/workspace/sumo/* KBs
me="$(whoami)"
cp ~/workspace/sigmakee/config.xml ~/.sigmakee/KBs
sed -i "s/theuser/$me/g" KBs/config.xml
cd ~/Programs
gunzip WordNet-3.0.tar.gz
tar -xvf WordNet-3.0.tar
cp WordNet-3.0/dict/* ~/.sigmakee/KBs/WordNetMappings/
cd ~/Programs/E
sudo apt-get install make
sudo apt-get install gcc
./configure
make
make install
cd ~
sudo apt-get install graphviz
echo "export SIGMA_HOME=~/.sigmakee" >> .bashrc
echo "export ONTOLOGYPORTAL_GIT=~/workspace" >> .bashrc
echo "export CATALINA_OPTS=\"$CATALINA_OPTS -Xms500M -Xmx2500M\"" >> .bashrc
echo "export CATALINA_HOME=~/Programs/apache-tomcat-8.5.23" >> .bashrc
source .bashrc
cd ~/workspace/sigmakee
sudo add-apt-repository universe
sudo apt-get update
sudo apt-get install ant
ant

To test run

  java  -Xmx2500m -classpath ~/workspace/sigmakee/build/classes:/home/theuser/workspace/sigmakee/build/lib/*
    com.articulate.sigma.KB


Start Tomcat with
  $CATALINA_HOME/bin/startup.sh

Point your browser at http://localhost:8080/sigma/login.html


Debugging

- If login.html redirects you to init.jsp that means the system is still initializing. Wait a minute or two and try
again.
- If you are repeatedly getting 404s, check the port value in ~/.sigmakee/KBs/config.xml. 8080 for local,
9090 for Vagrant
- If you are on mac and getting errors related to not finding jars when running com.articulate.sigma.KB, copy all jars
from /home/theuser/workspace/sigmakee/build/lib/ to /Library/Java/Extensions

Apple install notes
===================

use "curl -o filename URL" if you don't have wget installed
java will be installed in /usr/libexec/java_home
get git from the xcode tools - "xcode-select --install"
instead of .bashrc edit .bash_profile

install Homebrew from http://brew.sh

mkdir workspace
mkdir Programs
cd Programs
curl -O 'http://www-us.apache.org/dist/tomcat/tomcat-8/v8.5.23/bin/apache-tomcat-8.5.23.zip'
curl -O 'http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz'
curl -O 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.0/E.tgz'
tar -xvzf E.tgz
unzip apache-tomcat-8.5.23.zip
rm apache-tomcat-8.5.23.zip
cd ~/Programs/apache-tomcat-8.5.23/bin
chmod 777 *
cd ~/workspace/
sudo apt-get install git
git clone https://github.com/ontologyportal/sigmakee
git clone https://github.com/ontologyportal/sumo
cd ~
mkdir .sigmakee
cd .sigmakee
mkdir KBs
cp -R ~/workspace/sumo/* KBs
me="$(whoami)"
cp ~/workspace/sigmakee/config.xml ~/.sigmakee/KBs
sed -i "s/theuser/$me/g" config.xml
cd ~/Programs
gunzip WordNet-3.0.tar.gz
tar -xvf WordNet-3.0.tar
cp WordNet-3.0/dict/* ~/.sigmakee/KBs/WordNetMappings/
cd ~/Programs/E
brew install make
brew install gcc
./configure
make
make install
cd ~
brew install graphviz
echo "export SIGMA_HOME=~/.sigmakee" >> .bashrc
echo "export ONTOLOGYPORTAL_GIT=~/workspace" >> .bashrc
echo "export CATALINA_OPTS=\"$CATALINA_OPTS -Xms500M -Xmx2500M\"" >> .bashrc
echo "export CATALINA_HOME=~/Programs/apache-tomcat-8.5.23" >> .bashrc
source .bashrc
cd ~/workspace/sigmakee
brew install ant
ant

To test run

  java  -Xmx2500m -classpath ~/workspace/sigmakee/build/classes:~/workspace/sigmakee/build/lib/*
    com.articulate.sigma.KB


Start Tomcat with
  $CATALINA_HOME/bin/startup.sh

Point your browser at http://localhost:8080/sigma/login.html


Debugging
- If login.html redirects you to init.jsp that means the system is still initializing. Wait a minute or two and try
again.
- If you are repeatedly getting 404s, check the port value in ~/.sigmakee/KBs/config.xml. 8080 for local,
9090 for Vagrant
- If you are on mac and getting errors related to not finding jars when running com.articulate.sigma.KB, copy all jars
from ~/workspace/sigmakee/build/lib/ to /Library/Java/Extensions


jUnit testing on the command line
=================================

java  -Xmx2500m -classpath
  ~/workspace/sigmakee/build/classes:~/workspace/sigmakee/build/lib/*
  org.junit.runner.JUnitCore com.articulate.sigma.UnitTestSuite

python Interface
================

Compile SigmaKEE then run with

user@user-machine:~/workspace/sigmakee$ python
Python 2.7.12 (default, Nov 19 2016, 06:48:10)
[GCC 5.4.0 20160609] on linux2
Type "help", "copyright", "credits" or "license" for more information.
>>> from py4j.java_gateway import JavaGateway
>>> gateway = JavaGateway()
>>> sigma_app = gateway.entry_point
>>> print(sigma_app.getTerms())

set([u'-1', u'-3', u'-6', u'-7235', u'.5', u'<=>', u'=>', u'AAA-Rating', u'AAM', u'AAV', u'ABPFn', u'ABTest', u'ACPowerSource', ...

Look at com.articulate.sigma.KBmanager.pythonServer() to expose the API of more classes
than just com.articulate.sigma.KB


Account Management
======================

Create the account database with

java -Xmx5G -cp $SIGMA_SRC/build/classes:$SIGMA_SRC/build/lib/* com.articulate.delphi.PasswordService -c

Then create the administrator account and password

java -Xmx5G -cp $SIGMA_SRC/build/classes:$SIGMA_SRC/build/lib/* com.articulate.delphi.PasswordService -c

You can use Sigma without being administrator, but you'll have limited use of its functionality.


Old Installation Notes
======================

Install via script from source on Linux or Mac OS with
bash <(curl -L https://raw.githubusercontent.com/ontologyportal/sigmakee/master/install.sh)

Note that you need to enter the entire statement above, including calling "bash".

Users should also see

https://sourceforge.net/p/sigmakee/wiki/required_data_files/
Mac instructions - https://sourceforge.net/p/sigmakee/wiki/Sigma%20Setup%20on%20Mac/ 
Ubuntu - https://sourceforge.net/p/sigmakee/wiki/Setting%20up%20Sigma%20on%20Ubuntu/

You can also install Sigma on a Vagrant virtual machine.  You'll need VirtualBox too
https://www.virtualbox.org/

> mkdir sigma_vagrant
> cd sigma_vagrant
> wget https://raw.githubusercontent.com/ontologyportal/sigmakee/master/Vagrantfile
> vagrant up
> vagrant ssh
> bash <(curl -L https://raw.githubusercontent.com/ontologyportal/sigmakee/master/install-vagrant.sh)

follow the prompts and Sigma will be running.  Then on the browser of your host machine, go to
http://localhost:9090/sigma/login.html

To run natural language interpretation from the command line in the virtual machine,
run the following additional steps

> cd ~
> mkdir Programs
> cd Programs
> cp /vagrant/Downloads/stanford-corenlp-full-2015-12-09.zip .
> unzip stanford-corenlp-full-2015-12-09.zip
> cd stanford-corenlp-full-2015-12-09
> export SIGMA_HOME="/home/vagrant/.sigmakee"
> jar -xf stanford-corenlp-3.6.0-models.jar
> cd ~/Programs/stanford-corenlp-full-2015-12-09
> java  -Xmx2500m -classpath  /home/vagrant/workspace/sigma/sigma/build/classes:/home/vagrant/workspace/sigma/sigma/build/lib/*  com.articulate.sigma.semRewrite.Interpreter -i


