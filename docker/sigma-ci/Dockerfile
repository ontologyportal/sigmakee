FROM tomcat:9.0.97-jdk21-temurin-noble AS builder

# Follow instructions from: https://github.com/vprover/vampire/wiki/Source-Build-for-Users
# for buildling the latest vampire w/ latest z3

# Latest Vampire
ARG VAMPIRE_GIT='https://github.com/vprover/vampire'

# Here, we are utilizing a known Vampire build w/ Z3 matched to that build
#ARG VAMPIRE_GIT='https://github.com/vprover/vampire/archive/refs/tags/v4.9casc2024.tar.gz'
#ARG Z3_GIT='https://github.com/Z3Prover/z3/archive/refs/tags/z3-4.12.3.tar.gz'

# Latest E Theorem Prover (2024-05-01)
ARG E_PROVER='http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_3.1/E.tgz'
ARG WORD_NET='https://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz'

# We're in /usr/local/tomcat
RUN apt-get update && apt-get install -y --install-recommends \
    build-essential \
    cmake \
    git \
    python3 ;\
    wget $E_PROVER && tar xf E.tgz ;\
    cd E ;\
    ./configure && make -j2 ;\
    cd .. ;\
    wget $WORD_NET && tar xf WordNet-3.0.tar.gz ;\
    git clone $VAMPIRE_GIT ;\
    cd vampire ;\
    git submodule update --init ;\
    mkdir build ;\
    mkdir -p z3/build && cd z3/build ;\
    cmake .. -DZ3_SINGLE_THREADED=1 -DCMAKE_BUILD_TYPE=Release ;\
    make -j`nproc` ;\
    cd ../../build ;\
    cmake .. ;\
    make -j`nproc` ;\
    cd .. ;\
    cp build/vampire vampire ;\
    ./checks/sanity vampire

# For a specific version of Vampire and z3 using CLANG
#RUN apt update && apt install -y --install-recommends \
#    build-essential \
#    clang \
#    cmake \
#    git \
#    python3 ;\
#    wget $E_PROVER &&\
#    tar xf E.tgz ;\
#    cd E ;\
#    ./configure && make CC="clang" CXX="clang++" ;\
#    cd .. ;\
#    wget $WORD_NET &&\
#    tar xf WordNet-3.0.tar.gz ;\
#    wget $VAMPIRE_GIT &&\
#    tar -xzf v4.9casc2024.tar.gz ;\
#    wget $Z3_GIT &&\
#    tar -xzf z3-4.12.3.tar.gz ;\
#    mv z3-z3-4.12.3/* vampire-4.9casc2024/z3 ;\
#    cd vampire-4.9casc2024 ;\
#    mkdir build ;\
#    mkdir -p z3/build && cd z3/build ;\
#    CC=clang CXX=clang++ cmake .. -DZ3_SINGLE_THREADED=1 -DCMAKE_BUILD_TYPE=Release ;\
#    make -j`nproc` ;\
#    cd ../../build ;\
#    CC=clang CXX=clang++ cmake .. ;\
#    make -j`nproc` ;\
#    cd .. ;\
#    cp build/bin/vampire_* vampire ;\
#    ./checks/sanity vampire

#################################################
# runtime image.
FROM tomcat:9.0.97-jdk21-temurin-noble AS runtime

# Ensure the package list is up to date
RUN apt-get update && apt-get install -y --no-install-recommends \
    ant \
    ant-optional \
    git \
    graphviz

COPY --from=builder \
    /usr/local/tomcat/E/PROVER/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /usr/local/tomcat/WordNet-3.0 /opt/WordNet-3.0

#COPY --from=builder \
#    /usr/local/tomcat/vampire-4.9casc2024/vampire /usr/local/bin/vampire
#
#COPY --from=builder \
#    /usr/local/tomcat/vampire-4.9casc2024/z3/build/libz3.* /usr/local/bin/

COPY --from=builder \
    /usr/local/tomcat/vampire/vampire /usr/local/bin/vampire

COPY --from=builder \
    /usr/local/tomcat/vampire/z3/build/libz3.* /usr/local/bin/

ENV LD_LIBRARY_PATH=/usr/local/bin:$LD_LIBRARY_PATH
