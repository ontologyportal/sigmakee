/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;
import jpl.fli.*;

//----------------------------------------------------------------------/
// Variable
/**
 * This class provides a Java represenation of a Prolog Variable.<p>
 * 
 * In a sense, the Variable is the only interesting class in the
 * jpl High-Level Interface, though it is also the most difficult
 * to reason about.
 * 
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
public class Variable
extends      Term
{
	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	/**
	 * A reference to the term_t (a reference to a term in
	 * to Prolog Engine) to which this term is bound.  This
	 * reference is only used in the course of translating a
	 * Term to a term_t.
	 */
	protected transient term_t term_ = null;

	//------------------------------------------------------------------/
	// Variable
	/**
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	Variable()
	{
	}

	//==================================================================/
	//  Converting Terms to term_ts
	//==================================================================/

	//------------------------------------------------------------------/
	// put
	/**
	 * To put a Variable, we want to be careful that the variable
	 * does not already occur in the Term.  If it does, we have
	 * already thrown it as a key in the hastable, so we can look
	 * it up and use the term_t indexed by the Variable as the
	 * term to put; this way, the term_t in the Prolog Engine 
	 * refers to the same term_t as any other occurrence of the
	 * same variable.  On the other hand, if the variable does not
	 * (already) occur in the Term, put it in the hashtable and
	 * put a new variable into the term.
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
		term_t t = (term_t) var_table.get( this );
		if ( t == null ){
			this.term_ = term;
			Prolog.put_variable( term );
			var_table.put( this, term );
		} else {
			this.term_ = t;
			Prolog.put_term( term, t );
		}
	}

	//==================================================================/
	//  Converting term_ts to Terms
	//==================================================================/
	
	//------------------------------------------------------------------/
	// from_term_t
	/**
	 * Converts a term_t to a Variable.  If the term_t is a
	 * variable, we just make a new Variable.
	 *
	 * We are careful to create a List.Nil object if indeed the
	 * atom is "[]".  Note that nil is an atom in Prolog and jpl.
	 * 
	 * @param   term     The term_t to convert
	 * @return           A new Variable
	 */
	// Implementation notes:  Is this right?  Couldn't there be
	// a Prolog formula with more than one term_t, but where
	// both refer to the same variable?  Someone help!
	//------------------------------------------------------------------/
	protected static Term
	from_term_t( Hashtable vars, term_t term )
	{
		StringHolder holder = new StringHolder();
		
		Prolog.get_chars( term, holder, Prolog.CVT_VARIABLE );
		
		Variable var = (Variable) vars.get( holder.value );
		if ( var == null ){
			var = new Variable();
			var.term_ = term;
			vars.put( holder.value, var );
		}
		return var;
	}

	//==================================================================/
	//  Computing Substitutions
	//==================================================================/


	//------------------------------------------------------------------/
	// computeSubstitution
	/**
	 * If this Variable instance is not already in the Hashtable,
	 * put the result of converting the term_t to which this variable
	 * has been unified to a Term in the Hashtable, keyed on this
	 * Variable instance.
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
		if ( bindings.get( this ) == null ){
			bindings.put( this, Term.from_term_t( vars, this.term_ ) );
		}
	}


	
	//------------------------------------------------------------------/
	// equals
	/**
	 * A Variable is equal to another when their corresponding term_ts
	 * (internal Prolog represenations) are equal.
	 * 
	 * @param   obj  The Object to compare.
	 * @return  true if the Object is the above condition applies.
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
		
		if ( ! (obj instanceof Variable) ){
			return false;
		}
		
		if ( term_ == null || ((Variable)obj).term_ == null ){
			return false;
		}
		
		return term_.equals( ((Variable)obj).term_ );
	}
	
	public java.lang.String
	debugString()
	{
		return "(Variable " + toString() + ")";
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
