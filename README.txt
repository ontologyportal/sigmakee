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


You can also follow the steps below to do a manual installation on linux.
Just replace /home/theuser with your directory names.  This procedure
assumes that you start from your home directory and are happy
with having directories created there.

mkdir workspace
mkdir Programs
cd Programs
wget 'http://nlp.stanford.edu/software/stanford-corenlp-full-2015-12-09.zip'
wget 'http://www.gtlib.gatech.edu/pub/apache/tomcat/tomcat-7/v7.0.68/bin/apache-tomcat-7.0.68.zip'
wget 'http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz'
wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_1.9/E.tgz'
tar -xvzf E.tgz
unzip apache-tomcat-7.0.68.zip
rm apache-tomcat-7.0.68.zip
unzip stanford-corenlp-full-2015-12-09.zip
rm stanford-corenlp-full-2015-12-09.zip
cd ~/Programs/stanford-corenlp-full-2015-12-09/
unzip stanford-corenlp-3.6.0-models.jar
cd ~/Programs
gunzip WordNet-3.0.tar.gz
tar -xvf WordNet-3.0.tar
cp WordNet-3.0/dict/* ~/.sigmakee/KBs/WordNetMappings/
cd ~/workspace/
sudo apt-get install git
git clone https://github.com/ontologyportal/sigmakee
git clone https://github.com/ontologyportal/sumo
mkdir .sigmakee
cd .sigmakee
mkdir KBs
cp -R ~/workspace/sumo/* KBs
cd ~/Programs/E
sudo apt-get install make
sudo apt-get install gcc
./configure
make
cd ~
echo "export SIGMA_HOME=/home/theuser/.sigmakee" >> .bashrc
echo "export CATALINA_OPTS=\"$CATALINA_OPTS -Xms500M -Xmx2500M\"" >> .bashrc
echo "export CATALINA_HOME=/var/tomcat/apache-tomcat-8.0.26" >> .bashrc
cd ~/workspace/sigmakee
ant
cd ~/Programs/stanford-corenlp-full-2015-12-09/
java  -Xmx2500m -classpath /home/theuser/workspace/sigmakee/build/classes:/home/theuser/workspace/sigmakee/build/lib/*  com.articulate.sigma.KB
