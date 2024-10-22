FROM tomcat:9.0.96-jdk11-temurin-jammy AS builder

# From: https://github.com/vprover/vampire/commit/8d999c1350ea5d7839cb98350371c3e4833b2cb6
ARG VAMPIRE_GIT=8d999c1350ea5d7839cb98350371c3e4833b2cb6
# Linked z3 from: https://github.com/Z3Prover/z3/releases/tag/z3-4.12.1
# git checkout $VAMPIRE_GIT ; \ <- not working (tdn) 10/21/24
# Followed instructions from: https://github.com/vprover/vampire/wiki/Source-Build-for-Users

RUN apt update; \
    apt-get install -y --no-install-recommends \
            build-essential \
            cmake \
            git \
    ; \
    wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.6/E.tgz' &&\
    tar xf E.tgz ; \
    cd E ; \
    ./configure && make ;\
    cd .. ;\
    wget 'https://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz' ; \
    tar xf WordNet-3.0.tar.gz ; \
    git clone https://github.com/vprover/vampire ; \
    cd vampire ; \
    git submodule update --init ; \
    cd .. ;\
    rm -rf vampire/z3 ; \
    mkdir vampire/z3/build ; \
    mkdir vampire/build ; \
    cd vampire/z3/build ; \
    cmake .. -DZ3_SINGLE_THREADED=1 -DCMAKE_BUILD_TYPE=Release ; \
    make -j`nproc` ; \
    cd ../../build ; \
    cmake .. ; \
    make -j`nproc` ; \
    cd .. ; \
    cp build/vampire vampire ; \
    ./checks/sanity vampire

#################################################
# runtime image.
FROM tomcat:9.0.73-jdk8-temurin-jammy AS runtime

COPY --from=builder \
    /usr/local/tomcat/E/PROVER/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /usr/local/tomcat/WordNet-3.0 /opt/WordNet-3.0

COPY --from=builder \
    /usr/local/tomcat/vampire/vampire /usr/local/bin/vampire

RUN apt update; \
    apt-get install -y --no-install-recommends \
            ant \
            ant-optional \
            git \
            graphviz
