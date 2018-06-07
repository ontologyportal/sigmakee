#!/bin/bash

su -l sumo -c /home/sumo/Programs/apache-tomcat-8.5.31/bin/startup.sh

RV=$?
echo "Return value is: $RV"

sleep 5

# Added this since the process must be running in the foreground.  I would suggest changing to the log for the application.
su -l sumo -c "tail -f /home/sumo/Programs/apache-tomcat-8.5.31/logs/*.log"

