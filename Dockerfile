FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV SIGMA_HOME=/root/.sigmakee
ENV ONTOLOGYPORTAL_GIT=/root/workspace
ENV SIGMA_SRC=/root/workspace/sigmakee
ENV PROGRAMS_DIR=/root/Programs
ENV CATALINA_OPTS="-Xmx10g -Xss1m"

VOLUME /root/.sigmakee

RUN apt-get update && apt-get install -y \
    ca-certificates curl unzip git ant make cmake gcc g++ graphviz \
    build-essential libcurl4-openssl-dev openjdk-21-jdk openjdk-21-jre-headless \
    jedit \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /root/workspace/sigmakee

# Only the installer scripts are needed to install prerequisites, so copy
# just those first: this layer (and everything below it, up to the ADD)
# only changes when the installer scripts themselves change, not on every
# source edit.
COPY scripts/installation ./scripts/installation

# Clones sibling repos (SUMO, SigmaUtils, sigmaAntlr, ...) and installs
# external programs (Tomcat, WordNet, Vampire, E, Leo-III). Independent of
# SigmaKEE's own source, so it stays cached across ordinary code changes.
ENV INSTALL_PHASE=deps
RUN bash scripts/installation/0_install.sh --phase deps

# Bring in the rest of the source. Only this layer and the build layer
# below are invalidated by day-to-day source changes.
ADD . /root/workspace/sigmakee

ENV INSTALL_PHASE=build
# --skip-verify: skip the live-webapp check (it boots Tomcat and polls it
# over HTTP), which belongs at container-run time, not image-build time.
# The image's CMD already starts Tomcat for real when the container runs.
RUN bash scripts/installation/0_install.sh --phase build --skip-verify

EXPOSE 8080

# The volume attaches to ~/.sigmakee, so we need to copy off its contents then copy them back on when first run
RUN cp -r /root/.sigmakee /root/sigmakee_home && \
    echo "(cp -r /root/sigmakee_home/* /root/.sigmakee && echo 'backed up sigmakee')" > /root/check.sh

WORKDIR /root/Programs/apache-tomcat-9.0.107

CMD ["/bin/bash", "-lc", "source /root/.bashrc && ./bin/startup.sh && tail -f ./logs/catalina.out"]
