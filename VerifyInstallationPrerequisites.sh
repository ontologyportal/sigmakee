#!/bin/bash
echo "Verifying that all pre-requisite programs have been installed, and all environment variables have been set."

# List of applications to check. Checking for 'dot' in place of "graphviz" and "dpkg-dev" instead of "build-essentials".
applications=("unzip" "git" "ant" "make" "cmake" "gcc" "dot" "g++" "java" "javac")

# List of environment variables to check
env_variables=("SIGMA_HOME" "ONTOLOGYPORTAL_GIT" "SIGMA_SRC" "CATALINA_OPTS" "CATALINA_HOME" "SIGMA_CP")

# Variable to track if any application or environment variable is missing
installation_failure=false

# Function to check if a command is available
check_app_installed() {
    app_display_name=$1
    if [ "$1" == "dot" ]; then
        app_display_name="graphviz"
    elif [ "$1" == "g++" ]; then
        app_display_name="build-essential"
    fi
    printf "Checking if %-15s is installed ...... " "$app_display_name"  # Align application names
    if command -v "$1" >/dev/null 2>&1; then
        printf "installed\n"
    else
        printf "NOT installed\n"
        installation_failure=true  # Set failure flag if an app is not installed
    fi
}

# Function to check if an environment variable is set
check_env_variable() {
    printf "Checking if environment variable %-20s is set ...... " "$1"  # Align env variable names
    if [ -z "${!1}" ]; then
        printf "NOT set\n"
        installation_failure=true  # Set failure flag if a variable is not set
    else
        printf "set\n"
    fi
}

# Function to check if CATALINA_HOME is on the PATH
check_catalina_on_path() {
    printf "Checking if CATALINA_HOME is on the PATH ...... "
    if echo "$PATH" | grep -qE "(\$CATALINA_HOME|$CATALINA_HOME)"; then
        printf "yes\n"
    else
        printf "no\n"
        echo "Current PATH: $PATH"
        installation_failure=true  # Set failure flag if CATALINA_HOME is not on PATH
    fi
}

# Check each application
for app in "${applications[@]}"; do
    check_app_installed "$app"
done

# Check each environment variable
for var in "${env_variables[@]}"; do
    check_env_variable "$var"
done


# Check if CATALINA_HOME is on the PATH
check_catalina_on_path

# Loop through each variable and print its value
for var in "${env_variables[@]}"
do
  echo "$var=${!var}"
done

if $installation_failure; then
    echo "MISSING PREREQUISITES"
fi
