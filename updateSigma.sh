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
cd $ONTOLOGYPORTAL_GIT/sigmaAntlr
git pull
$CATALINA_HOME/bin/shutdown.sh
echo "waiting 10 seconds"
sleep 10
rm $CATALINA_HOME/logs/*.*
rm $CATALINA_HOME/webapps/sigma/graph/*.*
rm $CATALINA_HOME/webapps/sigma/*.*
rm $SIGMA_HOME/KBs/*.ser
rm $SIGMA_HOME/KBs/WordNetMappings/*.ser
ant
$CATALINA_HOME/bin/startup.sh
echo "waiting 30 seconds"
sleep 30
curl -d userName="user" -d password="user" "http://localhost:8080/sigma/KBs.jsp?kb=SUMO&lang=EnglishLanguage&flang=SUO-KIF&term=Object" > result.html 

