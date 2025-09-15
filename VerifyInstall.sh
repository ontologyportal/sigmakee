#!/usr/bin/env bash


set -euo pipefail

#
# Helper functions
#
check_dirs_nonempty() {
  local dirs=("$@")
  for d in "${dirs[@]}"; do
    if [ ! -d "$d" ]; then
      echo "[MISSING DIRECTORY:] $d"
      exit 1
    fi
    echo "Found: $d"
    if [ ! "$(ls -A "$d")" ]; then
      echo "  $d is EMPTY."
      exit 1
    fi
  done
  echo "All directories found and non-empty."
}

check_files() {
  local files=("$@")

  for f in "${files[@]}"; do
    if [ -f "$f" ]; then
      echo "Found file: $f"
    else
      echo "[MISSING FILE:] File not found: $f"
      return 1
    fi
  done

  echo "Most relevant files found."
  return 0
}

print_header() {
  local COLOR='\033[1;34m' # Light blue color
  local NC='\033[0m' # No Color
  echo
  echo -e "=================================================================="
  echo -e ">>> ${COLOR}$1${NC}"
  echo "=================================================================="
}


########################################################
# Check if the output contains "MISSING PREREQUISITES"
########################################################
print_header "Checking that prerequisites were installed correctly"
output=$(source $SIGMA_SRC/VerifyInstallationPrerequisites.sh | tee /dev/tty)
if echo "$output" | grep -q "MISSING PREREQUISITES"; then
  echo "Error: Missing prerequisites detected. Exiting script."
  exit 1
fi
echo "All prerequisites met. Proceeding with checks."


######################################################################
######################################################################
##    Note: This next section roughly follows the build.xml script. ##
##    If it fails, look for the <target name="install" section.     ##
######################################################################
######################################################################


########################################################
# Check if folders exist and are not empty
########################################################
print_header "Checking relevant directories/files exist"
dir_list=(
  "$ONTOLOGYPORTAL_GIT"
  "$ONTOLOGYPORTAL_GIT/SigmaUtils"
  "$ONTOLOGYPORTAL_GIT/TPTP-ANTLR"
  "$ONTOLOGYPORTAL_GIT/sumo"
  "$ONTOLOGYPORTAL_GIT/sigmaAntlr"
  "$HOME/Programs"
  "$CATALINA_HOME/bin"
  "$CATALINA_HOME/webapps"
  "$HOME/Programs/WordNet-3.0"
  "$HOME/Programs/E"
  "$HOME/Programs/vampire"
  "$HOME/Programs/vampire/z3"
  "$HOME/Programs/vampire/z3/build"
  "$SIGMA_HOME/KBs"
  "$SIGMA_HOME/KBs/WordNetMappings"
)

check_dirs_nonempty "${dir_list[@]}"


########################################################
# Check if certain files exist, that various copying
# commands executed successfully.
########################################################
files=(
  "$SIGMA_HOME/KBs/Merge.kif"
  "$SIGMA_HOME/KBs/Mid-level-ontology.kif"
  "$SIGMA_HOME/KBs/config.xml"
  "$HOME/Programs/E/configure"
  "$HOME/Programs/E/PROVER/e_ltb_runner"
  "$HOME/Programs/vampire/build/vampire"
  "$HOME/Programs/vampire/z3/build/z3"
)

check_files "${files[@]}"


######################################################################
# Check if config.xml has been appropriately configured.
######################################################################
print_header "Checking .sigmakee/KBs/config.xml has proper configurations"
target_string="/home/theuser"
config_file="$SIGMA_HOME/KBs/config.xml"

if grep -qF "$target_string" "$config_file"; then
  echo "String \"$target_string\" found in $config_file. Exiting."
  exit 0
fi

strings=(
  "$HOME/Programs/E/PROVER/e_ltb_runner"
  "$HOME/Programs/vampire/build/vampire"
)

for s in "${strings[@]}"; do
  if ! grep -qF -- "$s" "$config_file"; then
    echo "Missing string from $config_file: $s"
    exit 1
  fi
done


echo "$config_file properly configured."


########################################################
# Vampire checks
########################################################
print_header "Verifying vampire build"

# Check if vampire exists and is in PATH
if ! command -v vampire > /dev/null 2>&1; then
  echo "vampire is NOT installed or not in PATH."
  exit 1
fi

# Run vampire --version and check exit status
vampire --version > /dev/null 2>&1
if [[ $? -eq 0 ]]; then
  echo "vampire is installed, on PATH, and runs correctly."
else
  echo "vampire is in PATH, but failed to run correctly."
  exit 1
fi

echo
echo "***VERIFIED SUCCESSFUL Sigma download and configuration (ant install)***"
echo

######################################################################
######################################################################
## Check ant build. (ant)                                           ##
######################################################################
######################################################################

print_header "Verifying Sigma build (i.e., ant)"

########################################################
# Check if folders exist and are not empty
########################################################
print_header "Checking directories and files created during build process"

dir_list=(
  "$ONTOLOGYPORTAL_GIT/sigmakee/.ivy"
)
check_dirs_nonempty "${dir_list[@]}"


########################################################
# Check if certain files exist, that various copying
# commands executed successfully.
########################################################
files=(
  "$ONTOLOGYPORTAL_GIT/sigmaAntlr/sigmaAntlr.jar"
  "$ONTOLOGYPORTAL_GIT/sigmakee/build/sigmakee.jar"
  "$CATALINA_HOME/webapps/sigma.war"
  "$CATALINA_HOME/bin/startup.sh"
  "$CATALINA_HOME/bin/shutdown.sh"
)

check_files "${files[@]}"


############################################################
# Run a basic test of the knowledge base.
# java -Xmx40g -cp "$SIGMA_CP" com.articulate.sigma.KB -t
############################################################
print_header "Knowledge Base stress test"

echo "Building and testing knowledge base ... This may take several minutes."
if java -Xmx40g -cp "$SIGMA_CP" com.articulate.sigma.KB -t 2>/dev/null | grep -qF "KB.test()"; then
  echo "Successfully built and ran stress tests on Knowledge Base."
else
  echo "Did not successfully run knowledge tests: java -Xmx40g -cp \"$SIGMA_CP\" com.articulate.sigma.KB -t"
  exit 1
fi





########################################################
# Check if tomcat actually works and starts.
########################################################
print_header "Sigmakee web server verification"

timeout=60  # seconds
interval=3  # seconds between attempts
elapsed=0

echo "Sigmakee is launched by a Tomcat server. Checking if Tomcat is functional."
if startup.sh | grep -qF "Tomcat started."; then
  echo "Tomcat successfully running."
else
  echo "There is a problem running 'startup.sh' found in $CATALINA_HOME/bin, used to start the Tomcat server."
  exit 1
fi

while true; do
  # Sometimes it takes some time to unpack the .war file, keep trying for a minute.
  output=$(curl -s "http://localhost:8080/sigma/login.html")
  if echo "$output" | grep -qF "<title>Sigma Login</title>"; then
    echo "Sigmakee is successfully running."
    break
  fi

  sleep $interval
  elapsed=$((elapsed + interval))

  if (( elapsed >= timeout )); then
    echo "Timeout waiting for Sigma login page after Tomcat startup."
    echo "Last curl output was:"
    echo "$output"
    if [ ! -d "$CATALINA_HOME/webapps/sigma" ]; then
      echo "[MISSING DIRECTORY:] $CATALINA_HOME/webapps/sigma"
      echo "Try running 'bash $ONTOLOGYPORTAL_GIT/sigmakee/VerifyInstallation.sh' in a couple minutes, sometimes it takes a while for the Tomcat server to build the sigmakee website from the sigma.war file."
    fi
    exit 1
  fi
done

# Shutdown Tomcat server
./shutdown.sh > /dev/null 2>&1 &

###################################################################
#
# Success, probably!!!!
#
###################################################################
echo "Finished build verification. Checks indicate a successful installation!"
