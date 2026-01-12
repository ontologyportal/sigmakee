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

SRCROOTDIR = ../../..

include $(SRCROOTDIR)/rules.mk

PACKAGENAME = jpl.fli
PACKAGEPATH = jpl/fli

SOURCES= \
	IntHolder.java \
	LongHolder.java \
	PointerHolder.java \
	DoubleHolder.java \
	StringHolder.java \
	term_t.java \
	atom_t.java \
	functor_t.java \
	predicate_t.java \
	qid_t.java \
	fid_t.java \
	module_t.java \
	Prolog.java \

one: $(SOURCES:.java=.o)

all:
	$(JCC) $(SOURCES)
	touch $(SOURCES:.java=.o)

clean:
	rm -f $(SOURCES:.java=.o)

remove:
	rm -rf $(CLASSDESTDIR)/$(PACKAGEPATH)

build: clean remove all
