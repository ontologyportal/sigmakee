Install via script from source on Linux or Mac OS with
bash <(curl -L http://sigmakee.cvs.sourceforge.net/viewvc/sigmakee/sigma/install.sh)

Users should also see

https://sourceforge.net/p/sigmakee/wiki/required_data_files/
Mac instructions - https://sourceforge.net/p/sigmakee/wiki/Sigma%20Setup%20on%20Mac/
Ubuntu - https://sourceforge.net/p/sigmakee/wiki/Setting%20up%20Sigma%20on%20Ubuntu/

You can also install Sigma on a Vagrant virtual machine.

> mkdir sigma_vagrant
> cd sigma_vagrant
> wget http://sigmakee.cvs.sourceforge.net/viewvc/sigmakee/sigma/Vagrantfile
> vagrant up
> vagrant ssh
> bash <(curl -L http://sigmakee.cvs.sourceforge.net/viewvc/sigmakee/sigma/install.sh)

follow the prompts and Sigma will be running.  Then on the browser of your host machine, go to
http://localhost:9090/sigma/login.html
