/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;
import jpl.fli.*;

//----------------------------------------------------------------------/
// Compound
/**
 * A Compound is a base class for all of the compound class types
 * (e.g., List, Tuple, etc.), but it can also be instantiated to
 * produce, for example, any functional expression.  For example, to
 * produce the term f(a), one might write:
 * <pre>
 * Term[] arg = { new Atom( "a" ) };
 * Compound f = new Compound( new Atom( "f" ), arg );
 * </pre>
 * 
 * See the List and Tuple classes for common extensions of this class.
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
 * @see    jpl.Term 
 * @see    jpl.List 
 * @see    jpl.Tuple 
 */
// Implementation notes:  
// 
//----------------------------------------------------------------------/
public class Compound
extends      Term
{
	//==================================================================/
	//  Attributes
	//==================================================================/

	/**
	 * the atom in this Compound
	 */
	protected Atom atom_ = null;
	
	/**
	 * the arguments in this Compound
	 */
	protected Term[] args_ = null;

	/**
	 * @return the atom in this Compound
	 */
	public final Atom
	atom()
	{
		return atom_;
	}
	
	/**
	 * @return the arguments in this Compound
	 */
	public final Term[]
	args()
	{
		return args_;
	}
	
	/**
	 * @return the ith argument in this Compound
	 */
	public final Term
	ith( int i)
	{
		return args_[i];
	}


	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/


	//------------------------------------------------------------------/
	// Compound
	/**
	 * Creates a Comound with an Atom and an array of arguments.  The
	 * length of the array determines the arity of the Compound.
	 * 
	 * @param   atom  the Atom in this Compound
	 * @param   args  the arguments in this Compound
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	Compound( Atom atom, Term args[] )
	{
		this.atom_ = atom;
		this.args_ = args;
	}

	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is short for
	 * <pre>
	 * new Compound( new Atom( name ), arg )
	 * </pre>
	 * 
	 * @param   name   the name for the Atom in this Compound
	 * @param   args   the arguments in this Compound
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	Compound( java.lang.String name, Term args[] )
	{
		this( new Atom( name ), args );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 * @param   t4    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 * @param   t4    a jpl.Term
	 * @param   t5    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 * @param   t4    a jpl.Term
	 * @param   t5    a jpl.Term
	 * @param   t6    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5,
		Term t6 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 * @param   t4    a jpl.Term
	 * @param   t5    a jpl.Term
	 * @param   t6    a jpl.Term
	 * @param   t7    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5,
		Term t6,
		Term t7 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6, t7 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7, t8 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 * @param   t4    a jpl.Term
	 * @param   t5    a jpl.Term
	 * @param   t6    a jpl.Term
	 * @param   t7    a jpl.Term
	 * @param   t8    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5,
		Term t6,
		Term t7,
		Term t8 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6, t7, t8 ) );
	}
	
	//------------------------------------------------------------------/
	// Compound
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Compound( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7, t8, t9 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the functor in this Compound
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 * @param   t4    a jpl.Term
	 * @param   t5    a jpl.Term
	 * @param   t6    a jpl.Term
	 * @param   t7    a jpl.Term
	 * @param   t8    a jpl.Term
	 * @param   t9    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Compound( 
		java.lang.String name, 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5,
		Term t6,
		Term t7,
		Term t8,
		Term t9 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6, t7, t8, t9 ) );
	}
	
	
	
	//==================================================================/
	//  Converting Terms to term_ts
	//==================================================================/

	//------------------------------------------------------------------/
	// put
	/**
	 * To put an Compound in a term, we create a sequence of term_t
	 * references from the Term terms_to_term_t method, and then
	 * use the Prolog cons_functor_v method to create a Prolog compound.
	 * 
	 * @param   var_table  A Hashtable containing term_t's that are
	 * bound to (have packed in) Prolog variables as elements.  The
	 * Elements are keyed by jpl.Variable instances.  Cf. the put()
	 * implementation in the Variable class.
	 * @param   term  A (previously created) term_t which is to be
	 * packed with a Prolog term-type appropriate to the Term type
	 * (e.g., Atom, Variable, Compound, etc.) on which the method is
	 * invoked.)
	 */
	// Implementation notes:  This method is over-ridden in each of
	// the Term's subclasses, but it is always called from them, as well.
	//------------------------------------------------------------------/
	protected final void
	put( Hashtable var_table, term_t term )
	{
		term_t term0 = Term.terms_to_term_ts( var_table, args_ );
		
		Prolog.cons_functor_v( 
			term, 
			Prolog.new_functor( 
				Prolog.new_atom( atom_.name_ ),
				args_.length ), 
			term0 );
	}

	//==================================================================/
	//  Converting term_ts to Terms
	//==================================================================/
	
	//------------------------------------------------------------------/
	// from_term_t
	/**
	 * Converts a term_t to a Compound.  In this case, we create
	 * an Atom and a list of Terms by calling from_term_t for each
	 * term_t reference we get from Prolog.get_arg (Not sure why
	 * we couldn't get a sequence from there, but...).<p>
	 * 
	 * We have to do a bit of type analysis to create Java objects
	 * of the right type.  For example, we don't want to create just
	 * a Compound if the term_t is a list or a tuple; we want to
	 * create a List or a Tuple, accordingly.
	 * 
	 * @param   term     The term_t to convert
	 * @return           A new Atom
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static Term
	from_term_t( Hashtable vars, term_t term )
	{
		atom_t       atom         = new atom_t();
		IntHolder    arity_holder = new IntHolder();
		
		Prolog.get_name_arity( term, atom, arity_holder );
		java.lang.String atom_name    = Prolog.atom_chars( atom );
		
		Term arg[] = new Term[arity_holder.value];
		
		for ( int i = 1;  i <= arity_holder.value;  ++i ){
			term_t termi = Prolog.new_term_ref();
			
			Prolog.get_arg( i, term, termi );
			arg[i-1] = Term.from_term_t( vars, termi );
		}
		
		if ( atom_name.equals( "." ) ){
			return 
				new List( 
					arg[0], arg[1] );
		} else if ( atom_name.equals( "," ) ) {
			if ( arg.length == 2 ){
				return 
					new Tuple.Pair( arg[0], arg[1] );
			} else {
				return 
					new Tuple( arg );
			}
		} else {
			return 
				new Compound( 
					new Atom( atom_name ), 
					arg );
		}
	}

	//==================================================================/
	//  Computing Substitutions
	//==================================================================/


	//------------------------------------------------------------------/
	// computeSubstitution
	/**
	 * Nothing needs to be done except to pass the buck to the args.
	 * 
	 * @param   table  table holding Term substitutions, keyed on
	 *                 Variables.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected final void
	computeSubstitution( Hashtable bindings, Hashtable vars )
	{
		Term.computeSubstitutions( bindings, vars, args_ );
	}
	
	
	//------------------------------------------------------------------/
	// toString
	/**
	 * Converts a Compound to its String form, atom( arg_1, ..., arg_n )
	 * 
	 * @return  string representation of an Compound
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public java.lang.String
	toString()
	{
		return atom_.toString() + "( " + Term.toString( args_ ) + " )";
	}
	
	public java.lang.String
	debugString()
	{
		return 
			"(Compound " + 
				atom_.debugString()        + " " +
				Term.debugString( args_ ) + ")";
	}
	
	//------------------------------------------------------------------/
	// equals
	/**
	 * Two Compounds are equal if their atoms are equal and their
	 * term arguments are equal.
	 * 
	 * @param   obj  the Object to compare
	 * @return  true if the Object satisfies the above condition
	 */
	// Implementation notes:
	// 
	//------------------------------------------------------------------/
	public final boolean
	equals( Object obj )
	{
		if ( this == obj ){
			return true;
		}
		
		if ( ! (obj instanceof Compound) ){
			return false;
		}
		
		return 
			atom_.equals( ((Compound)obj).atom_ ) &&
			Term.terms_equals( args_, ((Compound)obj).args_ );
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
