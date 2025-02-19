cd $ONTOLOGYPORTAL_GIT/sumo
git pull
cp *.kif $SIGMA_HOME/KBs
cp -R Translations $SIGMA_HOME/KBs
cp -R WordNetMappings $SIGMA_HOME/KBs
cp -R development $SIGMA_HOME/KBs
cd $ONTOLOGYPORTAL_GIT/SigmaUtils
git pull
cd $ONTOLOGYPORTAL_GIT/sigmakee
git pull
cd $ONTOLOGYPORTAL_GIT/TPTP-ANTLR
git pull
cd $ONTOLOGYPORTAL_GIT/SUMOjEdit
git pull
cd $ONTOLOGYPORTAL_GIT/sumonlp
git pull
rm $CATALINA_HOME/logs/*.*
rm $CATALINA_HOME/webapps/sigma/graph/*.*
rm $SIGMA_HOME/KBs/*.ser
rm $SIGMA_HOME/KBs/*.tptp
rm $SIGMA_HOME/KBs/WordNetMappings/*.ser
