FROM alpine:latest

# Install required libraries
RUN apk update
RUN apk upgrade
RUN apk add gcompat
RUN apk add bash
RUN apk add unzip
RUN apk add git
RUN apk add make
RUN apk add gcc
RUN apk add musl-dev
RUN apk add graphviz
RUN apk add apache-ant
RUN apk add zlib-dev
RUN apk add g++
RUN apk add linux-headers
RUN apk add openjdk11

# Setup home directory
RUN cd /root \
    && mkdir Programs \
    && mkdir workspace \
    && mkdir .sigmakee \
    && cd .sigmakee \
    && mkdir KBs

# Setup Bash
RUN cd /root \
    && touch .bashrc \
    && echo "alias dir='ls --color=auto --format=vertical -la'" >> .bashrc \
    && echo "export HISTSIZE=10000 HISTFILESIZE=100000" >> .bashrc \
    && echo "export SIGMA_HOME=/root/.sigmakee" >> .bashrc \
    && echo "export SIGMA_SRC=/root/workspace/sigmakee" >> .bashrc \
    && echo "export ONTOLOGYPORTAL_GIT=/root/workspace" >> .bashrc \
    && echo 'export CATALINA_OPTS="-Xmx10g"' >> .bashrc \
    && echo "export CATALINA_HOME=/root/Programs/apache-tomcat-9.0.96" >> .bashrc \
    && source .bashrc

# Setup Vampire
RUN cd /root/workspace \
    && git clone https://github.com/vprover/vampire \
    && cd vampire \
    && make -j 4 vampire_rel \
    && mv vampire_rel_master* vampire

# Setup Apache, E and Wordnet
RUN cd /root/Programs \
    && wget 'https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.96/bin/apache-tomcat-8.5.23.zip' \
    && wget 'https://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz' \
    && wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.0/E.tgz' \
    && tar -xvzf E.tgz \
    && unzip apache-tomcat-9.0.96.zip \
    && rm apache-tomcat-9.0.96.zip

RUN cd /root/Programs/apache-tomcat-9.0.96/bin \
    && chmod 777 *

RUN cd /root/Programs/apache-tomcat-9.0.96/webapps \
    && chmod 777 *

# Setup E
RUN cd /root/Programs/E \
    && ./configure \
    && make -j 4 \
    && make install

# Setup Wordnet
RUN cd /root/Programs \
    && gunzip WordNet-3.0.tar.gz \
    && tar -xvf WordNet-3.0.tar \
    && mkdir /root/.sigmakee/KBs/WordNetMappings \
    && cp WordNet-3.0/dict/* /root/.sigmakee/KBs/WordNetMappings/

# Clone ontologyportal git repos
RUN cd /root/workspace \
    && git clone https://github.com/ontologyportal/sigmakee \
    && git clone https://github.com/ontologyportal/sumo \
    && git clone https://github.com/ontologyportal/TPTP-ANTLR \
    && git clone https://github.com/ontologyportal/SigmaUtils

# Setup sigmakee
RUN cd /root/workspace/sigmakee \
    && source /root/.bashrc \
    && ant

# Add sumo kif knowledge-bases
RUN cp -R /root/workspace/sumo/* /root/.sigmakee/KBs
COPY kbs /root/.sigmakee/KBs

CMD cd /root && source .bashrc && /root/Programs/apache-tomcat-9.0.96/bin/startup.sh && tail -f /root/Programs/apache-tomcat-9.0.96/logs/catalina.out
