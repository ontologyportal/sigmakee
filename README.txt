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