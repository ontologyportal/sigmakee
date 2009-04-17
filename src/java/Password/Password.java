/**
 *	Password is a class to implement password encryption as used
 *	on Unix systems. It is compatible with the crypt(3c) system function.
 *	This version is a based on the DES encryption algorithm in
 *	Andrew Tanenbaum's book "Computer Networks". It was rewritten
 *	in C and used in Perl release 4.035. This version was rewritten
 *	in Java by David Scott, Siemens Ltd., Australia.
 *
 *	For further details on the methods in this class, refer to the
 *	Unix man pages for crypt(3c).
 */


public class Password
{
	
	private static byte[] InitialTr = {
		58,50,42,34,26,18,10, 2,60,52,44,36,28,20,12, 4,
		62,54,46,38,30,22,14, 6,64,56,48,40,32,24,16, 8,
		57,49,41,33,25,17, 9, 1,59,51,43,35,27,19,11, 3,
		61,53,45,37,29,21,13, 5,63,55,47,39,31,23,15, 7
	};
	
	private static byte[] FinalTr = {
		40, 8,48,16,56,24,64,32,39, 7,47,15,55,23,63,31,
		38, 6,46,14,54,22,62,30,37, 5,45,13,53,21,61,29,
		36, 4,44,12,52,20,60,28,35, 3,43,11,51,19,59,27,
		34, 2,42,10,50,18,58,26,33, 1,41, 9,49,17,57,25
	};
	
	private static byte[] swap = {
		33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,
		49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,
		 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,
		17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32
	};
	
	private static byte[] KeyTr1 = {
		57,49,41,33,25,17, 9, 1,58,50,42,34,26,18,
		10, 2,59,51,43,35,27,19,11, 3,60,52,44,36,
		63,55,47,39,31,23,15, 7,62,54,46,38,30,22,
		14, 6,61,53,45,37,29,21,13, 5,28,20,12, 4
	};
	
	private static byte[] KeyTr2 = {
		14,17,11,24, 1, 5, 3,28,15, 6,21,10,
		23,19,12, 4,26, 8,16, 7,27,20,13, 2,
		41,52,31,37,47,55,30,40,51,45,33,48,
		44,49,39,56,34,53,46,42,50,36,29,32
	};
	
	private static byte[] etr = {
		32, 1, 2, 3, 4, 5, 4, 5, 6, 7, 8, 9,
		 8, 9,10,11,12,13,12,13,14,15,16,17,
		16,17,18,19,20,21,20,21,22,23,24,25,
		24,25,26,27,28,29,28,29,30,31,32, 1
	};
	
	private static byte[] ptr = {
		16, 7,20,21,29,12,28,17, 1,15,23,26, 5,18,31,10,
		 2, 8,24,14,32,27, 3, 9,19,13,30, 6,22,11, 4,25
	};
	
	private static byte s_boxes[][] = 
	{
		{
			14, 4,13, 1, 2,15,11, 8, 3,10, 6,12, 5, 9, 0, 7,
			 0,15, 7, 4,14, 2,13, 1,10, 6,12,11, 9, 5, 3, 8,
			 4, 1,14, 8,13, 6, 2,11,15,12, 9, 7, 3,10, 5, 0,
			15,12, 8, 2, 4, 9, 1, 7, 5,11, 3,14,10, 0, 6,13
		},

		{ 
			15, 1, 8,14, 6,11, 3, 4, 9, 7, 2,13,12, 0, 5,10,
			 3,13, 4, 7,15, 2, 8,14,12, 0, 1,10, 6, 9,11, 5,
			 0,14, 7,11,10, 4,13, 1, 5, 8,12, 6, 9, 3, 2,15,
			13, 8,10, 1, 3,15, 4, 2,11, 6, 7,12, 0, 5,14, 9
		},

		{
			10, 0, 9,14, 6, 3,15, 5, 1,13,12, 7,11, 4, 2, 8,
			13, 7, 0, 9, 3, 4, 6,10, 2, 8, 5,14,12,11,15, 1,
			13, 6, 4, 9, 8,15, 3, 0,11, 1, 2,12, 5,10,14, 7,
			 1,10,13, 0, 6, 9, 8, 7, 4,15,14, 3,11, 5, 2,12
		},

		{
			 7,13,14, 3, 0, 6, 9,10, 1, 2, 8, 5,11,12, 4,15,
			13, 8,11, 5, 6,15, 0, 3, 4, 7, 2,12, 1,10,14, 9,
			10, 6, 9, 0,12,11, 7,13,15, 1, 3,14, 5, 2, 8, 4,
			 3,15, 0, 6,10, 1,13, 8, 9, 4, 5,11,12, 7, 2,14
		},

		{
			 2,12, 4, 1, 7,10,11, 6, 8, 5, 3,15,13, 0,14, 9,
			14,11, 2,12, 4, 7,13, 1, 5, 0,15,10, 3, 9, 8, 6,
			 4, 2, 1,11,10,13, 7, 8,15, 9,12, 5, 6, 3, 0,14,
			11, 8,12, 7, 1,14, 2,13, 6,15, 0, 9,10, 4, 5, 3
		},

		{
			12, 1,10,15, 9, 2, 6, 8, 0,13, 3, 4,14, 7, 5,11,
			10,15, 4, 2, 7,12, 9, 5, 6, 1,13,14, 0,11, 3, 8,
			 9,14,15, 5, 2, 8,12, 3, 7, 0, 4,10, 1,13,11, 6,
			 4, 3, 2,12, 9, 5,15,10,11,14, 1, 7, 6, 0, 8,13
		},

		{
			 4,11, 2,14,15, 0, 8,13, 3,12, 9, 7, 5,10, 6, 1,
			13, 0,11, 7, 4, 9, 1,10,14, 3, 5,12, 2,15, 8, 6,
			 1, 4,11,13,12, 3, 7,14,10,15, 6, 8, 0, 5, 9, 2,
			 6,11,13, 8, 1, 4,10, 7, 9, 5, 0,15,14, 2, 3,12
		},

		{
			13, 2, 8, 4, 6,15,11, 1,10, 9, 3,14, 5, 0,12, 7,
			 1,15,13, 8,10, 3, 7, 4,12, 5, 6,11, 0,14, 9, 2,
			 7,11, 4, 1, 9,12,14, 2, 0, 6,10,13,15, 3, 5, 8,
			 2, 1,14, 7, 4,10, 8,13,15,12, 9, 0, 3, 5, 6,11
		},
	};
	
	private static int rots[] = {
		1,1,2,2,2,2,2,2,1,2,2,2,2,2,2,1
	};

	private static byte[] key = new byte[64] ;
	private static byte[] EP = etr;

	/* *** Methods start here *** */
	private static void transpose ( byte[] data, byte[] t, int n )
	{
		byte[] x = new byte[64] ;
		System.arraycopy ( data, 0, x, 0, x.length ) ;
		
		while ( n-- > 0 )
		{
			data[n] = x[t[n]-1] ;
		}
	}

	private static void rotate ( byte[] key )
	{
		byte[] x = new byte[64] ;
		System.arraycopy ( key, 0, x, 0, x.length ) ;

		for ( int i = 0 ; i < 55 ; i++ )
		{
			x[i] = x[i+1] ;
		}
		x[27] = key[0] ;
		x[55] = key[28] ;
		System.arraycopy ( x, 0, key, 0, key.length ) ;
	}

	private static void f ( int i, byte[] key, byte[] a, byte[] x )
	{
		byte[]	e = new byte[64] ;
		byte[]	ikey = new byte[64] ;
		byte[]	y = new byte[64] ;

		System.arraycopy ( a, 0, e, 0, e.length ) ;
		transpose ( e, EP, 48 ) ;

		for ( int j = rots[i] ; j > 0 ; j-- )
		{
			rotate(key) ;
		}

		System.arraycopy ( key, 0, ikey, 0, ikey.length ) ;
		transpose ( ikey, KeyTr2, 48 ) ;

		for ( int j = 0 ; j < 48 ; j++ )
		{
			y[j] = (byte)(e[j] ^ ikey[j]) ;
		}

		for ( int j = 0 ; j < 8 ; j++ )
		{
			int k = j+1 ;
			int r = 32 * y[6*k - 6] +
					 8 * y[6*k - 5] +
					 4 * y[6*k - 4] +
					 2 * y[6*k - 3] +
					     y[6*k - 2] + 
					16 * y[6*k - 1]	;

			int xb = s_boxes[j][r] ;

			x[4*k - 4] = (byte)(( xb >> 3 ) & 1) ;
			x[4*k - 3] = (byte)(( xb >> 2 ) & 1) ;
			x[4*k - 2] = (byte)(( xb >> 1 ) & 1) ;
			x[4*k - 1] = (byte)(  xb & 1 );
		}
		transpose ( x, ptr, 32 ) ;
	}

	private static void definekey ( byte[] k )
	{
		System.arraycopy ( k, 0, key, 0, key.length ) ;
		transpose ( key, KeyTr1, 56 ) ;
	}

	private static void encrypt ( byte[] blck, int edflag )
	{
		byte[] p = blck ;

		transpose ( p, InitialTr, 64 ) ;

		for ( int i = 15 ; i >= 0 ; i-- )
		{
			int j = edflag > 0 ? i : 15 - i ;
			byte[] b = new byte[64] ;
			System.arraycopy ( p, 0, b, 0, b.length ) ;
			byte[] x = new byte[64] ;

			for ( int k = 31 ; k >= 0 ; k-- ) 
			{
				p[k] = b[k+32] ;
			}

			f ( j, key, p, x ) ;
			for ( int k = 31 ; k >= 0 ; k-- )
			{
				p[k+32] = (byte)(b[k] ^ x[k]) ;
			}
		}

		transpose ( p, swap, 64 ) ;
		transpose ( p, FinalTr, 64 ) ;
		blck = p ;
	}

	/**
	 *	Returns a String containing the encrypted passwd
	 *	
	 *	@param	strpw	A String containing the un-encrypted password
	 *	@param	strsalt	A 2 character String, containing the salt to
	 *					encrypt the password with.
	 *	@returns		String containing encrypted password.	
	 */
	public static String crypt ( String strpw, String strsalt )
	{
		char[] pw = strpw.toCharArray() ;
		char[] salt = strsalt.toCharArray() ;
		byte[] pwb = new byte[66] ;
		char[] result = new char[13] ;
		byte[] new_etr = new byte[etr.length] ;
		int n = 0 ;
		int m = 0 ;

		while ( m < pw.length && n < 64 )
		{
			for ( int j = 6 ; j >= 0 ; j-- )
			{
				pwb[n++] = (byte)(( pw[m] >> j ) & 1 ) ;
			}
			m++ ; // Increment pw
			pwb[n++] = 0 ;
		}

		while ( n < 64 )
		{
			pwb[n++] = 0 ;
		}

		definekey ( pwb ) ;

		for ( n = 0 ; n < 66 ; n++ )
		{
			pwb[n] = 0 ;
		}

		System.arraycopy ( etr, 0, new_etr, 0 , new_etr.length ) ;
		EP = new_etr ;

		for ( int i = 0 ; i < 2 ; i++ )
		{
			char c = salt[i] ;
			

			result[i] = c ;
			if ( c > 'Z' ) 
			{
				c -= 6 + 7 + '.' ;	// c was a lowercase letter
			} 
			else if ( c > '9' ) 
			{
				c -= 7 + '.' ;		// c was a uppercase letter
			}
			else {
				c -= '.' ;			// c was a digit, '.' or '/'
			}						// now, 0 <= c <= 63 

			for ( int j = 0 ; j < 6 ; j++ )
			{
				if ((( c >> j ) & 1) == 1 )
				{
					byte t			= (byte)(6*i + j) ;
					byte temp		= new_etr[t] ;
					new_etr[t]		= new_etr[t+24] ;
					new_etr[t+24]	= temp ;
				}
			}
		}

		if ( result[1] == 0 ) 
		{
			result[1] = result[0] ;
		}

		for ( int i = 0 ; i < 25 ; i++ )
		{
			encrypt ( pwb, 0 ) ;
		}
		EP = etr ;

		m = 2 ;
		n = 0 ;
		while ( n < 66 )
		{
			int c = 0 ;
			

			for ( int j = 6 ; j > 0 ; j-- ) 
			{
				c <<= 1 ;
				c |= pwb[n++] ;
			}

			c += '.' ;			// becomes >= '.'
			if ( c > '9' )
			{
				c += 7 ;		// not in [./0-9], becomes upper
			}

			if ( c > 'Z' )
			{
				c += 6 ;		// not in [A-Z], becomes lower
			}

			result[m++] = (char) c ;
		}
		return ( new String ( result ) );
	}
}
