FROM tomcat:9.0.97-jdk21-temurin-jammy AS builder

# Follow instructions from: https://github.com/vprover/vampire/wiki/Source-Build-for-Users
# for buildling the latest vampire w/ latest z3

# Here, we are utilizing a known Vampire build w/ Z3 matched to that build
ARG VAMPIRE_GIT='https://github.com/vprover/vampire/archive/refs/tags/v4.9casc2024.tar.gz'
ARG Z3_GIT='https://github.com/Z3Prover/z3/archive/refs/tags/z3-4.12.3.tar.gz'

# We're in /usr/local/tomcat
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
    wget $VAMPIRE_GIT &&\
    tar -xzf v4.9casc2024.tar.gz ;\
    wget $Z3_GIT &&\
    tar -xzf z3-4.12.3.tar.gz ;\
    mv z3-z3-4.12.3/* vampire-4.9casc2024/z3 ;\
    cd vampire-4.9casc2024 ;\
    mkdir build ;\
    mkdir -p z3/build && cd z3/build ;\
    cmake .. -DZ3_SINGLE_THREADED=1 -DCMAKE_BUILD_TYPE=Release ;\
    make -j`nproc` ;\
    cd ../../build ;\
    cmake .. ;\
    make -j`nproc` ;\
    cd .. ;\
    cp build/bin/vampire_z3_rel vampire ;\
    ./checks/sanity vampire

#################################################
# runtime image.
FROM tomcat:9.0.97-jdk21-temurin-jammy AS runtime

COPY --from=builder \
    /usr/local/tomcat/E/PROVER/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /usr/local/tomcat/WordNet-3.0 /opt/WordNet-3.0

COPY --from=builder \
    /usr/local/tomcat/vampire-4.9casc2024/vampire /usr/local/bin/vampire

COPY --from=builder \
    /usr/local/tomcat/vampire-4.9casc2024/z3/build/libz3.* /usr/local/bin/

RUN apt-get update && apt-get install -y --no-install-recommends \
    ant \
    ant-optional \
    git \
    graphviz
