/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;



//----------------------------------------------------------------------/
// List
/**
 * A List is a Compound, but with just two args, a head and a tail.
 * Use this class to create representations of Prolog lists:
 * <pre>
 * List el = 
 *     new List(
 *         new Atom( "a" ),
 *         new List(
 *             new Atom( "b" ),
 *             new List(
 *                 new Atom( "c" ),
 *                 List.NIL ) ) );
 * </pre>
 * This constructor (somewhat longwindedly) creates a List that 
 * corresponds to the Prolog list [a,b,c], or more accurately,
 * [a|[b|[c|[]]]].<p>
 * 
 * Note the following:
 * <menu>
 * <li> The special Term (static instance) List.NIL is of type
 *      List.Nil, which extends Atom, not List.  So the terminus
 *      of a List is an Atom, not a List.  This is consistent
 *      with the Prolog representation.</li>
 * <li> The List class provides 11 (convenience) list() methods for 
 *      creating Listing without the long-windedness of the above 
 *      constructor</li>
 * </menu>
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
public class List
extends      Compound
{
	/**
	 * @return the head of the List
	 */
	public final Term
	head()
	{
		return args_[0];
	}
	
	
	/**
	 * @return the tail of the List
	 */
	public final Term
	tail()
	{
		return args_[1];
	}
	
	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	// We only need one reference to this guy
	private static final Atom DOT = new Atom( "." );

	//------------------------------------------------------------------/
	// List
	/**
	 * This constructor is used to create a List.  Typically, the
	 * "last" item in a List is the List.NIL instance, or at least
	 * an instance of the List.Nil type, though as in Prolog,
	 * this need not necessarily be the case.  Lists are therefore
	 * usually built "inside-out", or from the tail to the head, though
	 * you can also use any of the 11 factory list() methods for creating
	 * Lists if the elements of the List are known beforehand.
	 * 
	 * @param   head  the List's head
	 * @param   tail  the List's tail
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public
	List( Term head, Term tail )
	{
		super( DOT, JPLUtil.toTermArray( head, tail ) );
	}

	//==================================================================/
	//  
	//==================================================================/

	//------------------------------------------------------------------/
	// tailIsNil
	/**
	 * @return  true if tail of this List is an instance of Nil; false o/w
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final boolean
	tailIsNil()
	{
		return args_[1] instanceof Nil;
	}
	
	//------------------------------------------------------------------/
	// toString
	/**
	 * @return  the String representation of this List.  In this case,
	 * we use the simplest Prolog represenation [H|T].  For example,
	 * the list [a,b,c] is represented [a|[b|[c|[]]]].
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public java.lang.String
	toString()
	{
		return "[" + args_[0] + " | " + args_[1] + "]";
	}
	
	public java.lang.String
	debugString()
	{
		return "(List " + Term.debugString( args_ ) + ")";
	}

	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [] (i.e., the Atom List.NIL.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Atom
	list()
	{
		return List.NIL;
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0 )
	{
		return 
			new List( t0, list() );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1 )
	{
		return 
			new List( t0, list( t1 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2 )
	{
		return 
			new List( t0, list( t1, t2 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3 )
	{
		return 
			new List( t0, list( t1, t2, t3 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3,t4]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3, Term t4 )
	{
		return 
			new List( t0, list( t1, t2, t3, t4 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3,t4,t5]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3, Term t4,
	      Term t5 )
	{
		return 
			new List( t0, list( t1, t2, t3, t4, t5 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3,t4,t5,t6]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3, Term t4,
	      Term t5, Term t6 )
	{
		return 
			new List( t0, list( t1, t2, t3, t4, t5,
			                    t6 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3,t4,t5,t6,t7]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3, Term t4,
	      Term t5, Term t6, Term t7 )
	{
		return 
			new List( t0, list( t1, t2, t3, t4, t5,
			                    t6, t7 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3,t4,t5,t6,t7,t8]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3, Term t4,
	      Term t5, Term t6, Term t7, Term t8 )
	{
		return 
			new List( t0, list( t1, t2, t3, t4, t5,
			                    t6, t7, t8 ) );
	}
	
	//------------------------------------------------------------------/
	// list
	/**
	 * @return  a jpl.List representing [t0,t1,t2,t3,t4,t5,t6,t7,t8,t9]
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static List
	list( Term t0, Term t1, Term t2, Term t3, Term t4,
	      Term t5, Term t6, Term t7, Term t8, Term t9 )
	{
		return 
			new List( t0, list( t1, t2, t3, t4, t5,
			                    t6, t7, t8, t9 ) );
	}
	
	//------------------------------------------------------------------/
	// length
	/**
	 * This method returns the length of a List.  It assumes that
	 * the List is List.Nil terminated.
	 * 
	 * @return  the length of the List
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public int
	length()
	{
		java.util.Enumeration e = elements();
		int                   i = 0;
		
		while ( e.hasMoreElements() ){
			e.nextElement();
			++i;
		}
		return i;
	}
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * This method returns an array containg the Terms in
	 * the List.  It assumes that the List is List.Nil terminated.
	 * 
	 * @return  a Term array containing the Terms in the List
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public Term[]
	toTermArray()
	{
		Term[]            terms = new Term[length()];
		java.util.Enumeration e = elements();
		
		for ( int i = 0;  e.hasMoreElements();  ++i ){
			terms[i] = (Term) e.nextElement();
		}
		return terms;
	}
	
	
	//==================================================================/
	// Nil
	/**
	 * The Nil class is used to terminate a List.
	 */
	// Implementation notes:  NOTE: Nil is not a List!  It would be
	// nice if it was, but it's not a list in Prolog, either.
	//==================================================================/
	public static class Nil
	extends Atom
	{
		public
		Nil()
		{
			super( "[]" );
		}
		
	}
	public static final Atom NIL = new Nil();
	
	//------------------------------------------------------------------/
	// 
	/**
	 * @return  
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	protected boolean
	nil_terminated()
	{
		List cons = this;
		Term tail = cons.tail();
		
		while ( tail instanceof List ){
			cons = (List) tail;
			tail = cons.tail();
		}
		
		return cons.tailIsNil();
	}
	
	//==================================================================/
	// ListEnumerator
	/**
	 * A ListEnumerator enumerates the elements of a List.  Note that
	 * trying to construct one of these on a List that is not nil-terminated
	 * will cause a JPLException to be thrown
	 */
	// Implementation notes:  
	// 
	//==================================================================/
	private class ListEnumerator
	implements java.util.Enumeration
	{
		private List cons_ = List.this;
		
		protected 
		ListEnumerator()
		{
			if ( !nil_terminated() ){
				throw new JPLException( "List is not a nil-terminated List" );
			}
		}
		
		public boolean
		hasMoreElements()
		{
			return cons_ != null;
		}
		
		public Object
		nextElement()
		{
			Term ret  = cons_.head();
			Term tail = cons_.tail();
			
			if ( tail instanceof List ){
				cons_ = (List)tail;
			} else {
				cons_ = null;
			}
			
			return ret;
		}
	}
	
	//------------------------------------------------------------------/
	// elements
	/**
	 * This method return an enumeration of a <i>nil-terminated</i> List.
	 * Calling this method on a List whose last cdr is not an instance of
	 * List.Nil will cause a JPLException to be thrown.
	 * 
	 * @return  A java.util.Enumeration of all the elements of the List
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public java.util.Enumeration
	elements()
	{
		return new ListEnumerator();
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
