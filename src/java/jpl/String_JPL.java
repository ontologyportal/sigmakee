/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;
import jpl.fli.*;

//----------------------------------------------------------------------/
// String
/**
 * A String is a Term with a java.lang.String value.  Use this 
 * class to represent Prolog strings.
 * <pre>
 * jpl.String s = new jpl.String( "Haddock's Eyes" );
 * </pre>
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
public class String_JPL
extends      Term
{
	//==================================================================/
	//  Attributes
	//==================================================================/

	/**
	 * the String's value
	 */
	java.lang.String value_ = null;
	
	/**
	 * @return the String's value
	 */
	public final java.lang.String
	value()
	{
		return value_;
	}
		
	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	//------------------------------------------------------------------/
	// String
	/**
	 * A String holds a java.lang.String and represents a Prolog String,
	 * whose internal representation may vary across Prolog implementations.
	 * 
	 * @param   value  the java.lang.String value of this String.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	String_JPL( java.lang.String value )
	{
		this.value_ = value;
	}

	//==================================================================/
	//  Converting Terms to term_ts
	//==================================================================/

	//------------------------------------------------------------------/
	// put
	/**
	 * To put an String in a term, we put the characters of the 
	 * String into the term_t.
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
	// Implementation notes: 
	// 
	//------------------------------------------------------------------/
	protected final void
	put( Hashtable var_table, term_t term )
	{
		Prolog.put_string_chars( term, value_ );
	}

	
	//==================================================================/
	//  Converting term_ts to Terms
	//==================================================================/
	
	//------------------------------------------------------------------/
	// from_term_t
	/**
	 * Converts a term_t to a String.  Assuming the term is an
	 * Prolog String, we just create a new String using the 
	 * characters in the Prolog String.
	 * 
	 * @param   term     The term_t to convert
	 * @return           A new String
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected static Term
	from_term_t( Hashtable vars, term_t term )
	{
		StringHolder holder = new StringHolder();
		Prolog.get_string( term, holder );
		
		return new String_JPL( holder.value );
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


	//==================================================================/
	//  
	//==================================================================/

	
	public java.lang.String
	toString()
	{
		return value_;
	}
	
	public java.lang.String
	debugString()
	{
		return "(String " + toString() + ")";
	}

	
	//------------------------------------------------------------------/
	// equals
	/**
	 * Two Strings are equal if their values are equal
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
		
		if ( ! (obj instanceof String_JPL) ){
			return false;
		}
		
		return value_.equals( ((String_JPL)obj).value_ );
	}}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
