/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;
import jpl.fli.*;

//----------------------------------------------------------------------/
// Integer
/**
 * An Integer is a Term with an int field.  Use this class to create
 * Java representation of Prolog integers:
 * <pre>
 * Integer i = new Integer( 1024 );
 * </pre>
 * 
 * An Integer can be used (and re-used) in Compound Terms.
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
 * @see jpl.Term
 * @see jpl.Compound
 */
// Implementation notes:  
// 
//----------------------------------------------------------------------/
public class Integer_JPL
extends      Term
{
	//==================================================================/
	//  Attributes
	//==================================================================/

	/**
	 * the Integer's value
	 */
	protected int value_;
	
	/**
	 * @return the Integer's value
	 */
	public final int
	value()
	{
		return value_;
	}

	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	//------------------------------------------------------------------/
	// Integer
	/**
	 * @param   value  This Integer's value
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	Integer_JPL( int value )
	{
		this.value_ = value;
	}

	//==================================================================/
	//  Converting Terms to term_ts
	//==================================================================/

	//------------------------------------------------------------------/
	// put
	/**
	 * To put an Integer in a term, we put an integer.
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
		Prolog.put_integer( term, value_ );
	}

	
	//==================================================================/
	//  Converting term_ts to Terms
	//==================================================================/
	
	//------------------------------------------------------------------/
	// from_term_t
	/**
	 * Converts a term_t to an Atom.  Assuming the term is an
	 * atom, we just create a new Atom using the term's name.
	 *
	 * We are careful to create a List.Nil object if indeed the
	 * atom is "[]".  Note that nil is an atom in Prolog and jpl.
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
		IntHolder int_holder = new IntHolder();

		Prolog.get_integer( term, int_holder );
		return new jpl.Integer_JPL( int_holder.value );
	}

	//==================================================================/
	//  Computing Substitutions
	//==================================================================/


	//------------------------------------------------------------------/
	// computeSubstitution
	/**
	 * Nothing needs to be done if the Term is an Atom.
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
	}

	
	
	//------------------------------------------------------------------/
	// toString
	/**
	 * Converts an Integer to its String form -- its value.
	 * 
	 * @return  String representation of an Integer
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public java.lang.String
	toString()
	{
		return "" + value_ + "";
	}
	
	public java.lang.String
	debugString()
	{
		return "(Integer " + toString() + ")";
	}
	
	//------------------------------------------------------------------/
	// equals
	/**
	 * Two Integers are equal if their values are equal
	 * 
	 * @param   obj  The Object to compare
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
		
		if ( ! (obj instanceof Integer_JPL) ){
			return false;
		}
		
		return value_ ==  ((Integer_JPL)obj).value_;
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
