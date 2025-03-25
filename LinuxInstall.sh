#!/bin/bash
set -e  # Exit the script if any command returns a non-zero exit status

echo "Installing Sigma"
echo "Downloading prerequisites"
# Update and install unzip
sudo apt-get update
sudo apt-get install -y unzip

# Install git
sudo apt-get install -y git

# Install ant
sudo apt-get install -y ant

# Install make
sudo apt-get install -y make

# Install cmake
sudo apt-get install -y cmake

# Install gcc
sudo apt-get install -y gcc

# Install graphviz
sudo apt-get install -y graphviz

# Install build-essential (required for compiling Vampire)
sudo apt-get install -y build-essential

# Install OpenJDK 23
sudo apt-get install -y default-jdk

# Add universe repository and update
sudo add-apt-repository -y universe
sudo apt-get update

echo "Pre-requisites have been installed."

# Check if .bashrc exists, and create it if not
if [ ! -f "$HOME/.bashrc" ]; then
    echo ".bashrc does not exist. Creating it now..."
    touch "$HOME/.bashrc"
else
    echo ".bashrc already exists."
fi

# Function to check and add a line to .bashrc
add_to_bashrc() {
    local LINE="$1"
    if ! grep -Fxq "$LINE" "$HOME/.bashrc"; then
        echo "Adding: $LINE to .bashrc..."
        echo "$LINE" >> "$HOME/.bashrc"
    else
        echo "$LINE already exists in .bashrc."
    fi
}

# Set environment variables in the current shell and add them to .bashrc
# NOTE: source ~/.bashrc doesn't seem to work in all cases.
set_and_persist() {
    local VAR_DECL="$1"
    eval "$VAR_DECL" # Set the variable in the current environment
    add_to_bashrc "$VAR_DECL" # Add the variable to .bashrc
}

# Add the necessary lines to .bashrc
add_to_bashrc "alias dir='ls --color=auto --format=vertical -la'"
add_to_bashrc "export HISTSIZE=10000 HISTFILESIZE=100000"
set_and_persist "export SIGMA_HOME=\"\$HOME/.sigmakee\""
set_and_persist "export ONTOLOGYPORTAL_GIT=\"\$HOME/workspace\""
set_and_persist "export SIGMA_SRC=\"\$ONTOLOGYPORTAL_GIT/sigmakee\""
set_and_persist "export CATALINA_OPTS=\"\$CATALINA_OPTS -Xmx10g -Xss1m\""
set_and_persist "export CATALINA_HOME=\"\$HOME/Programs/apache-tomcat-9.0.97\""
set_and_persist "export PATH=\"\$CATALINA_HOME/bin:\$PATH\""
set_and_persist "export SIGMA_CP=\"\$SIGMA_SRC/build/sigmakee.jar:\$SIGMA_SRC/lib/*\""

# Source the .bashrc file to apply changes
echo "Sourcing .bashrc to apply changes..."
source "$HOME/.bashrc"

# Create the Programs directory and navigate into it
if [ ! -d "$HOME/Programs" ]; then
    echo "Creating Programs directory in $HOME..."
    mkdir "$HOME/Programs"
else
    echo "Programs directory already exists in $HOME."
fi
cd "$HOME/Programs"

# Create the workspace directory and navigate into it
if [ ! -d "$HOME/workspace" ]; then
    echo "Creating workspace directory in $HOME..."
    mkdir "$HOME/workspace"
else
    echo "Workspace directory already exists in $HOME."
fi
cd "$HOME/workspace"



# Clone or update repositories
REPOS=(
    "https://github.com/ontologyportal/sigmakee"
    "https://github.com/ontologyportal/sumo"
    "https://github.com/ontologyportal/TPTP-ANTLR"
    "https://github.com/ontologyportal/SigmaUtils"
)

for REPO in "${REPOS[@]}"; do
    DIR_NAME=$(basename "$REPO" .git)
    if [ ! -d "$HOME/workspace/$DIR_NAME" ]; then
        echo "Cloning $REPO..."
        git clone "$REPO"
    else
        if [ -d "$HOME/workspace/$DIR_NAME" ]; then
            echo "$DIR_NAME repository already exists. Pulling the latest changes..."
            cd "$HOME/workspace/$DIR_NAME"
            git pull
            cd "$HOME/workspace"
        else
            echo "The folder $HOME/workspace/$DIR_NAME exists, but no Git repository found. Unable to do git pull. This probably means that a prior installation failed and wasn't completely cleaned up, in which case, the recommendation is deleting the $DIR_NAME folder and trying the install again. This script is exiting, to ensure nothing is deleted undesirably."
            exit 1
        fi
    fi
done

# Run the VerifyInstallationPrerequisites.sh script and print its output to the screen
output=$(source $SIGMA_SRC/VerifyInstallationPrerequisites.sh | tee /dev/tty)

# Check if the output contains "MISSING PREREQUISITES"
if echo "$output" | grep -q "MISSING PREREQUISITES"; then
  echo "Error: Missing prerequisites detected. Exiting script."
  exit 1
fi
echo "All prerequisites met. Proceeding with the script."


# Navigate to the sigmakee directory and run ant commands
cd "$HOME/workspace/sigmakee"
echo "Running ant install..."
output=$(env SIGMA_HOME="$SIGMA_HOME" \
             ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
             SIGMA_SRC="$SIGMA_SRC" \
             CATALINA_OPTS="$CATALINA_OPTS" \
             CATALINA_HOME="$CATALINA_HOME" \
             SIGMA_CP="$SIGMA_CP" \
             ant install 2>&1 | tee /dev/tty)
if echo "$output" | grep -q "BUILD FAILED"; then
    echo "BUILD FAILED detected. Exiting the script."
    exit 1
else
    echo "Install completed successfully."
fi

echo "Running ant to compile..."
output=$(env SIGMA_HOME="$SIGMA_HOME" \
             ONTOLOGYPORTAL_GIT="$ONTOLOGYPORTAL_GIT" \
             SIGMA_SRC="$SIGMA_SRC" \
             CATALINA_OPTS="$CATALINA_OPTS" \
             CATALINA_HOME="$CATALINA_HOME" \
             SIGMA_CP="$SIGMA_CP" \
             ant 2>&1 | tee /dev/tty)
if echo "$output" | grep -q "BUILD FAILED"; then
    echo "BUILD FAILED detected. Exiting the script."
    exit 1
else
    echo "Ant sigmakee compile completed successfully."
fi

echo "\n\nSIGMA has been installed! Close and re-open your command line interface (or run 'source ~/.bashrc')."
echo "To start the server:"
echo "startup.sh"
echo "Then point your browser to: http://localhost:8080/sigma/login.html"
echo "username: admin     password: admin"
echo "The first time logging in can take several minutes while the system is indexing. For low memory machines, restrict Knowledge Bases loaded in \$HOME/.sigmakee/KBs/config.xml"
echo "To shutdown the server:"
echo "shutdown.sh"
