#tabstop=4
##======================================================================/
## File:   $Id$
## Author: Fred Dushin <fadushin@syr.edu>
## Date:   $Date$
##
## Description:  
##
##
##----------------------------------------------------------------------/
## Copyright (C) 1998  Fred Dushin
## 
## This library is free software; you can redistribute it and/or
## modify it under the terms of the GNU Library Public License
## as published by the Free Software Foundation; either version 2
## of the License, or (at your option) any later version.
## 
## This library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU Library Public License for more details.
##======================================================================/

SRCROOTDIR = ../..

include $(SRCROOTDIR)/rules.mk

PACKAGENAME = jpl
PACKAGEPATH = jpl

SOURCES= \
	JPLException.java \
	PrologException.java \
	QueryInProgressException.java \
	Term.java \
	Atom.java \
	Integer.java \
	Long.java \
	Float.java \
	Variable.java \
	String.java \
	Compound.java \
	List.java \
	Tuple.java \
	Query.java \
	Version.java \
	JPL.java \
	Util.java


one: $(SOURCES:.java=.o)

all:
	cd fli; make all
	$(JCC) $(SOURCES)
	touch $(SOURCES:.java=.o)

clean:
	cd fli; make clean
	rm -f $(SOURCES:.java=.o)

remove:
	cd fli; make remove
	rm -f $(SOURCES:.java=.o)
	rm -rf $(CLASSDESTDIR)/$(PACKAGENAME)

build: clean remove all
