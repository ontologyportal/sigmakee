ARG IMAGE_ACCOUNT=apease
FROM $IMAGE_ACCOUNT/sigma-ci:latest AS builder

#################################################
# runtime image.
FROM tomcat:9.0.97-jdk21-temurin-noble AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
    graphviz

COPY --from=builder \
    /usr/local/bin/e_ltb_runner /usr/local/bin/e_ltb_runner

COPY --from=builder \
    /opt/WordNet-3.0 /opt/WordNet-3.0

COPY --from=builder \
    /usr/local/bin/vampire /usr/local/bin/vampire

COPY --from=builder \
    /usr/local/bin/libz3.* /usr/local/bin/

# Just the essentials
COPY ./sigmakee-runtime/config.xml /root/sigmakee-runtime/
COPY ./sigmakee-runtime/*.kif /root/sigmakee-runtime/
COPY ./sigmakee-runtime/Translations /root/sigmakee-runtime/
COPY ./sigmakee-runtime/WordNetMappings /root/sigmakee-runtime/
COPY ./sigmakee-runtime/development /root/sigmakee-runtime/

COPY ./sigmakee/build/WEB-INF/lib/*.jar /root/sigmakee/

COPY ./sigma.war $CATALINA_HOME/webapps/sigma.war

ENV SIGMA_HOME=/root/sigmakee-runtime
ENV LD_LIBRARY_PATH=/usr/local/bin:$LD_LIBRARY_PATH
ENV CATALINA_OPTS="$CATALINA_OPTS -Xmx10g -Xss1m"
