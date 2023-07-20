FROM tomcat:9.0.73-jdk8-temurin-jammy as builder

ARG VAMPIRE_GIT=8d999c1350ea5d7839cb98350371c3e4833b2cb6

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
    wget 'http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz' ; \
    tar xf WordNet-3.0.tar.gz ; \
    git clone --depth 1 https://github.com/vprover/vampire ; \
    cd vampire ; \
    git checkout $VAMPIRE_GIT ; \
    cd .. ;\
    wget https://github.com/Z3Prover/z3/archive/refs/tags/z3-4.12.1.tar.gz ; \
    tar -xzf z3-4.12.1.tar.gz ; \
    rm -rf vampire/z3 ; \
    mv z3-z3-4.12.1 vampire/z3 ; \
    mkdir vampire/z3/build ; \
    mkdir vampire/build ; \
    cd vampire/z3/build ; \
    cmake .. -DZ3_BUILD_EXECUTABLE=OFF -DZ3_BUILD_TEST_EXECUTABLES=OFF ; \
    make -j`nproc` ; \
    cd ../../build ; \
    cmake .. ; \
    make -j`nproc` ; \
    cd .. ; \
    cp build/bin/vampire_* vampire ; \
    ./checks/sanity vampire

#################################################
# runtime image.
FROM tomcat:9.0.73-jdk8-temurin-jammy as runtime

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
