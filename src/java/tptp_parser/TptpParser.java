// $ANTLR 2.7.5 (20050128): "tptp.g" -> "TptpParser.java"$

  package tptp_parser;

  import java.util.List;
  import java.util.LinkedList;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class TptpParser extends antlr.LLkParser       implements TptpTokenTypes
 {

protected TptpParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public TptpParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected TptpParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public TptpParser(TokenStream lexer) {
  this(lexer,1);
}

public TptpParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final TptpParserOutput.TptpInput  topLevelItem(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.TptpInput in = null;
		switch ( LA(1)) {
		case LOWER_WORD:
		{
			in=tptp_input(out);
			break;
		}
		case EOF:
		{
			match(Token.EOF_TYPE);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return in;
	}
	
	public final TptpParserOutput.TptpInput  tptp_input(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.TptpInput in;
		
		Token  str = null;
		
		String nm;
		TptpParserOutput.FormulaRole role;
		TptpParserOutput.ThfFormula thf;
		TptpParserOutput.FofFormula fof;
		TptpParserOutput.CnfFormula cnf;
		TptpParserOutput.Annotations ann;
		List<String> formulaSelection;

		str = LT(1);
		
		match(LOWER_WORD);
		{
		if (((LA(1)==LPR))&&(str.getText().equals("thf"))) {
			match(LPR);
			nm=name(out);
			match(COMMA);
			role=formula_role(out);
			match(COMMA);
			thf=thf_formula(out);
			ann=annotations(out);
			match(RPR);
			match(DOT);
			in=out.createThfAnnotated(nm, role, thf, ann, str.getLine());
		}
		else if (((LA(1)==LPR))&&(str.getText().equals("fof"))) {
			match(LPR);
			nm=name(out);
			match(COMMA);
			role=formula_role(out);
			match(COMMA); 
			fof=fof_formula(out);
			ann=annotations(out); 
			match(RPR);
			match(DOT);
			in=out.createFofAnnotated(nm, role, fof, ann, str.getLine());
		}
		else if (((LA(1)==LPR))&&(str.getText().equals("cnf"))) {
			match(LPR);
			nm=name(out);
			match(COMMA);
			role=formula_role(out);
			match(COMMA);
			cnf=cnf_formula(out);
			ann=annotations(out);
			match(RPR);
			match(DOT);
			in=out.createCnfAnnotated(nm, role, cnf, ann, str.getLine());
		}
		else if (((LA(1)==LPR))&&(str.getText().equals("include"))) {
			match(LPR);
			nm=file_name(out);
			formulaSelection=formula_selection(out);
			match(RPR);
			match(DOT);
			in=out.createIncludeDirective(nm, formulaSelection, str.getLine());
		}
		else if (((LA(1)==LPR))&&(str.getText().equals("input_formula"))) {
			match(LPR);
			nm=name(out);
			match(COMMA);
			role=formula_role(out);
			match(COMMA);
			fof=fof_formula(out);
			match(RPR);
			match(DOT);
			in=out.createFofAnnotated(nm, role, fof, null, str.getLine());
		}
		else if (((LA(1)==LPR))&&(str.getText().equals("input_clause"))) {
			match(LPR);
			nm=name(out);
			match(COMMA);
			role=formula_role(out);
			match(COMMA);
			cnf=tptp_literals(out);
			match(RPR);
			match(DOT);
			in=out.createCnfAnnotated(nm, role, cnf, null, str.getLine());
		}
		else if (((LA(1)==EOF||LA(1)==LOWER_WORD))&&(true)) {
			throw new antlr.RecognitionException(
			"unexpected high level construct '" + str.getText() + "'",
			getFilename(), str.getLine(), str.getColumn());
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		
		return in;
	}
	
	public final String  number(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  i = null;
		Token  u = null;
		Token  a = null;
		Token  r = null;

		{
		switch ( LA(1)) {
		case INTEGER:
		{
			i = LT(1);
			match(INTEGER);
			str = new String(i.getText());
			
			break;
		}
		case UNSIGNED_INTEGER:
		{
			u = LT(1);
			match(UNSIGNED_INTEGER);
			str = new String(u.getText());
			break;
		}
		case RATIONAL:
		{
			a = LT(1);
			match(RATIONAL);
			str = new String(a.getText());
			
			break;
		}
		case REAL:
		{
			r = LT(1);
			match(REAL);
			str = new String(r.getText());
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return str;
	}
	
	public final String  name(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  us = null;
		
		switch ( LA(1)) {
		case LOWER_WORD:
		case SINGLE_QUOTED:
		{
			str=atomic_word(out);
			break;
		}
		case UNSIGNED_INTEGER:
		{
			us = LT(1);
			match(UNSIGNED_INTEGER);
			str=us.getText();
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return str;
	}
	
	public final String  atomic_word(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  lw = null;
		Token  sq = null;
		
		switch ( LA(1)) {
		case LOWER_WORD:
		{
			lw = LT(1);
			match(LOWER_WORD);
			str=lw.getText();
			
			break;
		}
		case SINGLE_QUOTED:
		{
			sq = LT(1);
			match(SINGLE_QUOTED);
			str=sq.getText();
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return str;
	}
	
	public final String  atomic_defined_word(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  adw = null;
		
		adw = LT(1);
		match(DOLLAR_WORD);
		str=adw.getText();
		
		return str;
	}
	
	public final String  atomic_system_word(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  asw = null;
		
		asw = LT(1);
		match(DOLLAR_DOLLAR_WORD);
		str=asw.getText();
		
		return str;
	}
	
	public final String  thf_conn_term(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		switch ( LA(1)) {
		case AND:
		{
			match(AND);
			str="&";
			break;
		}
		case VLINE:
		{
			match(VLINE);
			str="|";
			break;
		}
		case EQUAL:
		{
			match(EQUAL);
			str="=";
			break;
		}
		case IMPLICATION:
		{
			match(IMPLICATION);
			str="=>";
			break;
		}
		case DISEQUIVALENCE:
		{
			match(DISEQUIVALENCE);
			str="<~>";
			break;
		}
		case NOT_OR:
		{
			match(NOT_OR);
			str="~|";
			break;
		}
		case NOT_AND:
		{
			match(NOT_AND);
			str="~&";
			break;
		}
		case EQUIVALENCE:
		{
			match(EQUIVALENCE);
			str="<=";
			break;
		}
		case REVERSE_IMPLICATION:
		{
			match(REVERSE_IMPLICATION);
			str="<=";
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return str;
	}
	
	public final TptpParserOutput.Term  term(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Term t;
		
		String var;
		switch ( LA(1)) {
		case LOWER_WORD:
		case SINGLE_QUOTED:
		{
			t=plain_term(out);
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case DOLLAR_WORD:
		case DISTINCT_OBJECT:
		{
			t=defined_term(out);
			break;
		}
		case DOLLAR_DOLLAR_WORD:
		{
			t=system_term(out);
			break;
		}
		case UPPER_WORD:
		{
			var=variable(out);
			t = out.createVariableTerm(var);
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final TptpParserOutput.Term  plain_term(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Term t;
		
		String str;
		List<TptpParserOutput.Term> args;
		
		
		str=atomic_word(out);
		{
		switch ( LA(1)) {
		case AND:
		case VLINE:
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case RPR:
		case COMMA:
		case RSB:
		case SEQUENT:
		{
			args = null;
			
			break;
		}
		case LPR:
		{
			match(LPR);
			args=arguments(out);
			match(RPR);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		t = out.createPlainTerm(str, args);
		
		return t;
	}
	
	public final TptpParserOutput.Term  defined_term(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Term t;
		
		Token  dq = null;
		String str;
		List<TptpParserOutput.Term> args = null;
		{
		switch ( LA(1) ) {
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		{
			str=number(out);
			
			break;
		}
		case DISTINCT_OBJECT:
		{
			dq = LT(1);
			match(DISTINCT_OBJECT);
			str = dq.getText();
			
			break;
		}
		case DOLLAR_WORD:
		{
			str=atomic_defined_word(out);
			{
			switch ( LA(1)) {
			case AND:
			case VLINE:
			case IMPLICATION:
			case DISEQUIVALENCE:
			case NOT_OR:
			case NOT_AND:
			case EQUIVALENCE:
			case REVERSE_IMPLICATION:
			case RPR:
			case COMMA:
			case RSB:
			case SEQUENT:
			{
				break;
			}
			case LPR:
			{
				match(LPR);
				args=arguments(out);
				match(RPR);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		t = out.createPlainTerm(str, args);
		return t;
	}
	
	public final TptpParserOutput.Term  system_term(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Term t;
		
		String str;
		List<TptpParserOutput.Term> args;
		
		
		str=atomic_system_word(out);
		{
		switch ( LA(1)) {
		case AND:
		case VLINE:
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case RPR:
		case COMMA:
		case RSB:
		case SEQUENT:
		{
			args = null;
			break;
		}
		case LPR:
		{
			match(LPR);
			args=arguments(out);
			match(RPR);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		t = out.createSystemTerm(str, args);
		
		return t;
	}
	
	public final String  variable(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  up = null;
		
		up = LT(1);
		match(UPPER_WORD);
		str = up.getText();
		
		return str;
	}
	
	public final LinkedList<TptpParserOutput.Term>  arguments(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.Term> args;
		
		TptpParserOutput.Term t;
			
		t=term(out);
		{
		switch (LA(1)) {
		case RPR:
		{
			args = new LinkedList<TptpParserOutput.Term>();
			
			break;
		}
		case COMMA:
		{
			match(COMMA);
			args=arguments(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		args.addFirst(t);
		
		return args;
	}
	
	public final TptpParserOutput.AtomicFormula  atomic_formula(
		TptpParserOutput out, boolean[] polarity
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.AtomicFormula af;
		
		Token  dq = null;
		String str;
		TptpParserOutput.Term t1;
		TptpParserOutput.Term t2;
		List<TptpParserOutput.Term> args;
		
		switch ( LA(1)) {
		case DOLLAR_DOLLAR_WORD:
		{
			str=atomic_system_word(out);
			{
			switch ( LA(1)) {
			case AND:
			case VLINE:
			case EQUAL:
			case IMPLICATION:
			case DISEQUIVALENCE:
			case NOT_OR:
			case NOT_AND:
			case EQUIVALENCE:
			case REVERSE_IMPLICATION:
			case RPR:
			case COMMA:
			case NOTEQUAL:
			case RSB:
			case SEQUENT:
			{
				args = null;
				break;
			}
			case LPR:
			{
				match(LPR);
				args=arguments(out);
				match(RPR);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case AND:
			case VLINE:
			case IMPLICATION:
			case DISEQUIVALENCE:
			case NOT_OR:
			case NOT_AND:
			case EQUIVALENCE:
			case REVERSE_IMPLICATION:
			case RPR:
			case COMMA:
			case RSB:
			case SEQUENT:
			{
				af = out.createSystemAtom(str, args);
				break;
			}
			case EQUAL:
			case NOTEQUAL:
			{
				t1 = out.createSystemTerm(str, args);
				{
				switch ( LA(1)) {
				case EQUAL:
				{
					match(EQUAL);
					break;
				}
				case NOTEQUAL:
				{
					match(NOTEQUAL);
					polarity[0] = false;
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				t2=term(out);
				af = out.createEqualityAtom(t1, t2);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			{
			switch ( LA(1)) {
			case INTEGER:
			case UNSIGNED_INTEGER:
			case RATIONAL:
			case REAL:
			case DISTINCT_OBJECT:
			{
				{
				switch ( LA(1)) {
				case INTEGER:
				case UNSIGNED_INTEGER:
				case RATIONAL:
				case REAL:
				{
					str=number(out);
					break;
				}
				case DISTINCT_OBJECT:
				{
					dq = LT(1);
					match(DISTINCT_OBJECT);
					str = dq.getText();
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				t1 = out.createPlainTerm(str, null);
				break;
			}
			case UPPER_WORD:
			{
				str=variable(out);
				t1 = out.createVariableTerm(str);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case EQUAL:
			{
				match(EQUAL);
				break;
			}
			case NOTEQUAL:
			{
				match(NOTEQUAL);
				polarity[0] = false;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			t2=term(out);
			af = out.createEqualityAtom(t1, t2);
			break;
		}
		default:
			if (((LA(1)==DOLLAR_WORD))&&( LT(1).getText().equals("$true")
    )) {
				match(DOLLAR_WORD);
				af = out.builtInTrue();
				
			}
			else if (((LA(1)==DOLLAR_WORD))&&( LT(1).getText().equals("$false"))) {
				match(DOLLAR_WORD);
				af = out.builtInFalse();
			}
			else if (((LA(1) >= LOWER_WORD && LA(1) <= DOLLAR_WORD))) {
				{
				switch ( LA(1)) {
				case DOLLAR_WORD:
				{
					str=atomic_defined_word(out);
					break;
				}
				case LOWER_WORD:
				case SINGLE_QUOTED:
				{
					str=atomic_word(out);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case AND:
				case VLINE:
				case EQUAL:
				case IMPLICATION:
				case DISEQUIVALENCE:
				case NOT_OR:
				case NOT_AND:
				case EQUIVALENCE:
				case REVERSE_IMPLICATION:
				case RPR:
				case COMMA:
				case NOTEQUAL:
				case RSB:
				case SEQUENT:
				{
					args = null;
					break;
				}
				case LPR:
				{
					match(LPR);
					args=arguments(out);
					match(RPR);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case AND:
				case VLINE:
				case IMPLICATION:
				case DISEQUIVALENCE:
				case NOT_OR:
				case NOT_AND:
				case EQUIVALENCE:
				case REVERSE_IMPLICATION:
				case RPR:
				case COMMA:
				case RSB:
				case SEQUENT:
				{
					af = out.createPlainAtom(str, args);
					break;
				}
				case EQUAL:
				case NOTEQUAL:
				{
					t1 = out.createPlainTerm(str, args);
					{
					switch ( LA(1)) {
					case EQUAL:
					{
						match(EQUAL);
						break;
					}
					case NOTEQUAL:
					{
						match(NOTEQUAL);
						polarity[0] = false;
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					t2=term(out);
					af = out.createEqualityAtom(t1, t2);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return af;
	}
	
	public final TptpParserOutput.ThfFormula  thf_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		TptpParserOutput.BinaryConnective bc;
		TptpParserOutput.ThfFormula thf_2;
		
		
		thf=thf_unitary_formula(out);
		{
		switch ( LA(1)) {
		case EOF:
		case RPR:
		case COMMA:
		case RSB:
		{
			break;
		}
		case EQUAL:
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case NOTEQUAL:
		case SEQUENT:
		{
			bc=thf_pair_connective();
			thf_2=thf_unitary_formula(out);
			thf = out.createThfBinaryFormula(thf,bc,thf_2);
			
			break;
		}
		case AND:
		{
			{
			int _cnt34=0;
			_loop34:
			do {
				if ((LA(1)==AND)) {
					match(AND);
					thf_2=thf_unitary_formula(out);
					thf = out.createThfBinaryFormula(thf,
					TptpParserOutput.BinaryConnective.And,thf_2);
					
				}
				else {
					if ( _cnt34>=1 ) { break _loop34; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt34++;
			} while (true);
			}
			break;
		}
		case VLINE:
		{
			{
			int _cnt36=0;
			_loop36:
			do {
				if ((LA(1)==VLINE)) {
					match(VLINE);
					thf_2=thf_unitary_formula(out);
					thf = out.createThfBinaryFormula(thf,
					TptpParserOutput.BinaryConnective.Or,thf_2);
					
				}
				else {
					if ( _cnt36>=1 ) { break _loop36; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt36++;
			} while (true);
			}
			break;
		}
		case APPLY:
		{
			{
			int _cnt38=0;
			_loop38:
			do {
				if ((LA(1)==APPLY)) {
					match(APPLY);
					thf_2=thf_unitary_formula(out);
					thf = out.createThfBinaryFormula(thf,
					TptpParserOutput.BinaryConnective.Apply,thf_2);
					
				}
				else {
					if ( _cnt38>=1 ) { break _loop38; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt38++;
			} while (true);
			}
			break;
		}
		case MAP:
		{
			match(MAP);
			thf=thf_mapping_type(out,thf);
			break;
		}
		case STAR:
		{
			{
			int _cnt40=0;
			_loop40:
			do {
				if ((LA(1)==STAR)) {
					match(STAR);
					thf_2=thf_unitary_formula(out);
					thf = out.createThfBinaryFormula(thf,
					TptpParserOutput.BinaryConnective.XProd,thf_2);
					
				}
				else {
					if ( _cnt40>=1 ) { break _loop40; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt40++;
			} while (true);
			}
			break;
		}
		case PLUS:
		{
			{
			int _cnt42=0;
			_loop42:
			do {
				if ((LA(1)==PLUS)) {
					match(PLUS);
					thf_2=thf_unitary_formula(out);
					thf = out.createThfBinaryFormula(thf,
					TptpParserOutput.BinaryConnective.Union,thf_2);
					
				}
				else {
					if ( _cnt42>=1 ) { break _loop42; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt42++;
			} while (true);
			}
			break;
		}
		case SUBTYPE:
		{
			{
			int _cnt44=0;
			_loop44:
			do {
				if ((LA(1)==SUBTYPE)) {
					match(SUBTYPE);
					thf_2=thf_unitary_formula(out);
					thf = out.createThfBinaryFormula(thf,
					TptpParserOutput.BinaryConnective.Subtype,thf_2);
					
				}
				else {
					if ( _cnt44>=1 ) { break _loop44; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt44++;
			} while (true);
			}
			break;
		}
		case COLON:
		{
			{
			match(COLON);
			thf_2=thf_top_level_type(out);
			thf = out.createThfBinaryFormula(thf,
			TptpParserOutput.BinaryConnective.Type,thf_2);
			
			}
			break;
		}
		case ASSIGN:
		{
			{
			match(ASSIGN);
			thf_2=thf_formula(out);
			thf = out.createThfBinaryFormula(thf,
			TptpParserOutput.BinaryConnective.Assign,thf_2);
			
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return thf;
	}
	
	public final TptpParserOutput.ThfFormula  thf_unitary_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		
		switch ( LA(1)) {
		case ASSIGN:
		case ALL:
		case EXIST:
		case LAMBDA:
		case BIGPI:
		case BIGSIGMA:
		case CHOICE:
		case DESCRIPTION:
		{
			thf=thf_quantified_formula(out);
			break;
		}
		case TILDA:
		{
			match(TILDA);
			thf=thf_atom_or_unary_formula(out,TptpParserOutput.UnaryConnective.Negation);
			break;
		}
		case SMALLPI:
		{
			match(SMALLPI);
			thf=thf_atom_or_unary_formula(out,TptpParserOutput.UnaryConnective.UnaryPi);
			break;
		}
		case SMALLSIGMA:
		{
			match(SMALLSIGMA);
			thf=thf_atom_or_unary_formula(out,TptpParserOutput.UnaryConnective.UnarySigma);
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DOLLAR_DOLLAR_WORD:
		case AND:
		case VLINE:
		case EQUAL:
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			thf=thf_atom(out);
			break;
		}
		case LPR:
		{
			match(LPR);
			thf=thf_formula(out);
			match(RPR);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return thf;
	}
	
	public final TptpParserOutput.BinaryConnective  thf_pair_connective() throws RecognitionException, TokenStreamException {
		TptpParserOutput.BinaryConnective bc;
		
		
		switch ( LA(1)) {
		case EQUAL:
		{
			match(EQUAL);
			bc = TptpParserOutput.BinaryConnective.Equal;
			
			break;
		}
		case NOTEQUAL:
		{
			match(NOTEQUAL);
			bc = TptpParserOutput.BinaryConnective.NotEqual;
			
			break;
		}
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case SEQUENT:
		{
			bc=binary_connective();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return bc;
	}
	
	public final TptpParserOutput.ThfFormula  thf_mapping_type(
		TptpParserOutput out,TptpParserOutput.ThfFormula formula
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		TptpParserOutput.ThfFormula rest;
		
		
		thf=thf_unitary_formula(out);
		{
		switch ( LA(1)) {
		case MAP:
		{
			match(MAP);
			rest=thf_mapping_type(out,thf);
			thf = out.createThfBinaryFormula(formula,
			TptpParserOutput.BinaryConnective.Map,rest);
			
			break;
		}
		case EOF:
		case RPR:
		case COMMA:
		case RSB:
		{
			thf = out.createThfBinaryFormula(formula,
			TptpParserOutput.BinaryConnective.Map,thf);
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return thf;
	}
	
	public final TptpParserOutput.ThfFormula  thf_top_level_type(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		
		thf=thf_formula(out);
		return thf;
	}
	
	public final TptpParserOutput.BinaryConnective  binary_connective() throws RecognitionException, TokenStreamException {
		TptpParserOutput.BinaryConnective bc;
		
		
		switch ( LA(1)) {
		case EQUIVALENCE:
		{
			match(EQUIVALENCE);
			bc = TptpParserOutput.BinaryConnective.Equivalence;
			
			break;
		}
		case IMPLICATION:
		{
			match(IMPLICATION);
			bc = TptpParserOutput.BinaryConnective.Implication;
			
			break;
		}
		case REVERSE_IMPLICATION:
		{
			match(REVERSE_IMPLICATION);
			bc = TptpParserOutput.BinaryConnective.ReverseImplication;
			
			break;
		}
		case DISEQUIVALENCE:
		{
			match(DISEQUIVALENCE);
			bc = TptpParserOutput.BinaryConnective.Disequivalence;
			
			break;
		}
		case NOT_OR:
		{
			match(NOT_OR);
			bc = TptpParserOutput.BinaryConnective.NotOr;
			
			break;
		}
		case NOT_AND:
		{
			match(NOT_AND);
			bc = TptpParserOutput.BinaryConnective.NotAnd;
			
			break;
		}
		case SEQUENT:
		{
			match(SEQUENT);
			bc = TptpParserOutput.BinaryConnective.Sequent;
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return bc;
	}
	
	public final TptpParserOutput.ThfFormula  thf_quantified_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula quantified;
		
		TptpParserOutput.Quantifier q;
		List<TptpParserOutput.ThfFormula> vars;
		TptpParserOutput.ThfFormula thf;
		
		
		q=thf_quantifier();
		match(LSB);
		vars=thf_variable_list(out);
		match(RSB);
		match(COLON);
		thf=thf_unitary_formula(out);
		quantified = out.createThfQuantifiedFormula(q, vars, thf);
		
		return quantified;
	}
	
	public final TptpParserOutput.ThfFormula  thf_atom_or_unary_formula(
		TptpParserOutput out,
TptpParserOutput.UnaryConnective connective
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		
		switch ( LA(1)) {
		case LPR:
		{
			match(LPR);
			thf=thf_formula(out);
			match(RPR);
			thf = out.createThfUnaryOf(connective,thf);
			
			break;
		}
		case EOF:
		case AND:
		case VLINE:
		case EQUAL:
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case RPR:
		case COMMA:
		case NOTEQUAL:
		case APPLY:
		case MAP:
		case STAR:
		case PLUS:
		case SUBTYPE:
		case COLON:
		case ASSIGN:
		case RSB:
		case SEQUENT:
		{
			thf = out.createThfPlainAtom(connective.toString(), null);
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return thf;
	}
	
	public final TptpParserOutput.ThfFormula  thf_atom(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		Token  dq = null;
		String str;
		TptpParserOutput.Term t1;
		TptpParserOutput.Term t2;
		List<TptpParserOutput.Term> args;
		
		
		switch ( LA(1)) {
		case DOLLAR_DOLLAR_WORD:
		{
			str=atomic_system_word(out);
			{
			switch ( LA(1)) {
			case EOF:
			case AND:
			case VLINE:
			case EQUAL:
			case IMPLICATION:
			case DISEQUIVALENCE:
			case NOT_OR:
			case NOT_AND:
			case EQUIVALENCE:
			case REVERSE_IMPLICATION:
			case RPR:
			case COMMA:
			case NOTEQUAL:
			case APPLY:
			case MAP:
			case STAR:
			case PLUS:
			case SUBTYPE:
			case COLON:
			case ASSIGN:
			case RSB:
			case SEQUENT:
			{
				args = null;
				
				break;
			}
			case LPR:
			{
				match(LPR);
				args=arguments(out);
				match(RPR);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			thf = out.createThfSystemAtom(str, args);
			
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			{
			switch ( LA(1)) {
			case INTEGER:
			case UNSIGNED_INTEGER:
			case RATIONAL:
			case REAL:
			case DISTINCT_OBJECT:
			{
				{
				switch ( LA(1)) {
				case INTEGER:
				case UNSIGNED_INTEGER:
				case RATIONAL:
				case REAL:
				{
					str=number(out);
					break;
				}
				case DISTINCT_OBJECT:
				{
					dq = LT(1);
					match(DISTINCT_OBJECT);
					str = dq.getText();
					
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				thf = out.createThfPlainAtom(str, null);
				
				break;
			}
			case UPPER_WORD:
			{
				str=variable(out);
				thf = out.createThfVariableAtom(str);
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case AND:
		case VLINE:
		case EQUAL:
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		{
			{
			str=thf_conn_term(out);
			thf = out.createThfPlainAtom(str, null);
			
			}
			break;
		}
		default:
			if (((LA(1)==DOLLAR_WORD))&&( LT(1).getText().equals("$true")
    )) {
				match(DOLLAR_WORD);
				thf = out.builtInThfTrue();
				
			}
			else if (((LA(1)==DOLLAR_WORD))&&( LT(1).getText().equals("$false")
    )) {
				match(DOLLAR_WORD);
				thf = out.builtInThfFalse();
				
			}
			else if (((LA(1) >= LOWER_WORD && LA(1) <= DOLLAR_WORD))) {
				{
				switch ( LA(1)) {
				case DOLLAR_WORD:
				{
					str=atomic_defined_word(out);
					break;
				}
				case LOWER_WORD:
				case SINGLE_QUOTED:
				{
					str=atomic_word(out);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case EOF:
				case AND:
				case VLINE:
				case EQUAL:
				case IMPLICATION:
				case DISEQUIVALENCE:
				case NOT_OR:
				case NOT_AND:
				case EQUIVALENCE:
				case REVERSE_IMPLICATION:
				case RPR:
				case COMMA:
				case NOTEQUAL:
				case APPLY:
				case MAP:
				case STAR:
				case PLUS:
				case SUBTYPE:
				case COLON:
				case ASSIGN:
				case RSB:
				case SEQUENT:
				{
					args = null;
					
					break;
				}
				case LPR:
				{
					match(LPR);
					args=arguments(out);
					match(RPR);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				thf = out.createThfPlainAtom(str, args);
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return thf;
	}
	
	public final TptpParserOutput.Quantifier  thf_quantifier() throws RecognitionException, TokenStreamException {
		TptpParserOutput.Quantifier q;
		
		
		switch ( LA(1)) {
		case ALL:
		{
			match(ALL);
			q = TptpParserOutput.Quantifier.ForAll;
			
			break;
		}
		case EXIST:
		{
			match(EXIST);
			q = TptpParserOutput.Quantifier.Exists;
			
			break;
		}
		case LAMBDA:
		{
			match(LAMBDA);
			q = TptpParserOutput.Quantifier.Lambda;
			
			break;
		}
		case BIGPI:
		{
			match(BIGPI);
			q = TptpParserOutput.Quantifier.QuantifierPi;
			
			break;
		}
		case BIGSIGMA:
		{
			match(BIGSIGMA);
			q = TptpParserOutput.Quantifier.QuantifierSigma;
			
			break;
		}
		case ASSIGN:
		{
			match(ASSIGN);
			q = TptpParserOutput.Quantifier.Assign;
			
			break;
		}
		case CHOICE:
		{
			match(CHOICE);
			q = TptpParserOutput.Quantifier.Choice;
			
			break;
		}
		case DESCRIPTION:
		{
			match(DESCRIPTION);
			q = TptpParserOutput.Quantifier.Description;
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return q;
	}
	
	public final LinkedList<TptpParserOutput.ThfFormula>  thf_variable_list(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.ThfFormula> vars;
		
		String var;
		TptpParserOutput.ThfFormula type;
		TptpParserOutput.ThfFormula onevar;
		
		
		var=variable(out);
		{
		switch ( LA(1)) {
		case COLON:
		{
			match(COLON);
			type=thf_top_level_type(out);
			onevar = out.createThfVariableAtom(var);
			onevar = out.createThfBinaryFormula(onevar,
			TptpParserOutput.BinaryConnective.Type,type);
			
			break;
		}
		case COMMA:
		case RSB:
		{
			onevar = out.createThfVariableAtom(var);
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case RSB:
		{
			vars = new LinkedList<TptpParserOutput.ThfFormula>();
			
			break;
		}
		case COMMA:
		{
			match(COMMA);
			vars=thf_variable_list(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		vars.addLast(onevar);
		
		return vars;
	}
	
	public final TptpParserOutput.ThfFormula  thf_unitary_type(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		
		thf=thf_unitary_formula(out);
		return thf;
	}
	
	public final TptpParserOutput.ThfFormula  thf_defn_constant(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ThfFormula thf;
		
		
		thf=thf_formula(out);
		return thf;
	}
	
	public final TptpParserOutput.FofFormula  fof_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.FofFormula fof;
		
		TptpParserOutput.BinaryConnective bc;
		TptpParserOutput.FofFormula fof_2;
		

		fof=unitary_formula(out);
		
		{
		switch ( LA(1)) {
		case RPR:
		case COMMA:
		{
			break;
		}
		case IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case EQUIVALENCE:
		case REVERSE_IMPLICATION:
		case SEQUENT:
		{
			bc=binary_connective();
			fof_2=unitary_formula(out);
			fof = out.createBinaryFormula(fof, bc, fof_2);
			
			break;
		}
		case AND:
		{
			{
			int _cnt70=0;
			_loop70:
			do {
				if ((LA(1)==AND)) {
					match(AND);
					fof_2=unitary_formula(out);
					fof = out.createBinaryFormula(fof,
					TptpParserOutput.BinaryConnective.And, fof_2);
					
				}
				else {
					if ( _cnt70>=1 ) { break _loop70; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt70++;
			} while (true);
			}
			break;
		}
		case VLINE:
		{
			{
			int _cnt72=0;
			_loop72:
			do {
				if ((LA(1)==VLINE)) {
					match(VLINE);
					fof_2=unitary_formula(out);
					fof = out.createBinaryFormula(fof,
					TptpParserOutput.BinaryConnective.Or, fof_2);
					
				}
				else {
					if ( _cnt72>=1 ) { break _loop72; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt72++;
			} while (true);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return fof;
	}
	
	public final TptpParserOutput.FofFormula  unitary_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.FofFormula fof;
		
		boolean[] polarity ={true};
		TptpParserOutput.AtomicFormula af;
	
		switch ( LA(1)) {
		case ALL:
		case EXIST:
		{
			fof=quantified_formula(out);
			break;
		}
		case TILDA:
		{
			fof=unary_formula(out);
			break;
		}
		case LPR:
		{
			match(LPR);
			fof=fof_formula(out);
			match(RPR);
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DOLLAR_DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			af=atomic_formula(out, polarity);
			fof = out.atomAsFormula(af);
			if (!polarity[0]) fof = out.createNegationOf(fof);
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return fof;
	}
	
	public final TptpParserOutput.FofFormula  quantified_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.FofFormula fof;
		
		TptpParserOutput.Quantifier q;
		List<String> vars;
		
		
		q=quantifier();
		match(LSB);
		vars=variable_list(out);
		match(RSB);
		match(COLON);
		fof=unitary_formula(out);
		fof = out.createQuantifiedFormula(q, vars, fof);
		return fof;
	}
	
	public final TptpParserOutput.FofFormula  unary_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.FofFormula fof;
		
		
		match(TILDA);
		fof=unitary_formula(out);
		fof = out.createNegationOf(fof);
		return fof;
	}
	
	public final TptpParserOutput.Quantifier  quantifier() throws RecognitionException, TokenStreamException {
		TptpParserOutput.Quantifier q;
		
		
		switch ( LA(1)) {
		case ALL:
		{
			match(ALL);
			q = TptpParserOutput.Quantifier.ForAll;
			break;
		}
		case EXIST:
		{
			match(EXIST);
			q = TptpParserOutput.Quantifier.Exists;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return q;
	}
	
	public final LinkedList<String>  variable_list(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<String> vars;
		
		String var;
		
		var=variable(out);
		{
		switch ( LA(1)) {
		case RSB:
		{
			vars = new LinkedList<String>();
			break;
		}
		case COMMA:
		{
			match(COMMA);
			vars=variable_list(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		vars.addLast(var);
		return vars;
	}
	
	public final TptpParserOutput.CnfFormula  cnf_formula(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.CnfFormula cnf;
		
		List<TptpParserOutput.Literal> lits;
		
		{
		switch ( LA(1)) {
		case LPR:
		{
			match(LPR);
			lits=disjunction(out);
			match(RPR);
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DOLLAR_DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		case TILDA:
		{
			lits=disjunction(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		cnf = out.createClause(lits);
		return cnf;
	}
	
	public final LinkedList<TptpParserOutput.Literal>  disjunction(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.Literal> lits;
		
		TptpParserOutput.Literal lit;
		
		lit=literal(out);
		{
		switch ( LA(1)) {
		case RPR:
		case COMMA:
		{
			lits = new LinkedList<TptpParserOutput.Literal>();
			break;
		}
		case VLINE:
		{
			match(VLINE);
			lits=disjunction(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		lits.addFirst(lit);
		return lits;
	}
	
	public final TptpParserOutput.Literal  literal(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Literal lit;
		
		boolean[] polarity ={true};
		TptpParserOutput.AtomicFormula af;		
		{
		switch ( LA(1)) {
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DOLLAR_DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			af=atomic_formula(out, polarity);
			break;
		}
		case TILDA:
		{
			match(TILDA);
			af=atomic_formula(out, polarity);
			polarity[0] = !polarity[0];
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		lit = out.createLiteral(Boolean.valueOf(polarity[0]), af);
		return lit;
	}
	
	public final TptpParserOutput.CnfFormula  tptp_literals(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.CnfFormula cnf;
		
		TptpParserOutput.Literal lit;
		LinkedList<TptpParserOutput.Literal> lits;
		
		
		match(LSB);
		{
		switch ( LA(1)) {
		case RSB:
		{
			lits = null;
			break;
		}
		case PLUSPLUS:
		case MINUSMINUS:
		{
			lits = new LinkedList<TptpParserOutput.Literal>();
			lit=tptp_literal(out);
			lits.add(lit);
			{
			_loop89:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					lit=tptp_literal(out);
					lits.add(lit);
				}
				else {
					break _loop89;
				}
				
			} while (true);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(RSB);
		cnf = out.createClause(lits);
		return cnf;
	}
	
	public final TptpParserOutput.Literal  tptp_literal(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Literal lit;
		
		boolean[] polarity ={true};
		boolean b;
		TptpParserOutput.AtomicFormula af;
		
		
		b=tptp_sign();
		af=atomic_formula(out, polarity);
		lit = out.createLiteral(Boolean.valueOf(b == polarity[0]), af);
		return lit;
	}
	
	public final boolean  tptp_sign() throws RecognitionException, TokenStreamException {
		boolean b;
		
		
		switch ( LA(1)) {
		case PLUSPLUS:
		{
			match(PLUSPLUS);
			b = true;
			break;
		}
		case MINUSMINUS:
		{
			match(MINUSMINUS);
			b = false;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return b;
	}
	
	public final TptpParserOutput.GeneralTerm  general_term(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.GeneralTerm t;
		
		TptpParserOutput.GeneralTerm gt;

		switch ( LA(1)) {
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			t=general_data(out);
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				gt=general_term(out);
				t = out.createGeneralColon(t, gt);
				break;
			}
			case RPR:
			case COMMA:
			case RSB:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case LSB:
		{
			t=general_list(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final TptpParserOutput.GeneralTerm  general_data(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.GeneralTerm t;
		
		Token  d = null;
		
		String str;
		List<TptpParserOutput.GeneralTerm> args;
		
		
		switch ( LA(1)) {
		case LOWER_WORD:
		case SINGLE_QUOTED:
		{
			str=atomic_word(out);
			{
			switch ( LA(1)) {
			case RPR:
			case COMMA:
			case COLON:
			case RSB:
			{
				args = null;
				break;
			}
			case LPR:
			{
				match(LPR);
				args=general_terms(out);
				match(RPR);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			t = out.createGeneralFunction(str, args);
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		{
			str=number(out);
			t = out.createGeneralDistinctObject(str);
			break;
		}
		case DISTINCT_OBJECT:
		{
			d = LT(1);
			match(DISTINCT_OBJECT);
			t = out.createGeneralDistinctObject(d.getText());
			break;
		}
		case UPPER_WORD:
		{
			str=variable(out);
			t = out.createGeneralVariable(str);
			break;
		}
		case DOLLAR_WORD:
		{
			t=formula_data(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final TptpParserOutput.GeneralTerm  general_list(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.GeneralTerm t;
		
		List<TptpParserOutput.GeneralTerm> list = null;
		
		
		match(LSB);
		{
		switch ( LA(1)) {
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		case LSB:
		{
			list=general_terms(out);
			break;
		}
		case RSB:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(RSB);
		t = out.createGeneralList(list);
		return t;
	}
	
	public final LinkedList<TptpParserOutput.GeneralTerm>  general_terms(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.GeneralTerm> list;
		
		TptpParserOutput.GeneralTerm t;
		
		t=general_term(out);
		{
		switch ( LA(1)) {
		case RPR:
		case RSB:
		{
			list = new LinkedList<TptpParserOutput.GeneralTerm>();
			break;
		}
		case COMMA:
		{
			match(COMMA);
			list=general_terms(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		list.addFirst(t);
		return list;
	}
	
	public final TptpParserOutput.GeneralTerm  formula_data(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.GeneralTerm t;
		
		Token  dw = null;
		
		TptpParserOutput.ThfFormula thf;
		TptpParserOutput.FofFormula fof;
		TptpParserOutput.CnfFormula cnf;
		TptpParserOutput.Term term;
		
		
		dw = LT(1);
		match(DOLLAR_WORD);
		{
		if (((LA(1)==LPR))&&(dw.getText().equals("$thf"))) {
			match(LPR);
			thf=thf_formula(out);
			match(RPR);
			t = out.createGeneralThfFormula(thf);
		}
		else if (((LA(1)==LPR))&&(dw.getText().equals("$fof"))) {
			match(LPR);
			fof=fof_formula(out);
			match(RPR);
			t = out.createGeneralFofFormula(fof);
		}
		else if (((LA(1)==LPR))&&(dw.getText().equals("$cnf"))) {
			match(LPR);
			cnf=cnf_formula(out);
			match(RPR);
			t = out.createGeneralCnfFormula(cnf);
		}
		else if (((LA(1)==LPR))&&(dw.getText().equals("$fot"))) {
			match(LPR);
			term=term(out);
			match(RPR);
			t = out.createGeneralTerm(term);
		}
		else if (((LA(1) >= INTEGER && LA(1) <= WHITESPACE))) {
			matchNot(EOF);
			throw new antlr.RecognitionException("illegal general term: '"
			+ dw.getText() + "'",
			getFilename(), dw.getLine(), dw.getColumn());
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		return t;
	}
	
	public final List<TptpParserOutput.InfoItem>  optional_info(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		List<TptpParserOutput.InfoItem> list;
		
		
		switch ( LA(1)) {
		case RPR:
		{
			list = null;
			break;
		}
		case COMMA:
		{
			match(COMMA);
			list=useful_info(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return list;
	}
	
	public final List<TptpParserOutput.InfoItem>  useful_info(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		List<TptpParserOutput.InfoItem> list = null;
		
		
		match(LSB);
		{
		switch ( LA(1)) {
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		case LSB:
		{
			list=info_items(out);
			break;
		}
		case RSB:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(RSB);
		return list;
	}
	
	public final LinkedList<TptpParserOutput.InfoItem>  info_items(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.InfoItem> list;
		
		TptpParserOutput.InfoItem infoItem;
		
		infoItem=info_item(out);
		{
		switch ( LA(1)) {
		case RSB:
		{
			list = new LinkedList<TptpParserOutput.InfoItem>();
			break;
		}
		case COMMA:
		{
			match(COMMA);
			list=info_items(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		list.addFirst(infoItem);
		return list;
	}
	
	public final TptpParserOutput.InfoItem  info_item(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.InfoItem item;
		
		Token  d = null;
		String str;
		TptpParserOutput.StatusValue value;
		TptpParserOutput.Source fileSource;
		TptpParserOutput.GeneralTerm genTerm, anotherGenTerm;
		List<TptpParserOutput.GeneralTerm> args;
		List<String> nameList;
		
		
		switch ( LA(1)) {
		case LOWER_WORD:
		case SINGLE_QUOTED:
		{
			str=atomic_word(out);
			{
			if (((LA(1)==LPR))&&(str.equals("description"))) {
				match(LPR);
				str=atomic_word(out);
				match(RPR);
				item = out.createDescriptionInfoItem(str);
			}
			else if (((LA(1)==LPR))&&(str.equals("iquote"))) {
				match(LPR);
				str=atomic_word(out);
				match(RPR);
				item = out.createIQuoteInfoItem(str);
			}
			else if (((LA(1)==LPR))&&(str.equals("status"))) {
				match(LPR);
				value=status_value(out);
				match(RPR);
				item = out.createInferenceStatusInfoItem(value);
			}
			else if (((LA(1)==LPR))&&(str.equals("assumption"))) {
				match(LPR);
				match(LSB);
				nameList=name_list(out);
				match(RSB);
				match(RPR);
				item = out.createAssumptionRecordInfoItem(nameList);
			}
			else if (((LA(1)==LPR))&&(str.equals("refutation"))) {
				match(LPR);
				fileSource=file_source(out);
				match(RPR);
				item = out.createRefutationInfoItem(fileSource);
			}
			else if ((_tokenSet_0.member(LA(1)))) {
				{
				switch ( LA(1)) {
				case LPR:
				{
					match(LPR);
					args=general_terms(out);
					match(RPR);
					genTerm = out.createGeneralFunction(str,args);
					break;
				}
				case COMMA:
				case COLON:
				case RSB:
				{
					genTerm = out.createGeneralFunction(str,null);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				switch ( LA(1)) {
				case COLON:
				{
					match(COLON);
					anotherGenTerm=general_term(out);
					genTerm = out.createGeneralColon(genTerm, anotherGenTerm);
					break;
				}
				case COMMA:
				case RSB:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				item = out.createGeneralFunctionInfoItem(genTerm);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			break;
		}
		case INTEGER:
		case UNSIGNED_INTEGER:
		case RATIONAL:
		case REAL:
		case DOLLAR_WORD:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		case LSB:
		{
			{
			switch ( LA(1)) {
			case INTEGER:
			case UNSIGNED_INTEGER:
			case RATIONAL:
			case REAL:
			{
				str=number(out);
				genTerm = out.createGeneralDistinctObject(str);
				break;
			}
			case DISTINCT_OBJECT:
			{
				d = LT(1);
				match(DISTINCT_OBJECT);
				genTerm = out.createGeneralDistinctObject(d.getText());
				break;
			}
			case LSB:
			{
				genTerm=general_list(out);
				break;
			}
			case UPPER_WORD:
			{
				str=variable(out);
				genTerm = out.createGeneralVariable(str);
				break;
			}
			case DOLLAR_WORD:
			{
				genTerm=formula_data(out);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				anotherGenTerm=general_term(out);
				genTerm = out.createGeneralColon(genTerm, anotherGenTerm);
				break;
			}
			case COMMA:
			case RSB:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			item = out.createGeneralFunctionInfoItem(genTerm);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return item;
	}
	
	public final TptpParserOutput.StatusValue  status_value(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.StatusValue value;
		
		Token  id = null;
		
		id = LT(1);
		match(LOWER_WORD);
		if (id.getText().equals("thm"))
		{value = TptpParserOutput.StatusValue.Thm;}
		else if (id.getText().equals("sat"))
		{value = TptpParserOutput.StatusValue.Sat;}
		else if (id.getText().equals("csa"))
		{value = TptpParserOutput.StatusValue.Csa;}
		else if (id.getText().equals("uns"))
		{value = TptpParserOutput.StatusValue.Uns;}
		else if (id.getText().equals("cth"))
		{value = TptpParserOutput.StatusValue.Cth;}
		else if (id.getText().equals("esa"))
		{value = TptpParserOutput.StatusValue.Esa;}
		else if (id.getText().equals("unknown"))
		{value = TptpParserOutput.StatusValue.Unknown;}
		else {/* ERROR. Unknown <status value> string */
		throw new antlr.RecognitionException("unknown status value: '"
		+ id.getText() + "'",
		getFilename(), id.getLine(), id.getColumn());
		}
		
		return value;
	}
	
	public final LinkedList<String>  name_list(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<String> list;
		
		String str;
		
		str=name(out);
		{
		switch ( LA(1)) {
		case RSB:
		{
			list = new LinkedList<String>();
			break;
		}
		case COMMA:
		{
			match(COMMA);
			list=name_list(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		list.addFirst(str);
		return list;
	}
	
	public final TptpParserOutput.Source  file_source(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Source src;
		
		Token  lw = null;
		String fileName;
		String fileInfo;
		
		
		lw = LT(1);
		match(LOWER_WORD);
		{
		if (((LA(1)==LPR))&&(lw.getText().equals("file"))) {
			match(LPR);
			fileName=file_name(out);
			fileInfo=file_info(out);
			match(RPR);
			src = out.createSourceFromFile(fileName, fileInfo);
		}
		else if ((LA(1)==RPR)) {
			throw new antlr.RecognitionException("file source expected"
			+ "but found " + lw,
			getFilename(), lw.getLine(), lw.getColumn());
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		return src;
	}
	
	public final TptpParserOutput.Source  source(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Source s;
		
		Token  ui = null;
		String str;
		String str2;
		List<TptpParserOutput.InfoItem> usefulInfo;
		List<TptpParserOutput.ParentInfo> parentList;
		/*    TptpParserOutput.IntroType introType; */
		String introType;
		LinkedList<TptpParserOutput.Source> listOfSources = null;
		
		
		switch ( LA(1)) {
		case LSB:
		{
			match(LSB);
			listOfSources=sources(out);
			match(RSB);
			s = out.createSourceFromListOfSources(listOfSources);
			
			break;
		}
		case UNSIGNED_INTEGER:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		{
			{
			switch ( LA(1)) {
			case LOWER_WORD:
			case SINGLE_QUOTED:
			{
				str=atomic_word(out);
				{
				if (((LA(1)==LPR))&&( str.equals("inference"))) {
					match(LPR);
					str=inference_rule(out);
					match(COMMA);
					usefulInfo=useful_info(out);
					match(COMMA);
					match(LSB);
					parentList=parent_list(out);
					match(RSB);
					match(RPR);
					s = out.createSourceFromInferenceRecord(str,usefulInfo,parentList);
					
				}
				else if (((LA(1)==LPR))&&( str.equals("introduced"))) {
					match(LPR);
					introType=intro_type(out);
					usefulInfo=optional_info(out);
					match(RPR);
					s = out.createInternalSource(introType, usefulInfo);
					
				}
				else if (((LA(1)==LPR))&&( str.equals("file"))) {
					match(LPR);
					str=file_name(out);
					str2=file_info(out);
					match(RPR);
					s = out.createSourceFromFile(str, str2);
					
				}
				else if (((LA(1)==LPR))&&( str.equals("creator"))) {
					match(LPR);
					str=creator_name(out);
					usefulInfo=optional_info(out);
					match(RPR);
					s = out.createSourceFromCreator(str, usefulInfo);
					
				}
				else if (((LA(1)==LPR))&&( str.equals("theory"))) {
					match(LPR);
					str=theory_name(out);
					usefulInfo=optional_info(out);
					match(RPR);
					s = out.createSourceFromTheory(str, usefulInfo);
					
				}
				else if (((_tokenSet_1.member(LA(1))))&&( str.equals("unknown"))) {
					s = out.createSourceFromName(new String("unknown"));
					
				}
				else if ((_tokenSet_1.member(LA(1)))) {
					s = out.createSourceFromName(str);
					
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				break;
			}
			case UNSIGNED_INTEGER:
			{
				ui = LT(1);
				match(UNSIGNED_INTEGER);
				s = out.createSourceFromName(ui.getText());
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return s;
	}
	
	public final LinkedList<TptpParserOutput.Source>  sources(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.Source> list;
		
		TptpParserOutput.Source sourceItem;
		
		sourceItem=source(out);
		{
		switch ( LA(1)) {
		case RSB:
		{
			list = new LinkedList<TptpParserOutput.Source>();
			
			break;
		}
		case COMMA:
		{
			match(COMMA);
			list=sources(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		list.addFirst(sourceItem);
		
		return list;
	}
	
	public final String  inference_rule(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		try {
			str=atomic_word(out);
		}
		catch (Exception ex) {
			str = "";
		}

		return str;
	}
	
	public final LinkedList<TptpParserOutput.ParentInfo>  parent_list(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		LinkedList<TptpParserOutput.ParentInfo> list;
		
		TptpParserOutput.ParentInfo info;
		
		info=parent_info(out);
		{
		switch ( LA(1)) {
		case RSB:
		{
			list = new LinkedList<TptpParserOutput.ParentInfo>();
			break;
		}
		case COMMA:
		{
			match(COMMA);
			list=parent_list(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		list.addFirst(info);
		return list;
	}
	
	public final String  intro_type(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		str=atomic_word(out);
		return str;
	}
	
	public final String  file_name(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		str=atomic_word(out);
		return str;
	}
	
	public final String  file_info(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		switch ( LA(1)) {
		case RPR:
		{
			str = null;
			break;
		}
		case COMMA:
		{
			match(COMMA);
			str=name(out);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return str;
	}
	
	public final String  creator_name(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		str=atomic_word(out);
		return str;
	}
	
	public final String theory_name(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str = null;
		
		Token  lw = null;
		
		lw = LT(1);
		match(LOWER_WORD);

		if (((LA(1)==RPR||LA(1)==COMMA))&&(lw.getText().equals("equality"))) {
			str = new String("equality");
		}
		else if (((LA(1)==RPR||LA(1)==COMMA))&&(lw.getText().equals("ac"))) {
			str = new String("ac");
		}
		else if (((LA(1)==RPR||LA(1)==COMMA))&&(lw.getText().equals("answers"))) {
			str = new String("answers");
		}
		else if ((LA(1)==RPR||LA(1)==COMMA)) {
			throw new antlr.RecognitionException("Error in TptpParser.theory_name(): unknown theory name: '"
				+ lw.getText() + "' in line " + lw.getLine(),
				getFilename(), lw.getLine(), lw.getColumn());
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		return str;
	}
	
	public final TptpParserOutput.ParentInfo  parent_info(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ParentInfo info;
		
		TptpParserOutput.Source src;
		TptpParserOutput.GeneralTerm t;
		
		
		src=source(out);
		t=parent_details(out);
		info = out.createParentInfo(src, t);
		return info;
	}
	
	public final TptpParserOutput.GeneralTerm  parent_details(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.GeneralTerm t;
		
		
		switch ( LA(1)) {
		case COLON:
		{
			match(COLON);
			t=general_list(out);
			break;
		}
		case COMMA:
		case RSB:
		{
			t = null;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final List<TptpParserOutput.InfoItem>  intro_info(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		List<TptpParserOutput.InfoItem> info;
		
		
		switch ( LA(1)) {
		case COMMA:
		{
			match(COMMA);
			info=useful_info(out);
			break;
		}
		case EOF:
		{
			info = null;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return info;
	}
	
	public final List<TptpParserOutput.TptpInput>  tptp_file(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		List<TptpParserOutput.TptpInput> list
                      = new LinkedList<TptpParserOutput.TptpInput>();
		
		TptpParserOutput.TptpInput in;
		
		
		{
		_loop135:
		do {
			if ((LA(1)==LOWER_WORD)) {
				in=tptp_input(out);
				list.add(in);
			}
			else {
				break _loop135;
			}
			
		} while (true);
		}
		return list;
	}
	
	public final TptpParserOutput.FormulaRole  formula_role(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.FormulaRole role;
		
		Token  str = null;
		
		str = LT(1);
		match(LOWER_WORD);
		{
		if (((LA(1)==COMMA))&&(str.getText().equals("axiom"))) {
			role = TptpParserOutput.FormulaRole.Axiom;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("hypothesis"))) {
			role = TptpParserOutput.FormulaRole.Hypothesis;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("definition"))) {
			role = TptpParserOutput.FormulaRole.Definition;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("assumption"))) {
			role = TptpParserOutput.FormulaRole.Assumption;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("lemma"))) {
			role = TptpParserOutput.FormulaRole.Lemma;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("theorem"))) {
			role = TptpParserOutput.FormulaRole.Theorem;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("conjecture"))) {
			role = TptpParserOutput.FormulaRole.Conjecture;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("question"))) {
			role = TptpParserOutput.FormulaRole.Question;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("negated_conjecture"))) {
			role = TptpParserOutput.FormulaRole.NegatedConjecture;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("plain"))) {
			role = TptpParserOutput.FormulaRole.Plain;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("answer"))) {
			role = TptpParserOutput.FormulaRole.Answer;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("fi_domain"))) {
			role = TptpParserOutput.FormulaRole.FiDomain;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("fi_functors"))) {
			role = TptpParserOutput.FormulaRole.FiFunctors;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("fi_predicates"))) {
			role = TptpParserOutput.FormulaRole.FiPredicates;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("type"))) {
			role = TptpParserOutput.FormulaRole.Type;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("unknown"))) {
			role = TptpParserOutput.FormulaRole.Unknown;
		}
		else if ((LA(1)==COMMA)) {
			/* ERROR. Unknown <type> string */
			throw new antlr.RecognitionException("unknown formula type: '"
			+ str.getText() + "'",
			getFilename(), str.getLine(), str.getColumn());
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		return role;
	}
	
	public final TptpParserOutput.Annotations  annotations(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.Annotations ann;
		
		TptpParserOutput.Source src;
		List<TptpParserOutput.InfoItem> usefulInfo;
		
		
		switch ( LA(1)) {
		case RPR:
		{
			ann = null;
			break;
		}
		case COMMA:
		{
			match(COMMA);
			src=source(out);
			usefulInfo=optional_info(out);
			ann = out.createAnnotations(src, usefulInfo);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return ann;
	}
	
	public final List<String>  formula_selection(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		List<String> list;
		
		String str;
		
		switch ( LA(1)) {
		case RPR:
		{
			list = null;
			break;
		}
		case COMMA:
		{
			match(COMMA);
			match(LSB);
			list=name_list(out);
			match(RSB);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return list;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"INTEGER",
		"UNSIGNED_INTEGER",
		"RATIONAL",
		"REAL",
		"LOWER_WORD",
		"SINGLE_QUOTED",
		"DOLLAR_WORD",
		"DOLLAR_DOLLAR_WORD",
		"AND",
		"VLINE",
		"EQUAL",
		"IMPLICATION",
		"DISEQUIVALENCE",
		"NOT_OR",
		"NOT_AND",
		"EQUIVALENCE",
		"REVERSE_IMPLICATION",
		"LPR",
		"RPR",
		"COMMA",
		"DISTINCT_OBJECT",
		"UPPER_WORD",
		"NOTEQUAL",
		"APPLY",
		"MAP",
		"STAR",
		"PLUS",
		"SUBTYPE",
		"COLON",
		"ASSIGN",
		"TILDA",
		"SMALLPI",
		"SMALLSIGMA",
		"LSB",
		"RSB",
		"ALL",
		"EXIST",
		"LAMBDA",
		"BIGPI",
		"BIGSIGMA",
		"CHOICE",
		"DESCRIPTION",
		"SEQUENT",
		"PLUSPLUS",
		"MINUSMINUS",
		"DOT",
		"ONE_LINE_COMMENT",
		"MANY_LINE_COMMENT",
		"WHITESPACE"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 279183360000L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 279185457152L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	
	}
