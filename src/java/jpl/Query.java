/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import jpl.fli.*;


//----------------------------------------------------------------------/
// Query
/**
 * A Query is an Object used to query the Prolog engine.  It consists of
 * an Atom (corresponding to the name of the predicate being queried)
 * and a list (array) of Terms, the arguments to the predicate.<p>
 * 
 * The Query class implements the Enumeration interface, and it is
 * through this interface that one obtains solutions.  The Enumeration
 * hasMoreElements() method returns true if the goal succeeded (false,
 * o/w), and if the goal did succeed, the nextElement() method returns
 * a Hashtable representing variable bindings; the elements in the
 * Hashtable are Terms, indexed by the Variables to which they are bound.
 * For example, if <i>p(a)</i> and <i>p(b)</i> are facts in the Prolog
 * database, then the following is equivalent to printing all
 * the solutions to the Prolog query <i>p(X)</i>:
 * 
 * <pre>
 * Variable X = new Variable();
 * Term arg[] = { X };
 * Query    q = new Query( "p", arg );
 * 
 * while ( q.hasMoreElements() ){
 *     Term bound_to_x = ((Hashtable)q.nextElement()).get( X );
 *     System.out.println( bound_to_x );
 * }
 * </pre>
 * 
 * Make sure to rewind the Query if you have not asked for all
 * its solutions.  To obtain just one solution from a Query, 
 * use the oneSolution() method.
 * To obtain all solutions, use the allSolutions() method.  Use the
 * query() method if the Query is a ground query.
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
public class
Query implements Enumeration
{
	
	//==================================================================/
	//  Attributes
	//==================================================================/
	
	/**
	 * This static reference is used to synchronize calls to the
	 * Low-Level Interface so that only one query can be active
	 * at a time.  Note that the hasMoreSolutuons, nextSolution,
	 * rewind, allSolutuons, and oneSolution method bodies are
	 * synchronized on this object reference, which, after initialization,
	 * is the Class object for the jpl.fli.Prolog class.
	 */
	private static Object lock_ = null;
	
	static {
		try {
			lock_ = Class.forName( "jpl.fli.Prolog" );
		} catch ( ClassNotFoundException cnfe ){
			throw new JPLException( "Query static initializer: Could not find jpl.Prolog class" );
		}
	}
	
	//------------------------------------------------------------------/
	// lock
	/**
	 * Use this method to obtain the lock that is used to synchronize
	 * all calls to the Low-Level Interface.  You should use this
	 * lock i) if you are using the hasMoreSolutions() and nextSolution()
	 * methods to enumerate the solutions to a Query, and ii) you need
	 * these calls to be thread-safe.  Example:
	 * <pre>
	 * Query query = // get a query somehow
	 * synchronized ( jpl.Query.lock() ){
	 *     while ( query.hasMoreElements() ){
	 *          Hashtable solution = query.nextSolution();
	 *          // process solution...
	 *     }
	 * }
	 * </pre>
	 * The lock so acquired is the same lock used internally by
	 * hasMoreSolutions and nextSolution methods; indeed, it is
	 * the Class Object for the jpl.fli.Prolog class, so any calls
	 * to the Low-Level Interface will be blocked, as well.  However,
	 * you should not intermix calls to the Low-Level Interface when using
	 * the High-Level Interface.
	 * 
	 * @return  the lock on any calls to the FLI
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Object
	lock()
	{
		return lock_;
	}
	
	
	/**
	 * the Atom corresponding to the predicate name in this Query
	 */
	protected Atom atom_ = null;
	/**
	 * the arguments to this Query
	 */
	protected Term args_[] = null;

	/**
	 * @return the Atom corresponding to the predicate name in this Query
	 */
	public final Atom
	atom()
	{
		return atom_;
	}
	
	/**
	 * @return the arguments to this Query
	 */
	public final Term[]
	args()
	{
		return args_;
	}
	
	
	//==================================================================/
	//  Contructors and Initialization
	//==================================================================/

	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor creates a Query object corresponding to a 
	 * Prolog query.  The predicate name is determined by the pred_atom
	 * parameter, and the arguments are given by the arg parameter.<p>
	 * 
	 * <b>NB.</b>  Creating an instance of the Query class does not
	 * result in a call to the Prolog Abstract Machine.
	 * 
	 * @param   atom  an Atom that names the predicate in this Query
	 * @param   args the arguments to this Query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( Atom atom, Term args[] )
	{
		this.atom_ = atom;
		this.args_ = args;
	}

	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( new Atom( name ), arg )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
	 * @param   args  the arguments to this Query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( java.lang.String name, Term args[] )
	{
		this( new Atom( name ), args );
	}
	
	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
	 * @param   t0    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( 
		java.lang.String name, 
		Term t0 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0 ) );
	}
	
	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( 
		java.lang.String name, 
		Term t0,
		Term t1 )
	{
		this( 
			new Atom( name ), 
			JPLUtil.toTermArray( t0, t1 ) );
	}
	
	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
	 * @param   t0    a jpl.Term
	 * @param   t1    a jpl.Term
	 * @param   t2    a jpl.Term
	 * @param   t3    a jpl.Term
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
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
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
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
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
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
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
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
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7, t8 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
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
	Query( 
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
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( 
	 *     new Atom( name ), 
	 *     Util.toTermArray( t0, t1, t2, t3, t4, 
	 *                       t5, t6, t7, t8, t9 ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
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
	Query( 
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

	// for use in following constructor
	private static final Term[] EMPTY_TERM_ARRAY = new Term[0];

	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor creates a Prolog query with no arguments
	 * (a "proposition" or "sentence").
	 * 
	 * @param   atom  an Atom that names the predicate in this Query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( Atom atom )
	{
		this( atom, EMPTY_TERM_ARRAY );
	}

	//------------------------------------------------------------------/
	// Query
	/**
	 * This constructor is shorthand for
	 * <pre>
	 * new Query( new Atom( name ) )
	 * </pre>
	 * 
	 * @param   name  the name of the predicate in this Query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public 
	Query( java.lang.String name )
	{
		this( new Atom( name ) );
	}


	//==================================================================/
	//  Making Prolog Queries
	//==================================================================/

	//------------------------------------------------------------------/
	// create_predicate_t
	/**
	 * @return  A predicate_t object for making Prolog queries
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	private predicate_t
	create_predicate_t()
	{
		return Prolog.predicate( atom_.name_, args_.length, null );
	}

	//------------------------------------------------------------------/
	// create_term_ts
	/**
	 * @return  A term_t object for making Prolog queries
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	private term_t
	create_term_ts()
	{
		return Term.terms_to_term_ts( new Hashtable(), args_ );
	}
	
	/**
	 * These variables are used and set across the hasMoreElements
	 * and nextElement Enumeration interface implementation
	 */
	private boolean      querying = false;
	private qid_t        qid;
	private predicate_t  predicate;
	private term_t       term0;
	private static Query querying_query = null;
		
	
	//------------------------------------------------------------------/
	// hasMoreSolutions
	/**
	 * This method returns true if making a Prolog Query using this
	 * Object's Atom and Terms succeeds.  It is designed to be used in
	 * conjunction with the nextSolution() method to retrieve one or
	 * more substitutions in the form of Hashtables.  To iterate through
	 * all the solutions to a Query, for example, one might write
	 * <pre>
	 * Query q = // obtain Query reference
	 * while ( q.hasMoreSolutions() ){
	 *     Hashtable solution = q.nextSolution();
	 *     // process solution...
	 * }
	 * </pre>
	 * To ensure thread-safety, you should wrap sequential calls to
	 * this method in a synchronized block, using the static
	 * lock method to obtain the monitor.
	 * <pre>
	 * Query q = // obtain Query reference
	 * synchronized ( jpl.Query.lock() ){
	 *     while ( q.hasMoreElements() ){
	 *          Hashtable solution = q.nextSolution();
	 *          // process solution...
	 *     }
	 * }
	 * </pre>
	 * <p>
	 * If this method is called while another is in progress, a
	 * QueryInProgressException will be thrown with the currently
	 * executing Query.
	 * 
	 * @return  true if the Prolog query succeeds; false, o/w.
	 * 
	 * @see jpl.Query#lock
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final boolean
	hasMoreSolutions()
	{
		synchronized ( lock_ ){
			// 
			// Check to see if anyone is doing a query; if there is another
			// query in process and it is not us, then
			// throw a QueryInProgressException
			// 
			if ( querying_query != null && querying_query != this ){
				throw new QueryInProgressException( querying_query );
			}
			querying_query = this;

			// 
			// If we are not already querying, open a query through the FLI
			// 
			if ( !querying ){
				predicate = create_predicate_t();
				term0     = create_term_ts();
				qid       = Prolog.open_query( null, Prolog.Q_NORMAL, predicate, term0 );

				querying       = true; // for subsequent calls by this
				querying_query = this; // for calls by any other Query object
			}

			// 
			// Get the next solution; if it's false, close the query;
			// otherwise, keep it open for subsequent calls to this method.
			// 
			int rval = Prolog.next_solution( qid );
			if ( rval == 0 ){

				// Check to see if the reason for failure was
				// as a result of a call to throw/1.  If so, build
				// the exception term now.
				term_t exception_term_t = Prolog.exception( qid );
				Term   exception_term   = null;
				if ( exception_term_t.value != 0L ){
					exception_term =
						Term.from_term_t(
							new Hashtable(),
							exception_term_t );
				}

				// in any event, close the query
				Prolog.close_query( qid );
				querying       = false; // so we can start this Query again
				querying_query = null;  // so that someone else can Query

				// if an exception was thrown in Prolog, throw
				// a PrologException in Java
				if ( exception_term_t.value != 0L ){
					throw new PrologException( exception_term );
				}
			}
			// return the value of the call to prolog
			return rval != 0 ? true : false;
		}
	}

	//------------------------------------------------------------------/
	// nextSolution
	/**
	 * This method returns a java.util.Hashtable, which represents
	 * a substitution of Terms for Variables in the Term list in this
	 * Query.  The Hashtable contains instances of Terms, keyed on
	 * Variable instances in the Term list.
	 * <p>
	 * For example, if a Query has an occurrence of a jpl.Variable,
	 * say, named X, one can obtain the Term bound to X in the solution
	 * by looking up X in the Hashtable.
	 * <pre>
	 * Variable X = new Variable();
	 * Query q = // obtain Query reference (with X in the Term array)
	 * while ( q.hasMoreSolutions() ){
	 *     Hashtable solution = q.nextSolution();
	 *     // make t the Term bound to X in the solution
	 *     Term t = (Term)solution.get( X );
	 *     // ...
	 * }
	 * </pre>
	 * Programmers should obey the following rules when using this method.
	 * <menu>
	 * <li> The nextSolution() method should only be called after the
	 * hasMoreSolutions() method returns true; otherwise a JPLException
	 * will be raised, indicating that no Query is in progress.
	 * <li> The nextSolution() and hasMoreSolutions() should be called
	 * in the same thread of execution, at least for a given Query
	 * instance.
	 * <li> The nextSolution() method should not be called while
	 * another Thread is in the process of evaluating a Query.  The
	 * JPL High-Level interface is designed to be thread safe, and
	 * is thread-safe as long as the previous two rules are obeyed.
	 * </menu>
	 * 
	 * This method will throw a JPLException if no query is in progress.
	 * It will throw a QueryInProgressException if another Query
	 * (besides this one) is in progress while this method is called.
	 * 
	 * @return  A Hashtable representing a substitution.
	 * 
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final Hashtable
	nextSolution()
	{
		synchronized ( lock_ ){
			if ( !querying ){
				throw new JPLException( "No Query is in process" );
			}

			// 
			// Check to see if anyone is doing a query; if there is another
			// query in process and it is not us (denoted by the qid), then
			// this is a user error and throw a runtime exception.  Otherwise,
			// either a query is not in process, or it is and we are the ones
			// doing it, so get the next element.
			// 
			if ( querying_query != null && querying_query != this ){
				throw new QueryInProgressException( querying_query );
			} else {
				Hashtable substitution = new Hashtable();
				Term.computeSubstitutions( substitution, new Hashtable(), args_ );
				return substitution;
			}
		}
	}
	
	
	//------------------------------------------------------------------/
	// hasMoreElements
	/**
	 * This method is completes the java.util.Enumeration
	 * interface.  It is a wrapper for hasMoreSolutions.
	 * 
	 * @return  true if the Prolog query succeeds; false, o/w.
	 * 
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final boolean
	hasMoreElements()
	{
		return hasMoreSolutions();
	}
	/**
	 * @deprecated Substitution too hard to spell; use *Solution* instead.
	 */
	public final boolean
	hasMoreSubstitutions()
	{
		return hasMoreSolutions();
	}

	//------------------------------------------------------------------/
	// nextElement
	/**
	 * This method is completes the java.util.Enumeration
	 * interface.  It is a wrapper for nextSolution.
	 * <p>
	 * This method will throw a QueryInProgressException if another Query
	 * (besides this one) is in progress while this method is called.
	 * 
	 * @return  A Hashtable representing a substitution.
	 * 
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final Object
	nextElement()
	{
		return nextSolution();
	}
	/**
	 * @deprecated Substitution too hard to spell; use *Solution* instead.
	 */
	public final Hashtable
	nextSubstitution()
	{
		return nextSolution();
	}

	//------------------------------------------------------------------/
	// rewind
	/**
	 * This method is used to rewind the query so that the query
	 * may be re-run, even if the Query qua Enumeration has more 
	 * elements.  Calling rewind() on an exhausted Enumeration has
	 * no effect.<p>
	 * 
	 * Here is a way to get the first 3 solutions to a Query,
	 * while subsequently being able to use the same Query object to
	 * obtain new solutions:
	 * <pre>
	 * Query q = new Query( predicate, args );
	 * int i = 0;
	 * for ( int i = 0; i < 3 && q.hasMoreSolutions();  ++i ){
	 *     Hasthable sub = (Hashtable) q.nextSolution();
	 *     ...
	 * }
	 * q.rewind();
	 * </pre><p>
	 * 
	 * This method will throw a QueryInProgressException if another Query
	 * (besides this one) is in progress while this method is called.
	 * It is safe to call this method if no query is in progress.
	 * 
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final void
	rewind()
	{
		synchronized ( lock_ ){
			// 
			// Check to see if anyone is doing a query; if there is another
			// query in process and it is not us (denoted by the qid), then
			// 
			// 
			if ( querying ){
				if ( querying_query != null && querying_query != this ){
					// this should never happen; if the state of a 
					// Query is querying, then it is the only query in progress
					// throw a QueryInProgressException
					throw new QueryInProgressException( querying_query );
				} else {
					Prolog.close_query( qid );
					querying       = false; // so we can start this Query again
					querying_query = null;  // so other Queries can start
				}
			}
		}
	}
	
	//------------------------------------------------------------------/
	// allSolutions
	/**
	 * @return an array of Hashtables, each of which is a solution
	 * (in order) of the Query.  If the return value is null, this
	 * means the Query has no solutions.<p>
	 * 
	 * This method will throw a QueryInProgressException if another Query
	 * (besides this one) is in progress while this method is called.
	 * 
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final Hashtable[]
	allSolutions()
	{
		synchronized ( lock_ ){
			rewind();

			Hashtable solutions[] = null;
			Vector    v = new Vector();

			while ( hasMoreSolutions() ){
				v.addElement( nextSolution() );
			}

			// otherwise, turn the Vector to an array
			int n = v.size();
			// choke if there were no solutions
			if ( n == 0 ){
				return null;
			}
			solutions = new Hashtable[n];
			v.copyInto( solutions );

			return solutions;
		}
	}
	
	//------------------------------------------------------------------/
	// oneSolution
	/**
	 * @return one solution, if it has one.  If the return value is 
	 * null, this means that the Query has no solutions; otherwise,
	 * the solution will be a (possibly empty) Hashtable.<p>
	 * 
	 * This method will throw a QueryInProgressException if another Query
	 * (besides this one) is in progress while this method is called.
	 * 
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final Hashtable
	oneSolution()
	{
		synchronized ( lock_ ){
			Hashtable solution = null;

			rewind();
			if ( hasMoreSolutions() ){
				solution = nextSolution();
			}
			rewind();

			return solution;
		}
	}
		
	//------------------------------------------------------------------/
	// query
	/**
	 * @return the value of the Query.<p>
	 * 
	 * This method should only be called for ground queries, since
	 * the results of any bindings are discarded.
	 * <p>
	 * This method will throw a QueryInProgressException if another Query
	 * (besides this one) is in progress while this method is called.
	 * 
	 * @see jpl.Query#hasMoreElements
	 * @see jpl.Query#nextElement
	 * @see jpl.Query#hasMoreSolutions
	 * @see jpl.Query#nextSolution
	 * @see jpl.Query#rewind
	 * @see jpl.Query#oneSolution
	 * @see jpl.Query#allSolutions
	 * @see jpl.Query#query
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public final boolean
	query()
	{
		return oneSolution() != null;
	}

	//------------------------------------------------------------------/
	// printSolution
	/**
	 * Prints a substitution to stdout.  For testing.
	 * 
	 * @param   substitution  The substitution to print.
	 * @deprecated use Util.toString instead.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static void
	printSolution( Hashtable substitution )
	{
		Enumeration vars = substitution.keys();
		
		System.out.println( "Substitutions: " );		
		while ( vars.hasMoreElements() ){
			Variable var = (Variable) vars.nextElement();
			System.out.print( var + " = " );
			System.out.println( substitution.get( var ) );
		}
	}
	/**
	 * @deprecated use Util.toString instead.
	 */
	public static void
	printSubstitution( Hashtable substitution )
	{
		printSolution( substitution );
	}
		
	//==================================================================/
	//  misc
	//==================================================================/


	//------------------------------------------------------------------/
	// toString
	/**
	 * Returns the String representation of a Query.
	 * 
	 * @return  the String representation of a Query
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
			"(Query " + 
				atom_.debugString()        + " " +
				Term.debugString( args_ ) + ")";
	}

	//------------------------------------------------------------------/
	// print_internal_rep
	/**
	 * For debugging...
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	private void
	print_internal_rep( predicate_t predicate, term_t term0 )
	{
		atom_t atom = new atom_t();
		IntHolder arity = new IntHolder();
		System.err.print( "got: " );
		Prolog.predicate_info( predicate, atom, arity, new module_t() );

		System.err.print( atom.toString() + "( " );
		System.err.print( term_t.toString( arity.value, term0 ) +" )" );
		System.err.println();
	}
}
