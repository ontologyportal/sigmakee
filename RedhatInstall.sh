#!/bin/bash
#################################
# NOTE! This is experimental and has only been tested on one RedHat server
#################################

echo "Installing Sigma"
echo "Downloading prerequisites"
# Update and install unzip
sudo yum update
sudo yum install -y unzip

# Install git
sudo yum install -y git

# Install ant - must be manual on Redhat since yum ant requires JDK 1.8

# Install make
sudo yum install -y make

# Install cmake
sudo yum install -y cmake

# Install gcc
sudo yum install -y gcc

# Install graphviz
sudo yum install -y graphviz

sudo yum install -y httpd

# Install build-essential (required for compiling Vampire)
sudo yum groupinstall "Development Tools"

# Install OpenJDK 21
sudo yum install java-21-openjdk-devel

# uncomment to have a web server for installation on a server
# sudo yum install -y httpd
# sudo systemctl enable httpd  # For CentOS/RHEL
# echo '<h1>default page</h1>' > /home/www/html/index.html

# Add universe repository and update
# sudo add-apt-repository -y universe # not for RedHat
sudo yum update

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


# Add the necessary lines to .bashrc
add_to_bashrc "alias dir='ls --color=auto --format=vertical -la'"
add_to_bashrc "export HISTSIZE=10000 HISTFILESIZE=100000"
add_to_bashrc "export SIGMA_HOME=\"\$HOME/.sigmakee\""
add_to_bashrc "export ONTOLOGYPORTAL_GIT=\"\$HOME/workspace\""
add_to_bashrc "export SIGMA_SRC=\"\$ONTOLOGYPORTAL_GIT/sigmakee\""
add_to_bashrc "export CATALINA_OPTS=\"\$CATALINA_OPTS -Xmx10g -Xss1m\""
add_to_bashrc "export CATALINA_HOME=\"\$HOME/Programs/apache-tomcat-9.0.97\""
add_to_bashrc "export SIGMA_CP=\"\$SIGMA_SRC/build/sigmakee.jar:\$SIGMA_SRC/lib/*\""

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

#install ant

wget https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.15-bin.zip
unzip apache-ant-1.10.15-bin.zip 

add_to_bashrc "export JAVA_HOME=\"/usr/lib/jvm/java-21-openjdk-21.0.6.0.7-1.el8.x86_64\""
add_to_bashrc "export ANT_HOME=\"/home/apease/Programs/apache-ant-1.10.15\""
add_to_bashrc "export PATH=\"\$CATALINA_HOME/bin:\$ANT_HOME/bin:\$PATH\""

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
    "https://github.com/ontologyportal/sigmaAntlr"
    "https://github.com/ontologyportal/SigmaUtils"
)

for REPO in "${REPOS[@]}"; do
    DIR_NAME=$(basename "$REPO" .git)
    if [ ! -d "$HOME/workspace/$DIR_NAME" ]; then
        echo "Cloning $REPO..."
        git clone "$REPO"
    else
        echo "$DIR_NAME repository already exists. Pulling the latest changes..."
        cd "$HOME/workspace/$DIR_NAME"
        git pull
        cd "$HOME/workspace"
    fi
done

# Navigate to the sigmakee directory and run ant commands
cd "$HOME/workspace/sigmakee"
echo "Running ant install..."
output=$(ant install 2>&1 | tee /dev/tty)
if echo "$output" | grep -q "BUILD FAILED"; then
    echo "BUILD FAILED detected. Exiting the script."
    exit 1
else
    echo "Install completed successfully."
fi

echo "Running ant to compile..."
output=$(ant 2>&1 | tee /dev/tty)
if echo "$output" | grep -q "BUILD FAILED"; then
    echo "BUILD FAILED detected. Exiting the script."
    exit 1
else
    echo "Ant sigmakee compile completed successfully."
fi

echo "SIGMA has been installed! To start the server:"
echo "startup.sh"
echo "Then point your browser to: http://localhost:8080/sigma/login.html"
echo "username: admin     password: admin"
echo "The first time logging in can take several minutes while the system is indexing. For low memory machines, restrict Knowledge Bases loaded in \$HOME/.sigmakee/KBs/config.xml"
echo "To shutdown the server:"
echo "shutdown.sh"
