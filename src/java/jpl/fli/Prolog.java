/*This code is copyrighted by Teknowledge (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Teknowledge in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/ 

 
package jpl.fli;



//----------------------------------------------------------------------/
// Prolog
/**
 * This class consists only of constants (static finals) and static
 * native methods.  The constants and methods defined herein are in
 * (almost) strict 1-1 correpsondence with the functions in the Prolog
 * FLI by the same name (except without the PL_, SQ_, etc. prefixes).<p>
 *
 * See the file jpl_fli_Prolog.c for the native implementations of these
 * methods.  Refer to your local Prolog FLI documentations for the meanings
 * of these methods, and observe the following:<p>
 *
 * <menu>
 * <li> The types and signatures of the following methods are almost
 * in 1-1 correspondence with the Prolog FLI.  The Prolog types
 * term_t, atom_t, functor_t, etc. are mirrored in this package with
 * classes by the same name, making the C and java uses of these
 * interfaces similar.</li>
 * <li> As term_t, functor_t, etc. types are Java classes, they are
 * passed to these methods <b>by vlaue</b>; however, calling these
 * methods on such class instances does have side effects.  In general,
 * the value fields of these instances will be modified, in much the
 * same way the term_t, functor_t, etc. Prolog instances would be
 * modified.</li>
 * <li> The exceptions to this rule occur when maintaining the same
 * signature would be impossible, e.g., when the Prolog FLI functions
 * require <i>pointers</i>; in this case, the signatures have been
 * modified to take *Holder classes (Int, Double, String, etc.),
 * to indicate a call by reference parameter.
 * <li> Functions which take variable-length argument lists in C
 * take arrays in Java; however, due to the fact that Java does not
 * support variable length argument lists, these methods currently
 * have an upper bound of 5 arguments.  Sorry about that.
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
// Not all of the constants and methods are here.  Some will be
// added as this software matures.
//----------------------------------------------------------------------/
public final class Prolog
{
	static {
		System.loadLibrary("jpl");
	}

	/* term types */
	public static final int
		VARIABLE     = 1,
		ATOM         = 2,
		INTEGER      = 3,
		FLOAT        = 4,
		STRING       = 5,
		TERM         = 6,
		FUNCTOR      = 10,
		LIST         = 11,
		CHARS        = 12,
		POINTER      = 13;

	public static final int
		succeed  = 1,
		fail     = 0;

	/* query flags */
	public static final int
		Q_NORMAL          = 0x02,
		Q_NODEBUG         = 0x04,
		Q_CATCH_EXCEPTION = 0x08,
		Q_PASS_EXCEPTION  = 0x10;

	/* conversion flags */
	public static final int
		CVT_ATOM     = 0x0001,
		CVT_STRING   = 0x0002,
		VT_LIST      = 0x0004,
		CVT_INTEGER  = 0x0008,
		CVT_FLOAT    = 0x0010,
		CVT_VARIABLE = 0x0020,
		CVT_NUMBER   = (CVT_INTEGER | CVT_FLOAT),
		CVT_ATOMIC   = (CVT_NUMBER|CVT_ATOM | CVT_STRING),
		CVT_ALL      = 0x00ff,

		BUF_DISCARDABLE = 0x0000,
		BUF_RING        = 0x0100,
		BUF_MALLOC      = 0x0200;

	/* Creating and destroying term-refs */
	public synchronized static native term_t new_term_refs( int n );
	public synchronized static native term_t new_term_ref();
	public synchronized static native term_t copy_term_ref( term_t from );
	public synchronized static native void   reset_term_refs( term_t r );

	/* Constants */
	public synchronized static native atom_t    new_atom( String s );
	public synchronized static native String    atom_chars( atom_t a );
	public synchronized static native functor_t new_functor( atom_t f, int a );
	public synchronized static native atom_t    functor_name( functor_t f );
	public synchronized static native int       functor_arity( functor_t f );

	/* Get Java-values from Prolog terms */
	public synchronized static native int get_atom( term_t t, atom_t a );
	public synchronized static native int get_atom_chars( term_t t, StringHolder a );
	public synchronized static native int get_string( term_t t, StringHolder s );
	public synchronized static native int get_list_chars( term_t l, StringHolder s, int flags );
	public synchronized static native int get_chars( term_t t, StringHolder s, int flags );
	public synchronized static native int get_integer( term_t t, IntHolder i );
	public synchronized static native int get_long( term_t t, LongHolder l );
	public synchronized static native int get_pointer( term_t t, PointerHolder ptr );
	public synchronized static native int get_float( term_t t, DoubleHolder d );
	public synchronized static native int get_functor( term_t t, functor_t f );
	public synchronized static native int get_name_arity( term_t t, atom_t name, IntHolder arity );
	public synchronized static native int get_module( term_t t, module_t module );
	public synchronized static native int get_arg( int index, term_t t, term_t a );
	public synchronized static native int get_list( term_t l, term_t h, term_t t );
	public synchronized static native int get_head( term_t l, term_t h );
	public synchronized static native int get_tail( term_t l, term_t t );
	public synchronized static native int get_nil( term_t l );

	/* Verify types */
	public synchronized static native int term_type( term_t t );
	public synchronized static native int is_variable( term_t t );
	public synchronized static native int is_atom( term_t t );
	public synchronized static native int is_integer( term_t t );
	public synchronized static native int is_string( term_t t );
	public synchronized static native int is_float( term_t t );
	public synchronized static native int is_compound( term_t t );
	public synchronized static native int is_functor( term_t t, functor_t f );
	public synchronized static native int is_list( term_t t );
	public synchronized static native int is_atomic( term_t t );
	public synchronized static native int is_number( term_t t );

	/* Assign to term-references */
	public synchronized static native void put_variable( term_t t );
	public synchronized static native void put_atom( term_t t, atom_t a );
	public synchronized static native void put_atom_chars( term_t t, String chars );
	public synchronized static native void put_string_chars( term_t t, String chars );
	public synchronized static native void put_list_chars( term_t t, String chars);
	public synchronized static native void put_integer( term_t t, long i );
	public synchronized static native void put_pointer(term_t t, PointerHolder ptr);
	public synchronized static native void put_float( term_t t, double f );
	public synchronized static native void put_functor( term_t t, functor_t functor );
	public synchronized static native void put_list( term_t l );
	public synchronized static native void put_nil( term_t l );
	public synchronized static native void put_term( term_t t1, term_t t2 );
	// this method should be deprecated:
	protected synchronized static native void cons_functor( term_t h, functor_t f, term_t terms[] );
	public synchronized static native void cons_functor_v( term_t h, functor_t fd, term_t a0 );
	public synchronized static native void cons_list( term_t l, term_t h, term_t t );

	/* Unify term-references */
	public synchronized static native int unify( term_t t1, term_t t2 );

	/* Modules */
	public synchronized static native module_t   context();
	public synchronized static native atom_t     module_name( module_t module );
	public synchronized static native module_t   new_module( atom_t name );
	public synchronized static native int        strip_module( term_t in, module_t m, term_t out );

	/* Foreign context frames */
	public synchronized static native fid_t open_foreign_frame();
	public synchronized static native void  close_foreign_frame( fid_t cid );
	public synchronized static native void  discard_foreign_frame( fid_t cid );

	/* Finding predicates */
	public synchronized static native predicate_t pred( functor_t f, module_t m );
	public synchronized static native predicate_t predicate( String name, int arity, String module );
	public synchronized static native int predicate_info( predicate_t pred, atom_t name, IntHolder arity, module_t module);

	/* Call-back */
	public synchronized static native qid_t open_query( module_t m, int flags, predicate_t pred, term_t t0 );
	public synchronized static native int next_solution( qid_t qid );
	public synchronized static native void close_query( qid_t qid );
	public synchronized static native void cut_query( qid_t qid );

	/* Simplified (but less flexible) call-back */
	public synchronized static native int    call( term_t t, module_t m );
	public synchronized static native int    call_predicate( module_t m, int debug, predicate_t pred, term_t t0 );
	public synchronized static native term_t exception( qid_t qid );

	public synchronized static native int  initialise( int argc, String argv[] );
	public synchronized static native void halt( int status );




	/* a simple test; write a test.pl file and run!! */
	public static void
	main( String argv[] )
	{
		Prolog.initialise( argv.length, argv );

		atom_t atom = Prolog.new_atom( "test" );
		term_t term = Prolog.new_term_ref();
		Prolog.put_atom( term, atom );
		predicate_t pred = Prolog.predicate( "consult", 1, null );
		qid_t qid = Prolog.open_query( null, Q_NORMAL, pred, term );

		System.out.println(
			Prolog.next_solution( qid ) );

		Prolog.close_query( qid );

		Prolog.halt( 0 );
	}
}

//345678901234567890123456789012346578901234567890123456789012345678901234567890
