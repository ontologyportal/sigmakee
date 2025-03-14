# Build status

![badge](https://github.com/ontologyportal/sigmakee/actions/workflows/ant.yml/badge.svg)

# Test status

![badge](https://github.com/ontologyportal/sigmakee/actions/workflows/test-report.yml/badge.svg)

# Notice
[Version notes](https://github.com/ontologyportal/sigmakee/wiki/Version-notes)

# Introduction

Sigma is an integrated development environment for logical theories that
extend the Suggested Upper Merged Ontology (SUMO).  There is a public installation
with read-only functions enabled linked from http://www.ontologyportal.org

# Installation Instructions
## Recommended Methods
* [Linux installation](https://github.com/ontologyportal/sigmakee/wiki/Linux)
* [MacOS installation](https://github.com/ontologyportal/sigmakee/blob/master/INSTALL.MacOS)
* [Windows installation](https://github.com/ontologyportal/sigmakee/wiki/Windows-installation)

## Other methods (power users)
* [Container-Based installation (Docker)](https://github.com/ontologyportal/sigmakee/wiki/Container%E2%80%90Based-installation)
* [Vagrant Virtual Machine installation](https://github.com/ontologyportal/sigmakee/wiki/Vagrant-Virtual-Machine-installation)
* [Old Installation Notes (depracated)](https://github.com/ontologyportal/sigmakee/wiki/Old-Installation-notes)

## Miscellaneous
* [Optional Vampire manual installation](https://github.com/ontologyportal/sigmakee/wiki/Vampire-installation)
* [Common problems with installation](https://github.com/ontologyportal/sigmakee/wiki/Common-problems-with-installation)

After installing, recommended reading is the Sigma manual
https://github.com/ontologyportal/sigmakee/blob/master/doc/manual/SigmaManual.pdf
There is a video on installing Sigma, as well as many others about related tools at
https://www.youtube.com/playlist?list=PLpBQIgki3izeUmFD8c65INdmxRNjjxOzP

## Running Sigma
Sigma is hosted on a Tomcat server. To start Tomcat, execute:
```sh
$CATALINA_HOME/bin/startup.sh
```
or simply
```sh
startup.sh
```
since $CATALINA_HOME/bin is on your PATH

Point your browser at http://localhost:8080/sigma/login.html\
Default credentials are: admin/admin



#### Test run
```sh
java -Xmx10g -Xss1m -cp $SIGMA_CP \
    com.articulate.sigma.KB -c Object Transaction
```


#### Debugging
- If login.html redirects you to init.jsp that means the system is still initializing.
Wait a minute or two and try again.
- If you get an initial login error, try turning off your network card and try again.
Some intranets block server requests that are not recognized.

If you want to monitor the server's condition and if it started successfully you can run:
```sh
tail -f $CATALINA_HOME/logs/catalina.out
```





## jEdit Integration (optional)

If you install [jEdit](http://jedit.org) and configure Sigma properly, you can click on a source\
file and line number for a statement in the Sigma Browse page and be taken to\
the editor, open on that line. By default, the edit feature will make use of your $ONTOLOGYPORTAL_GIT\
environment variable, and try to open the file in that location in the "sumo" module. Recommended practice\
is to edit .kif files in your local Git repository and then copy them to the .sigmakee/KBs directory. If\
you wish to edit in a different location, either a different repository or a different directory altogether,\
the you can set the editDir configuration variable in your config.xml file, for example

```xml
  <preference name="editDir" value="/home/user/workspace/myproject" />
```
If you wish to install jEdit so that there's no path to it, for example, on a shared machine as a\
user-specific program, you'll need to set a path in config.xml, for example:

```xml
  <preference name="jedit" value="/home/user/jedit/jedit" />
```

## jUnit testing on the command line

```sh
java -Xmx10g -Xss1m -cp $SIGMA_CP:\
  $SIGMA_SRC/build/test/classes \
  org.junit.runner.JUnitCore \
  com.articulate.sigma.UnitTestSuite
```

one test method at a time can be run with help from the SingleJUnitTestRunner class,
for example

```sh
java -Xmx10g -Xss1m -cp $SIGMA_CP:\
  $SIGMA_SRC/build/test/classes \
  com.articulate.sigma.SingleJUnitTestRunner \
  com.articulate.sigma.KbIntegrationTest#testIsChildOf3
```
You will have to edit the resources files that correspond to config.xml to conform to your
paths. They are in test/integration/java/resources/config*.xml and test/unit/java/resources/config*.xml

An alternative, and possibly easier way for command line invocation of unit
tests are to run these Ant tasks:

```sh
ant test.unit
ant test.integration
```

To run both of these in sequence with a single command
```sh
ant test
```

## RESTful Interface

see the [SigmaRest project](https://github.com/ontologyportal/SigmaRest)


## Python Interface

make sure you have python3

```sh
python --version
```

make sure you have pip or install with

```sh
sudo apt install python3-pip
```

install the py4j module

```sh
pip3 install py4j
```

Compile SigmaKEE then run with

```sh
java -Xmx10g -Xss1m -cp $SIGMA_CP com.articulate.sigma.KBmanager -p
```

then start python

```sh
user@user-machine:~/workspace/sigmakee$ python3
Python 3.8.2 (default, Jul 16 2020, 14:00:26)
[GCC 9.3.0] on linux

Type "help", "copyright", "credits" or "license" for more information.
>>> from py4j.java_gateway import JavaGateway
>>> gateway = JavaGateway()
>>> sigma_app = gateway.entry_point
>>> print(sigma_app.getTerms())

set([u'-1', u'-3', u'-6', u'-7235', u'.5', u'<=>', u'=>', u'AAA-Rating', u'AAM', u'AAV', u'ABPFn', u'ABTest', u'ACPowerSource', ...
```

Look at com.articulate.sigma.KBmanager.pythonServer() to expose the API of more classes
than just com.articulate.sigma.KB


## Account Management

Create the account database with

```sh
java -Xmx4g -cp $SIGMA_CP com.articulate.sigma.PasswordService -c
```

Then create the administrator account and password

```sh
java -Xmx4g -cp $SIGMA_CP com.articulate.sigma.PasswordService -a
```

Default user/pw: admin/admin

You can use Sigma without being administrator, but you'll have limited use of
its functionality.

You'll also need to set a parameter in your config.xml file. If you chose
user "admin", then your config.xml will need this line added.

```xml
  <preference name="dbUser" value="admin" />
```

To handle the account registration feature, you'll need to have an email account and supply the
password in the .bashrc file where your Sigma installation is runnning.  Gmail might be convenient
for this. Change the password "my_pass" to your password on Gmail (or other service that you specify)

```sh
export SIGMA_EMAIL_PASS="my_pass"
export SIGMA_EMAIL_SERVER="smtp.gmail.com"
```

There are three types of user roles: "guest", "user" and "admin".  Guests are users who have not
registered. They can access read-only functions that are not computationally expensive.
Registered users are granted access to computationally more expensive features. Admin users
have access to all Sigma functions. Currently, this control is hard coded into the JSP pages
that will check for user roles. At some point in the future this may be changed to a more flexible
scheme of access rights driven from a file or database mapping roles to allowed functions.

You'll need to start the database server with

```sh
 java -jar lib/h2-2.3.232.jar -webAllowOthers -tcpAllowOthers
```
and you'll need to change JDBCstring in PasswordService.java to your path instead of /home/apease
and recompile



## To build/run/debug/test using the NetBeans IDE
Define a nbproject/private/private.properties file with these keys:

\# private properties\
javaapis.dir=${user.home}/javaapis\
workspace=${javaapis.dir}/INSAFE

\# The default installation space is: ~/workspace. However, it can be anywhere on\
\# your system as long as you define the "workspace" key above.

catalina.home=${path.to.your.tomcat9}

private.resources.dir=nbproject/private/resources\
main.config=${private.resources.dir}/config.xml\
integration.config=${private.resources.dir}/config_topAndMid.xml\
unit.config=${private.resources.dir}/config_topOnly.xml

\# The above properties allow you to keep and restore the various forms of\
\# config.xml that get overwritten when running Unit Tests. Copy these files\
\# to the respective "resources" directory complete with your personal system\
\# paths replacing the "/home/theuser/" pseudos. config.xml is found in the\
\# base directory and the other two are found in test/*/resources directories

\# JavaMail properties\
user=${your.email.user.name}\
my.email=${user}@${your.email.domain}\
my.name=${your.name}


## User Interface

There's not enough documentation on Sigma so I'm starting a bit here in preparation for a real
manual.

Sigma has a number of functions controlled through its JSP-based interface.

* [AddConstituent.jsp](web/jsp/AddConstituent.jsp)
  - adds a constituent SUO-KIF file to a knowledge base.  Accessible only to
    admin users.  It just responds to a command from another page and has no UI of its own.
    Redirects back to KBs.jsp when done.

* [AllPictures.jsp](web/jsp/AllPictures.jsp)
  - shows all the pictures linked to a term at once.  Accessible to all users.

* [ApproveUser.jsp](web/jsp/ApproveUser.jsp)
  - handles approving a new user.  Accessible only to admin users. Redirects
  to KB.jsp once acknowledge.

* [AskTell.jsp](web/jsp/AskTell.jsp)
  - interface to the local theorem provers. Accessible only to admin users. This function
  has not been well maintained and the interfaces to SystemOnTPTP and LEO-II may be out of date.

* [BrowseBody.jsp](web/jsp/BrowseBody.jsp)
  - shows terms, axioms, lexicon links, etc

* [BrowseExtra.jsp](web/jsp/BrowseExtra.jsp)
  - includes Prelude.jsp and Postlude.jsp

* [BrowseHeader.jsp](web/jsp/BrowseHeader.jsp)
  - primary ontology browser controls including term and word search, as well as
  the menu for selecting natural language and formal language

* [Browse.jsp](web/jsp/Browse.jsp)
  - top level browsing JSP that includes Prelude.jsp, BrowseHeader.jsp, BrowseBody.jsp
  and Postlude.jsp.  Really just a shell for the included JSPs

* [CCheck.jsp](web/jsp/CCheck.jsp)
  - interface to KBmanager.initiateCCheck() that initiates consistency checking of a KB.
  Handles selection of which theorem prover to use, and several parameters.  Includes Prelude.jsp
  and Postlude.jsp

* [CELT.jsp](web/jsp/CELT.jsp)
  - Obsolete.  Handles invocation of the Controlled English to Logic Translation system,
  which is now superceded by the Semantic Rewriting approach in the sigmanlp project.

* [CreateUser.jsp](web/jsp/CreateUser.jsp)
  - handles a request from a user to create an account.  Creates a guest account
  and sends mail to the moderator for approval.  Has no UI.

* [Diag.jsp](web/jsp/Diag.jsp)
  - Interface to run tests in Diagnostics.java.  Depends on Prelude.jsp and Postlude.jsp.
  Accessible for "admin" and "user" but not unregistered guest users

* [EditFile.jsp](web/jsp/EditFile.jsp)
  - Obsolete

* [EditStmt.jsp](web/jsp/EditStmt.jsp)
  - Obsolete

* [Graph.jsp](web/jsp/Graph.jsp)
  - Create graphs of binary relationships as a graphical view and as indented text.
  Relies on Prelude.jsp and Postlude.jsp

* [InferenceTestSuite.jsp](web/jsp/InferenceTestSuite.jsp)
  - run a suite of inference tests with the prover and parameters selected.
  Depends on Prelude.jsp and Postlude.jsp.  Accessible only for admin users.

* [init.jsp](web/jsp/init.jsp)
  - A status page with periodic automatic refresh to catch requests to Sigma during the
  process of initialization.

* [InstFiller.jsp](web/jsp/InstFiller.jsp)
  - Page to allow simple editing of ground formulas.  Deprecated.  Accessible only
  for admin users. Depends on Prelude.jsp and Postlude.jsp.

* [Intersect.jsp](web/jsp/Intersect.jsp)
  - Find appearances of two or more terms in the same axiom.  Depends on
  Prelude.jsp and Postlude.jsp.

* [KBs.jsp](web/jsp/KBs.jsp)
  - Main entry point for creating or selecting a knowledge base.  Some functions are
  available for "admin" or "user" roles but not "guest". Depends on Prelude.jsp and Postlude.jsp.

* [login.html](web/jsp/login.html)
  - login page for Sigma that handles existing accounts and new account registration.
  Existing accounts are dispatched to login.jsp and registrations are dispatched to
  Registration.jsp

* [login.jsp](web/jsp/login.jsp)
  - handle the login process by using PasswordService.jsp to validate a login against
  an H2 database of account info.  This page has no UI.

* [Manifest.jsp](web/jsp/Manifest.jsp)
  - UI for handling loading and saving KB constituents including saving in various
  exportable formats, such as OWL. Depends on Prelude.jsp and Postlude.jsp. Most functionality
  is limited to admin users.

* [Mapping.jsp](web/jsp/Mapping.jsp)
  - Use some simple string matching approaches to suggest equivalences between terms
  in two files.  Depends on Prelude.jsp and Postlude.jsp. Access limited to admin users.

* [MiscUtilities.jsp](web/jsp/MiscUtilities.jsp)
  - Depends on Prelude.jsp and Postlude.jsp. Most functionality
  is limited to admin users. Little utilities to generate dot graph files and OWL versions of
  the Open Multilingual Wordnet content.

* [ModeratorApproval.jsp](web/jsp/ModeratorApproval.jsp)
  - Functionality is limited to admin users. Dispatches to ApproveUser.jsp
  when user clicks "ok".

* [OMW.jsp](web/jsp/OMW.jsp)
  - Display results from the many languages in Open Multilingual Wordnet linked to
  WordNet synsets. Depends on Prelude.jsp and Postlude.jsp.

* [OWL.jsp](web/jsp/OWL.jsp)
  - Display all the axioms for a given term in OWL format, if expressible in that
  language.

* [Postlude.jsp](web/jsp/Postlude.jsp)
  - Encapsulates footing information displayed on most pages.

* [Prelude.jsp](web/jsp/Prelude.jsp)
  - Encapsulates header information displayed on most pages.

* [ProcessFile.jsp](web/jsp/ProcessFile.jsp)
  - No UI. Called from MiscUtilities.jsp to generate KIF from other
  formats. Calls DocGen.dataFileToKifFile() to do the real work. Depends on Prelude.jsp and
  Postlude.jsp. Access limited to admin users.

* [Properties.jsp](web/jsp/Properties.jsp)
  - An interface for setting many of Sigma's parameters.  Some of these are
  also accessible from the browsing interface, such as the language for the natural
  language paraphrases. Depends on Prelude.jsp and Postlude.jsp. Access limited to admin users.

* [Register.jsp](web/jsp/Register.jsp)
  - An interface to allow people to submit a request for registration and
  priviledges beyond that of unregistered guest users.  Calls on CreateUser.jsp

* [Save.jsp](web/jsp/Save.jsp)
  - not used

* [SimpleBrowseBody.jsp](web/jsp/SimpleBrowseBody.jsp)
  - Analogue to BrowseBody.jsp but showing only simple axioms in a simple
  and non-technical language

* [SimpleBrowseHeader.jsp](web/jsp/SimpleBrowseHeader.jsp)
  - Analogue to BrowseHeader.jsp for simple axioms

* [SimpleBrowse.jsp](web/jsp/SimpleBrowse.jsp)
  - Analogue to Browse.jsp for simple axioms

* [SystemOnTPTP.jsp](web/jsp/SystemOnTPTP.jsp)
  - Interface to the SystemOnTPTP system hosted as U. Miami that collects
  dozens of theorem provers with a common programmatic interface.  This code has not been
  maintained so use at your own risk. Depends on Prelude.jsp and Postlude.jsp. Access
  limited to admin users.

* [TreeView.jsp](web/jsp/TreeView.jsp)
  - A simple tree view of the taxonomic structure of the ontology with collapsable
  nodes.  Uses the SimpleBrowse.jsp code to display axioms for any node in the taxonomy.

* [WNDiag.jsp](web/jsp/WNDiag.jsp)
  - Diagnostics for WordNet. Depends on Prelude.jsp and Postlude.jsp. Accessible
  for "admin" and "user" but not unregistered guest users.

* [WordNet.jsp](web/jsp/WordNet.jsp)
  - show synsets and SUMO-WordNet mappings for a word. Depends on Prelude.jsp
  and Postlude.jsp.

* [WordSenseFile.jsp](web/jsp/WordSenseFile.jsp)
  - show word sense disambiguation and sentiment analysis results for a
  file of text.  Calls WordNet.wn.sumoFileDisplay() for the real work. Depends on
  Prelude.jsp and Postlude.jsp.

* [WordSense.jsp](web/jsp/WordSense.jsp)
  - show word sense disambiguation and sentiment analysis results for a
  sentence.  Calls WordNet.wn.sumoSentenceDisplay() for the real work. Depends on
  Prelude.jsp and Postlude.jsp.
