ARG IMAGE_ACCOUNT=apease
FROM $IMAGE_ACCOUNT/sigma-ci:latest as builder

#################################################
# runtime image.
FROM tomcat:9.0.73-jdk8-temurin-jammy as runtime
#FROM eclipse-temurin:8-jdk-jammy

RUN apt update; \
    apt-get install -y --no-install-recommends \
            graphviz

COPY --from=builder \
    /usr/local/bin/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /opt/WordNet-3.0 /opt/WordNet-3.0

COPY --from=builder \
    /usr/local/bin/vampire /usr/local/bin/vampire

COPY ./sigmakee-runtime /root/sigmakee-runtime/

COPY ./sigmakee/build/WEB-INF/lib/*.jar /root/sigmakee/

COPY ./sigmakee/build/sigmakee.jar /root/sigmakee/

ENV SIGMA_HOME=/root/sigmakee-runtime
