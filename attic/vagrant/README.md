This folder contains various Vagrant configs.

- vagrant.build
Was originally in /build/vagrant
- vagrant.root
Vagrant files in root folder
- vagrant.root.folder
Vagrant files in /vagrant

Below are original Vagrant instructions from root README

## Vagrant Virtual Machine installation

If you want an additional level of system independence and security, you can also
run the docker image in a virtual machine.  For Vagrant you would do the following -

Install it if you haven't already -

```sh
sudo apt-get install vagrant
```
then get a virtual machine image -

```sh
vagrant box add ubuntu/xenial64
vagrant init ubuntu/xenial64
```

You'll need to add two commands to the Vagrantfile configuration

```ruby
config.vm.network "forwarded_port", guest: 8080, host: 8888
config.vm.provider "virtualbox" do |vb|
  vb.memory = "9000"
end
```

Then execute the following

```sh
vagrant up
vagrant ssh
```

then

Access from a browser with http://localhost:8888/sigma/login.html . Use admin for username and admin for password

## Old Installation Notes
You can also install Sigma on a Vagrant virtual machine.  You'll need VirtualBox too
https://www.virtualbox.org/

```sh
mkdir sigma_vagrant
cd sigma_vagrant
wget https://raw.githubusercontent.com/ontologyportal/sigmakee/master/Vagrantfile
vagrant up
vagrant ssh
bash <(curl -L https://raw.githubusercontent.com/ontologyportal/sigmakee/master/install-vagrant.sh)
```

follow the prompts and Sigma will be running.  Then on the browser of your host machine, go to
http://localhost:9090/sigma/login.html

To run natural language interpretation from the command line in the virtual machine,
run the following additional steps

```sh
cd ~
mkdir Programs
cd Programs
cp /vagrant/Downloads/stanford-corenlp-full-2018-01-31.zip .
unzip stanford-corenlp-full-2018-01-31.zip
cd stanford-corenlp-full-2018-01-31
export SIGMA_HOME="/home/vagrant/.sigmakee"
jar -xf stanford-corenlp-3.9.0-models.jar
cd ~/Programs/stanford-corenlp-full-2018-01-31
java  -Xmx2500m -classpath  /home/vagrant/workspace/sigma/sigma/build/classes:/home/vagrant/workspace/sigma/sigma/build/lib/*  com.articulate.sigma.semRewrite.Interpreter -i
```
