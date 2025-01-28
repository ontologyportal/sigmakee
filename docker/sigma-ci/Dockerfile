FROM tomcat:9.0.97-jdk21-temurin-jammy AS builder

# Follow instructions from: https://github.com/vprover/vampire/wiki/Source-Build-for-Users
# for buildling latest vampire w/ latest z3

ARG VAMPIRE_GIT='https://github.com/vprover/vampire/archive/refs/tags/v4.9casc2024.tar.gz'
ARG Z3_GIT='https://github.com/Z3Prover/z3/archive/refs/tags/z3-4.12.3.tar.gz'

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    git \
    python3 ;\
    wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.6/E.tgz' &&\
    tar xf E.tgz ;\
    cd E ;\
    ./configure && make ;\
    cd .. ;\
    wget 'https://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz' &&\
    tar xf WordNet-3.0.tar.gz ;\
#    git clone https://github.com/vprover/vampire ;\
    wget $VAMPIRE_GIT ;\
    mkdir -p vampire && tar -xzf v4.9casc2024.tar.gz -C vampire --strip-components=1 ;\
#    git submodule update --init ;\
    wget $Z3_GIT ;\
    mkdir -p z3 && tar -xzf z3-4.12.3.tar.gz -C z3 --strip-components=1 ;\
    rm -rf vampire/z3 ; \
    mv z3/* vampire/z3 ;\
    cd vampire ;\
    mkdir build ;\
    mkdir z3/build && cd z3/build ;\
    cmake .. -DZ3_SINGLE_THREADED=1 -DCMAKE_BUILD_TYPE=Release ;\
    make -j`nproc` ;\
    cd ../../build ;\
    cmake .. ;\
    make -j`nproc` ;\
    cd .. ;\
    cp build/vampire vampire ;\
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

RUN apt-get update && apt-get install -y --no-install-recommends \
    ant \
    ant-optional \
    git \
    graphviz
