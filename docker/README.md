Introduction
============

Files provided in this folder can be used to build a docker container that
provides SigmaKEE with the E prover while SUMO and WordNet mappings are
downloaded outside of the container. When running the container, SUMO,
WordNetMappings folder, and ``config.xml`` will be mounted inside the container.
Thus, it is possible to keep SUMO updated with the GitHub repo, to change the
configuration file when needed (e.g., to use other port than 8080) and also to
rebuild the container to update SigmaKEE.

Building
========

The build.sh script can be used to build the docker container and download the
necessary files in separate folders. This script creates two folders (SUMO and
WordNetMappins) and copies config.xml from the newly created container.

The SUMO folder will contain the clone of SUMO's GitHub repository and
WordNetMappings will contain the WordNet dictionaries, as described to be done
in the SigmaKEE installation documents.

Since some changes are done in the ``config.xml`` file while building the container,
the build.sh script will run the SigmaKEE container once, copy config.xml to the
current folder and stop the container.

Running
=======

When running the container, the necessary folders and ``config.xml`` file should
be mounted in the correct paths. This can be done with the ``--mount`` option of
``docker run``, i.e.:

``docker run -d --rm -p 8080:8080 --m --mount type=bind,src=`pwd`/SUMO,dst=$KBDIR \
--mount type=bind,src=`pwd`/config.xml,dst=$KBDIR/config.xml \
--mount type=bind,src=`pwd`/WordNetMappings,dst=$KBDIR/WordNetMappings sigma:kee``

Running for the first time
--------------------------

When running for the first time, SigmaKEE may stop responding after the login
and redirect the browser to init.jsp. One workaround is to run the container
without WordNetMappings mounted in the first time and make the login. SigmaKEE
should update SUMO's KBs and work fine after.

After this first run, the container can be stop and WordNetMappings can be
mounted inside the container without any problem.


Customization
=============

Changes in the ``config.xml`` can be done to make the container work as necessary:

- Default port: the default port for SigmaKEE is 8080 but it is possible to
  change it in the ``config.xml`` file. When doing so, it is also necessary to
  change the run.sh script to map the new port of the container to the port on
  the host system.

- Run as a server: the config.xml file has a line stating the ``hostname`` and is
  by default set to ``localhost``. When this is changed (e.g., to the IP address
  of the host running the container) it's possible to use SigmaKEE from other
  computers.
