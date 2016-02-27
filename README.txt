Install via script from source on Linux or Mac OS with 
bash <(curl -L https://raw.githubusercontent.com/ontologyportal/sigmakee/master/install.sh)

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
> bash <(curl -L https://raw.githubusercontent.com/ontologyportal/sigmakee/master/install.sh)

follow the prompts and Sigma will be running.  Then on the browser of your host machine, go to
http://localhost:9090/sigma/login.html

To run natural language interpretation from the command line in the virtual machine,
run the following additional steps

> cd ~
> mkdir Programs
> cd Programs
> cp /vagrant/Downloads/stanford-corenlp-full-2015-01-29.zip .
> unzip stanford-corenlp-full-2015-01-29.zip
> cd stanford-corenlp-full-2015-01-30
> export SIGMA_HOME="/home/vagrant/.sigmakee"
> jar -xf stanford-corenlp-3.5.1-models.jar
> cd ~/Programs/stanford-corenlp-full-2015-01-30
> java  -Xmx1500m -classpath  /home/vagrant/workspace/sigma/sigma/build/classes:/home/vagrant/workspace/sigma/sigma/build/lib/*  com.articulate.sigma.semRewrite.Interpreter -i