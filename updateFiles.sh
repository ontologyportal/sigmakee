cd $ONTOLOGYPORTAL_GIT/sumo
git pull
cp *.kif $SIGMA_HOME/KBs
cp -R Translations $SIGMA_HOME/KBs
cp -R WordNetMappings $SIGMA_HOME/KBs
cp -R development $SIGMA_HOME/KBs
rm $CATALINA_HOME/logs/*.*
rm $CATALINA_HOME/webapps/sigma/graph/*.*
rm $SIGMA_HOME/KBs/*.ser
rm $SIGMA_HOME/KBs/WordNetMappings/*.ser