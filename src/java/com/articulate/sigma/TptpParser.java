package com.articulate.sigma;

// $ANTLR 2.7.5 (20050128): "tptp.g" -> "TptpParser.java"$

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

import java.util.List;              
import java.util.LinkedList; 

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
		TptpParserOutput.FofFormula fof;
		TptpParserOutput.CnfFormula cnf;
		TptpParserOutput.Annotations ann;
		List<String> formulaSelection;
		
		
		str = LT(1);
		match(LOWER_WORD);
		{
		if (((LA(1)==LPR))&&(str.getText().equals("fof"))) {
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
			str = new String("+") + u.getText();
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
		case REAL:
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
		case RPR:
		case COMMA:
		case AND:
		case VLINE:
		case EQUIVALENCE:
		case IMPLICATION:
		case REVERSE_IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case RSB:
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
		
		{
		switch ( LA(1)) {
		case INTEGER:
		case UNSIGNED_INTEGER:
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
		t = out.createPlainTerm(str, null);
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
		case RPR:
		case COMMA:
		case AND:
		case VLINE:
		case EQUIVALENCE:
		case IMPLICATION:
		case REVERSE_IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		case RSB:
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
		switch ( LA(1)) {
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
			case RPR:
			case COMMA:
			case EQUAL:
			case NOTEQUAL:
			case AND:
			case VLINE:
			case EQUIVALENCE:
			case IMPLICATION:
			case REVERSE_IMPLICATION:
			case DISEQUIVALENCE:
			case NOT_OR:
			case NOT_AND:
			case RSB:
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
			case RPR:
			case COMMA:
			case AND:
			case VLINE:
			case EQUIVALENCE:
			case IMPLICATION:
			case REVERSE_IMPLICATION:
			case DISEQUIVALENCE:
			case NOT_OR:
			case NOT_AND:
			case RSB:
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
		case REAL:
		case DISTINCT_OBJECT:
		case UPPER_WORD:
		{
			{
			switch ( LA(1)) {
			case INTEGER:
			case UNSIGNED_INTEGER:
			case REAL:
			case DISTINCT_OBJECT:
			{
				t1=defined_term(out);
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
			if (((LA(1)==DOLLAR_WORD))&&(LT(1).getText().equals("$true"))) {
				match(DOLLAR_WORD);
				af = out.builtInTrue();
			}
			else if (((LA(1)==DOLLAR_WORD))&&(LT(1).getText().equals("$false"))) {
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
				case RPR:
				case COMMA:
				case EQUAL:
				case NOTEQUAL:
				case AND:
				case VLINE:
				case EQUIVALENCE:
				case IMPLICATION:
				case REVERSE_IMPLICATION:
				case DISEQUIVALENCE:
				case NOT_OR:
				case NOT_AND:
				case RSB:
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
				case RPR:
				case COMMA:
				case AND:
				case VLINE:
				case EQUIVALENCE:
				case IMPLICATION:
				case REVERSE_IMPLICATION:
				case DISEQUIVALENCE:
				case NOT_OR:
				case NOT_AND:
				case RSB:
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
		case EQUIVALENCE:
		case IMPLICATION:
		case REVERSE_IMPLICATION:
		case DISEQUIVALENCE:
		case NOT_OR:
		case NOT_AND:
		{
			bc=binary_connective();
			fof_2=unitary_formula(out);
			fof = out.createBinaryFormula(fof, bc, fof_2);
			break;
		}
		case AND:
		{
			{
			int _cnt31=0;
			_loop31:
			do {
				if ((LA(1)==AND)) {
					match(AND);
					fof_2=unitary_formula(out);
					fof = out.createBinaryFormula(fof, 
					TptpParserOutput.BinaryConnective.And, fof_2);
				}
				else {
					if ( _cnt31>=1 ) { break _loop31; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt31++;
			} while (true);
			}
			break;
		}
		case VLINE:
		{
			{
			int _cnt33=0;
			_loop33:
			do {
				if ((LA(1)==VLINE)) {
					match(VLINE);
					fof_2=unitary_formula(out);
					fof = out.createBinaryFormula(fof, 
					TptpParserOutput.BinaryConnective.Or, fof_2);
				}
				else {
					if ( _cnt33>=1 ) { break _loop33; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt33++;
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
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return bc;
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
		vars.addFirst(var);
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
		lit = out.createLiteral(new Boolean(polarity[0]), af);
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
			_loop50:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					lit=tptp_literal(out);
					lits.add(lit);
				}
				else {
					break _loop50;
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
		lit = out.createLiteral(new Boolean(b == polarity[0]), af);
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
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DISTINCT_OBJECT:
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
			case RSB:
			case COLON:
			{
				args = null;
				break;
			}
			case LPR:
			{
				match(LPR);
				args=general_arguments(out);
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
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DISTINCT_OBJECT:
		case LSB:
		{
			list=general_arguments(out);
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
	
	public final LinkedList<TptpParserOutput.GeneralTerm>  general_arguments(
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
			list=general_arguments(out);
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
		case REAL:
		case LOWER_WORD:
		case SINGLE_QUOTED:
		case DISTINCT_OBJECT:
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
					args=general_arguments(out);
					match(RPR);
					genTerm = out.createGeneralFunction(str,args);
					break;
				}
				case COMMA:
				case RSB:
				case COLON:
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
		case REAL:
		case DISTINCT_OBJECT:
		case LSB:
		{
			{
			switch ( LA(1)) {
			case INTEGER:
			case UNSIGNED_INTEGER:
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
		if (id.getText().equals("tau"))      
		{value = TptpParserOutput.StatusValue.Tau;}
		else if (id.getText().equals("tac"))
		{value = TptpParserOutput.StatusValue.Tac;}
		else if (id.getText().equals("eqv"))
		{value = TptpParserOutput.StatusValue.Eqv;}
		else if (id.getText().equals("thm"))
		{value = TptpParserOutput.StatusValue.Thm;}
		else if (id.getText().equals("sat"))
		{value = TptpParserOutput.StatusValue.Sat;}
		else if (id.getText().equals("cax"))
		{value = TptpParserOutput.StatusValue.Cax;}
		else if (id.getText().equals("noc"))
		{value = TptpParserOutput.StatusValue.Noc;}
		else if (id.getText().equals("csa"))
		{value = TptpParserOutput.StatusValue.Csa;}
		else if (id.getText().equals("cth"))
		{value = TptpParserOutput.StatusValue.Cth;}
		else if (id.getText().equals("ceq"))
		{value = TptpParserOutput.StatusValue.Ceq;}
		else if (id.getText().equals("unc"))
		{value = TptpParserOutput.StatusValue.Unc;}
		else if (id.getText().equals("uns"))
		{value = TptpParserOutput.StatusValue.Uns;}
		else if (id.getText().equals("sab"))
		{value = TptpParserOutput.StatusValue.Sab;}
		else if (id.getText().equals("sam"))
		{value = TptpParserOutput.StatusValue.Sam;}
		else if (id.getText().equals("sar"))
		{value = TptpParserOutput.StatusValue.Sar;}
		else if (id.getText().equals("sap"))
		{value = TptpParserOutput.StatusValue.Sap;}
		else if (id.getText().equals("csp"))
		{value = TptpParserOutput.StatusValue.Csp;}
		else if (id.getText().equals("csr"))
		{value = TptpParserOutput.StatusValue.Csr;}
		else if (id.getText().equals("csm"))
		{value = TptpParserOutput.StatusValue.Csm;}
		else if (id.getText().equals("csb"))
		{value = TptpParserOutput.StatusValue.Csb;}
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
		TptpParserOutput.IntroType introType;
		
		
		switch ( LA(1)) {
		case LOWER_WORD:
		case SINGLE_QUOTED:
		{
			str=atomic_word(out);
			{
			if (((LA(1)==LPR))&&(str.equals("inference"))) {
				match(LPR);
				str=inference_rule(out);
				match(COMMA);
				usefulInfo=useful_info(out);
				match(COMMA);
				match(LSB);
				parentList=parent_list(out);
				match(RSB);
				match(RPR);
				s = out.createSourceFromInferenceRecord(
				str, usefulInfo, parentList);
				
			}
			else if (((LA(1)==LPR))&&(str.equals("introduced"))) {
				match(LPR);
				introType=intro_type(out);
				usefulInfo=optional_info(out);
				match(RPR);
				s = out.createInternalSource(introType, usefulInfo);
			}
			else if (((LA(1)==LPR))&&(str.equals("file"))) {
				match(LPR);
				str=file_name(out);
				str2=file_info(out);
				match(RPR);
				s = out.createSourceFromFile(str, str2);
			}
			else if (((LA(1)==LPR))&&(str.equals("creator"))) {
				match(LPR);
				str=creator_name(out);
				usefulInfo=optional_info(out);
				match(RPR);
				s = out.createSourceFromCreator(str, usefulInfo);
			}
			else if (((LA(1)==LPR))&&(str.equals("theory"))) {
				match(LPR);
				str=theory_name(out);
				usefulInfo=optional_info(out);
				match(RPR);
				s = out.createSourceFromTheory(str, usefulInfo);
			}
			else if (((_tokenSet_1.member(LA(1))))&&(str.equals("unknown"))) {
				s = out.createSourceFromName(
				new String("unknown"));
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
		return s;
	}
	
	public final String  inference_rule(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		str=atomic_word(out);
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
	
	public final TptpParserOutput.IntroType  intro_type(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.IntroType type;
		
		Token  str = null;
		
		str = LT(1);
		match(LOWER_WORD);
		{
		if (((LA(1)==RPR||LA(1)==COMMA))&&(str.getText().equals("definition"))) {
			type = TptpParserOutput.IntroType.Definition;
		}
		else if (((LA(1)==RPR||LA(1)==COMMA))&&(str.getText().equals("axiom_of_choice"))) {
			type = TptpParserOutput.IntroType.AxiomOfChoice;
		}
		else if (((LA(1)==RPR||LA(1)==COMMA))&&(str.getText().equals("tautology"))) {
			type = TptpParserOutput.IntroType.Tautology;
		}
		else if (((LA(1)==RPR||LA(1)==COMMA))&&(str.getText().equals("assumption"))) {
			type = TptpParserOutput.IntroType.Assumption;
		}
		else if ((LA(1)==RPR||LA(1)==COMMA)) {
			throw new antlr.RecognitionException("unknown intro type: '"
			+ str.getText() + "'",
			getFilename(), str.getLine(), str.getColumn());
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		return type;
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
	
	public final String  theory_name(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		Token  lw = null;
		
		lw = LT(1);
		match(LOWER_WORD);
		{
		if (((LA(1)==RPR||LA(1)==COMMA))&&(lw.getText().equals("equality"))) {
			str = new String("equality");
		}
		else if (((LA(1)==RPR||LA(1)==COMMA))&&(lw.getText().equals("ac"))) {
			str = new String("ac");
		}
		else if ((LA(1)==RPR||LA(1)==COMMA)) {
			throw new antlr.RecognitionException("unknown theory name: '"
			+ lw.getText() + "'",
			getFilename(), lw.getLine(), lw.getColumn());
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		return str;
	}
	
	public final TptpParserOutput.ParentInfo  parent_info(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		TptpParserOutput.ParentInfo info;
		
		TptpParserOutput.Source src;
		String str;
		
		
		src=source(out);
		str=parent_details(out);
		info = out.createParentInfo(src, str);
		return info;
	}
	
	public final String  parent_details(
		TptpParserOutput out
	) throws RecognitionException, TokenStreamException {
		String str;
		
		
		switch ( LA(1)) {
		case COLON:
		{
			match(COLON);
			str=atomic_word(out);
			break;
		}
		case COMMA:
		case RSB:
		{
			str = null;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return str;
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
		_loop92:
		do {
			if ((LA(1)==LOWER_WORD)) {
				in=tptp_input(out);
				list.add(in);
			}
			else {
				break _loop92;
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
		else if (((LA(1)==COMMA))&&(str.getText().equals("negated_conjecture"))) {
			role = TptpParserOutput.FormulaRole.NegatedConjecture;
		}
		else if (((LA(1)==COMMA))&&(str.getText().equals("plain"))) {
			role = TptpParserOutput.FormulaRole.Plain;
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
		"REAL",
		"LOWER_WORD",
		"SINGLE_QUOTED",
		"DOLLAR_WORD",
		"DOLLAR_DOLLAR_WORD",
		"LPR",
		"RPR",
		"COMMA",
		"DISTINCT_OBJECT",
		"UPPER_WORD",
		"EQUAL",
		"NOTEQUAL",
		"AND",
		"VLINE",
		"EQUIVALENCE",
		"IMPLICATION",
		"REVERSE_IMPLICATION",
		"DISEQUIVALENCE",
		"NOT_OR",
		"NOT_AND",
		"TILDA",
		"LSB",
		"RSB",
		"COLON",
		"ALL",
		"EXIST",
		"PLUSPLUS",
		"MINUSMINUS",
		"DOT",
		"ONE_LINE_COMMENT",
		"MANY_LINE_COMMENT",
		"STAR",
		"WHITESPACE"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 805316608L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 805318656L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	
	}
