/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl;

import jpl.fli.Prolog;


//----------------------------------------------------------------------/
// JPL
/**
 * The JPL class contains initialization and termination methods for 
 * the High-Level interface.  The Prolog engine must be initialized 
 * before any queries are made.
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
public class JPL
{
	protected static final boolean DEBUG = false;
	private static boolean  _initialized = false;
	
	//------------------------------------------------------------------/
	// init
	/**
	 * Initializes the Prolog engine, using the String argument
	 * parameters passed.  For parameter options, consult your local
	 * Prolog documentation.  The parameter values are passed directly
	 * to initialization routines for the Prolog environment.<p>
	 * 
	 * This method must be called before making any queries.
	 * 
	 * @param   argv   Initialization parameter list
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static void
	init( java.lang.String argv[] )
	{
		if ( !_initialized ){
			Prolog.initialise( argv.length, argv );
			_initialized = true;
		}
	}
	
	//------------------------------------------------------------------/
	// init
	/**
	 * Default initializor.  Starts the engine with pl, the default
	 * Prolog command, with the initial goal to be "true" (eliminates
	 * that initial message).
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static void
	init()
	{
		java.lang.String argv[] = { "pl", "-g", "true" };
		init( argv );
	}
	
	//------------------------------------------------------------------/
	// halt
	/**
	 * Terminates the Prolog session.<p>
	 * 
	 * <b>Note.</b>  This method calls the FLI halt() method with a
	 * status of 0, but the halt method currently is a no-op in SWI.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static void
	halt()
	{
		Prolog.halt( 0 );
	}
	
	
	// a static reference to the current Version
	private static final Version version_ = new Version();
	
	//------------------------------------------------------------------/
	// version
	/**
	 * @return the running version of JPL.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static Version
	version()
	{
		return version_;
	}
	
	//------------------------------------------------------------------/
	// version_string
	/**
	 * @return the running version (in String form) of JPL.
	 */
	// Implementation notes:  
	// 
	//------------------------------------------------------------------/
	public static java.lang.String
	version_string()
	{
		return 
			"JPL " +
			version_.major + "." +
			version_.minor + "." +
			version_.patch + "-" +
			version_.status;
	}
	
	public static void
	main( java.lang.String argv[] )
	{
		System.out.println( version_string() );
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
