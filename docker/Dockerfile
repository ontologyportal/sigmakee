#June 3, 2018:  Revision to redue the space used up by this image
FROM centos:latest
MAINTAINER Adam Pease

# Install a bunch of software we need (maybe)
RUN yum install -y  \
    git \
    sudo \
    unzip \
    wget \
# Install make and gcc
    make \
    gcc \
# Handle sup. packages
    graphviz \
    ant; yum clean all

# Add the sumo user and add to wheel (i.e., make sudoer no pwd)
RUN useradd sumo && \
    usermod -aG wheel sumo && \
    sed -i 's/%wheel/# %wheel/' /etc/sudoers && \
    echo "%wheel     ALL=(ALL)     NOPASSWD: ALL" | tee -a /etc/sudoers

# Add java to the image
COPY jdk-8u171-linux-x64.rpm .
RUN rpm -ivh jdk-8u171-linux-x64.rpm && rm jdk-8u171-linux-x64.rpm

# Copy in the BASHRC
COPY bashrc .
RUN  cp bashrc /home/sumo/.bashrc && \
     chown sumo:sumo /home/sumo/.bashrc

# Copy in the sigmastart.sh script
COPY sigmastart.sh .
RUN chmod u+x sigmastart.sh

#Copy bashrc into the image and create a bunch of required directories
RUN su -l sumo -c "mkdir /home/sumo/workspace; mkdir /home/sumo/Programs/; mkdir /home/sumo/Programs/bins" && \
    su -l sumo -c "cd /home/sumo/Programs; wget http://www-us.apache.org/dist/tomcat/tomcat-9/v9.0.10/bin/apache-tomcat-9.0.10.zip" && \
    su -l sumo -c "cd /home/sumo/Programs; wget http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz" && \
    su -l sumo -c "cd /home/sumo/Programs; wget http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.0/E.tgz" && \
    su -l sumo -c "cd /home/sumo/Programs; tar -xvzf E.tgz" && \
    su -l sumo -c "cd /home/sumo/Programs; unzip apache-tomcat-9.0.10.zip" && \
    su -l sumo -c "cd /home/sumo/Programs; mv E.tgz bins; mv apache-tomcat-9.0.10.zip bins" && \
    su -l sumo -c "cd /home/sumo/Programs/apache-tomcat-9.0.10/bin/; chmod 777 *" && \
    su -l sumo -c "cd /home/sumo/workspace; git clone https://github.com/ontologyportal/sigmakee; git clone https://github.com/ontologyportal/sumo" && \
    su -l sumo -c "cd /home/sumo; mkdir .sigmakee; cd .sigmakee; mkdir KBs; cp -R /home/sumo/workspace/sumo/* KBs; cp /home/sumo/workspace/sigmakee/config.xml /home/sumo/.sigmakee/KBs" && \
    su -l sumo -c "sed -i 's/theuser/sumo/g' /home/sumo/.sigmakee/KBs/config.xml" && \
    su -l sumo -c "sed -i 's/~/\/home\/sumo/g' /home/sumo/.sigmakee/KBs/config.xml" && \
    su -l sumo -c "cd /home/sumo/Programs; gunzip WordNet-3.0.tar.gz; tar -xvf WordNet-3.0.tar; cp WordNet-3.0/dict/* /home/sumo/.sigmakee/KBs/WordNetMappings/" && \
    su -l sumo -c "cd /home/sumo/Programs/E; ./configure; make; make install; cd /home/sumo" && \
    su -l sumo -c "source /home/sumo/.bashrc; cd /home/sumo/workspace/sigmakee; ant" && \
    #The following line removes a bunch of stuff - I think it is fine.
    su -l sumo -c "rm -Rf /home/sumo/Programs/bins; rm -Rf /home/sumo/Programs/*.tar; rm -Rf /home/sumo/workspace"
    # This test may not be necessary - but for now keeping it in.
    # Removing this test probably
    # Note - put it above the previous line (before removing workspace i you want it to run).
    # su -l sumo -c "java -Xmx2500m -classpath /home/sumo/workspace/sigmakee/build/classes:/home/sumo/workspace/sigmakee/build/lib/* com.articulate.sigma.KB 2>/home/sumo/testerror.log"
# Done for now!