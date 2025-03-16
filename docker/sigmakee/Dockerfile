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
COPY ./sigmakee-runtime/KBs/config.xml /root/sigmakee-runtime/KBs/
COPY ./sigmakee-runtime/KBs/*.kif /root/sigmakee-runtime/KBs/
COPY ./sigmakee-runtime/KBs/Translations /root/sigmakee-runtime/KBs/Translations/
COPY ./sigmakee-runtime/KBs/WordNetMappings /root/sigmakee-runtime/KBs/WordNetMappings/
COPY ./sigmakee-runtime/KBs/development /root/sigmakee-runtime/KBs/development/

# Not necessary since everything is in the WAR
COPY ./sigmakee/lib/*.jar /root/sigmakee/
COPY ./sigmakee/build/sigmakee.jar /root/sigmakee/

COPY ./sigma.war $CATALINA_HOME/webapps/sigma.war

ENV SIGMA_HOME=/root/sigmakee-runtime
ENV LD_LIBRARY_PATH=/usr/local/bin:$LD_LIBRARY_PATH
ENV CATALINA_OPTS="$CATALINA_OPTS -Xmx10g -Xss1m"
