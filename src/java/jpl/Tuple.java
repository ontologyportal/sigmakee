/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;



//----------------------------------------------------------------------/
// Tuple
/**
 * A Tuple is a Compound, used to represent Prolog tuples, the closest
 * thing Prolog has to a data structure.
 * <pre>
 * Tuple triple = 
 *     new Tuple(
 *         jpl.Util.toTermArray(
 *             new Atom( "a" ),
 *             new Atom( "b" ),
 *             new Atom( "c" ) ) );
 * </pre>
 *
 * This constructor (using the jpl.Util class for convenience)
 * creates a triple corresponding to the Prolog tuple (a,b,c).<p>
 * 
 * The Tuple.Pair class can be used to create 2-element tuples.
 * Use the let() method to obtain a reference to the ith (starting
 * from 0) element in the Tuple.
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
 * @see jpl.Compound
 */
// Implementation notes:
//    
//    Tuples, in Prolog, are actually binary terms.  Internally, the
//    Prolog Tuple (a,b,c,d,e) is represented as follows: 
//    
//                           ,
//                          / \
//                         /   \
//                        a     ,
//                             / \
//                            /   \
//                           c     \
//                                  ,
//                                 / \
//                                /   \
//                               d     e
//    
//    That is, the ',' functor in Prolog is a binary functor.
//    
//    
//    
//    
//    
//    
//    
//----------------------------------------------------------------------/
public class Tuple
extends      Compound
{
	//==================================================================/
	//  Attributes
	//==================================================================/

	/**
	 * @return the ith element in the tuple (starting from 0);
	 * null if the index is out of bounds
	 */
	public final Term
	elt( int i )
	{
		Tuple tuple = this;
		Term  kth   = this.args_[0];
		
		for ( int k = 0;  k < i;  ++k ){
			if ( tuple.args_[1] instanceof Tuple ){
				tuple = (Tuple) tuple.args_[1];
				kth   = tuple.args_[0];
			} else {

				if ( k == i - 1 ){
					kth = tuple.args_[1];
				} else {
					// out of bounds
					return null;
				}
			}
		}
		
		return kth;
	}

	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	// we only need one of these guys
	private static final Atom COMMA = new Atom( "," );
	
	private static Term[]
	tuple_chain( Term elts[] )
	{
		if ( elts.length < 2 ){
			throw new JPLException( "Error: A Tuple must have 2 or more elements" );
		} else if ( elts.length == 2 ){
			return elts;
		} else {
			return ((Tuple)tuple_chain( elts, 0 )).args_;
		}
	}
	
	private static Term
	tuple_chain( Term elts[], int i )
	{
		if ( i < elts.length - 1 ){
			Term args[] = new Term[2];
			
			args[0] = elts[i];
			args[1] = tuple_chain( elts, i+1 );
			return new Tuple( args );
		} else {
			return elts[i];
		}
	}

	//------------------------------------------------------------------/
	// Tuple
	/**
	 * Create a Tuple whose arity is determined by the size of the
	 * input array.
	 * 
	 * @param   elts the Terms in the Tuple
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	Tuple( Term elts[] )
	{
		super( COMMA, tuple_chain( elts ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1 ) )
	 * </pre>
	 * 
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Tuple( 
		Term t0,
		Term t1 )
	{
		this( 
			JPLUtil.toTermArray( t0, t1 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2 ) )
	 * </pre>
	 * 
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Tuple( 
		Term t0,
		Term t1,
		Term t2 )
	{
		this( 
			JPLUtil.toTermArray( t0, t1, t2 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3 ) )
	 * </pre>
	 * 
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Tuple( 
		Term t0,
		Term t1,
		Term t2,
		Term t3 )
	{
		this( 
			JPLUtil.toTermArray( t0, t1, t2, t3 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3, t4 ) )
	 * </pre>
	 * 
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
	Tuple( 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4 )
	{
		this( 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5 ) )
	 * </pre>
	 * 
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
	Tuple( 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5 )
	{
		this( 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6 ) )
	 * </pre>
	 * 
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
	Tuple( 
		Term t0,
		Term t1,
		Term t2,
		Term t3,
		Term t4,
		Term t5,
		Term t6 )
	{
		this( 
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7 ) )
	 * </pre>
	 * 
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
	Tuple( 
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
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6, t7 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7, t8 ) )
	 * </pre>
	 * 
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
	Tuple( 
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
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6, t7, t8 ) );
	}
	
	//------------------------------------------------------------------/
	// Tuple
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Tuple( 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7, t8, t9 ) )
	 * </pre>
	 * 
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
	Tuple( 
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
			JPLUtil.toTermArray( t0, t1, t2, t3, t4, 
			                  t5, t6, t7, t8, t9 ) );
	}

	
	
	//------------------------------------------------------------------/
	// toString
	/**
	 * @return the String representation of a Tuple, in this case,
	 * of the form (t0,...,tn-1).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public java.lang.String
	toString()
	{
		Tuple tuple = this;
		java.lang.String s = "(";
		
		while ( true ){
			s += tuple.args_[0] + ", ";
			if ( tuple.args_[1] instanceof Tuple ){
				tuple = (Tuple) tuple.args_[1];
			} else {
				return s + tuple.args_[1].toString() + ")";
			}
		}
		
	}
	
	public java.lang.String
	debugString()
	{
		return "(Tuple " + Term.debugString( args_ ) + ")";
	}


	//==================================================================/
	// Pair
	/**
	 * A Pair is a two-element Tuple
	 */
	// Implementation notes:
	// 
	//==================================================================/
	public static class
	Pair extends Tuple
	{
		public
		Pair( Term a, Term b )
		{
			super( JPLUtil.toTermArray( a, b ) );
		}
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
