#!/bin/bash

#install packages from apt
apt update
apt upgrade -y -qq
apt install -y -qq tomcat8 ant git make gcc graphviz openjdk-8-jdk-headless glances
apt clean -y -qq
apt autoclean -y -qq

# set environment variables
echo "export SIGMADIR=/sigma" >> ~/.bashrc && source ~/.bashrc
echo "export WORKSPACE=$SIGMADIR/workspace" >> ~/.bashrc && source ~/.bashrc
echo "export PROGRAMS=$SIGMADIR/Programs" >> ~/.bashrc && source ~/.bashrc
echo "export SIGMA_HOME=$SIGMADIR/sigmakee" >> ~/.bashrc && source ~/.bashrc
echo "export KBDIR=$SIGMA_HOME/KBs" >> ~/.bashrc && source ~/.bashrc
echo "export SUMO_SRC=$WORKSPACE/sumo" >> ~/.bashrc && source ~/.bashrc
echo "export SIGMA_SRC=$WORKSPACE/sigmakee" >> ~/.bashrc && source ~/.bashrc
echo "export ONTOLOGYPORTAL_GIT=$WORKSPACE" >> ~/.bashrc && source ~/.bashrc

echo "export PATH=$PATH:$JAVA_HOME/bin" >> ~/.bashrc && source ~/.bashrc

# when using debian tomcat installation from apt the *.war file should be placed
# in a different location. Instead of changing the ant build script, we can
# change the CATALINA_HOME var for the user

#echo "export CATALINA_HOME=/usr/share/tomcat8" >> ~/.bashrc && source ~/.bashrc
echo "export CATALINA_HOME=/var/lib/tomcat8" >> ~/.bashrc && source ~/.bashrc

echo "export CATALINA_OPTS=\"-Xmx10g\"" >> ~/.bashrc

source ~/.bashrc

mkdir -p $WORKSPACE $PROGRAMS $KBDIR

# install E prover
cd $PROGRAMS
wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.0/E.tgz'
tar xvf E.tgz
rm E.tgz
cd $PROGRAMS/E
./configure && make && make install

# clone and build SigmaKEE
cd $WORKSPACE
git clone https://github.com/ontologyportal/sigmakee
cd $SIGMA_SRC
sed -i "s/theuser/$ME/g" config.xml
cp config.xml $KBDIR
ant

# clone SUMO
cd $WORKSPACE
git clone -v https://github.com/ontologyportal/sumo $SUMO_SRC
cp -r $SUMO_SRC/* $KBDIR

# download WordNet
cd $WORKSPACE
wget -q 'https://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz'
tar xf WordNet-3.0.tar.gz
mv WordNet-3.0/dict/* $KBDIR/WordNetMappings
rm -r WordNet-3.0*

# changes in the config.xml file to make it work
cd $KBDIR
sed -i "s/.sigmakee/sigmakee/g" config.xml
sed -i 's/~/\'"$SIGMADIR"'/g' config.xml
#rm -r $WORKSPACE
chmod -R 777 $SIGMADIR
ln -s $SIGMA_HOME $CATALINA_HOME/null
