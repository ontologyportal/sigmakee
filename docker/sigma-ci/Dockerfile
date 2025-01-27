FROM tomcat:9.0.97-jdk21-temurin-jammy AS builder

# Followed instructions from: https://github.com/vprover/vampire/wiki/Source-Build-for-Users
# for buildling latest vampire w/ latest z3

RUN apt update; \
    apt-get install -y --no-install-recommends \
        build-essential \
        cmake \
        git \
        python3 \
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
    mkdir build ; \
    mkdir z3/build && cd z3/build ; \
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
FROM tomcat:9.0.97-jdk21-temurin-jammy AS runtime

COPY --from=builder \
    /usr/local/tomcat/E/PROVER/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /usr/local/tomcat/WordNet-3.0 /opt/WordNet-3.0

COPY --from=builder \
    /usr/local/tomcat/vampire/vampire /usr/local/bin/vampire

COPY --from=builder \
    /usr/local/tomcat/vampire/z3/build/libz3.* /usr/local/bin/

RUN apt update; \
    apt-get install -y --no-install-recommends \
        ant \
        ant-optional \
        git \
        graphviz
