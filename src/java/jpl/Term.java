/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;
import jpl.fli.*;


//----------------------------------------------------------------------/
// Term
/**
 * A Term is a base class for the many different kinds of Term
 * (Atom, Variable, Compound, etc.).  Programmers do not create
 * instances of Term classes directly; rather, they should create
 * instances of Term subclasses (Atom, Variable, Compound, etc.),
 * which will inherit functionality from this class.
 * 
 * <hr><i>
 * Copyright (C) 1998  Fred Dushin<p>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.<p>
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library Public License for more details.<p>
 * </i><hr>
 * @author  Fred Dushin <fadushin@syr.edu>
 * @version $Revision$
 */
// Implementation notes:  
// 
//----------------------------------------------------------------------/
public abstract class Term
{
	//==================================================================/
	//  Attributes
	//==================================================================/


	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	//------------------------------------------------------------------/
	// Term
	/**
	 * This default constructor is provided in order for subclasses
	 * to be able to define their own default constructors.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected
	Term()
	{
	}


	//==================================================================/
	//  Converting Terms to term_ts
	// 
	// To convert a Term to a term_t, we need to traverse the Term
	// structure and build a corresponding Prolog term_t object.
	// There are some issues:
	// 
	// - Prolog term_ts rely on the *consecutive* nature of term_t
	//   references.  In particular, to build a compound structure
	//   in the Prolog FLI, one must *first* determine the arity of the
	//   compound, create a *sequence* of term_t references, and then
	//   put atoms, functors, etc. into those term references.  We
	//   do this in these methods first determinint the arity of the
	//   Compound, and then by "puting" a type into a term_t.
	//   The "put" methd is defined differently for each Term subclass.
	// 
	// - What if we are trying to make a term_t from a Term, but the
	//   Term has multiple instances of the same Variable?  We want
	//   to ensure that one Prolog variable will be created, or else
	//   queries will give incorrect answers.  We thus pass a Hashtable
	//   (var_table) through these methods.  The table contains term_t 
	//   instances, keyed on Variable instances.
	//==================================================================/

	//------------------------------------------------------------------/
	// put
	/**
	 * Cache the reference to the Prolog term_t here.
	 * 
	 * @param   var_table  A Hashtable containing term_t's that are
	 * bound to (have been put in) Prolog variables as elements.  The
	 * Elements are keyed by jpl.Variable instances.  Cf. the put()
	 * implementation in the Variable class.
	 * @param   term  A (previously created) term_t which is to be
	 * put with a Prolog term-type appropriate to the Term type
	 * (e.g., Atom, Variable, Compound, etc.) on which the method is
	 * invoked.)
	 */
	// Implementation notes:  This method is over-ridden in each of
	// the Term's subclasses, but it is always called from them, as well.
	//------------------------------------------------------------------/
	protected abstract void
	put( Hashtable var_table, term_t term );


	//------------------------------------------------------------------/
	// terms_to_term_ts
	/**
	 * This static method converts an array of Terms to a *consecutive* 
	 * sequence of term_t objects.  Note that the first term_t object
	 * returned is a term_t class (structure); the succeeding term_t
	 * objects are consecutive references obtained by incrementing the
	 * *value* field of the term_t.
	 * 
	 * @param   var_table  A Hashtable containing term_t's that are
	 * bound to (have be put in in) Prolog variables as elements.  The
	 * Elements are keyed by jpl.Variable instances.  Cf. the put()
	 * implementation in the Variable class.
	 * @param   arg   An array of jpl.Term references.
	 * @return  consecutive term_t references (first of which is
	 * a structure)
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static term_t
	terms_to_term_ts( Hashtable var_table, Term arg[] )
	{
		// 
		// first create a sequence of term_ts.  The 0th term_t
		// will be a jpl.fli.term_t.  Successive Prolog term_t 
		// references will reside in the Prolog engine, and
		// can be obtained by term0.value+i.
		// 
		term_t term0 = Prolog.new_term_refs( arg.length );
		
		// 
		// for each new term reference, construct a prolog term
		// by puting an appropriate Prolog type into the reference.
		// This is more or less the protocol for building terms in
		// Prolog.
		// 
		long ith_term_t = term0.value;
		for ( int i = 0;  i < arg.length;  ++i, ++ith_term_t ){			
			term_t term = new term_t();
			term.value  = ith_term_t;
			arg[i].put( var_table, term );
		}
		
		return term0;
	}
	
	//==================================================================/
	//  Converting term_ts to Terms
	// 
	// Converting back to Terms from term_ts is complex; one
	// issue is that there is not as much type information in a
	// term_t as one would expect.  We can learn that a term_t
	// is a compund term, but not, for example, that it is a list
	// or a tuple.  In this case, we must inspect the name of the
	// term_t ("." = list; "," = tuple).
	// 
	// Another problem concerns variable bindings.  We illustrate
	// with several examples.  First, consider the prolog fact
	// 
	//     p( f( X, X ) ).
	// 
	// And the query ?- p( Y ).  A solution should be y = f( X, X ),
	// and indeed, if this query is run, the term_t to which Y will
	// be unified is a compound, f( X, X ).  The problem is, how do
	// we know, in converting the term_ts to Terms in the compound f
	// whether we should create one Variable or two?  This begs the
	// question, how do we _identify_ Variables in JPL?  The answer
	// to the latter question is, by reference; two Variable (java) 
	// references refer to the same variable iff they are, in memory,
	// the same Variable.  That is, they satisfy the Java == relation.
	// (Note that this condition is _not_ true of the other Term types.)
	// 
	// Given this design decision, therefore, we should create a
	// single Variable instance and a Compound instance whose 2 arg
	// values point to the same Variable.  We therefore need to keep
	// track, in converting a term_t to a Term (in particular, in
	// converting a term_t whose type is variable to a Variable), of
	// which Variables have been created.  We do this by using the vars
	// Hashtable, which gets passed recursively though the from_term_t
	// methods; this table holds the Variable instances that have been
	// created, keyed by the unique and internal-to-Prolog string
	// representation of the variable.
	//==================================================================/

	//------------------------------------------------------------------/
	// from_term_t
	/**
	 * This method calls from_term_t on each term in the consecutive list
	 * of term_ts.  A temporary jpl.term_t structure must be created
	 * in order to extract type information from the Prolog engine.
	 * 
	 * @param   vars      A Hashtable containing jpl.Variable instances
	 * as elements, indexed by their *Prolog* names, which are guaranteed
	 * to be unique.  Cf. the from_term_t method of the Variable class.
	 * @param   n         The number of consecutive term_ts
	 * @param   term0     The 0th term_t (structure); subsequent
	 *                    term_ts are not structures.
	 * @return            An array of converted Terms
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static Term[]
	from_term_t( Hashtable vars, int n, term_t term0 )
	{
		// create an array on n term references
		Term rval[] = new Term[n];
		
		// 
		// for each term_t (from 0...n-1), create a term_t
		// (temporary) structure and dispatch the translation
		// to a Term to the static from_term_t method of the Term
		// class.  This will perform (Prolog) type analysis on the
		// term_t and call the appropriate static method to create
		// a Term of the right type (e.g., Atom, Variable, List, etc.)
		// 
		long ith_term_t = term0.value;
		for ( int i = 0;  i < n;  ++i, ++ith_term_t ){
			term_t term = new term_t();
			term.value = ith_term_t;
			
			rval[i] = Term.from_term_t( vars, term );
		}
		
		return rval;
	}
	
	//------------------------------------------------------------------/
	// from_term_t
	/**
	 * We do some type analysis on the term_t then forward the
	 * call to the appropriate class
	 * 
	 * @param   vars      A Hashtable containing jpl.Variable instances
	 * as elements, indexed by their *Prolog* names, which are guaranteed
	 * to be unique.  Cf. the from_term_t method of the Variable class.
	 * @param   term  The term_t to convert
	 * @return        The converted class.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static Term
	from_term_t( Hashtable vars, term_t term )
	{
		int type = Prolog.term_type( term );
		
		switch( type ){
		case Prolog.VARIABLE:
			return Variable.from_term_t( vars, term );
		case Prolog.ATOM:
			return Atom.from_term_t( vars, term );
		case Prolog.INTEGER:
			return Integer_JPL.from_term_t( vars, term );
		case Prolog.FLOAT:
			return Float_JPL.from_term_t( vars, term );
		case Prolog.STRING:
			return String_JPL.from_term_t( vars, term );
		case Prolog.TERM:
			return Compound.from_term_t( vars, term );
		default:
			System.err.println( "Term.from_term_t: unknown term type" );
			return null;
		}
	}
	
	//==================================================================/
	//  Computing Substitutions
	// 
	// Once a solution has been found, the Prolog term_t references
	// will have been unified and will refer to new terms.  To compute
	// a substitution, we traverse the (original) Term structure, looking
	// at the term_t reference in the Term.  The only case we really care
	// about is if the (original) Term is a Variable; if so, the term_t
	// back in the Prolog engine contains the unified term.  In this case,
	// we can store this term in a Hashtable, keyed by the Variable with
	// which the term was unified.
	//==================================================================/


	//------------------------------------------------------------------/
	// computeSubstitution
	/**
	 * This method computes a substitution from a Term.  The bindings
	 * Hashtable stores Terms, keyed by Variables.  Thus, a
	 * substitution is as it is in mathematical logic, a sequence
	 * of the form \sigma = {t_0/x_0, ..., t_n/x_n}.  Once the
	 * substitution is computed, the substitution should satisfy
	 * 
	 *   \sigma T = t
	 * 
	 * where T is the Term from which the substitution is computed,
	 * and t is the term_t which results from the Prolog query.<p>
	 * 
	 * A second Hashtable, vars, is required; this table holds
	 * the Variables that occur (thus far) in the unified term.
	 * The Variable instances in this table are guaranteed to be
	 * unique and are keyed on Strings which are Prolog internal
	 * representations of the variables.
	 * 
	 * @param   bingings  table holding Term substitutions, keyed on
	 * Variables.
	 * @param   vars  A Hashtable holding the Variables that occur
	 * thus far in the term; keyed by internal (Prolog) string rep.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected abstract void
	computeSubstitution( Hashtable bindings, Hashtable vars );
	
	//------------------------------------------------------------------/
	// computeSubstitutions
	/**
	 * Just calls computeSubstitution in each Term in the list.
	 * 
	 * @param   table  table holding Term substitutions, keyed on
	 *                 Variables.
	 * @param   arg    a list of Terms
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static void
	computeSubstitutions( Hashtable bindings, Hashtable vars, Term arg[] )
	{
		for ( int i = 0;  i < arg.length;  ++i ){
			arg[i].computeSubstitution( bindings, vars );
		}		
	}
	
	//------------------------------------------------------------------/
	// terms_equals
	/**
	 * This method is used to determine the Terms in two Term arrays
	 * are pairwise equal, where two Terms are equal if they satisfy
	 * the equals predicate (defined differently in each Term subclass).
	 * 
	 * @param   t1  an array of Terms
	 * @param   t2  an array of Terms
	 * @return  true if all of the Terms in the arrays are pairwise equal
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static boolean
	terms_equals( Term[] t1, Term[] t2 )
	{
		if ( t1.length != t2.length ){
			return false;
		}
		
		for ( int i = 0;  i < t1.length;  ++i ){
			if ( !t1[i].equals( t2[i] ) ){
				return false;
			}
		}
		return true;
	}
	
// 	//------------------------------------------------------------------/
// 	// equals
// 	/**
// 	 * Terms (Variables, actually) are keys in Hashtables.  This
// 	 * method overrides the Object implementation so that 
// 	 * 
// 	 * @param   obj  The Object to compare.
// 	 * @return  true if the Object is the same as this or if
// 	 * the Object's term_t is equal to this's.
// 	 */
// 	// Implementation notes:  I'm not sure if this is needed any more...
// 	// 
// 	//------------------------------------------------------------------/
// 	public boolean
// 	equals( Object obj )
// 	{
// 		if ( this == obj ){
// 			return true;
// 		}
// 		
// 		if ( ! (obj instanceof Term) ){
// 			return false;
// 		}
// 		
// 		if ( term == null || ((Term)obj).term == null ){
// 			return false;
// 		}
// 		
// 		return this.term.equals( ((Term)obj).term );
// 	}

	//------------------------------------------------------------------/
	// toString
	/**
	 * Converts a list of Terms to a String.
	 * 
	 * @param   arg    An aaray of Terms to convert
	 * @return  String representation of a list of Terems
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static java.lang.String
	toString( Term arg[] )
	{
		java.lang.String s = "";
		
		for ( int i = 0;  i < arg.length;  ++i ){
			s += arg[i].toString();
			if ( i != arg.length -  1 ){
				s += ", ";
			}
		}
		
		return s;
	}
	
	public abstract java.lang.String
	debugString();
	
	public static java.lang.String
	debugString( Term arg[] )
	{
		java.lang.String s = "[";
		
		for ( int i = 0;  i < arg.length;  ++i ){
			s += arg[i].debugString();
			if ( i != arg.length -  1 ){
				s += ", ";
			}
		}
		
		return s + "]";
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
