#!/bin/bash

KBDIR=/sigma/sigmakee/KBs

DOCKER_RUN="docker run \
            -it --rm \
            --name sigmakee \
            -p 8080:8080 \
            --mount type=bind,src=`pwd`/SUMO,dst=$KBDIR \
            --mount type=bind,src=`pwd`/config.xml,dst=$KBDIR/config.xml \
            sigma:kee"

$DOCKER_RUN
