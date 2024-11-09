ARG IMAGE_ACCOUNT=apease
FROM $IMAGE_ACCOUNT/sigma-ci:latest AS builder

#################################################
# runtime image.
FROM tomcat:9.0.96-jdk11-temurin-jammy AS runtime

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

COPY ./sigma.war $CATALINA_HOME/webapps/sigma.war

ENV SIGMA_HOME=/root/sigmakee-runtime
ENV CATALINA_OPTS="$CATALINA_OPTS -Xmx10g -Xss1m"
