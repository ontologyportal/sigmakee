/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;



//----------------------------------------------------------------------/
// QueryInProgressException
/**
 * An exception of this type is thrown if a Query is made while 
 * another Query is in progress, for example, if the JPL programmer
 * has negglected to close a query by exhausting all solutions or by failing
 * to rweind() a Query, or in multi-threaded situations.
 * <p>
 * To prevent this kind of exception from being thrown, first make 
 * absolutely sure that all Queries are being closed properly, either
 * by exhausting all solutions in a Query (i.e., hasMoreSolutions()
 * returns false), or by explicitly calling rewind().  If your program
 * involves multi-threading, make sure you obtain the lock from the
 * Query object before entering a hasMoreSolutions()/nextSolution loop:
 * <pre>
 * Query query = // get a query somehow
 * synchronized ( jpl.Query.lock() ){
 *     while ( query.hasMoreElements() ){
 *          Hashtable solution = query.nextSolution();
 *          // process solution...
 *     }
 * }
 * </pre>
 * <b>Note:</b>  In most situations such care is not necessary, since
 * the Query.query(), Query,oneSolution(), and Query.allSolutions()
 * methods are thread-safe.  However, users may have reason
 * to use the hasMoreSolutions() and nextSolutions() methods, which,
 * while synchronized, do allow Queries to be opened between calls to
 * these methods.
 * <p>
 * If you catch this Exception, you can usese the query() accessor to 
 * obtain a reference to the Query that is in progress.
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
public final class
QueryInProgressException extends JPLException
{
	private Query query_ = null;
	
	protected 
	QueryInProgressException( Query query )
	{
		super( "QueryInProgressException: Query in progress=" + query.toString() );
		
		this.query_ = query;
	}
	
	/**
	 * @return a reference to the Query that is in progress
	 */
	public final Query
	query()
	{
		return this.query_;
	}
}
