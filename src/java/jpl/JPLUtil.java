/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Hashtable;

//----------------------------------------------------------------------/
// Util
/**
 * This class provides a bunch of static utility methods for the JPL
 * High-Level Interface.
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
public final class JPLUtil
{
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray()
	{
		Term t[] = new Term[0];
		
		return t;
	}
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0 )
	{
		Term t[] = { t0 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1 )
	{
		Term t[] = { t0, t1 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2 )
	{
		Term t[] = { t0, t1, t2 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3 )
	{
		Term t[] = { t0, t1, t2, t3 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3, Term t4 )
	{
		Term t[] = { t0, t1, t2, t3, t4 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3, Term t4, 
	           Term t5 )
	{
		Term t[] = { t0, t1, t2, t3, t4, t5 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3, Term t4, 
	           Term t5, Term t6 )
	{
		Term t[] = { t0, t1, t2, t3, t4, t5, t6 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3, Term t4, 
	           Term t5, Term t6, Term t7 )
	{
		Term t[] = { t0, t1, t2, t3, t4, t5, t6, t7 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3, Term t4, 
	           Term t5, Term t6, Term t7, Term t8 )
	{
		Term t[] = { t0, t1, t2, t3, t4, t5, t6, t7, t8 };
		
		return t;
	}
	
	
	//------------------------------------------------------------------/
	// toTermArray
	/**
	 * Creates an array of Terms, holding Terms in parameter(s).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term[]
	toTermArray( Term t0, Term t1, Term t2, Term t3, Term t4, 
	           Term t5, Term t6, Term t7, Term t8, Term t9 )
	{
		Term t[] = { t0, t1, t2, t3, t4, t5, t6, t7, t8, t9 };
		
		return t;
	}


	//------------------------------------------------------------------/
	// termArrayToList
	/**
	 * Converts an array of Terms to a jpl.List (if there are one or
	 * more Terms in the array), or jpl.List.NIL if there are no
	 * Terms in the array.  (Since the end of a jpl.List is an Atom,
	 * the return type of this method must be Term, not List, for
	 * full generality.)
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Term
	termArrayToList( Term t[] )
	{
		Term list = List.NIL;
		for ( int i = t.length-1;  i >= 0;  --i ){
			list = new List( t[i], list );
		}

		return list;
	}



	//------------------------------------------------------------------/
	// toString
	/**
	 * converts a solution, in the form of a Hashtable, to a String.
	 * This method is really only for testing
	 * 
	 * @param   solution  The solution to stringify.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static java.lang.String
	toString( Hashtable solution )
	{
		java.util.Enumeration vars = solution.keys();
		
		java.lang.String s = "Solutions: ";
		while ( vars.hasMoreElements() ){
			Variable var = (Variable) vars.nextElement();
			s += var + " = " + solution.get( var );
		}
		return s;
	}
}
