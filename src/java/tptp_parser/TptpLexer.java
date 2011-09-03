// $ANTLR 2.7.5 (20050128): "tptp.g" -> "TptpLexer.java"$

  package tptp_parser;

  import java.util.List;
  import java.util.LinkedList;

import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.ANTLRException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.CharScanner;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.CommonToken;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.SemanticException;

public class TptpLexer extends antlr.CharScanner implements TptpTokenTypes, TokenStream
 {

    public void incrLine() {newline(); /* ANTLR's function*/ }
  public TptpLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public TptpLexer(Reader in) {
	this(new CharBuffer(in));
}
public TptpLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public TptpLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(true);
	literals = new Hashtable();
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling

			try {   // for lexical error handling
				switch ( LA(1)) {
				case '%':
				{
					mONE_LINE_COMMENT(true);
					theRetToken=_returnToken;
					break;
				}
				case '/':
				{
					mMANY_LINE_COMMENT(true);
					theRetToken=_returnToken;
					break;
				}
				case 'A':  case 'B':  case 'C':  case 'D':
				case 'E':  case 'F':  case 'G':  case 'H':
				case 'I':  case 'J':  case 'K':  case 'L':
				case 'M':  case 'N':  case 'O':  case 'P':
				case 'Q':  case 'R':  case 'S':  case 'T':
				case 'U':  case 'V':  case 'W':  case 'X':
				case 'Y':  case 'Z':
				{
					mUPPER_WORD(true);
					theRetToken=_returnToken;
					break;
				}
				case 'a':  case 'b':  case 'c':  case 'd':
				case 'e':  case 'f':  case 'g':  case 'h':
				case 'i':  case 'j':  case 'k':  case 'l':
				case 'm':  case 'n':  case 'o':  case 'p':
				case 'q':  case 'r':  case 's':  case 't':
				case 'u':  case 'v':  case 'w':  case 'x':
				case 'y':  case 'z':
				{
					mLOWER_WORD(true);
					theRetToken=_returnToken;
					break;
				}
				case '\'':
				{
					mSINGLE_QUOTED(true);
					theRetToken=_returnToken;
					break;
				}
				case '"':
				{
					mDISTINCT_OBJECT(true);
					theRetToken=_returnToken;
					break;
				}
				case ',':
				{
					mCOMMA(true);
					theRetToken=_returnToken;
					break;
				}
				case '(':
				{
					mLPR(true);
					theRetToken=_returnToken;
					break;
				}
				case ')':
				{
					mRPR(true);
					theRetToken=_returnToken;
					break;
				}
				case '[':
				{
					mLSB(true);
					theRetToken=_returnToken;
					break;
				}
				case ']':
				{
					mRSB(true);
					theRetToken=_returnToken;
					break;
				}
				case '&':
				{
					mAND(true);
					theRetToken=_returnToken;
					break;
				}
				case '|':
				{
					mVLINE(true);
					theRetToken=_returnToken;
					break;
				}
				case '^':
				{
					mLAMBDA(true);
					theRetToken=_returnToken;
					break;
				}
				case '>':
				{
					mMAP(true);
					theRetToken=_returnToken;
					break;
				}
				case '*':
				{
					mSTAR(true);
					theRetToken=_returnToken;
					break;
				}
				case '.':
				{
					mDOT(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if ((LA(1)=='$') && ((LA(2) >= 'a' && LA(2) <= 'z'))) {
						mDOLLAR_WORD(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='$') && (LA(2)=='$')) {
						mDOLLAR_DOLLAR_WORD(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='<')) {
						mSUBTYPE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='!') && (LA(2)=='=')) {
						mNOTEQUAL(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='+') && (LA(2)=='+')) {
						mPLUSPLUS(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='-') && (LA(2)=='-')) {
						mSEQUENT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='!') && (LA(2)=='!')) {
						mSMALLPI(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='?') && (LA(2)=='?')) {
						mSMALLSIGMA(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='!') && (LA(2)=='>')) {
						mBIGPI(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='?') && (LA(2)=='*')) {
						mBIGSIGMA(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='@') && (LA(2)=='+')) {
						mCHOICE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='@') && (LA(2)=='-')) {
						mDESCRIPTION(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)==':') && (LA(2)=='=')) {
						mASSIGN(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='=') && (LA(2)=='>')) {
						mIMPLICATION(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='~')) {
						mDISEQUIVALENCE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='~') && (LA(2)=='|')) {
						mNOT_OR(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='~') && (LA(2)=='&')) {
						mNOT_AND(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='=')) {
						mEQUIVALENCE(true);
						theRetToken=_returnToken;
					}
					else if ((_tokenSet_0.member(LA(1))) && (true)) {
						mUNSIGNED_INTEGER(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='@') && (true)) {
						mAPPLY(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='~') && (true)) {
						mTILDA(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='=') && (true)) {
						mEQUAL(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='!') && (true)) {
						mALL(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='?') && (true)) {
						mEXIST(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)==':') && (true)) {
						mCOLON(true);
						theRetToken=_returnToken;
					}
					else if ((_tokenSet_1.member(LA(1)))) {
						mWHITESPACE(true);
						theRetToken=_returnToken;
					}
				else {
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				}
				if ( _returnToken==null ) continue tryAgain; // found SKIP token
				_ttype = _returnToken.getType();
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	public final void mONE_LINE_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ONE_LINE_COMMENT;
		int _saveIndex;
		
		match('%');
		{
		if (((LA(1)=='$') && (LA(2)=='$'))&&(LA(1) == '$' && LA(2) == '$')) {
			match("$$");
		}
		else {
		}
		
		}
		{
		_loop148:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				{
				match(_tokenSet_2);
				}
			}
			else {
				break _loop148;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			_ttype = Token.SKIP;
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMANY_LINE_COMMENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MANY_LINE_COMMENT;
		int _saveIndex;
		
		match("/*");
		{
		if (((LA(1)=='$') && (LA(2)=='$'))&&(LA(1) == '$' && LA(2) == '$')) {
			match("$$");
		}
		else if (((LA(1) >= '\u0000' && LA(1) <= '\u00ff')) && ((LA(2) >= '\u0000' && LA(2) <= '\u00ff'))) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		{
		_loop156:
		do {
			// nongreedy exit test
			if ((LA(1)=='*') && (LA(2)=='/')) break _loop156;
			if ((LA(1)=='*') && ((LA(2) >= '\u0000' && LA(2) <= '\u00ff'))) {
				match('*');
			}
			else {
				boolean synPredMatched153 = false;
				if (((LA(1)=='\r') && (LA(2)=='\n'))) {
					int _m153 = mark();
					synPredMatched153 = true;
					inputState.guessing++;
					try {
						{
						match("\r\n");
						}
					}
					catch (RecognitionException pe) {
						synPredMatched153 = false;
					}
					rewind(_m153);
					inputState.guessing--;
				}
				if ( synPredMatched153 ) {
					match("\r\n");
					if ( inputState.guessing==0 ) {
						incrLine();
					}
				}
				else if ((LA(1)=='\n'||LA(1)=='\r') && ((LA(2) >= '\u0000' && LA(2) <= '\u00ff'))) {
					{
					switch ( LA(1)) {
					case '\n':
					{
						match('\n');
						break;
					}
					case '\r':
					{
						match('\r');
						break;
					}
					default:
					{
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
					}
					}
					}
					if ( inputState.guessing==0 ) {
						incrLine();
					}
				}
				else if ((_tokenSet_3.member(LA(1)))) {
					{
					match(_tokenSet_3);
					}
				}
				else {
					break _loop156;
				}
				}
			} while (true);
			}
			match("*/");
			if ( inputState.guessing==0 ) {
				_ttype = Token.SKIP;
			}
			if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
				_token = makeToken(_ttype);
				_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
			}
			_returnToken = _token;
		}
		
	public final void mDOLLAR_WORD(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DOLLAR_WORD;
		int _saveIndex;
		
		match("$");
		matchRange('a','z');
		{
		_loop159:
		do {
			switch ( LA(1)) {
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':
			{
				matchRange('a','z');
				break;
			}
			case 'A':  case 'B':  case 'C':  case 'D':
			case 'E':  case 'F':  case 'G':  case 'H':
			case 'I':  case 'J':  case 'K':  case 'L':
			case 'M':  case 'N':  case 'O':  case 'P':
			case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':
			case 'Y':  case 'Z':
			{
				matchRange('A','Z');
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				matchRange('0','9');
				break;
			}
			case '_':
			{
				match('_');
				break;
			}
			default:
			{
				break _loop159;
			}
			}
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDOLLAR_DOLLAR_WORD(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DOLLAR_DOLLAR_WORD;
		int _saveIndex;
		
		match("$$");
		matchRange('a','z');
		{
		_loop162:
		do {
			switch ( LA(1)) {
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':
			{
				matchRange('a','z');
				break;
			}
			case 'A':  case 'B':  case 'C':  case 'D':
			case 'E':  case 'F':  case 'G':  case 'H':
			case 'I':  case 'J':  case 'K':  case 'L':
			case 'M':  case 'N':  case 'O':  case 'P':
			case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':
			case 'Y':  case 'Z':
			{
				matchRange('A','Z');
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				matchRange('0','9');
				break;
			}
			case '_':
			{
				match('_');
				break;
			}
			default:
			{
				break _loop162;
			}
			}
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mUPPER_WORD(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UPPER_WORD;
		int _saveIndex;
		
		matchRange('A','Z');
		{
		_loop165:
		do {
			switch ( LA(1)) {
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':
			{
				matchRange('a','z');
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				matchRange('0','9');
				break;
			}
			case 'A':  case 'B':  case 'C':  case 'D':
			case 'E':  case 'F':  case 'G':  case 'H':
			case 'I':  case 'J':  case 'K':  case 'L':
			case 'M':  case 'N':  case 'O':  case 'P':
			case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':
			case 'Y':  case 'Z':
			{
				matchRange('A','Z');
				break;
			}
			case '_':
			{
				match('_');
				break;
			}
			default:
			{
				break _loop165;
			}
			}
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLOWER_WORD(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LOWER_WORD;
		int _saveIndex;
		
		matchRange('a','z');
		{
		_loop168:
		do {
			switch ( LA(1)) {
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':
			{
				matchRange('a','z');
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				matchRange('0','9');
				break;
			}
			case 'A':  case 'B':  case 'C':  case 'D':
			case 'E':  case 'F':  case 'G':  case 'H':
			case 'I':  case 'J':  case 'K':  case 'L':
			case 'M':  case 'N':  case 'O':  case 'P':
			case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':
			case 'Y':  case 'Z':
			{
				matchRange('A','Z');
				break;
			}
			case '_':
			{
				match('_');
				break;
			}
			default:
			{
				break _loop168;
			}
			}
		} while (true);
		}
		_ttype = testLiteralsTable(_ttype);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSINGLE_QUOTED(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SINGLE_QUOTED;
		int _saveIndex;
		Token lw=null;
		
		match("'");
		{
		boolean synPredMatched172 = false;
		if ((((LA(1) >= 'a' && LA(1) <= 'z')) && (_tokenSet_4.member(LA(2))))) {
			int _m172 = mark();
			synPredMatched172 = true;
			inputState.guessing++;
			try {
				{
				mLOWER_WORD(false);
				match("'");
				}
			}
			catch (RecognitionException pe) {
				synPredMatched172 = false;
			}
			rewind(_m172);
			inputState.guessing--;
		}
		if ( synPredMatched172 ) {
			mLOWER_WORD(true);
			lw=_returnToken;
			if ( inputState.guessing==0 ) {
				_token = lw;
			}
		}
		else if (((LA(1) >= ' ' && LA(1) <= '~')) && (true)) {
			{
			_loop175:
			do {
				switch ( LA(1)) {
				case ' ':  case '!':  case '"':  case '#':
				case '$':  case '%':  case '&':
				{
					matchRange('\40','\46');
					break;
				}
				case '(':  case ')':  case '*':  case '+':
				case ',':  case '-':  case '.':  case '/':
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':  case ':':  case ';':
				case '<':  case '=':  case '>':  case '?':
				case '@':  case 'A':  case 'B':  case 'C':
				case 'D':  case 'E':  case 'F':  case 'G':
				case 'H':  case 'I':  case 'J':  case 'K':
				case 'L':  case 'M':  case 'N':  case 'O':
				case 'P':  case 'Q':  case 'R':  case 'S':
				case 'T':  case 'U':  case 'V':  case 'W':
				case 'X':  case 'Y':  case 'Z':  case '[':
				{
					matchRange('\50','\133');
					break;
				}
				case ']':  case '^':  case '_':  case '`':
				case 'a':  case 'b':  case 'c':  case 'd':
				case 'e':  case 'f':  case 'g':  case 'h':
				case 'i':  case 'j':  case 'k':  case 'l':
				case 'm':  case 'n':  case 'o':  case 'p':
				case 'q':  case 'r':  case 's':  case 't':
				case 'u':  case 'v':  case 'w':  case 'x':
				case 'y':  case 'z':  case '{':  case '|':
				case '}':  case '~':
				{
					matchRange('\135','\176');
					break;
				}
				case '\\':
				{
					match('\\');
					{
					switch ( LA(1)) {
					case '\\':
					{
						match('\\');
						break;
					}
					case '\'':
					{
						match("'");
						break;
					}
					default:
					{
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
					}
					}
					}
					break;
				}
				default:
				{
					break _loop175;
				}
				}
			} while (true);
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		match("'");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDISTINCT_OBJECT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DISTINCT_OBJECT;
		int _saveIndex;
		
		match('"');
		{
		_loop183:
		do {
			switch ( LA(1)) {
			case ' ':  case '!':
			{
				matchRange('\40','\41');
				break;
			}
			case '#':  case '$':  case '%':  case '&':
			case '\'':  case '(':  case ')':  case '*':
			case '+':  case ',':  case '-':  case '.':
			case '/':  case '0':  case '1':  case '2':
			case '3':  case '4':  case '5':  case '6':
			case '7':  case '8':  case '9':  case ':':
			case ';':  case '<':  case '=':  case '>':
			case '?':  case '@':  case 'A':  case 'B':
			case 'C':  case 'D':  case 'E':  case 'F':
			case 'G':  case 'H':  case 'I':  case 'J':
			case 'K':  case 'L':  case 'M':  case 'N':
			case 'O':  case 'P':  case 'Q':  case 'R':
			case 'S':  case 'T':  case 'U':  case 'V':
			case 'W':  case 'X':  case 'Y':  case 'Z':
			case '[':
			{
				matchRange('\43','\133');
				break;
			}
			case ']':  case '^':  case '_':  case '`':
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':  case '{':  case '|':
			case '}':  case '~':
			{
				matchRange('\135','\176');
				break;
			}
			case '\\':
			{
				match('\\');
				{
				switch ( LA(1)) {
				case '\\':
				{
					match('\\');
					break;
				}
				case '"':
				{
					match('"');
					break;
				}
				default:
				{
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
				}
				}
				}
				break;
			}
			case '\t':
			{
				match('\11');
				break;
			}
			case '\u000b':
			{
				match('\013');
				break;
			}
			case '\u000c':
			{
				match('\014');
				break;
			}
			case '\u001b':
			{
				match('\033');
				break;
			}
			case '\n':  case '\r':
			{
				{
				switch ( LA(1)) {
				case '\n':
				{
					match('\n');
					break;
				}
				case '\r':
				{
					{
					boolean synPredMatched182 = false;
					if (((LA(1)=='\r') && (LA(2)=='\n'))) {
						int _m182 = mark();
						synPredMatched182 = true;
						inputState.guessing++;
						try {
							{
							match("\r\n");
							}
						}
						catch (RecognitionException pe) {
							synPredMatched182 = false;
						}
						rewind(_m182);
						inputState.guessing--;
					}
					if ( synPredMatched182 ) {
						match("\r\n");
					}
					else if ((LA(1)=='\r') && (_tokenSet_5.member(LA(2)))) {
						match('\r');
					}
					else {
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
					}
					
					}
					break;
				}
				default:
				{
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
				}
				}
				}
				if ( inputState.guessing==0 ) {
					incrLine();
				}
				break;
			}
			default:
				if (((LA(1) >= '\u007f' && LA(1) <= '\u00ff'))) {
					matchRange('\177','\377');
				}
			else {
				break _loop183;
			}
			}
		} while (true);
		}
		match('"');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mUNSIGNED_INTEGER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UNSIGNED_INTEGER;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '+':
		{
			_saveIndex=text.length();
			match('+');
			text.setLength(_saveIndex);
			if ( inputState.guessing==0 ) {
				_ttype = INTEGER;/* it is unsigned integer. "+" is removed */
			}
			break;
		}
		case '-':
		{
			match('-');
			if ( inputState.guessing==0 ) {
				_ttype = INTEGER;/* it is signed integer */
			}
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		{
		switch ( LA(1)) {
		case '0':
		{
			match('0');
			break;
		}
		case '1':  case '2':  case '3':  case '4':
		case '5':  case '6':  case '7':  case '8':
		case '9':
		{
			matchRange('1','9');
			{
			_loop188:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					break _loop188;
				}
				
			} while (true);
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		{
		if ((LA(1)=='/')) {
			match('/');
			{
			int _cnt191=0;
			_loop191:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					if ( _cnt191>=1 ) { break _loop191; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt191++;
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				_ttype = RATIONAL;
			}
		}
		else {
		}
		
		}
		{
		if ((LA(1)=='.')) {
			match('.');
			{
			int _cnt194=0;
			_loop194:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					matchRange('0','9');
				}
				else {
					if ( _cnt194>=1 ) { break _loop194; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt194++;
			} while (true);
			}
			if ( inputState.guessing==0 ) {
				_ttype = REAL;
			}
		}
		else {
		}
		
		}
		{
		if ((LA(1)=='E'||LA(1)=='e')) {
			{
			switch ( LA(1)) {
			case 'e':
			{
				match('e');
				break;
			}
			case 'E':
			{
				match('E');
				break;
			}
			default:
			{
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			}
			}
			{
			switch ( LA(1)) {
			case '+':
			{
				_saveIndex=text.length();
				match('+');
				text.setLength(_saveIndex);
				if ( inputState.guessing==0 ) {
					/* normalization: "+" is removed */
				}
				break;
			}
			case '-':
			{
				match('-');
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				break;
			}
			default:
			{
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			}
			}
			{
			switch ( LA(1)) {
			case '0':
			{
				match('0');
				break;
			}
			case '1':  case '2':  case '3':  case '4':
			case '5':  case '6':  case '7':  case '8':
			case '9':
			{
				matchRange('1','9');
				{
				_loop200:
				do {
					if (((LA(1) >= '0' && LA(1) <= '9'))) {
						matchRange('0','9');
					}
					else {
						break _loop200;
					}
					
				} while (true);
				}
				break;
			}
			default:
			{
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			}
			}
			if ( inputState.guessing==0 ) {
				_ttype = REAL;
			}
		}
		else {
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mINTEGER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INTEGER;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mREAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = REAL;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOMMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMMA;
		int _saveIndex;
		
		match(",");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLPR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LPR;
		int _saveIndex;
		
		match("(");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRPR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RPR;
		int _saveIndex;
		
		match(")");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLSB(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LSB;
		int _saveIndex;
		
		match("[");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRSB(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RSB;
		int _saveIndex;
		
		match("]");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAND(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AND;
		int _saveIndex;
		
		match("&");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mVLINE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = VLINE;
		int _saveIndex;
		
		match("|");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAPPLY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = APPLY;
		int _saveIndex;
		
		match("@");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLAMBDA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LAMBDA;
		int _saveIndex;
		
		match("^");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMAP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MAP;
		int _saveIndex;
		
		match(">");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSUBTYPE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SUBTYPE;
		int _saveIndex;
		
		match("<<");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSTAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STAR;
		int _saveIndex;
		
		match("*");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mTILDA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = TILDA;
		int _saveIndex;
		
		match("~");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEQUAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EQUAL;
		int _saveIndex;
		
		match("=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNOTEQUAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NOTEQUAL;
		int _saveIndex;
		
		match("!=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPLUSPLUS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PLUSPLUS;
		int _saveIndex;
		
		match("++");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSEQUENT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SEQUENT;
		int _saveIndex;
		
		match("--");
		{
		if ((LA(1)=='>')) {
			match('>');
		}
		else {
			if ( inputState.guessing==0 ) {
				_ttype = MINUSMINUS;
			}
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mMINUSMINUS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MINUSMINUS;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mALL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ALL;
		int _saveIndex;
		
		match("!");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEXIST(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EXIST;
		int _saveIndex;
		
		match("?");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSMALLPI(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SMALLPI;
		int _saveIndex;
		
		match("!!");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSMALLSIGMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SMALLSIGMA;
		int _saveIndex;
		
		match("??");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mBIGPI(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BIGPI;
		int _saveIndex;
		
		match("!>");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mBIGSIGMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BIGSIGMA;
		int _saveIndex;
		
		match("?*");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCHOICE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CHOICE;
		int _saveIndex;
		
		match("@+");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDESCRIPTION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DESCRIPTION;
		int _saveIndex;
		
		match("@-");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOLON(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COLON;
		int _saveIndex;
		
		match(":");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mASSIGN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ASSIGN;
		int _saveIndex;
		
		match(":=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDOT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DOT;
		int _saveIndex;
		
		match(".");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mIMPLICATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = IMPLICATION;
		int _saveIndex;
		
		match("=>");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDISEQUIVALENCE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DISEQUIVALENCE;
		int _saveIndex;
		
		match("<~>");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNOT_OR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NOT_OR;
		int _saveIndex;
		
		match("~|");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNOT_AND(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NOT_AND;
		int _saveIndex;
		
		match("~&");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEQUIVALENCE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EQUIVALENCE;
		int _saveIndex;
		
		match("<=");
		{
		if ((LA(1)=='>')) {
			match('>');
		}
		else {
			if ( inputState.guessing==0 ) {
				_ttype = REVERSE_IMPLICATION;
			}
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mREVERSE_IMPLICATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = REVERSE_IMPLICATION;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mWHITESPACE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WHITESPACE;
		int _saveIndex;
		
		{
		if ((_tokenSet_6.member(LA(1)))) {
			{
			switch ( LA(1)) {
			case '\u0003':  case '\u0004':  case '\u0005':  case '\u0006':
			case '\u0007':  case '\u0008':
			{
				matchRange('\003','\010');
				break;
			}
			case '\t':
			{
				match('\t');
				break;
			}
			case '\u000b':
			{
				match('\013');
				break;
			}
			case '\u000c':
			{
				match('\f');
				break;
			}
			case '\u000e':  case '\u000f':  case '\u0010':  case '\u0011':
			case '\u0012':  case '\u0013':  case '\u0014':  case '\u0015':
			case '\u0016':  case '\u0017':  case '\u0018':  case '\u0019':
			case '\u001a':  case '\u001b':  case '\u001c':  case '\u001d':
			case '\u001e':  case '\u001f':
			{
				matchRange('\016','\037');
				break;
			}
			case ' ':
			{
				match(' ');
				break;
			}
			default:
				if (((LA(1) >= '\u007f' && LA(1) <= '\u00ff'))) {
					matchRange('\177','\377');
				}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			}
			}
		}
		else if ((LA(1)=='\n'||LA(1)=='\r')) {
			{
			if ((LA(1)=='\r') && (LA(2)=='\n')) {
				match("\r\n");
			}
			else if ((LA(1)=='\n')) {
				match('\n');
			}
			else if ((LA(1)=='\r') && (true)) {
				match('\r');
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			
			}
			if ( inputState.guessing==0 ) {
				incrLine();
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		if ( inputState.guessing==0 ) {
			_ttype = Token.SKIP;
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 287992881640112128L, 0L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[8];
		data[0]=8589934584L;
		data[1]=-9223372036854775808L;
		for (int i = 2; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[8];
		data[0]=-9217L;
		for (int i = 1; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[8];
		data[0]=-4398046520321L;
		for (int i = 1; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 287949450930814976L, 576460745995190270L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = new long[8];
		data[0]=-4160733696L;
		for (int i = 1; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = new long[8];
		data[0]=8589925368L;
		data[1]=-9223372036854775808L;
		for (int i = 2; i<=3; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	
	}
