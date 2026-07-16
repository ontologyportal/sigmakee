FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV SIGMA_HOME=/root/.sigmakee
ENV ONTOLOGYPORTAL_GIT=/root/workspace
ENV SIGMA_SRC=/root/workspace/sigmakee
ENV PROGRAMS_DIR=/root/Programs
ENV CATALINA_OPTS="-Xmx10g -Xss1m"

RUN apt-get update && apt-get install -y \
    ca-certificates curl unzip git ant make cmake gcc g++ graphviz \
    build-essential libcurl4-openssl-dev openjdk-21-jdk openjdk-21-jre-headless \
    jedit \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /root/workspace

RUN git clone --branch development https://github.com/ontologyportal/sigmakee.git

WORKDIR /root/workspace/sigmakee

RUN bash scripts/installation/install.sh

EXPOSE 8080

CMD ["bash", "-lc", "source ~/.bashrc && startup.sh && tail -f $CATALINA_HOME/logs/catalina.out"]