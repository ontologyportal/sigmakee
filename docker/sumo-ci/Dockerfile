#################################################
# builder image.
ARG IMAGE_ACCOUNT=apease
FROM $IMAGE_ACCOUNT/sigma-ci:latest AS builder

#################################################
# runtime image.
FROM ubuntu:latest AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
    graphviz
#RUN apt-get update && apt-get install -y --no-install-recommends \
#    graphviz \
#    sudo

COPY --from=builder \
    /usr/local/bin/e_ltb_runner /usr/local/bin/e_ltb_runner
COPY --from=builder \
    /usr/local/bin/vampire /usr/local/bin/vampire
COPY --from=builder \
    /usr/local/bin/libz3.* /usr/local/bin/

# Already configured for top, mid and language ontologies
COPY ./sigmakee-runtime/KBs/config.xml /root/sigmakee-runtime/KBs/
#COPY ./sigmakee-runtime/KBs/*.fof /root/sigmakee-runtime/KBs/
#COPY ./sigmakee-runtime/KBs/*.tptp /root/sigmakee-runtime/KBs/

COPY ./sigmakee/lib/*.jar /root/sigmakee/
COPY ./sigmakee/build/sigmakee.jar /root/sigmakee/

ENV SIGMA_HOME=/root/sigmakee-runtime
ENV LD_LIBRARY_PATH=/usr/local/bin
