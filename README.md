# Build status

![badge](https://github.com/ontologyportal/sigmakee/actions/workflows/ant.yml/badge.svg)

# Test status

![badge](https://github.com/ontologyportal/sigmakee/actions/workflows/test-report.yml/badge.svg)


[Version notes](https://github.com/ontologyportal/sigmakee/wiki/Version-notes)

# Introduction

Sigma is an integrated development environment for logical theories that extend the Suggested Upper Merged Ontology (SUMO).  There is a public installation with read-only functions enabled linked from http://www.ontologyportal.org - click on the "Browse" link.

# Installation Instructions
## Recommended Methods
* [Linux installation](https://github.com/ontologyportal/sigmakee/wiki/Linux-installation)
* [MacOS installation](https://github.com/ontologyportal/sigmakee/wiki/MacOS-installation)
* [Windows installation](https://github.com/ontologyportal/sigmakee/wiki/Windows-installation)

## Other methods (power users)
* [Container-Based installation (Docker)](https://github.com/ontologyportal/sigmakee/wiki/Container%E2%80%90Based-installation)
* [Vagrant Virtual Machine installation](https://github.com/ontologyportal/sigmakee/wiki/Vagrant-Virtual-Machine-installation)
* [Old Installation Notes (deprecated)](https://github.com/ontologyportal/sigmakee/wiki/Old-Installation-notes)

## Miscellaneous
* [Optional Vampire manual installation](https://github.com/ontologyportal/sigmakee/wiki/Vampire-installation)
* [Common problems with installation](https://github.com/ontologyportal/sigmakee/wiki/Common-problems-with-installation)

After installing, recommended reading is the Sigma manual
* [Sigma Manual](https://github.com/ontologyportal/sigmakee/blob/master/doc/manual/SigmaManual.pdf)

There is a video on installing Sigma, as well as many others about related tools at
* [Ontology Talk - Sigma Installation](https://www.youtube.com/playlist?list=PLpBQIgki3izeUmFD8c65INdmxRNjjxOzP)


# Running Sigma
To run, simply:
```sh
startup.sh
```
Point your browser to [http://localhost:8080/sigma/login.html](http://localhost:8080/sigma/login.html)

Default credentials are: admin/admin

For other tips and debugging techniques:
* [Running Sigma](https://github.com/ontologyportal/sigmakee/wiki/Running-Sigma)


## jEdit Installation
It is highly recommended to build Ontologies with the SUMO plugin for jEdit. This plugin does text highlighting, syntax and type checking, and other features useful for building Ontologies. 
* [Install SUMO plugin for jEdit](https://github.com/ontologyportal/SUMOjEdit)

### jEdit Integration with Sigmakee (optional)
jEdit can additionally be integrated into Sigmakee, which will allow you to click on a source file and line number for a statement in the Sigma Browse page and be taken to the editor, open on that line.
* [jEdit Sigmakee integration](https://github.com/ontologyportal/sigmakee/wiki/jEdit-Integration-with-Sigma)


## jUnit testing on the command line
* [jUnit testing on the command line](https://github.com/ontologyportal/sigmakee/wiki/jUnit-Testing-on-the-Command-Line)

## RESTful Interface
* See the [SigmaRest project](https://github.com/ontologyportal/SigmaRest)


## Python Interface
* [Python Interface](https://github.com/ontologyportal/sigmakee/wiki/Python-Interface)


## Account Management
* [Account Management](https://github.com/ontologyportal/sigmakee/wiki/Account-Management)

## Netbeans IDE
* [Working with Netbeans](https://github.com/ontologyportal/sigmakee/wiki/Working-with-NetBeans-IDE)
