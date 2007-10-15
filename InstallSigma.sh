#!/bin/bash

# InstallSigma.sh

TOMCAT_ROOT=
TOMCAT_WEBAPPS_DIR=
SIGMA_ROOT=
SIGMA_KBS_DIR=

PKG_DIR=`pwd`
PKG_KBS_DIR="${PKG_DIR%\/}/KBs"
PKG_TESTS_DIR="${PKG_KBS_DIR%\/}/tests"
PKG_INFERENCE_DIR="${PKG_DIR%\/}/inference"

usage ()
{

cat << EOF

  Usage: $0

  $0 expects to find subdirectories named KBs and inference
  immediately under the location where it is run, and so
  should be invoked in the directory created when the Sigma
  .zip archive is unzipped.

  $0 need not be run as root (superuser), but the user must
  have sufficient permissions to modify (write, delete) files
  under Tomcat's webapps directory.

EOF

}

# Make sure ./KBs and ./inference are readable.
if [[ ! ((-r "$PKG_KBS_DIR") && (-r "$PKG_INFERENCE_DIR")) ]]
  then
    usage
    exit 1
fi

# If CATALINA_HOME is set, try to use it.
if [[ "$CATALINA_HOME" && (-d "${CATALINA_HOME%\/}/webapps") ]]
  then
    TOMCAT_ROOT="$CATALINA_HOME"
fi

# Prompt the user for TOMCAT_ROOT, if necessary
while [[ (! "$TOMCAT_ROOT") || (! -d "$TOMCAT_ROOT") ]]
do
cat << PROMPT1

  Please enter the complete pathname of the Tomcat root
  directory.  This is the directory that contains the
  subdirectories named bin and webapps.

PROMPT1

  read -p "  Pathname: " TOMCAT_ROOT

  if [[ ! -d "$TOMCAT_ROOT" ]]
    then 
      echo
      echo "  $TOMCAT_ROOT is not a directory."
      echo "  Please try again."
  elif [[ ! -w "$TOMCAT_ROOT" ]]
    then 
      echo
      echo "  $TOMCAT_ROOT is not writable."
      echo "  You must run $0 with sufficient permissions."
      usage
      exit 1
  fi
done

SIGMA_ROOT=$SIGMA_HOME

# If SIGMA_ROOT exists, but is not writable, exit.
if [[ "$SIGMA_ROOT" && ((-d "$SIGMA_ROOT") && (! -w "$SIGMA_ROOT")) ]]
  then
    echo
    echo "  $SIGMA_ROOT is not writable."
    echo "  You must run $0 with sufficient permissions."
    usage
    exit 1
fi

# Prompt the user for SIGMA_ROOT, if necessary.
while [[ (! "$SIGMA_ROOT") || (! -d "$SIGMA_ROOT") ]]
do
cat << PROMPT2

  Please enter the complete pathname of the directory in
  which the Sigma KBs directory should be saved.  This
  could be your home directory or the Tomcat root
  directory, but need not be either.

PROMPT2

  read -p "  Pathname: " SIGMA_ROOT

  if [[ ! -d "$SIGMA_ROOT" ]]
    then 
      echo
      echo "  $SIGMA_ROOT is not a directory."
      echo "  Please try again."
  elif [[ ! -w "$SIGMA_ROOT" ]]
    then 
      echo
      echo "  $SIGMA_ROOT is not writable."
      echo "  You must run $0 with sufficient permissions."
      usage
      exit 1
  fi
done

TOMCAT_WEBAPPS_DIR="${TOMCAT_ROOT%\/}/webapps"

if [[ ! -d "$TOMCAT_WEBAPPS_DIR" ]]
  then
    cat << EOF3

  $TOMCAT_WEBAPPS_DIR is not a directory.  
  Please make sure that Tomcat has been correctly installed.

EOF3

  exit 1
fi

if [[ ! -w "$TOMCAT_WEBAPPS_DIR" ]]
  then
    cat << EOF4

  $TOMCAT_WEBAPPS_DIR is not writable.  
  You must run $0 with sufficient permissions.

EOF4

  usage
  exit 1
fi

# Remove [Tomcat]/webapps/sigma if it exists.
WEBAPPS_SIGMA_DIR="${TOMCAT_WEBAPPS_DIR%\/}/sigma"
if [[ (-d "$WEBAPPS_SIGMA_DIR") && (-w "$WEBAPPS_SIGMA_DIR") ]]
  then
    rm -rf "$WEBAPPS_SIGMA_DIR"
fi

# Remove [Tomcat]/work/Catalina/localhost/sigma if it exists.
WORK_SIGMA_DIR="${TOMCAT_ROOT%\/}/work/Catalina/localhost/sigma"
if [[ (-d "$WORK_SIGMA_DIR") && (-w "$WORK_SIGMA_DIR") ]]
  then
    rm -rf "$WORK_SIGMA_DIR"
fi

# Rename an existing Sigma web apps archive, if possible.
SIGMA_WAR="${TOMCAT_WEBAPPS_DIR%\/}/sigma.war"
if [[ -w "$SIGMA_WAR" ]]
  then
    mv "$SIGMA_WAR" "${SIGMA_WAR}.old"
fi

# Copy the Sigma web apps archive.
cp sigma.war "$SIGMA_WAR"
if [[ -e "$SIGMA_WAR" ]]
  then
    chmod a+rw "$SIGMA_WAR"
    echo
    echo "  Wrote ${SIGMA_WAR}."
  else
    echo
    echo "  Could not write ${SIGMA_WAR}."
    exit 1
fi

# Create the Sigma KBs directory.
SIGMA_KBS_DIR="${SIGMA_ROOT%\/}/KBs"
if [[ ! -e "$SIGMA_KBS_DIR" ]]
  then
    mkdir "$SIGMA_KBS_DIR"
    chmod a+rw "$SIGMA_KBS_DIR"
fi

if [[ ! ((-e "$SIGMA_KBS_DIR") && (-w "$SIGMA_KBS_DIR")) ]]
  then
    cat << EOF5

  $SIGMA_KBS_DIR could not be created.  
  You must run $0 with sufficient permissions.

EOF5

  usage
  exit 1
fi

# Copy files from the install package KBs directory.
for fn in `ls ${PKG_KBS_DIR%\/}/*.kif \
              ${PKG_KBS_DIR%\/}/*.sense \
              ${PKG_KBS_DIR%\/}/*.exc \
              ${PKG_KBS_DIR%\/}/*.txt`
do
  TARGET_FN="${SIGMA_KBS_DIR%\/}/${fn#$PKG_KBS_DIR\/}"
  if [[ (-e "$TARGET_FN") && (-w "$TARGET_FN") ]]
    then
      mv $TARGET_FN "${TARGET_FN}.old"
  fi
  cp $fn $TARGET_FN
  chmod a+rw $TARGET_FN
  if [[ -r "$TARGET_FN" ]]
    then
      echo "  Wrote ${TARGET_FN}."
    else
      echo "  Could not write ${TARGET_FN}."
      exit 1
  fi
done

# This is the input directory in the sense that this is
# the directory from which the tests will be used as
# input for inference testing.
TESTS_INPUT_DIR="${SIGMA_KBS_DIR%\/}/tests"
if [[ ! -d "$TESTS_INPUT_DIR" ]]
  then
    mkdir "$TESTS_INPUT_DIR"
    chmod a+rw "$TESTS_INPUT_DIR"
fi

if [[ ! ((-d "$TESTS_INPUT_DIR") && (-w "$TESTS_INPUT_DIR")) ]]
  then
    cat << EOF22

  $TESTS_INPUT_DIR could not be created.  
  You must run $0 with sufficient permissions.

EOF22

  usage
  exit 1
fi

# Copy test files, if any.
if [[ -d "$PKG_TESTS_DIR" ]]
  then
    for fn in `ls ${PKG_TESTS_DIR%\/}/*.tq`
    do
      TARGET_FN="${TESTS_INPUT_DIR%\/}/${fn#$PKG_TESTS_DIR\/}"
      if [[ (-e "$TARGET_FN") && (-w "$TARGET_FN") ]]
        then
          mv $TARGET_FN "${TARGET_FN}.old"
      fi
      cp $fn $TARGET_FN
      chmod a+rw $TARGET_FN
      if [[ -r "$TARGET_FN" ]]
        then
          echo "  Wrote ${TARGET_FN}."
        else
          echo "  Could not write ${TARGET_FN}."
          exit 1
      fi
    done
fi

# Check the permissions of the kif-linux executable.
KIF_EXEC="${PKG_INFERENCE_DIR%\/}/kif-linux"
if [[ ! -x "$KIF_EXEC" ]]
  then
    chmod a+x "${KIF_EXEC}"
fi

# Create the initial configuration file.
CFG_FILE="${SIGMA_KBS_DIR%\/}/config.xml"
if [[ -e "$CFG_FILE" ]]
  then
    mv "$CFG_FILE" "${CFG_FILE}.old"
fi

cat > "$CFG_FILE" << EOF42
<configuration>
  <preference value="${SIGMA_ROOT%\/}" name="baseDir" />
  <preference value="${SIGMA_KBS_DIR%\/}" name="kbDir" />
  <preference value="${KIF_EXEC}" name="inferenceEngine" />
  <preference value="${SIGMA_KBS_DIR%\/}/tests" name="inferenceTestDir" />
  <preference value="${WEBAPPS_SIGMA_DIR%\/}/tests" name="testOutputDir" />
  <preference value="yes" name="typePrefix" />
  <preference value="no" name="holdsPrefix" />
  <preference value="yes" name="cache" />
  <preference value="yes" name="TPTP" />
  <preference value="" name="prolog" />
  <preference value="" name="celtdir" />
  <preference value="no" name="loadCELT" />
  <preference value="yes" name="showcached" />
  <preference value="" name="lineNumberCommand" />
  <preference value="" name="editorCommand" />
  <preference value="localhost" name="hostname" />
  <preference value="8080" name="port" />
  <preference value="SUMO" name="sumokbname" />
  <kb name="SUMO">
    <constituent filename="${SIGMA_KBS_DIR%\/}/Merge.kif" />
    <constituent filename="${SIGMA_KBS_DIR%\/}/english_format.kif" />
  </kb>
</configuration>

EOF42

if [[ -e "$CFG_FILE" ]]
  then
    chmod a+rw "$CFG_FILE"
    echo "  Wrote $CFG_FILE"
fi

echo
echo "  Sigma files installed!"
echo

exit 0

