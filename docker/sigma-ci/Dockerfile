FROM tomcat:9.0.73-jdk8-temurin-jammy as builder

RUN apt update; \
    apt-get install -y --no-install-recommends \
            build-essential \
    ; \
    wget 'http://wwwlehre.dhbw-stuttgart.de/~sschulz/WORK/E_DOWNLOAD/V_2.6/E.tgz' &&\
    tar xf E.tgz ; \
    cd E ; \
    ./configure && make ;\
    cd .. ;\
    wget 'http://wordnetcode.princeton.edu/3.0/WordNet-3.0.tar.gz' ; \
    tar xf WordNet-3.0.tar.gz

#################################################
# runtime image.
FROM tomcat:9.0.73-jdk8-temurin-jammy as runtime

COPY --from=builder \
    /usr/local/tomcat/E/PROVER/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /usr/local/tomcat/WordNet-3.0 /opt/WordNet-3.0

RUN apt update; \
    apt-get install -y --no-install-recommends \
            ant \
            ant-optional \
            git \
            graphviz
