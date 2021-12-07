// Generated from tptp_v7_0_0_0.g4 by ANTLR 4.7.2
package tptp_parser_v70;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class tptp_v7_0_0_0Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, Or=44, And=45, Iff=46, 
		Impl=47, If=48, Niff=49, Nor=50, Nand=51, Not=52, ForallComb=53, TyForall=54, 
		Infix_inequality=55, Infix_equality=56, Forall=57, ExistsComb=58, TyExists=59, 
		Exists=60, Lambda=61, ChoiceComb=62, Choice=63, DescriptionComb=64, Description=65, 
		EqComb=66, App=67, Assignment=68, Arrow=69, Star=70, Plus=71, Subtype_sign=72, 
		Gentzen_arrow=73, Real=74, Signed_real=75, Unsigned_real=76, Rational=77, 
		Signed_rational=78, Unsigned_rational=79, Integer=80, Signed_integer=81, 
		Unsigned_integer=82, Decimal=83, Positive_decimal=84, Decimal_exponent=85, 
		Decimal_fraction=86, Dot_decimal=87, Exp_integer=88, Signed_exp_integer=89, 
		Unsigned_exp_integer=90, Dollar_word=91, Dollar_dollar_word=92, Upper_word=93, 
		Lower_word=94, Single_quoted=95, Distinct_object=96, WS=97, Line_comment=98, 
		Block_comment=99;
	public static final int
		RULE_tptp_file = 0, RULE_tptp_input = 1, RULE_annotated_formula = 2, RULE_tpi_annotated = 3, 
		RULE_tpi_formula = 4, RULE_thf_annotated = 5, RULE_tfx_annotated = 6, 
		RULE_tff_annotated = 7, RULE_tcf_annotated = 8, RULE_fof_annotated = 9, 
		RULE_cnf_annotated = 10, RULE_annotations = 11, RULE_formula_role = 12, 
		RULE_thf_formula = 13, RULE_thf_logic_formula = 14, RULE_thf_binary_formula = 15, 
		RULE_thf_binary_pair = 16, RULE_thf_binary_tuple = 17, RULE_thf_or_formula = 18, 
		RULE_thf_and_formula = 19, RULE_thf_apply_formula = 20, RULE_thf_unitary_formula = 21, 
		RULE_thf_quantified_formula = 22, RULE_thf_quantification = 23, RULE_thf_variable_list = 24, 
		RULE_thf_variable = 25, RULE_thf_typed_variable = 26, RULE_thf_unary_formula = 27, 
		RULE_thf_atom = 28, RULE_thf_function = 29, RULE_thf_conn_term = 30, RULE_thf_conditional = 31, 
		RULE_thf_let = 32, RULE_thf_arguments = 33, RULE_thf_type_formula = 34, 
		RULE_thf_typeable_formula = 35, RULE_thf_subtype = 36, RULE_thf_top_level_type = 37, 
		RULE_thf_unitary_type = 38, RULE_thf_apply_type = 39, RULE_thf_binary_type = 40, 
		RULE_thf_mapping_type = 41, RULE_thf_xprod_type = 42, RULE_thf_union_type = 43, 
		RULE_thf_sequent = 44, RULE_thf_tuple = 45, RULE_thf_formula_list = 46, 
		RULE_tfx_formula = 47, RULE_tfx_logic_formula = 48, RULE_tff_formula = 49, 
		RULE_tff_logic_formula = 50, RULE_tff_binary_formula = 51, RULE_tff_binary_nonassoc = 52, 
		RULE_tff_binary_assoc = 53, RULE_tff_or_formula = 54, RULE_tff_and_formula = 55, 
		RULE_tff_unitary_formula = 56, RULE_tff_quantified_formula = 57, RULE_tff_variable_list = 58, 
		RULE_tff_variable = 59, RULE_tff_typed_variable = 60, RULE_tff_unary_formula = 61, 
		RULE_tff_atomic_formula = 62, RULE_tff_conditional = 63, RULE_tff_let = 64, 
		RULE_tff_let_term_defns = 65, RULE_tff_let_term_list = 66, RULE_tff_let_term_defn = 67, 
		RULE_tff_let_term_binding = 68, RULE_tff_let_formula_defns = 69, RULE_tff_let_formula_list = 70, 
		RULE_tff_let_formula_defn = 71, RULE_tff_let_formula_binding = 72, RULE_tff_sequent = 73, 
		RULE_tff_formula_tuple = 74, RULE_tff_formula_tuple_list = 75, RULE_tff_typed_atom = 76, 
		RULE_tff_subtype = 77, RULE_tff_top_level_type = 78, RULE_tf1_quantified_type = 79, 
		RULE_tff_monotype = 80, RULE_tff_unitary_type = 81, RULE_tff_atomic_type = 82, 
		RULE_tff_type_arguments = 83, RULE_tff_mapping_type = 84, RULE_tff_xprod_type = 85, 
		RULE_tcf_formula = 86, RULE_tcf_logic_formula = 87, RULE_tcf_quantified_formula = 88, 
		RULE_fof_formula = 89, RULE_fof_logic_formula = 90, RULE_fof_binary_formula = 91, 
		RULE_fof_binary_nonassoc = 92, RULE_fof_binary_assoc = 93, RULE_fof_or_formula = 94, 
		RULE_fof_and_formula = 95, RULE_fof_unitary_formula = 96, RULE_fof_quantified_formula = 97, 
		RULE_fof_variable_list = 98, RULE_fof_unary_formula = 99, RULE_fof_infix_unary = 100, 
		RULE_fof_atomic_formula = 101, RULE_fof_plain_atomic_formula = 102, RULE_fof_defined_atomic_formula = 103, 
		RULE_fof_defined_plain_formula = 104, RULE_fof_defined_infix_formula = 105, 
		RULE_fof_system_atomic_formula = 106, RULE_fof_plain_term = 107, RULE_fof_defined_term = 108, 
		RULE_fof_defined_atomic_term = 109, RULE_fof_defined_plain_term = 110, 
		RULE_fof_system_term = 111, RULE_fof_arguments = 112, RULE_fof_term = 113, 
		RULE_fof_function_term = 114, RULE_tff_conditional_term = 115, RULE_tff_let_term = 116, 
		RULE_tff_tuple_term = 117, RULE_fof_sequent = 118, RULE_fof_formula_tuple = 119, 
		RULE_fof_formula_tuple_list = 120, RULE_cnf_formula = 121, RULE_cnf_disjunction = 122, 
		RULE_cnf_literal = 123, RULE_thf_quantifier = 124, RULE_th0_quantifier = 125, 
		RULE_th1_quantifier = 126, RULE_thf_pair_connective = 127, RULE_thf_unary_connective = 128, 
		RULE_th1_unary_connective = 129, RULE_tff_pair_connective = 130, RULE_fof_quantifier = 131, 
		RULE_binary_connective = 132, RULE_assoc_connective = 133, RULE_unary_connective = 134, 
		RULE_type_constant = 135, RULE_type_functor = 136, RULE_defined_type = 137, 
		RULE_system_type = 138, RULE_atom = 139, RULE_untyped_atom = 140, RULE_defined_proposition = 141, 
		RULE_defined_predicate = 142, RULE_defined_infix_pred = 143, RULE_constant = 144, 
		RULE_functor = 145, RULE_system_constant = 146, RULE_system_functor = 147, 
		RULE_defined_constant = 148, RULE_defined_functor = 149, RULE_defined_term = 150, 
		RULE_variable = 151, RULE_source = 152, RULE_sources = 153, RULE_dag_source = 154, 
		RULE_inference_record = 155, RULE_inference_rule = 156, RULE_inference_parents = 157, 
		RULE_parent_list = 158, RULE_parent_info = 159, RULE_parent_details = 160, 
		RULE_internal_source = 161, RULE_intro_type = 162, RULE_external_source = 163, 
		RULE_file_source = 164, RULE_file_info = 165, RULE_theory = 166, RULE_theory_name = 167, 
		RULE_creator_source = 168, RULE_creator_name = 169, RULE_optional_info = 170, 
		RULE_useful_info = 171, RULE_info_items = 172, RULE_info_item = 173, RULE_formula_item = 174, 
		RULE_description_item = 175, RULE_iquote_item = 176, RULE_inference_item = 177, 
		RULE_inference_status = 178, RULE_status_value = 179, RULE_inference_info = 180, 
		RULE_assumptions_record = 181, RULE_refutation = 182, RULE_new_symbol_record = 183, 
		RULE_new_symbol_list = 184, RULE_principal_symbol = 185, RULE_include = 186, 
		RULE_formula_selection = 187, RULE_name_list = 188, RULE_general_term = 189, 
		RULE_general_data = 190, RULE_general_function = 191, RULE_formula_data = 192, 
		RULE_general_list = 193, RULE_general_terms = 194, RULE_name = 195, RULE_atomic_word = 196, 
		RULE_atomic_defined_word = 197, RULE_atomic_system_word = 198, RULE_number = 199, 
		RULE_file_name = 200;
	private static String[] makeRuleNames() {
		return new String[] {
			"tptp_file", "tptp_input", "annotated_formula", "tpi_annotated", "tpi_formula", 
			"thf_annotated", "tfx_annotated", "tff_annotated", "tcf_annotated", "fof_annotated", 
			"cnf_annotated", "annotations", "formula_role", "thf_formula", "thf_logic_formula", 
			"thf_binary_formula", "thf_binary_pair", "thf_binary_tuple", "thf_or_formula", 
			"thf_and_formula", "thf_apply_formula", "thf_unitary_formula", "thf_quantified_formula", 
			"thf_quantification", "thf_variable_list", "thf_variable", "thf_typed_variable", 
			"thf_unary_formula", "thf_atom", "thf_function", "thf_conn_term", "thf_conditional", 
			"thf_let", "thf_arguments", "thf_type_formula", "thf_typeable_formula", 
			"thf_subtype", "thf_top_level_type", "thf_unitary_type", "thf_apply_type", 
			"thf_binary_type", "thf_mapping_type", "thf_xprod_type", "thf_union_type", 
			"thf_sequent", "thf_tuple", "thf_formula_list", "tfx_formula", "tfx_logic_formula", 
			"tff_formula", "tff_logic_formula", "tff_binary_formula", "tff_binary_nonassoc", 
			"tff_binary_assoc", "tff_or_formula", "tff_and_formula", "tff_unitary_formula", 
			"tff_quantified_formula", "tff_variable_list", "tff_variable", "tff_typed_variable", 
			"tff_unary_formula", "tff_atomic_formula", "tff_conditional", "tff_let", 
			"tff_let_term_defns", "tff_let_term_list", "tff_let_term_defn", "tff_let_term_binding", 
			"tff_let_formula_defns", "tff_let_formula_list", "tff_let_formula_defn", 
			"tff_let_formula_binding", "tff_sequent", "tff_formula_tuple", "tff_formula_tuple_list", 
			"tff_typed_atom", "tff_subtype", "tff_top_level_type", "tf1_quantified_type", 
			"tff_monotype", "tff_unitary_type", "tff_atomic_type", "tff_type_arguments", 
			"tff_mapping_type", "tff_xprod_type", "tcf_formula", "tcf_logic_formula", 
			"tcf_quantified_formula", "fof_formula", "fof_logic_formula", "fof_binary_formula", 
			"fof_binary_nonassoc", "fof_binary_assoc", "fof_or_formula", "fof_and_formula", 
			"fof_unitary_formula", "fof_quantified_formula", "fof_variable_list", 
			"fof_unary_formula", "fof_infix_unary", "fof_atomic_formula", "fof_plain_atomic_formula", 
			"fof_defined_atomic_formula", "fof_defined_plain_formula", "fof_defined_infix_formula", 
			"fof_system_atomic_formula", "fof_plain_term", "fof_defined_term", "fof_defined_atomic_term", 
			"fof_defined_plain_term", "fof_system_term", "fof_arguments", "fof_term", 
			"fof_function_term", "tff_conditional_term", "tff_let_term", "tff_tuple_term", 
			"fof_sequent", "fof_formula_tuple", "fof_formula_tuple_list", "cnf_formula", 
			"cnf_disjunction", "cnf_literal", "thf_quantifier", "th0_quantifier", 
			"th1_quantifier", "thf_pair_connective", "thf_unary_connective", "th1_unary_connective", 
			"tff_pair_connective", "fof_quantifier", "binary_connective", "assoc_connective", 
			"unary_connective", "type_constant", "type_functor", "defined_type", 
			"system_type", "atom", "untyped_atom", "defined_proposition", "defined_predicate", 
			"defined_infix_pred", "constant", "functor", "system_constant", "system_functor", 
			"defined_constant", "defined_functor", "defined_term", "variable", "source", 
			"sources", "dag_source", "inference_record", "inference_rule", "inference_parents", 
			"parent_list", "parent_info", "parent_details", "internal_source", "intro_type", 
			"external_source", "file_source", "file_info", "theory", "theory_name", 
			"creator_source", "creator_name", "optional_info", "useful_info", "info_items", 
			"info_item", "formula_item", "description_item", "iquote_item", "inference_item", 
			"inference_status", "status_value", "inference_info", "assumptions_record", 
			"refutation", "new_symbol_record", "new_symbol_list", "principal_symbol", 
			"include", "formula_selection", "name_list", "general_term", "general_data", 
			"general_function", "formula_data", "general_list", "general_terms", 
			"name", "atomic_word", "atomic_defined_word", "atomic_system_word", "number", 
			"file_name"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'tpi('", "','", "').'", "'thf('", "'tfx('", "'tff('", "'tcf('", 
			"'fof('", "'cnf('", "'('", "')'", "'['", "']'", "':'", "'$ite('", "'$let('", 
			"'[]'", "'{}'", "'{'", "'}'", "'$ite_f('", "'$let_tf('", "'$let_ff('", 
			"'$ite_t('", "'$let_ft('", "'$let_tt('", "'inference('", "'introduced('", 
			"'file('", "'theory('", "'creator('", "'description('", "'iquote('", 
			"'status('", "'assumptions('", "'refutation('", "'new_symbols('", "'include('", 
			"'$thf('", "'$tff('", "'$fof('", "'$cnf('", "'$fot('", "'|'", "'&'", 
			"'<=>'", "'=>'", "'<='", "'<~>'", "'~|'", "'~&'", "'~'", "'!!'", "'!>'", 
			"'!='", "'='", "'!'", "'??'", "'?*'", "'?'", "'^'", "'@@+'", "'@+'", 
			"'@@-'", "'@-'", "'@='", "'@'", "':='", "'>'", "'*'", "'+'", "'<<'", 
			"'-->'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, "Or", "And", "Iff", "Impl", 
			"If", "Niff", "Nor", "Nand", "Not", "ForallComb", "TyForall", "Infix_inequality", 
			"Infix_equality", "Forall", "ExistsComb", "TyExists", "Exists", "Lambda", 
			"ChoiceComb", "Choice", "DescriptionComb", "Description", "EqComb", "App", 
			"Assignment", "Arrow", "Star", "Plus", "Subtype_sign", "Gentzen_arrow", 
			"Real", "Signed_real", "Unsigned_real", "Rational", "Signed_rational", 
			"Unsigned_rational", "Integer", "Signed_integer", "Unsigned_integer", 
			"Decimal", "Positive_decimal", "Decimal_exponent", "Decimal_fraction", 
			"Dot_decimal", "Exp_integer", "Signed_exp_integer", "Unsigned_exp_integer", 
			"Dollar_word", "Dollar_dollar_word", "Upper_word", "Lower_word", "Single_quoted", 
			"Distinct_object", "WS", "Line_comment", "Block_comment"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "tptp_v7_0_0_0.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public tptp_v7_0_0_0Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class Tptp_fileContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(tptp_v7_0_0_0Parser.EOF, 0); }
		public List<Tptp_inputContext> tptp_input() {
			return getRuleContexts(Tptp_inputContext.class);
		}
		public Tptp_inputContext tptp_input(int i) {
			return getRuleContext(Tptp_inputContext.class,i);
		}
		public Tptp_fileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tptp_file; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTptp_file(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTptp_file(this);
		}
	}

	public final Tptp_fileContext tptp_file() throws RecognitionException {
		Tptp_fileContext _localctx = new Tptp_fileContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_tptp_file);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__37))) != 0)) {
				{
				{
				setState(402);
				tptp_input();
				}
				}
				setState(407);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(408);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tptp_inputContext extends ParserRuleContext {
		public Annotated_formulaContext annotated_formula() {
			return getRuleContext(Annotated_formulaContext.class,0);
		}
		public IncludeContext include() {
			return getRuleContext(IncludeContext.class,0);
		}
		public Tptp_inputContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tptp_input; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTptp_input(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTptp_input(this);
		}
	}

	public final Tptp_inputContext tptp_input() throws RecognitionException {
		Tptp_inputContext _localctx = new Tptp_inputContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_tptp_input);
		try {
			setState(412);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__3:
			case T__4:
			case T__5:
			case T__6:
			case T__7:
			case T__8:
				enterOuterAlt(_localctx, 1);
				{
				setState(410);
				annotated_formula();
				}
				break;
			case T__37:
				enterOuterAlt(_localctx, 2);
				{
				setState(411);
				include();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Annotated_formulaContext extends ParserRuleContext {
		public Thf_annotatedContext thf_annotated() {
			return getRuleContext(Thf_annotatedContext.class,0);
		}
		public Tfx_annotatedContext tfx_annotated() {
			return getRuleContext(Tfx_annotatedContext.class,0);
		}
		public Tff_annotatedContext tff_annotated() {
			return getRuleContext(Tff_annotatedContext.class,0);
		}
		public Tcf_annotatedContext tcf_annotated() {
			return getRuleContext(Tcf_annotatedContext.class,0);
		}
		public Fof_annotatedContext fof_annotated() {
			return getRuleContext(Fof_annotatedContext.class,0);
		}
		public Cnf_annotatedContext cnf_annotated() {
			return getRuleContext(Cnf_annotatedContext.class,0);
		}
		public Tpi_annotatedContext tpi_annotated() {
			return getRuleContext(Tpi_annotatedContext.class,0);
		}
		public Annotated_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotated_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAnnotated_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAnnotated_formula(this);
		}
	}

	public final Annotated_formulaContext annotated_formula() throws RecognitionException {
		Annotated_formulaContext _localctx = new Annotated_formulaContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_annotated_formula);
		try {
			setState(421);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__3:
				enterOuterAlt(_localctx, 1);
				{
				setState(414);
				thf_annotated();
				}
				break;
			case T__4:
				enterOuterAlt(_localctx, 2);
				{
				setState(415);
				tfx_annotated();
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 3);
				{
				setState(416);
				tff_annotated();
				}
				break;
			case T__6:
				enterOuterAlt(_localctx, 4);
				{
				setState(417);
				tcf_annotated();
				}
				break;
			case T__7:
				enterOuterAlt(_localctx, 5);
				{
				setState(418);
				fof_annotated();
				}
				break;
			case T__8:
				enterOuterAlt(_localctx, 6);
				{
				setState(419);
				cnf_annotated();
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 7);
				{
				setState(420);
				tpi_annotated();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tpi_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Tpi_formulaContext tpi_formula() {
			return getRuleContext(Tpi_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Tpi_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tpi_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTpi_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTpi_annotated(this);
		}
	}

	public final Tpi_annotatedContext tpi_annotated() throws RecognitionException {
		Tpi_annotatedContext _localctx = new Tpi_annotatedContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_tpi_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(423);
			match(T__0);
			setState(424);
			name();
			setState(425);
			match(T__1);
			setState(426);
			formula_role();
			setState(427);
			match(T__1);
			setState(428);
			tpi_formula();
			setState(430);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(429);
				annotations();
				}
			}

			setState(432);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tpi_formulaContext extends ParserRuleContext {
		public Fof_formulaContext fof_formula() {
			return getRuleContext(Fof_formulaContext.class,0);
		}
		public Tpi_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tpi_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTpi_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTpi_formula(this);
		}
	}

	public final Tpi_formulaContext tpi_formula() throws RecognitionException {
		Tpi_formulaContext _localctx = new Tpi_formulaContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_tpi_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(434);
			fof_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Thf_formulaContext thf_formula() {
			return getRuleContext(Thf_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Thf_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_annotated(this);
		}
	}

	public final Thf_annotatedContext thf_annotated() throws RecognitionException {
		Thf_annotatedContext _localctx = new Thf_annotatedContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_thf_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(436);
			match(T__3);
			setState(437);
			name();
			setState(438);
			match(T__1);
			setState(439);
			formula_role();
			setState(440);
			match(T__1);
			setState(441);
			thf_formula();
			setState(443);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(442);
				annotations();
				}
			}

			setState(445);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tfx_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Tfx_formulaContext tfx_formula() {
			return getRuleContext(Tfx_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Tfx_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tfx_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTfx_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTfx_annotated(this);
		}
	}

	public final Tfx_annotatedContext tfx_annotated() throws RecognitionException {
		Tfx_annotatedContext _localctx = new Tfx_annotatedContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_tfx_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(447);
			match(T__4);
			setState(448);
			name();
			setState(449);
			match(T__1);
			setState(450);
			formula_role();
			setState(451);
			match(T__1);
			setState(452);
			tfx_formula();
			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(453);
				annotations();
				}
			}

			setState(456);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Tff_formulaContext tff_formula() {
			return getRuleContext(Tff_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Tff_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_annotated(this);
		}
	}

	public final Tff_annotatedContext tff_annotated() throws RecognitionException {
		Tff_annotatedContext _localctx = new Tff_annotatedContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_tff_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(458);
			match(T__5);
			setState(459);
			name();
			setState(460);
			match(T__1);
			setState(461);
			formula_role();
			setState(462);
			match(T__1);
			setState(463);
			tff_formula();
			setState(465);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(464);
				annotations();
				}
			}

			setState(467);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tcf_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Tcf_formulaContext tcf_formula() {
			return getRuleContext(Tcf_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Tcf_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tcf_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTcf_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTcf_annotated(this);
		}
	}

	public final Tcf_annotatedContext tcf_annotated() throws RecognitionException {
		Tcf_annotatedContext _localctx = new Tcf_annotatedContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_tcf_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(469);
			match(T__6);
			setState(470);
			name();
			setState(471);
			match(T__1);
			setState(472);
			formula_role();
			setState(473);
			match(T__1);
			setState(474);
			tcf_formula();
			setState(476);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(475);
				annotations();
				}
			}

			setState(478);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Fof_formulaContext fof_formula() {
			return getRuleContext(Fof_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Fof_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_annotated(this);
		}
	}

	public final Fof_annotatedContext fof_annotated() throws RecognitionException {
		Fof_annotatedContext _localctx = new Fof_annotatedContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_fof_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(480);
			match(T__7);
			setState(481);
			name();
			setState(482);
			match(T__1);
			setState(483);
			formula_role();
			setState(484);
			match(T__1);
			setState(485);
			fof_formula();
			setState(487);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(486);
				annotations();
				}
			}

			setState(489);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Cnf_annotatedContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Formula_roleContext formula_role() {
			return getRuleContext(Formula_roleContext.class,0);
		}
		public Cnf_formulaContext cnf_formula() {
			return getRuleContext(Cnf_formulaContext.class,0);
		}
		public AnnotationsContext annotations() {
			return getRuleContext(AnnotationsContext.class,0);
		}
		public Cnf_annotatedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cnf_annotated; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterCnf_annotated(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitCnf_annotated(this);
		}
	}

	public final Cnf_annotatedContext cnf_annotated() throws RecognitionException {
		Cnf_annotatedContext _localctx = new Cnf_annotatedContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_cnf_annotated);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(491);
			match(T__8);
			setState(492);
			name();
			setState(493);
			match(T__1);
			setState(494);
			formula_role();
			setState(495);
			match(T__1);
			setState(496);
			cnf_formula();
			setState(498);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(497);
				annotations();
				}
			}

			setState(500);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationsContext extends ParserRuleContext {
		public SourceContext source() {
			return getRuleContext(SourceContext.class,0);
		}
		public Optional_infoContext optional_info() {
			return getRuleContext(Optional_infoContext.class,0);
		}
		public AnnotationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAnnotations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAnnotations(this);
		}
	}

	public final AnnotationsContext annotations() throws RecognitionException {
		AnnotationsContext _localctx = new AnnotationsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_annotations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(502);
			match(T__1);
			setState(503);
			source();
			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(504);
				optional_info();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Formula_roleContext extends ParserRuleContext {
		public TerminalNode Lower_word() { return getToken(tptp_v7_0_0_0Parser.Lower_word, 0); }
		public Formula_roleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formula_role; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFormula_role(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFormula_role(this);
		}
	}

	public final Formula_roleContext formula_role() throws RecognitionException {
		Formula_roleContext _localctx = new Formula_roleContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_formula_role);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(507);
			match(Lower_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_formulaContext extends ParserRuleContext {
		public Thf_logic_formulaContext thf_logic_formula() {
			return getRuleContext(Thf_logic_formulaContext.class,0);
		}
		public Thf_sequentContext thf_sequent() {
			return getRuleContext(Thf_sequentContext.class,0);
		}
		public Thf_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_formula(this);
		}
	}

	public final Thf_formulaContext thf_formula() throws RecognitionException {
		Thf_formulaContext _localctx = new Thf_formulaContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_thf_formula);
		try {
			setState(511);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(509);
				thf_logic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(510);
				thf_sequent();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_logic_formulaContext extends ParserRuleContext {
		public Thf_binary_formulaContext thf_binary_formula() {
			return getRuleContext(Thf_binary_formulaContext.class,0);
		}
		public Thf_unitary_formulaContext thf_unitary_formula() {
			return getRuleContext(Thf_unitary_formulaContext.class,0);
		}
		public Thf_type_formulaContext thf_type_formula() {
			return getRuleContext(Thf_type_formulaContext.class,0);
		}
		public Thf_subtypeContext thf_subtype() {
			return getRuleContext(Thf_subtypeContext.class,0);
		}
		public Thf_logic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_logic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_logic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_logic_formula(this);
		}
	}

	public final Thf_logic_formulaContext thf_logic_formula() throws RecognitionException {
		Thf_logic_formulaContext _localctx = new Thf_logic_formulaContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_thf_logic_formula);
		try {
			setState(517);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(513);
				thf_binary_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(514);
				thf_unitary_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(515);
				thf_type_formula();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(516);
				thf_subtype();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_binary_formulaContext extends ParserRuleContext {
		public Thf_binary_pairContext thf_binary_pair() {
			return getRuleContext(Thf_binary_pairContext.class,0);
		}
		public Thf_binary_tupleContext thf_binary_tuple() {
			return getRuleContext(Thf_binary_tupleContext.class,0);
		}
		public Thf_binary_typeContext thf_binary_type() {
			return getRuleContext(Thf_binary_typeContext.class,0);
		}
		public Thf_binary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_binary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_binary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_binary_formula(this);
		}
	}

	public final Thf_binary_formulaContext thf_binary_formula() throws RecognitionException {
		Thf_binary_formulaContext _localctx = new Thf_binary_formulaContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_thf_binary_formula);
		try {
			setState(522);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(519);
				thf_binary_pair();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(520);
				thf_binary_tuple();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(521);
				thf_binary_type();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_binary_pairContext extends ParserRuleContext {
		public List<Thf_unitary_formulaContext> thf_unitary_formula() {
			return getRuleContexts(Thf_unitary_formulaContext.class);
		}
		public Thf_unitary_formulaContext thf_unitary_formula(int i) {
			return getRuleContext(Thf_unitary_formulaContext.class,i);
		}
		public Thf_pair_connectiveContext thf_pair_connective() {
			return getRuleContext(Thf_pair_connectiveContext.class,0);
		}
		public Thf_binary_pairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_binary_pair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_binary_pair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_binary_pair(this);
		}
	}

	public final Thf_binary_pairContext thf_binary_pair() throws RecognitionException {
		Thf_binary_pairContext _localctx = new Thf_binary_pairContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_thf_binary_pair);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(524);
			thf_unitary_formula();
			setState(525);
			thf_pair_connective();
			setState(526);
			thf_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_binary_tupleContext extends ParserRuleContext {
		public Thf_or_formulaContext thf_or_formula() {
			return getRuleContext(Thf_or_formulaContext.class,0);
		}
		public Thf_and_formulaContext thf_and_formula() {
			return getRuleContext(Thf_and_formulaContext.class,0);
		}
		public Thf_apply_formulaContext thf_apply_formula() {
			return getRuleContext(Thf_apply_formulaContext.class,0);
		}
		public Thf_binary_tupleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_binary_tuple; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_binary_tuple(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_binary_tuple(this);
		}
	}

	public final Thf_binary_tupleContext thf_binary_tuple() throws RecognitionException {
		Thf_binary_tupleContext _localctx = new Thf_binary_tupleContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_thf_binary_tuple);
		try {
			setState(531);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(528);
				thf_or_formula(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(529);
				thf_and_formula(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(530);
				thf_apply_formula(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_or_formulaContext extends ParserRuleContext {
		public List<Thf_unitary_formulaContext> thf_unitary_formula() {
			return getRuleContexts(Thf_unitary_formulaContext.class);
		}
		public Thf_unitary_formulaContext thf_unitary_formula(int i) {
			return getRuleContext(Thf_unitary_formulaContext.class,i);
		}
		public TerminalNode Or() { return getToken(tptp_v7_0_0_0Parser.Or, 0); }
		public Thf_or_formulaContext thf_or_formula() {
			return getRuleContext(Thf_or_formulaContext.class,0);
		}
		public Thf_or_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_or_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_or_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_or_formula(this);
		}
	}

	public final Thf_or_formulaContext thf_or_formula() throws RecognitionException {
		return thf_or_formula(0);
	}

	private Thf_or_formulaContext thf_or_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Thf_or_formulaContext _localctx = new Thf_or_formulaContext(_ctx, _parentState);
		Thf_or_formulaContext _prevctx = _localctx;
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_thf_or_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(534);
			thf_unitary_formula();
			setState(535);
			match(Or);
			setState(536);
			thf_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(543);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Thf_or_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_thf_or_formula);
					setState(538);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(539);
					match(Or);
					setState(540);
					thf_unitary_formula();
					}
					} 
				}
				setState(545);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Thf_and_formulaContext extends ParserRuleContext {
		public List<Thf_unitary_formulaContext> thf_unitary_formula() {
			return getRuleContexts(Thf_unitary_formulaContext.class);
		}
		public Thf_unitary_formulaContext thf_unitary_formula(int i) {
			return getRuleContext(Thf_unitary_formulaContext.class,i);
		}
		public TerminalNode And() { return getToken(tptp_v7_0_0_0Parser.And, 0); }
		public Thf_and_formulaContext thf_and_formula() {
			return getRuleContext(Thf_and_formulaContext.class,0);
		}
		public Thf_and_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_and_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_and_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_and_formula(this);
		}
	}

	public final Thf_and_formulaContext thf_and_formula() throws RecognitionException {
		return thf_and_formula(0);
	}

	private Thf_and_formulaContext thf_and_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Thf_and_formulaContext _localctx = new Thf_and_formulaContext(_ctx, _parentState);
		Thf_and_formulaContext _prevctx = _localctx;
		int _startState = 38;
		enterRecursionRule(_localctx, 38, RULE_thf_and_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(547);
			thf_unitary_formula();
			setState(548);
			match(And);
			setState(549);
			thf_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(556);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Thf_and_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_thf_and_formula);
					setState(551);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(552);
					match(And);
					setState(553);
					thf_unitary_formula();
					}
					} 
				}
				setState(558);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Thf_apply_formulaContext extends ParserRuleContext {
		public List<Thf_unitary_formulaContext> thf_unitary_formula() {
			return getRuleContexts(Thf_unitary_formulaContext.class);
		}
		public Thf_unitary_formulaContext thf_unitary_formula(int i) {
			return getRuleContext(Thf_unitary_formulaContext.class,i);
		}
		public TerminalNode App() { return getToken(tptp_v7_0_0_0Parser.App, 0); }
		public Thf_apply_formulaContext thf_apply_formula() {
			return getRuleContext(Thf_apply_formulaContext.class,0);
		}
		public Thf_apply_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_apply_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_apply_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_apply_formula(this);
		}
	}

	public final Thf_apply_formulaContext thf_apply_formula() throws RecognitionException {
		return thf_apply_formula(0);
	}

	private Thf_apply_formulaContext thf_apply_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Thf_apply_formulaContext _localctx = new Thf_apply_formulaContext(_ctx, _parentState);
		Thf_apply_formulaContext _prevctx = _localctx;
		int _startState = 40;
		enterRecursionRule(_localctx, 40, RULE_thf_apply_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(560);
			thf_unitary_formula();
			setState(561);
			match(App);
			setState(562);
			thf_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(569);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Thf_apply_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_thf_apply_formula);
					setState(564);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(565);
					match(App);
					setState(566);
					thf_unitary_formula();
					}
					} 
				}
				setState(571);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Thf_unitary_formulaContext extends ParserRuleContext {
		public Thf_quantified_formulaContext thf_quantified_formula() {
			return getRuleContext(Thf_quantified_formulaContext.class,0);
		}
		public Thf_unary_formulaContext thf_unary_formula() {
			return getRuleContext(Thf_unary_formulaContext.class,0);
		}
		public Thf_atomContext thf_atom() {
			return getRuleContext(Thf_atomContext.class,0);
		}
		public Thf_conditionalContext thf_conditional() {
			return getRuleContext(Thf_conditionalContext.class,0);
		}
		public Thf_letContext thf_let() {
			return getRuleContext(Thf_letContext.class,0);
		}
		public Thf_tupleContext thf_tuple() {
			return getRuleContext(Thf_tupleContext.class,0);
		}
		public Thf_logic_formulaContext thf_logic_formula() {
			return getRuleContext(Thf_logic_formulaContext.class,0);
		}
		public Thf_unitary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_unitary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_unitary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_unitary_formula(this);
		}
	}

	public final Thf_unitary_formulaContext thf_unitary_formula() throws RecognitionException {
		Thf_unitary_formulaContext _localctx = new Thf_unitary_formulaContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_thf_unitary_formula);
		try {
			setState(582);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(572);
				thf_quantified_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(573);
				thf_unary_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(574);
				thf_atom();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(575);
				thf_conditional();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(576);
				thf_let();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(577);
				thf_tuple();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(578);
				match(T__9);
				setState(579);
				thf_logic_formula();
				setState(580);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_quantified_formulaContext extends ParserRuleContext {
		public Thf_quantificationContext thf_quantification() {
			return getRuleContext(Thf_quantificationContext.class,0);
		}
		public Thf_unitary_formulaContext thf_unitary_formula() {
			return getRuleContext(Thf_unitary_formulaContext.class,0);
		}
		public Thf_quantified_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_quantified_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_quantified_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_quantified_formula(this);
		}
	}

	public final Thf_quantified_formulaContext thf_quantified_formula() throws RecognitionException {
		Thf_quantified_formulaContext _localctx = new Thf_quantified_formulaContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_thf_quantified_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(584);
			thf_quantification();
			setState(585);
			thf_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_quantificationContext extends ParserRuleContext {
		public Thf_quantifierContext thf_quantifier() {
			return getRuleContext(Thf_quantifierContext.class,0);
		}
		public Thf_variable_listContext thf_variable_list() {
			return getRuleContext(Thf_variable_listContext.class,0);
		}
		public Thf_quantificationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_quantification; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_quantification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_quantification(this);
		}
	}

	public final Thf_quantificationContext thf_quantification() throws RecognitionException {
		Thf_quantificationContext _localctx = new Thf_quantificationContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_thf_quantification);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(587);
			thf_quantifier();
			setState(588);
			match(T__11);
			setState(589);
			thf_variable_list();
			setState(590);
			match(T__12);
			setState(591);
			match(T__13);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_variable_listContext extends ParserRuleContext {
		public List<Thf_variableContext> thf_variable() {
			return getRuleContexts(Thf_variableContext.class);
		}
		public Thf_variableContext thf_variable(int i) {
			return getRuleContext(Thf_variableContext.class,i);
		}
		public Thf_variable_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_variable_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_variable_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_variable_list(this);
		}
	}

	public final Thf_variable_listContext thf_variable_list() throws RecognitionException {
		Thf_variable_listContext _localctx = new Thf_variable_listContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_thf_variable_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(593);
			thf_variable();
			setState(598);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(594);
				match(T__1);
				setState(595);
				thf_variable();
				}
				}
				setState(600);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_variableContext extends ParserRuleContext {
		public Thf_typed_variableContext thf_typed_variable() {
			return getRuleContext(Thf_typed_variableContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Thf_variableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_variable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_variable(this);
		}
	}

	public final Thf_variableContext thf_variable() throws RecognitionException {
		Thf_variableContext _localctx = new Thf_variableContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_thf_variable);
		try {
			setState(603);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(601);
				thf_typed_variable();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(602);
				variable();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_typed_variableContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Thf_top_level_typeContext thf_top_level_type() {
			return getRuleContext(Thf_top_level_typeContext.class,0);
		}
		public Thf_typed_variableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_typed_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_typed_variable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_typed_variable(this);
		}
	}

	public final Thf_typed_variableContext thf_typed_variable() throws RecognitionException {
		Thf_typed_variableContext _localctx = new Thf_typed_variableContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_thf_typed_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(605);
			variable();
			setState(606);
			match(T__13);
			setState(607);
			thf_top_level_type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_unary_formulaContext extends ParserRuleContext {
		public Thf_unary_connectiveContext thf_unary_connective() {
			return getRuleContext(Thf_unary_connectiveContext.class,0);
		}
		public Thf_logic_formulaContext thf_logic_formula() {
			return getRuleContext(Thf_logic_formulaContext.class,0);
		}
		public Thf_unary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_unary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_unary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_unary_formula(this);
		}
	}

	public final Thf_unary_formulaContext thf_unary_formula() throws RecognitionException {
		Thf_unary_formulaContext _localctx = new Thf_unary_formulaContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_thf_unary_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(609);
			thf_unary_connective();
			setState(610);
			match(T__9);
			setState(611);
			thf_logic_formula();
			setState(612);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_atomContext extends ParserRuleContext {
		public Thf_functionContext thf_function() {
			return getRuleContext(Thf_functionContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Defined_termContext defined_term() {
			return getRuleContext(Defined_termContext.class,0);
		}
		public Thf_conn_termContext thf_conn_term() {
			return getRuleContext(Thf_conn_termContext.class,0);
		}
		public Thf_atomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_atom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_atom(this);
		}
	}

	public final Thf_atomContext thf_atom() throws RecognitionException {
		Thf_atomContext _localctx = new Thf_atomContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_thf_atom);
		try {
			setState(618);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Dollar_word:
			case Dollar_dollar_word:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(614);
				thf_function();
				}
				break;
			case Upper_word:
				enterOuterAlt(_localctx, 2);
				{
				setState(615);
				variable();
				}
				break;
			case Real:
			case Rational:
			case Integer:
			case Distinct_object:
				enterOuterAlt(_localctx, 3);
				{
				setState(616);
				defined_term();
				}
				break;
			case Or:
			case And:
			case Iff:
			case Impl:
			case If:
			case Niff:
			case Nor:
			case Nand:
			case Not:
			case ForallComb:
			case Infix_inequality:
			case Infix_equality:
			case ExistsComb:
			case ChoiceComb:
			case DescriptionComb:
			case EqComb:
			case Assignment:
				enterOuterAlt(_localctx, 4);
				{
				setState(617);
				thf_conn_term();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_functionContext extends ParserRuleContext {
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public FunctorContext functor() {
			return getRuleContext(FunctorContext.class,0);
		}
		public Thf_argumentsContext thf_arguments() {
			return getRuleContext(Thf_argumentsContext.class,0);
		}
		public Defined_functorContext defined_functor() {
			return getRuleContext(Defined_functorContext.class,0);
		}
		public System_functorContext system_functor() {
			return getRuleContext(System_functorContext.class,0);
		}
		public Thf_functionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_function(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_function(this);
		}
	}

	public final Thf_functionContext thf_function() throws RecognitionException {
		Thf_functionContext _localctx = new Thf_functionContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_thf_function);
		try {
			setState(636);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(620);
				atom();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(621);
				functor();
				setState(622);
				match(T__9);
				setState(623);
				thf_arguments();
				setState(624);
				match(T__10);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(626);
				defined_functor();
				setState(627);
				match(T__9);
				setState(628);
				thf_arguments();
				setState(629);
				match(T__10);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(631);
				system_functor();
				setState(632);
				match(T__9);
				setState(633);
				thf_arguments();
				setState(634);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_conn_termContext extends ParserRuleContext {
		public Thf_pair_connectiveContext thf_pair_connective() {
			return getRuleContext(Thf_pair_connectiveContext.class,0);
		}
		public Assoc_connectiveContext assoc_connective() {
			return getRuleContext(Assoc_connectiveContext.class,0);
		}
		public Thf_unary_connectiveContext thf_unary_connective() {
			return getRuleContext(Thf_unary_connectiveContext.class,0);
		}
		public Thf_conn_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_conn_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_conn_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_conn_term(this);
		}
	}

	public final Thf_conn_termContext thf_conn_term() throws RecognitionException {
		Thf_conn_termContext _localctx = new Thf_conn_termContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_thf_conn_term);
		try {
			setState(641);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Iff:
			case Impl:
			case If:
			case Niff:
			case Nor:
			case Nand:
			case Infix_inequality:
			case Infix_equality:
			case Assignment:
				enterOuterAlt(_localctx, 1);
				{
				setState(638);
				thf_pair_connective();
				}
				break;
			case Or:
			case And:
				enterOuterAlt(_localctx, 2);
				{
				setState(639);
				assoc_connective();
				}
				break;
			case Not:
			case ForallComb:
			case ExistsComb:
			case ChoiceComb:
			case DescriptionComb:
			case EqComb:
				enterOuterAlt(_localctx, 3);
				{
				setState(640);
				thf_unary_connective();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_conditionalContext extends ParserRuleContext {
		public List<Thf_logic_formulaContext> thf_logic_formula() {
			return getRuleContexts(Thf_logic_formulaContext.class);
		}
		public Thf_logic_formulaContext thf_logic_formula(int i) {
			return getRuleContext(Thf_logic_formulaContext.class,i);
		}
		public Thf_conditionalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_conditional; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_conditional(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_conditional(this);
		}
	}

	public final Thf_conditionalContext thf_conditional() throws RecognitionException {
		Thf_conditionalContext _localctx = new Thf_conditionalContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_thf_conditional);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(643);
			match(T__14);
			setState(644);
			thf_logic_formula();
			setState(645);
			match(T__1);
			setState(646);
			thf_logic_formula();
			setState(647);
			match(T__1);
			setState(648);
			thf_logic_formula();
			setState(649);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_letContext extends ParserRuleContext {
		public Thf_unitary_formulaContext thf_unitary_formula() {
			return getRuleContext(Thf_unitary_formulaContext.class,0);
		}
		public Thf_formulaContext thf_formula() {
			return getRuleContext(Thf_formulaContext.class,0);
		}
		public Thf_letContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_let; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_let(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_let(this);
		}
	}

	public final Thf_letContext thf_let() throws RecognitionException {
		Thf_letContext _localctx = new Thf_letContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_thf_let);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(651);
			match(T__15);
			setState(652);
			thf_unitary_formula();
			setState(653);
			match(T__1);
			setState(654);
			thf_formula();
			setState(655);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_argumentsContext extends ParserRuleContext {
		public Thf_formula_listContext thf_formula_list() {
			return getRuleContext(Thf_formula_listContext.class,0);
		}
		public Thf_argumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_arguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_arguments(this);
		}
	}

	public final Thf_argumentsContext thf_arguments() throws RecognitionException {
		Thf_argumentsContext _localctx = new Thf_argumentsContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_thf_arguments);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(657);
			thf_formula_list();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_type_formulaContext extends ParserRuleContext {
		public Thf_typeable_formulaContext thf_typeable_formula() {
			return getRuleContext(Thf_typeable_formulaContext.class,0);
		}
		public Thf_top_level_typeContext thf_top_level_type() {
			return getRuleContext(Thf_top_level_typeContext.class,0);
		}
		public Thf_type_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_type_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_type_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_type_formula(this);
		}
	}

	public final Thf_type_formulaContext thf_type_formula() throws RecognitionException {
		Thf_type_formulaContext _localctx = new Thf_type_formulaContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_thf_type_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(659);
			thf_typeable_formula();
			setState(660);
			match(T__13);
			setState(661);
			thf_top_level_type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_typeable_formulaContext extends ParserRuleContext {
		public Thf_atomContext thf_atom() {
			return getRuleContext(Thf_atomContext.class,0);
		}
		public Thf_logic_formulaContext thf_logic_formula() {
			return getRuleContext(Thf_logic_formulaContext.class,0);
		}
		public Thf_typeable_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_typeable_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_typeable_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_typeable_formula(this);
		}
	}

	public final Thf_typeable_formulaContext thf_typeable_formula() throws RecognitionException {
		Thf_typeable_formulaContext _localctx = new Thf_typeable_formulaContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_thf_typeable_formula);
		try {
			setState(668);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Or:
			case And:
			case Iff:
			case Impl:
			case If:
			case Niff:
			case Nor:
			case Nand:
			case Not:
			case ForallComb:
			case Infix_inequality:
			case Infix_equality:
			case ExistsComb:
			case ChoiceComb:
			case DescriptionComb:
			case EqComb:
			case Assignment:
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Dollar_dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
			case Distinct_object:
				enterOuterAlt(_localctx, 1);
				{
				setState(663);
				thf_atom();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(664);
				match(T__9);
				setState(665);
				thf_logic_formula();
				setState(666);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_subtypeContext extends ParserRuleContext {
		public List<Thf_atomContext> thf_atom() {
			return getRuleContexts(Thf_atomContext.class);
		}
		public Thf_atomContext thf_atom(int i) {
			return getRuleContext(Thf_atomContext.class,i);
		}
		public TerminalNode Subtype_sign() { return getToken(tptp_v7_0_0_0Parser.Subtype_sign, 0); }
		public Thf_subtypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_subtype; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_subtype(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_subtype(this);
		}
	}

	public final Thf_subtypeContext thf_subtype() throws RecognitionException {
		Thf_subtypeContext _localctx = new Thf_subtypeContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_thf_subtype);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(670);
			thf_atom();
			setState(671);
			match(Subtype_sign);
			setState(672);
			thf_atom();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_top_level_typeContext extends ParserRuleContext {
		public Thf_unitary_typeContext thf_unitary_type() {
			return getRuleContext(Thf_unitary_typeContext.class,0);
		}
		public Thf_mapping_typeContext thf_mapping_type() {
			return getRuleContext(Thf_mapping_typeContext.class,0);
		}
		public Thf_apply_typeContext thf_apply_type() {
			return getRuleContext(Thf_apply_typeContext.class,0);
		}
		public Thf_top_level_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_top_level_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_top_level_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_top_level_type(this);
		}
	}

	public final Thf_top_level_typeContext thf_top_level_type() throws RecognitionException {
		Thf_top_level_typeContext _localctx = new Thf_top_level_typeContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_thf_top_level_type);
		try {
			setState(677);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(674);
				thf_unitary_type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(675);
				thf_mapping_type();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(676);
				thf_apply_type();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_unitary_typeContext extends ParserRuleContext {
		public Thf_unitary_formulaContext thf_unitary_formula() {
			return getRuleContext(Thf_unitary_formulaContext.class,0);
		}
		public Thf_unitary_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_unitary_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_unitary_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_unitary_type(this);
		}
	}

	public final Thf_unitary_typeContext thf_unitary_type() throws RecognitionException {
		Thf_unitary_typeContext _localctx = new Thf_unitary_typeContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_thf_unitary_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(679);
			thf_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_apply_typeContext extends ParserRuleContext {
		public Thf_apply_formulaContext thf_apply_formula() {
			return getRuleContext(Thf_apply_formulaContext.class,0);
		}
		public Thf_apply_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_apply_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_apply_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_apply_type(this);
		}
	}

	public final Thf_apply_typeContext thf_apply_type() throws RecognitionException {
		Thf_apply_typeContext _localctx = new Thf_apply_typeContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_thf_apply_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(681);
			thf_apply_formula(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_binary_typeContext extends ParserRuleContext {
		public Thf_mapping_typeContext thf_mapping_type() {
			return getRuleContext(Thf_mapping_typeContext.class,0);
		}
		public Thf_xprod_typeContext thf_xprod_type() {
			return getRuleContext(Thf_xprod_typeContext.class,0);
		}
		public Thf_union_typeContext thf_union_type() {
			return getRuleContext(Thf_union_typeContext.class,0);
		}
		public Thf_binary_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_binary_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_binary_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_binary_type(this);
		}
	}

	public final Thf_binary_typeContext thf_binary_type() throws RecognitionException {
		Thf_binary_typeContext _localctx = new Thf_binary_typeContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_thf_binary_type);
		try {
			setState(686);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(683);
				thf_mapping_type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(684);
				thf_xprod_type(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(685);
				thf_union_type(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_mapping_typeContext extends ParserRuleContext {
		public List<Thf_unitary_typeContext> thf_unitary_type() {
			return getRuleContexts(Thf_unitary_typeContext.class);
		}
		public Thf_unitary_typeContext thf_unitary_type(int i) {
			return getRuleContext(Thf_unitary_typeContext.class,i);
		}
		public TerminalNode Arrow() { return getToken(tptp_v7_0_0_0Parser.Arrow, 0); }
		public Thf_mapping_typeContext thf_mapping_type() {
			return getRuleContext(Thf_mapping_typeContext.class,0);
		}
		public Thf_mapping_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_mapping_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_mapping_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_mapping_type(this);
		}
	}

	public final Thf_mapping_typeContext thf_mapping_type() throws RecognitionException {
		Thf_mapping_typeContext _localctx = new Thf_mapping_typeContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_thf_mapping_type);
		try {
			setState(696);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(688);
				thf_unitary_type();
				setState(689);
				match(Arrow);
				setState(690);
				thf_unitary_type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(692);
				thf_unitary_type();
				setState(693);
				match(Arrow);
				setState(694);
				thf_mapping_type();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_xprod_typeContext extends ParserRuleContext {
		public List<Thf_unitary_typeContext> thf_unitary_type() {
			return getRuleContexts(Thf_unitary_typeContext.class);
		}
		public Thf_unitary_typeContext thf_unitary_type(int i) {
			return getRuleContext(Thf_unitary_typeContext.class,i);
		}
		public TerminalNode Star() { return getToken(tptp_v7_0_0_0Parser.Star, 0); }
		public Thf_xprod_typeContext thf_xprod_type() {
			return getRuleContext(Thf_xprod_typeContext.class,0);
		}
		public Thf_xprod_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_xprod_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_xprod_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_xprod_type(this);
		}
	}

	public final Thf_xprod_typeContext thf_xprod_type() throws RecognitionException {
		return thf_xprod_type(0);
	}

	private Thf_xprod_typeContext thf_xprod_type(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Thf_xprod_typeContext _localctx = new Thf_xprod_typeContext(_ctx, _parentState);
		Thf_xprod_typeContext _prevctx = _localctx;
		int _startState = 84;
		enterRecursionRule(_localctx, 84, RULE_thf_xprod_type, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(699);
			thf_unitary_type();
			setState(700);
			match(Star);
			setState(701);
			thf_unitary_type();
			}
			_ctx.stop = _input.LT(-1);
			setState(708);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Thf_xprod_typeContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_thf_xprod_type);
					setState(703);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(704);
					match(Star);
					setState(705);
					thf_unitary_type();
					}
					} 
				}
				setState(710);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Thf_union_typeContext extends ParserRuleContext {
		public List<Thf_unitary_typeContext> thf_unitary_type() {
			return getRuleContexts(Thf_unitary_typeContext.class);
		}
		public Thf_unitary_typeContext thf_unitary_type(int i) {
			return getRuleContext(Thf_unitary_typeContext.class,i);
		}
		public TerminalNode Plus() { return getToken(tptp_v7_0_0_0Parser.Plus, 0); }
		public Thf_union_typeContext thf_union_type() {
			return getRuleContext(Thf_union_typeContext.class,0);
		}
		public Thf_union_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_union_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_union_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_union_type(this);
		}
	}

	public final Thf_union_typeContext thf_union_type() throws RecognitionException {
		return thf_union_type(0);
	}

	private Thf_union_typeContext thf_union_type(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Thf_union_typeContext _localctx = new Thf_union_typeContext(_ctx, _parentState);
		Thf_union_typeContext _prevctx = _localctx;
		int _startState = 86;
		enterRecursionRule(_localctx, 86, RULE_thf_union_type, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(712);
			thf_unitary_type();
			setState(713);
			match(Plus);
			setState(714);
			thf_unitary_type();
			}
			_ctx.stop = _input.LT(-1);
			setState(721);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Thf_union_typeContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_thf_union_type);
					setState(716);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(717);
					match(Plus);
					setState(718);
					thf_unitary_type();
					}
					} 
				}
				setState(723);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Thf_sequentContext extends ParserRuleContext {
		public List<Thf_tupleContext> thf_tuple() {
			return getRuleContexts(Thf_tupleContext.class);
		}
		public Thf_tupleContext thf_tuple(int i) {
			return getRuleContext(Thf_tupleContext.class,i);
		}
		public TerminalNode Gentzen_arrow() { return getToken(tptp_v7_0_0_0Parser.Gentzen_arrow, 0); }
		public Thf_sequentContext thf_sequent() {
			return getRuleContext(Thf_sequentContext.class,0);
		}
		public Thf_sequentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_sequent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_sequent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_sequent(this);
		}
	}

	public final Thf_sequentContext thf_sequent() throws RecognitionException {
		Thf_sequentContext _localctx = new Thf_sequentContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_thf_sequent);
		try {
			setState(732);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__11:
			case T__16:
			case T__17:
			case T__18:
				enterOuterAlt(_localctx, 1);
				{
				setState(724);
				thf_tuple();
				setState(725);
				match(Gentzen_arrow);
				setState(726);
				thf_tuple();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(728);
				match(T__9);
				setState(729);
				thf_sequent();
				setState(730);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_tupleContext extends ParserRuleContext {
		public Thf_formula_listContext thf_formula_list() {
			return getRuleContext(Thf_formula_listContext.class,0);
		}
		public Thf_tupleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_tuple; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_tuple(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_tuple(this);
		}
	}

	public final Thf_tupleContext thf_tuple() throws RecognitionException {
		Thf_tupleContext _localctx = new Thf_tupleContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_thf_tuple);
		try {
			setState(744);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(734);
				match(T__16);
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(735);
				match(T__11);
				setState(736);
				thf_formula_list();
				setState(737);
				match(T__12);
				}
				break;
			case T__17:
				enterOuterAlt(_localctx, 3);
				{
				setState(739);
				match(T__17);
				}
				break;
			case T__18:
				enterOuterAlt(_localctx, 4);
				{
				setState(740);
				match(T__18);
				setState(741);
				thf_formula_list();
				setState(742);
				match(T__19);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_formula_listContext extends ParserRuleContext {
		public List<Thf_logic_formulaContext> thf_logic_formula() {
			return getRuleContexts(Thf_logic_formulaContext.class);
		}
		public Thf_logic_formulaContext thf_logic_formula(int i) {
			return getRuleContext(Thf_logic_formulaContext.class,i);
		}
		public Thf_formula_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_formula_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_formula_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_formula_list(this);
		}
	}

	public final Thf_formula_listContext thf_formula_list() throws RecognitionException {
		Thf_formula_listContext _localctx = new Thf_formula_listContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_thf_formula_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(746);
			thf_logic_formula();
			setState(751);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(747);
				match(T__1);
				setState(748);
				thf_logic_formula();
				}
				}
				setState(753);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tfx_formulaContext extends ParserRuleContext {
		public Tfx_logic_formulaContext tfx_logic_formula() {
			return getRuleContext(Tfx_logic_formulaContext.class,0);
		}
		public Thf_sequentContext thf_sequent() {
			return getRuleContext(Thf_sequentContext.class,0);
		}
		public Tfx_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tfx_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTfx_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTfx_formula(this);
		}
	}

	public final Tfx_formulaContext tfx_formula() throws RecognitionException {
		Tfx_formulaContext _localctx = new Tfx_formulaContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_tfx_formula);
		try {
			setState(756);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(754);
				tfx_logic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(755);
				thf_sequent();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tfx_logic_formulaContext extends ParserRuleContext {
		public Thf_logic_formulaContext thf_logic_formula() {
			return getRuleContext(Thf_logic_formulaContext.class,0);
		}
		public Tfx_logic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tfx_logic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTfx_logic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTfx_logic_formula(this);
		}
	}

	public final Tfx_logic_formulaContext tfx_logic_formula() throws RecognitionException {
		Tfx_logic_formulaContext _localctx = new Tfx_logic_formulaContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_tfx_logic_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(758);
			thf_logic_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_formulaContext extends ParserRuleContext {
		public Tff_logic_formulaContext tff_logic_formula() {
			return getRuleContext(Tff_logic_formulaContext.class,0);
		}
		public Tff_typed_atomContext tff_typed_atom() {
			return getRuleContext(Tff_typed_atomContext.class,0);
		}
		public Tff_sequentContext tff_sequent() {
			return getRuleContext(Tff_sequentContext.class,0);
		}
		public Tff_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_formula(this);
		}
	}

	public final Tff_formulaContext tff_formula() throws RecognitionException {
		Tff_formulaContext _localctx = new Tff_formulaContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_tff_formula);
		try {
			setState(763);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(760);
				tff_logic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(761);
				tff_typed_atom();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(762);
				tff_sequent();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_logic_formulaContext extends ParserRuleContext {
		public Tff_binary_formulaContext tff_binary_formula() {
			return getRuleContext(Tff_binary_formulaContext.class,0);
		}
		public Tff_unitary_formulaContext tff_unitary_formula() {
			return getRuleContext(Tff_unitary_formulaContext.class,0);
		}
		public Tff_subtypeContext tff_subtype() {
			return getRuleContext(Tff_subtypeContext.class,0);
		}
		public Tff_logic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_logic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_logic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_logic_formula(this);
		}
	}

	public final Tff_logic_formulaContext tff_logic_formula() throws RecognitionException {
		Tff_logic_formulaContext _localctx = new Tff_logic_formulaContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_tff_logic_formula);
		try {
			setState(768);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(765);
				tff_binary_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(766);
				tff_unitary_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(767);
				tff_subtype();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_binary_formulaContext extends ParserRuleContext {
		public Tff_binary_nonassocContext tff_binary_nonassoc() {
			return getRuleContext(Tff_binary_nonassocContext.class,0);
		}
		public Tff_binary_assocContext tff_binary_assoc() {
			return getRuleContext(Tff_binary_assocContext.class,0);
		}
		public Tff_binary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_binary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_binary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_binary_formula(this);
		}
	}

	public final Tff_binary_formulaContext tff_binary_formula() throws RecognitionException {
		Tff_binary_formulaContext _localctx = new Tff_binary_formulaContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_tff_binary_formula);
		try {
			setState(772);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(770);
				tff_binary_nonassoc();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(771);
				tff_binary_assoc();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_binary_nonassocContext extends ParserRuleContext {
		public List<Tff_unitary_formulaContext> tff_unitary_formula() {
			return getRuleContexts(Tff_unitary_formulaContext.class);
		}
		public Tff_unitary_formulaContext tff_unitary_formula(int i) {
			return getRuleContext(Tff_unitary_formulaContext.class,i);
		}
		public Binary_connectiveContext binary_connective() {
			return getRuleContext(Binary_connectiveContext.class,0);
		}
		public Tff_binary_nonassocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_binary_nonassoc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_binary_nonassoc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_binary_nonassoc(this);
		}
	}

	public final Tff_binary_nonassocContext tff_binary_nonassoc() throws RecognitionException {
		Tff_binary_nonassocContext _localctx = new Tff_binary_nonassocContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_tff_binary_nonassoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(774);
			tff_unitary_formula();
			setState(775);
			binary_connective();
			setState(776);
			tff_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_binary_assocContext extends ParserRuleContext {
		public Tff_or_formulaContext tff_or_formula() {
			return getRuleContext(Tff_or_formulaContext.class,0);
		}
		public Tff_and_formulaContext tff_and_formula() {
			return getRuleContext(Tff_and_formulaContext.class,0);
		}
		public Tff_binary_assocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_binary_assoc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_binary_assoc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_binary_assoc(this);
		}
	}

	public final Tff_binary_assocContext tff_binary_assoc() throws RecognitionException {
		Tff_binary_assocContext _localctx = new Tff_binary_assocContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_tff_binary_assoc);
		try {
			setState(780);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(778);
				tff_or_formula(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(779);
				tff_and_formula(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_or_formulaContext extends ParserRuleContext {
		public List<Tff_unitary_formulaContext> tff_unitary_formula() {
			return getRuleContexts(Tff_unitary_formulaContext.class);
		}
		public Tff_unitary_formulaContext tff_unitary_formula(int i) {
			return getRuleContext(Tff_unitary_formulaContext.class,i);
		}
		public TerminalNode Or() { return getToken(tptp_v7_0_0_0Parser.Or, 0); }
		public Tff_or_formulaContext tff_or_formula() {
			return getRuleContext(Tff_or_formulaContext.class,0);
		}
		public Tff_or_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_or_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_or_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_or_formula(this);
		}
	}

	public final Tff_or_formulaContext tff_or_formula() throws RecognitionException {
		return tff_or_formula(0);
	}

	private Tff_or_formulaContext tff_or_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Tff_or_formulaContext _localctx = new Tff_or_formulaContext(_ctx, _parentState);
		Tff_or_formulaContext _prevctx = _localctx;
		int _startState = 108;
		enterRecursionRule(_localctx, 108, RULE_tff_or_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(783);
			tff_unitary_formula();
			setState(784);
			match(Or);
			setState(785);
			tff_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(792);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Tff_or_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_tff_or_formula);
					setState(787);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(788);
					match(Or);
					setState(789);
					tff_unitary_formula();
					}
					} 
				}
				setState(794);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Tff_and_formulaContext extends ParserRuleContext {
		public List<Tff_unitary_formulaContext> tff_unitary_formula() {
			return getRuleContexts(Tff_unitary_formulaContext.class);
		}
		public Tff_unitary_formulaContext tff_unitary_formula(int i) {
			return getRuleContext(Tff_unitary_formulaContext.class,i);
		}
		public TerminalNode And() { return getToken(tptp_v7_0_0_0Parser.And, 0); }
		public Tff_and_formulaContext tff_and_formula() {
			return getRuleContext(Tff_and_formulaContext.class,0);
		}
		public Tff_and_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_and_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_and_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_and_formula(this);
		}
	}

	public final Tff_and_formulaContext tff_and_formula() throws RecognitionException {
		return tff_and_formula(0);
	}

	private Tff_and_formulaContext tff_and_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Tff_and_formulaContext _localctx = new Tff_and_formulaContext(_ctx, _parentState);
		Tff_and_formulaContext _prevctx = _localctx;
		int _startState = 110;
		enterRecursionRule(_localctx, 110, RULE_tff_and_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(796);
			tff_unitary_formula();
			setState(797);
			match(And);
			setState(798);
			tff_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(805);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Tff_and_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_tff_and_formula);
					setState(800);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(801);
					match(And);
					setState(802);
					tff_unitary_formula();
					}
					} 
				}
				setState(807);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Tff_unitary_formulaContext extends ParserRuleContext {
		public Tff_quantified_formulaContext tff_quantified_formula() {
			return getRuleContext(Tff_quantified_formulaContext.class,0);
		}
		public Tff_unary_formulaContext tff_unary_formula() {
			return getRuleContext(Tff_unary_formulaContext.class,0);
		}
		public Tff_atomic_formulaContext tff_atomic_formula() {
			return getRuleContext(Tff_atomic_formulaContext.class,0);
		}
		public Tff_conditionalContext tff_conditional() {
			return getRuleContext(Tff_conditionalContext.class,0);
		}
		public Tff_letContext tff_let() {
			return getRuleContext(Tff_letContext.class,0);
		}
		public Tff_logic_formulaContext tff_logic_formula() {
			return getRuleContext(Tff_logic_formulaContext.class,0);
		}
		public Tff_unitary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_unitary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_unitary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_unitary_formula(this);
		}
	}

	public final Tff_unitary_formulaContext tff_unitary_formula() throws RecognitionException {
		Tff_unitary_formulaContext _localctx = new Tff_unitary_formulaContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_tff_unitary_formula);
		try {
			setState(817);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,40,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(808);
				tff_quantified_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(809);
				tff_unary_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(810);
				tff_atomic_formula();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(811);
				tff_conditional();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(812);
				tff_let();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(813);
				match(T__9);
				setState(814);
				tff_logic_formula();
				setState(815);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_quantified_formulaContext extends ParserRuleContext {
		public Fof_quantifierContext fof_quantifier() {
			return getRuleContext(Fof_quantifierContext.class,0);
		}
		public Tff_variable_listContext tff_variable_list() {
			return getRuleContext(Tff_variable_listContext.class,0);
		}
		public Tff_unitary_formulaContext tff_unitary_formula() {
			return getRuleContext(Tff_unitary_formulaContext.class,0);
		}
		public Tff_quantified_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_quantified_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_quantified_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_quantified_formula(this);
		}
	}

	public final Tff_quantified_formulaContext tff_quantified_formula() throws RecognitionException {
		Tff_quantified_formulaContext _localctx = new Tff_quantified_formulaContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_tff_quantified_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(819);
			fof_quantifier();
			setState(820);
			match(T__11);
			setState(821);
			tff_variable_list();
			setState(822);
			match(T__12);
			setState(823);
			match(T__13);
			setState(824);
			tff_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_variable_listContext extends ParserRuleContext {
		public List<Tff_variableContext> tff_variable() {
			return getRuleContexts(Tff_variableContext.class);
		}
		public Tff_variableContext tff_variable(int i) {
			return getRuleContext(Tff_variableContext.class,i);
		}
		public Tff_variable_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_variable_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_variable_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_variable_list(this);
		}
	}

	public final Tff_variable_listContext tff_variable_list() throws RecognitionException {
		Tff_variable_listContext _localctx = new Tff_variable_listContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_tff_variable_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(826);
			tff_variable();
			setState(831);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(827);
				match(T__1);
				setState(828);
				tff_variable();
				}
				}
				setState(833);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_variableContext extends ParserRuleContext {
		public Tff_typed_variableContext tff_typed_variable() {
			return getRuleContext(Tff_typed_variableContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Tff_variableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_variable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_variable(this);
		}
	}

	public final Tff_variableContext tff_variable() throws RecognitionException {
		Tff_variableContext _localctx = new Tff_variableContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_tff_variable);
		try {
			setState(836);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(834);
				tff_typed_variable();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(835);
				variable();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_typed_variableContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Tff_atomic_typeContext tff_atomic_type() {
			return getRuleContext(Tff_atomic_typeContext.class,0);
		}
		public Tff_typed_variableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_typed_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_typed_variable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_typed_variable(this);
		}
	}

	public final Tff_typed_variableContext tff_typed_variable() throws RecognitionException {
		Tff_typed_variableContext _localctx = new Tff_typed_variableContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_tff_typed_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(838);
			variable();
			setState(839);
			match(T__13);
			setState(840);
			tff_atomic_type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_unary_formulaContext extends ParserRuleContext {
		public Unary_connectiveContext unary_connective() {
			return getRuleContext(Unary_connectiveContext.class,0);
		}
		public Tff_unitary_formulaContext tff_unitary_formula() {
			return getRuleContext(Tff_unitary_formulaContext.class,0);
		}
		public Fof_infix_unaryContext fof_infix_unary() {
			return getRuleContext(Fof_infix_unaryContext.class,0);
		}
		public Tff_unary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_unary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_unary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_unary_formula(this);
		}
	}

	public final Tff_unary_formulaContext tff_unary_formula() throws RecognitionException {
		Tff_unary_formulaContext _localctx = new Tff_unary_formulaContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_tff_unary_formula);
		try {
			setState(846);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Not:
				enterOuterAlt(_localctx, 1);
				{
				setState(842);
				unary_connective();
				setState(843);
				tff_unitary_formula();
				}
				break;
			case T__17:
			case T__18:
			case T__23:
			case T__24:
			case T__25:
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Dollar_dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
			case Distinct_object:
				enterOuterAlt(_localctx, 2);
				{
				setState(845);
				fof_infix_unary();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_atomic_formulaContext extends ParserRuleContext {
		public Fof_atomic_formulaContext fof_atomic_formula() {
			return getRuleContext(Fof_atomic_formulaContext.class,0);
		}
		public Tff_atomic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_atomic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_atomic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_atomic_formula(this);
		}
	}

	public final Tff_atomic_formulaContext tff_atomic_formula() throws RecognitionException {
		Tff_atomic_formulaContext _localctx = new Tff_atomic_formulaContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_tff_atomic_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(848);
			fof_atomic_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_conditionalContext extends ParserRuleContext {
		public List<Tff_logic_formulaContext> tff_logic_formula() {
			return getRuleContexts(Tff_logic_formulaContext.class);
		}
		public Tff_logic_formulaContext tff_logic_formula(int i) {
			return getRuleContext(Tff_logic_formulaContext.class,i);
		}
		public Tff_conditionalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_conditional; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_conditional(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_conditional(this);
		}
	}

	public final Tff_conditionalContext tff_conditional() throws RecognitionException {
		Tff_conditionalContext _localctx = new Tff_conditionalContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_tff_conditional);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(850);
			match(T__20);
			setState(851);
			tff_logic_formula();
			setState(852);
			match(T__1);
			setState(853);
			tff_logic_formula();
			setState(854);
			match(T__1);
			setState(855);
			tff_logic_formula();
			setState(856);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_letContext extends ParserRuleContext {
		public Tff_let_term_defnsContext tff_let_term_defns() {
			return getRuleContext(Tff_let_term_defnsContext.class,0);
		}
		public Tff_formulaContext tff_formula() {
			return getRuleContext(Tff_formulaContext.class,0);
		}
		public Tff_let_formula_defnsContext tff_let_formula_defns() {
			return getRuleContext(Tff_let_formula_defnsContext.class,0);
		}
		public Tff_letContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let(this);
		}
	}

	public final Tff_letContext tff_let() throws RecognitionException {
		Tff_letContext _localctx = new Tff_letContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_tff_let);
		try {
			setState(870);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__21:
				enterOuterAlt(_localctx, 1);
				{
				setState(858);
				match(T__21);
				setState(859);
				tff_let_term_defns();
				setState(860);
				match(T__1);
				setState(861);
				tff_formula();
				setState(862);
				match(T__10);
				}
				break;
			case T__22:
				enterOuterAlt(_localctx, 2);
				{
				setState(864);
				match(T__22);
				setState(865);
				tff_let_formula_defns();
				setState(866);
				match(T__1);
				setState(867);
				tff_formula();
				setState(868);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_term_defnsContext extends ParserRuleContext {
		public Tff_let_term_defnContext tff_let_term_defn() {
			return getRuleContext(Tff_let_term_defnContext.class,0);
		}
		public Tff_let_term_listContext tff_let_term_list() {
			return getRuleContext(Tff_let_term_listContext.class,0);
		}
		public Tff_let_term_defnsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_term_defns; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_term_defns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_term_defns(this);
		}
	}

	public final Tff_let_term_defnsContext tff_let_term_defns() throws RecognitionException {
		Tff_let_term_defnsContext _localctx = new Tff_let_term_defnsContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_tff_let_term_defns);
		try {
			setState(877);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__9:
			case Forall:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(872);
				tff_let_term_defn();
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(873);
				match(T__11);
				setState(874);
				tff_let_term_list();
				setState(875);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_term_listContext extends ParserRuleContext {
		public List<Tff_let_term_defnContext> tff_let_term_defn() {
			return getRuleContexts(Tff_let_term_defnContext.class);
		}
		public Tff_let_term_defnContext tff_let_term_defn(int i) {
			return getRuleContext(Tff_let_term_defnContext.class,i);
		}
		public Tff_let_term_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_term_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_term_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_term_list(this);
		}
	}

	public final Tff_let_term_listContext tff_let_term_list() throws RecognitionException {
		Tff_let_term_listContext _localctx = new Tff_let_term_listContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_tff_let_term_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(879);
			tff_let_term_defn();
			setState(884);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(880);
				match(T__1);
				setState(881);
				tff_let_term_defn();
				}
				}
				setState(886);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_term_defnContext extends ParserRuleContext {
		public TerminalNode Forall() { return getToken(tptp_v7_0_0_0Parser.Forall, 0); }
		public Tff_variable_listContext tff_variable_list() {
			return getRuleContext(Tff_variable_listContext.class,0);
		}
		public Tff_let_term_defnContext tff_let_term_defn() {
			return getRuleContext(Tff_let_term_defnContext.class,0);
		}
		public Tff_let_term_bindingContext tff_let_term_binding() {
			return getRuleContext(Tff_let_term_bindingContext.class,0);
		}
		public Tff_let_term_defnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_term_defn; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_term_defn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_term_defn(this);
		}
	}

	public final Tff_let_term_defnContext tff_let_term_defn() throws RecognitionException {
		Tff_let_term_defnContext _localctx = new Tff_let_term_defnContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_tff_let_term_defn);
		try {
			setState(895);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Forall:
				enterOuterAlt(_localctx, 1);
				{
				setState(887);
				match(Forall);
				setState(888);
				match(T__11);
				setState(889);
				tff_variable_list();
				setState(890);
				match(T__12);
				setState(891);
				match(T__13);
				setState(892);
				tff_let_term_defn();
				}
				break;
			case T__9:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 2);
				{
				setState(894);
				tff_let_term_binding();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_term_bindingContext extends ParserRuleContext {
		public Fof_plain_termContext fof_plain_term() {
			return getRuleContext(Fof_plain_termContext.class,0);
		}
		public TerminalNode Infix_equality() { return getToken(tptp_v7_0_0_0Parser.Infix_equality, 0); }
		public Fof_termContext fof_term() {
			return getRuleContext(Fof_termContext.class,0);
		}
		public Tff_let_term_bindingContext tff_let_term_binding() {
			return getRuleContext(Tff_let_term_bindingContext.class,0);
		}
		public Tff_let_term_bindingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_term_binding; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_term_binding(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_term_binding(this);
		}
	}

	public final Tff_let_term_bindingContext tff_let_term_binding() throws RecognitionException {
		Tff_let_term_bindingContext _localctx = new Tff_let_term_bindingContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_tff_let_term_binding);
		try {
			setState(905);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(897);
				fof_plain_term();
				setState(898);
				match(Infix_equality);
				setState(899);
				fof_term();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(901);
				match(T__9);
				setState(902);
				tff_let_term_binding();
				setState(903);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_formula_defnsContext extends ParserRuleContext {
		public Tff_let_formula_defnContext tff_let_formula_defn() {
			return getRuleContext(Tff_let_formula_defnContext.class,0);
		}
		public Tff_let_formula_listContext tff_let_formula_list() {
			return getRuleContext(Tff_let_formula_listContext.class,0);
		}
		public Tff_let_formula_defnsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_formula_defns; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_formula_defns(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_formula_defns(this);
		}
	}

	public final Tff_let_formula_defnsContext tff_let_formula_defns() throws RecognitionException {
		Tff_let_formula_defnsContext _localctx = new Tff_let_formula_defnsContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_tff_let_formula_defns);
		try {
			setState(912);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__9:
			case Forall:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(907);
				tff_let_formula_defn();
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(908);
				match(T__11);
				setState(909);
				tff_let_formula_list();
				setState(910);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_formula_listContext extends ParserRuleContext {
		public List<Tff_let_formula_defnContext> tff_let_formula_defn() {
			return getRuleContexts(Tff_let_formula_defnContext.class);
		}
		public Tff_let_formula_defnContext tff_let_formula_defn(int i) {
			return getRuleContext(Tff_let_formula_defnContext.class,i);
		}
		public Tff_let_formula_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_formula_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_formula_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_formula_list(this);
		}
	}

	public final Tff_let_formula_listContext tff_let_formula_list() throws RecognitionException {
		Tff_let_formula_listContext _localctx = new Tff_let_formula_listContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_tff_let_formula_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(914);
			tff_let_formula_defn();
			setState(919);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(915);
				match(T__1);
				setState(916);
				tff_let_formula_defn();
				}
				}
				setState(921);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_formula_defnContext extends ParserRuleContext {
		public TerminalNode Forall() { return getToken(tptp_v7_0_0_0Parser.Forall, 0); }
		public Tff_variable_listContext tff_variable_list() {
			return getRuleContext(Tff_variable_listContext.class,0);
		}
		public Tff_let_formula_defnContext tff_let_formula_defn() {
			return getRuleContext(Tff_let_formula_defnContext.class,0);
		}
		public Tff_let_formula_bindingContext tff_let_formula_binding() {
			return getRuleContext(Tff_let_formula_bindingContext.class,0);
		}
		public Tff_let_formula_defnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_formula_defn; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_formula_defn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_formula_defn(this);
		}
	}

	public final Tff_let_formula_defnContext tff_let_formula_defn() throws RecognitionException {
		Tff_let_formula_defnContext _localctx = new Tff_let_formula_defnContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_tff_let_formula_defn);
		try {
			setState(930);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Forall:
				enterOuterAlt(_localctx, 1);
				{
				setState(922);
				match(Forall);
				setState(923);
				match(T__11);
				setState(924);
				tff_variable_list();
				setState(925);
				match(T__12);
				setState(926);
				match(T__13);
				setState(927);
				tff_let_formula_defn();
				}
				break;
			case T__9:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 2);
				{
				setState(929);
				tff_let_formula_binding();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_formula_bindingContext extends ParserRuleContext {
		public Fof_plain_atomic_formulaContext fof_plain_atomic_formula() {
			return getRuleContext(Fof_plain_atomic_formulaContext.class,0);
		}
		public TerminalNode Iff() { return getToken(tptp_v7_0_0_0Parser.Iff, 0); }
		public Tff_unitary_formulaContext tff_unitary_formula() {
			return getRuleContext(Tff_unitary_formulaContext.class,0);
		}
		public Tff_let_formula_bindingContext tff_let_formula_binding() {
			return getRuleContext(Tff_let_formula_bindingContext.class,0);
		}
		public Tff_let_formula_bindingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_formula_binding; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_formula_binding(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_formula_binding(this);
		}
	}

	public final Tff_let_formula_bindingContext tff_let_formula_binding() throws RecognitionException {
		Tff_let_formula_bindingContext _localctx = new Tff_let_formula_bindingContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_tff_let_formula_binding);
		try {
			setState(940);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(932);
				fof_plain_atomic_formula();
				setState(933);
				match(Iff);
				setState(934);
				tff_unitary_formula();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(936);
				match(T__9);
				setState(937);
				tff_let_formula_binding();
				setState(938);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_sequentContext extends ParserRuleContext {
		public List<Tff_formula_tupleContext> tff_formula_tuple() {
			return getRuleContexts(Tff_formula_tupleContext.class);
		}
		public Tff_formula_tupleContext tff_formula_tuple(int i) {
			return getRuleContext(Tff_formula_tupleContext.class,i);
		}
		public TerminalNode Gentzen_arrow() { return getToken(tptp_v7_0_0_0Parser.Gentzen_arrow, 0); }
		public Tff_sequentContext tff_sequent() {
			return getRuleContext(Tff_sequentContext.class,0);
		}
		public Tff_sequentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_sequent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_sequent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_sequent(this);
		}
	}

	public final Tff_sequentContext tff_sequent() throws RecognitionException {
		Tff_sequentContext _localctx = new Tff_sequentContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_tff_sequent);
		try {
			setState(950);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__11:
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(942);
				tff_formula_tuple();
				setState(943);
				match(Gentzen_arrow);
				setState(944);
				tff_formula_tuple();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(946);
				match(T__9);
				setState(947);
				tff_sequent();
				setState(948);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_formula_tupleContext extends ParserRuleContext {
		public Tff_formula_tuple_listContext tff_formula_tuple_list() {
			return getRuleContext(Tff_formula_tuple_listContext.class,0);
		}
		public Tff_formula_tupleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_formula_tuple; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_formula_tuple(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_formula_tuple(this);
		}
	}

	public final Tff_formula_tupleContext tff_formula_tuple() throws RecognitionException {
		Tff_formula_tupleContext _localctx = new Tff_formula_tupleContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_tff_formula_tuple);
		try {
			setState(957);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(952);
				match(T__16);
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(953);
				match(T__11);
				setState(954);
				tff_formula_tuple_list();
				setState(955);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_formula_tuple_listContext extends ParserRuleContext {
		public List<Tff_logic_formulaContext> tff_logic_formula() {
			return getRuleContexts(Tff_logic_formulaContext.class);
		}
		public Tff_logic_formulaContext tff_logic_formula(int i) {
			return getRuleContext(Tff_logic_formulaContext.class,i);
		}
		public Tff_formula_tuple_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_formula_tuple_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_formula_tuple_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_formula_tuple_list(this);
		}
	}

	public final Tff_formula_tuple_listContext tff_formula_tuple_list() throws RecognitionException {
		Tff_formula_tuple_listContext _localctx = new Tff_formula_tuple_listContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_tff_formula_tuple_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(959);
			tff_logic_formula();
			setState(964);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(960);
				match(T__1);
				setState(961);
				tff_logic_formula();
				}
				}
				setState(966);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_typed_atomContext extends ParserRuleContext {
		public Untyped_atomContext untyped_atom() {
			return getRuleContext(Untyped_atomContext.class,0);
		}
		public Tff_top_level_typeContext tff_top_level_type() {
			return getRuleContext(Tff_top_level_typeContext.class,0);
		}
		public Tff_typed_atomContext tff_typed_atom() {
			return getRuleContext(Tff_typed_atomContext.class,0);
		}
		public Tff_typed_atomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_typed_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_typed_atom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_typed_atom(this);
		}
	}

	public final Tff_typed_atomContext tff_typed_atom() throws RecognitionException {
		Tff_typed_atomContext _localctx = new Tff_typed_atomContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_tff_typed_atom);
		try {
			setState(975);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Dollar_dollar_word:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(967);
				untyped_atom();
				setState(968);
				match(T__13);
				setState(969);
				tff_top_level_type();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(971);
				match(T__9);
				setState(972);
				tff_typed_atom();
				setState(973);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_subtypeContext extends ParserRuleContext {
		public Untyped_atomContext untyped_atom() {
			return getRuleContext(Untyped_atomContext.class,0);
		}
		public TerminalNode Subtype_sign() { return getToken(tptp_v7_0_0_0Parser.Subtype_sign, 0); }
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public Tff_subtypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_subtype; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_subtype(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_subtype(this);
		}
	}

	public final Tff_subtypeContext tff_subtype() throws RecognitionException {
		Tff_subtypeContext _localctx = new Tff_subtypeContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_tff_subtype);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(977);
			untyped_atom();
			setState(978);
			match(Subtype_sign);
			setState(979);
			atom();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_top_level_typeContext extends ParserRuleContext {
		public Tff_atomic_typeContext tff_atomic_type() {
			return getRuleContext(Tff_atomic_typeContext.class,0);
		}
		public Tff_mapping_typeContext tff_mapping_type() {
			return getRuleContext(Tff_mapping_typeContext.class,0);
		}
		public Tf1_quantified_typeContext tf1_quantified_type() {
			return getRuleContext(Tf1_quantified_typeContext.class,0);
		}
		public Tff_top_level_typeContext tff_top_level_type() {
			return getRuleContext(Tff_top_level_typeContext.class,0);
		}
		public Tff_top_level_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_top_level_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_top_level_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_top_level_type(this);
		}
	}

	public final Tff_top_level_typeContext tff_top_level_type() throws RecognitionException {
		Tff_top_level_typeContext _localctx = new Tff_top_level_typeContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_tff_top_level_type);
		try {
			setState(988);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(981);
				tff_atomic_type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(982);
				tff_mapping_type();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(983);
				tf1_quantified_type();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(984);
				match(T__9);
				setState(985);
				tff_top_level_type();
				setState(986);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tf1_quantified_typeContext extends ParserRuleContext {
		public TerminalNode TyForall() { return getToken(tptp_v7_0_0_0Parser.TyForall, 0); }
		public Tff_variable_listContext tff_variable_list() {
			return getRuleContext(Tff_variable_listContext.class,0);
		}
		public Tff_monotypeContext tff_monotype() {
			return getRuleContext(Tff_monotypeContext.class,0);
		}
		public Tf1_quantified_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tf1_quantified_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTf1_quantified_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTf1_quantified_type(this);
		}
	}

	public final Tf1_quantified_typeContext tf1_quantified_type() throws RecognitionException {
		Tf1_quantified_typeContext _localctx = new Tf1_quantified_typeContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_tf1_quantified_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(990);
			match(TyForall);
			setState(991);
			match(T__11);
			setState(992);
			tff_variable_list();
			setState(993);
			match(T__12);
			setState(994);
			match(T__13);
			setState(995);
			tff_monotype();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_monotypeContext extends ParserRuleContext {
		public Tff_atomic_typeContext tff_atomic_type() {
			return getRuleContext(Tff_atomic_typeContext.class,0);
		}
		public Tff_mapping_typeContext tff_mapping_type() {
			return getRuleContext(Tff_mapping_typeContext.class,0);
		}
		public Tff_monotypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_monotype; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_monotype(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_monotype(this);
		}
	}

	public final Tff_monotypeContext tff_monotype() throws RecognitionException {
		Tff_monotypeContext _localctx = new Tff_monotypeContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_tff_monotype);
		try {
			setState(1002);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(997);
				tff_atomic_type();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(998);
				match(T__9);
				setState(999);
				tff_mapping_type();
				setState(1000);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_unitary_typeContext extends ParserRuleContext {
		public Tff_atomic_typeContext tff_atomic_type() {
			return getRuleContext(Tff_atomic_typeContext.class,0);
		}
		public Tff_xprod_typeContext tff_xprod_type() {
			return getRuleContext(Tff_xprod_typeContext.class,0);
		}
		public Tff_unitary_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_unitary_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_unitary_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_unitary_type(this);
		}
	}

	public final Tff_unitary_typeContext tff_unitary_type() throws RecognitionException {
		Tff_unitary_typeContext _localctx = new Tff_unitary_typeContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_tff_unitary_type);
		try {
			setState(1009);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1004);
				tff_atomic_type();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(1005);
				match(T__9);
				setState(1006);
				tff_xprod_type(0);
				setState(1007);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_atomic_typeContext extends ParserRuleContext {
		public Type_constantContext type_constant() {
			return getRuleContext(Type_constantContext.class,0);
		}
		public Defined_typeContext defined_type() {
			return getRuleContext(Defined_typeContext.class,0);
		}
		public Type_functorContext type_functor() {
			return getRuleContext(Type_functorContext.class,0);
		}
		public Tff_type_argumentsContext tff_type_arguments() {
			return getRuleContext(Tff_type_argumentsContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Tff_atomic_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_atomic_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_atomic_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_atomic_type(this);
		}
	}

	public final Tff_atomic_typeContext tff_atomic_type() throws RecognitionException {
		Tff_atomic_typeContext _localctx = new Tff_atomic_typeContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_tff_atomic_type);
		try {
			setState(1019);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,60,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1011);
				type_constant();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1012);
				defined_type();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1013);
				type_functor();
				setState(1014);
				match(T__9);
				setState(1015);
				tff_type_arguments();
				setState(1016);
				match(T__10);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1018);
				variable();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_type_argumentsContext extends ParserRuleContext {
		public List<Tff_atomic_typeContext> tff_atomic_type() {
			return getRuleContexts(Tff_atomic_typeContext.class);
		}
		public Tff_atomic_typeContext tff_atomic_type(int i) {
			return getRuleContext(Tff_atomic_typeContext.class,i);
		}
		public Tff_type_argumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_type_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_type_arguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_type_arguments(this);
		}
	}

	public final Tff_type_argumentsContext tff_type_arguments() throws RecognitionException {
		Tff_type_argumentsContext _localctx = new Tff_type_argumentsContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_tff_type_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1021);
			tff_atomic_type();
			setState(1026);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1022);
				match(T__1);
				setState(1023);
				tff_atomic_type();
				}
				}
				setState(1028);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_mapping_typeContext extends ParserRuleContext {
		public Tff_unitary_typeContext tff_unitary_type() {
			return getRuleContext(Tff_unitary_typeContext.class,0);
		}
		public TerminalNode Arrow() { return getToken(tptp_v7_0_0_0Parser.Arrow, 0); }
		public Tff_atomic_typeContext tff_atomic_type() {
			return getRuleContext(Tff_atomic_typeContext.class,0);
		}
		public Tff_mapping_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_mapping_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_mapping_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_mapping_type(this);
		}
	}

	public final Tff_mapping_typeContext tff_mapping_type() throws RecognitionException {
		Tff_mapping_typeContext _localctx = new Tff_mapping_typeContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_tff_mapping_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1029);
			tff_unitary_type();
			setState(1030);
			match(Arrow);
			setState(1031);
			tff_atomic_type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_xprod_typeContext extends ParserRuleContext {
		public Tff_unitary_typeContext tff_unitary_type() {
			return getRuleContext(Tff_unitary_typeContext.class,0);
		}
		public TerminalNode Star() { return getToken(tptp_v7_0_0_0Parser.Star, 0); }
		public Tff_atomic_typeContext tff_atomic_type() {
			return getRuleContext(Tff_atomic_typeContext.class,0);
		}
		public Tff_xprod_typeContext tff_xprod_type() {
			return getRuleContext(Tff_xprod_typeContext.class,0);
		}
		public Tff_xprod_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_xprod_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_xprod_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_xprod_type(this);
		}
	}

	public final Tff_xprod_typeContext tff_xprod_type() throws RecognitionException {
		return tff_xprod_type(0);
	}

	private Tff_xprod_typeContext tff_xprod_type(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Tff_xprod_typeContext _localctx = new Tff_xprod_typeContext(_ctx, _parentState);
		Tff_xprod_typeContext _prevctx = _localctx;
		int _startState = 170;
		enterRecursionRule(_localctx, 170, RULE_tff_xprod_type, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(1034);
			tff_unitary_type();
			setState(1035);
			match(Star);
			setState(1036);
			tff_atomic_type();
			}
			_ctx.stop = _input.LT(-1);
			setState(1043);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Tff_xprod_typeContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_tff_xprod_type);
					setState(1038);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(1039);
					match(Star);
					setState(1040);
					tff_atomic_type();
					}
					} 
				}
				setState(1045);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Tcf_formulaContext extends ParserRuleContext {
		public Tcf_logic_formulaContext tcf_logic_formula() {
			return getRuleContext(Tcf_logic_formulaContext.class,0);
		}
		public Tff_typed_atomContext tff_typed_atom() {
			return getRuleContext(Tff_typed_atomContext.class,0);
		}
		public Tcf_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tcf_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTcf_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTcf_formula(this);
		}
	}

	public final Tcf_formulaContext tcf_formula() throws RecognitionException {
		Tcf_formulaContext _localctx = new Tcf_formulaContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_tcf_formula);
		try {
			setState(1048);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1046);
				tcf_logic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1047);
				tff_typed_atom();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tcf_logic_formulaContext extends ParserRuleContext {
		public Tcf_quantified_formulaContext tcf_quantified_formula() {
			return getRuleContext(Tcf_quantified_formulaContext.class,0);
		}
		public Cnf_formulaContext cnf_formula() {
			return getRuleContext(Cnf_formulaContext.class,0);
		}
		public Tcf_logic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tcf_logic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTcf_logic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTcf_logic_formula(this);
		}
	}

	public final Tcf_logic_formulaContext tcf_logic_formula() throws RecognitionException {
		Tcf_logic_formulaContext _localctx = new Tcf_logic_formulaContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_tcf_logic_formula);
		try {
			setState(1052);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Forall:
				enterOuterAlt(_localctx, 1);
				{
				setState(1050);
				tcf_quantified_formula();
				}
				break;
			case T__9:
			case T__17:
			case T__18:
			case T__23:
			case T__24:
			case T__25:
			case Not:
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Dollar_dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
			case Distinct_object:
				enterOuterAlt(_localctx, 2);
				{
				setState(1051);
				cnf_formula();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tcf_quantified_formulaContext extends ParserRuleContext {
		public TerminalNode Forall() { return getToken(tptp_v7_0_0_0Parser.Forall, 0); }
		public Tff_variable_listContext tff_variable_list() {
			return getRuleContext(Tff_variable_listContext.class,0);
		}
		public Cnf_formulaContext cnf_formula() {
			return getRuleContext(Cnf_formulaContext.class,0);
		}
		public Tcf_quantified_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tcf_quantified_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTcf_quantified_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTcf_quantified_formula(this);
		}
	}

	public final Tcf_quantified_formulaContext tcf_quantified_formula() throws RecognitionException {
		Tcf_quantified_formulaContext _localctx = new Tcf_quantified_formulaContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_tcf_quantified_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1054);
			match(Forall);
			setState(1055);
			match(T__11);
			setState(1056);
			tff_variable_list();
			setState(1057);
			match(T__12);
			setState(1058);
			match(T__13);
			setState(1059);
			cnf_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_formulaContext extends ParserRuleContext {
		public Fof_logic_formulaContext fof_logic_formula() {
			return getRuleContext(Fof_logic_formulaContext.class,0);
		}
		public Fof_sequentContext fof_sequent() {
			return getRuleContext(Fof_sequentContext.class,0);
		}
		public Fof_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_formula(this);
		}
	}

	public final Fof_formulaContext fof_formula() throws RecognitionException {
		Fof_formulaContext _localctx = new Fof_formulaContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_fof_formula);
		try {
			setState(1063);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1061);
				fof_logic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1062);
				fof_sequent();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_logic_formulaContext extends ParserRuleContext {
		public Fof_binary_formulaContext fof_binary_formula() {
			return getRuleContext(Fof_binary_formulaContext.class,0);
		}
		public Fof_unitary_formulaContext fof_unitary_formula() {
			return getRuleContext(Fof_unitary_formulaContext.class,0);
		}
		public Fof_logic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_logic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_logic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_logic_formula(this);
		}
	}

	public final Fof_logic_formulaContext fof_logic_formula() throws RecognitionException {
		Fof_logic_formulaContext _localctx = new Fof_logic_formulaContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_fof_logic_formula);
		try {
			setState(1067);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1065);
				fof_binary_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1066);
				fof_unitary_formula();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_binary_formulaContext extends ParserRuleContext {
		public Fof_binary_nonassocContext fof_binary_nonassoc() {
			return getRuleContext(Fof_binary_nonassocContext.class,0);
		}
		public Fof_binary_assocContext fof_binary_assoc() {
			return getRuleContext(Fof_binary_assocContext.class,0);
		}
		public Fof_binary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_binary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_binary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_binary_formula(this);
		}
	}

	public final Fof_binary_formulaContext fof_binary_formula() throws RecognitionException {
		Fof_binary_formulaContext _localctx = new Fof_binary_formulaContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_fof_binary_formula);
		try {
			setState(1071);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1069);
				fof_binary_nonassoc();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1070);
				fof_binary_assoc();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_binary_nonassocContext extends ParserRuleContext {
		public List<Fof_unitary_formulaContext> fof_unitary_formula() {
			return getRuleContexts(Fof_unitary_formulaContext.class);
		}
		public Fof_unitary_formulaContext fof_unitary_formula(int i) {
			return getRuleContext(Fof_unitary_formulaContext.class,i);
		}
		public Binary_connectiveContext binary_connective() {
			return getRuleContext(Binary_connectiveContext.class,0);
		}
		public Fof_binary_nonassocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_binary_nonassoc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_binary_nonassoc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_binary_nonassoc(this);
		}
	}

	public final Fof_binary_nonassocContext fof_binary_nonassoc() throws RecognitionException {
		Fof_binary_nonassocContext _localctx = new Fof_binary_nonassocContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_fof_binary_nonassoc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1073);
			fof_unitary_formula();
			setState(1074);
			binary_connective();
			setState(1075);
			fof_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_binary_assocContext extends ParserRuleContext {
		public Fof_or_formulaContext fof_or_formula() {
			return getRuleContext(Fof_or_formulaContext.class,0);
		}
		public Fof_and_formulaContext fof_and_formula() {
			return getRuleContext(Fof_and_formulaContext.class,0);
		}
		public Fof_binary_assocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_binary_assoc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_binary_assoc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_binary_assoc(this);
		}
	}

	public final Fof_binary_assocContext fof_binary_assoc() throws RecognitionException {
		Fof_binary_assocContext _localctx = new Fof_binary_assocContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_fof_binary_assoc);
		try {
			setState(1079);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1077);
				fof_or_formula(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1078);
				fof_and_formula(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_or_formulaContext extends ParserRuleContext {
		public List<Fof_unitary_formulaContext> fof_unitary_formula() {
			return getRuleContexts(Fof_unitary_formulaContext.class);
		}
		public Fof_unitary_formulaContext fof_unitary_formula(int i) {
			return getRuleContext(Fof_unitary_formulaContext.class,i);
		}
		public TerminalNode Or() { return getToken(tptp_v7_0_0_0Parser.Or, 0); }
		public Fof_or_formulaContext fof_or_formula() {
			return getRuleContext(Fof_or_formulaContext.class,0);
		}
		public Fof_or_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_or_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_or_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_or_formula(this);
		}
	}

	public final Fof_or_formulaContext fof_or_formula() throws RecognitionException {
		return fof_or_formula(0);
	}

	private Fof_or_formulaContext fof_or_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Fof_or_formulaContext _localctx = new Fof_or_formulaContext(_ctx, _parentState);
		Fof_or_formulaContext _prevctx = _localctx;
		int _startState = 188;
		enterRecursionRule(_localctx, 188, RULE_fof_or_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(1082);
			fof_unitary_formula();
			setState(1083);
			match(Or);
			setState(1084);
			fof_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(1091);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,69,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Fof_or_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_fof_or_formula);
					setState(1086);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(1087);
					match(Or);
					setState(1088);
					fof_unitary_formula();
					}
					} 
				}
				setState(1093);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,69,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Fof_and_formulaContext extends ParserRuleContext {
		public List<Fof_unitary_formulaContext> fof_unitary_formula() {
			return getRuleContexts(Fof_unitary_formulaContext.class);
		}
		public Fof_unitary_formulaContext fof_unitary_formula(int i) {
			return getRuleContext(Fof_unitary_formulaContext.class,i);
		}
		public TerminalNode And() { return getToken(tptp_v7_0_0_0Parser.And, 0); }
		public Fof_and_formulaContext fof_and_formula() {
			return getRuleContext(Fof_and_formulaContext.class,0);
		}
		public Fof_and_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_and_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_and_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_and_formula(this);
		}
	}

	public final Fof_and_formulaContext fof_and_formula() throws RecognitionException {
		return fof_and_formula(0);
	}

	private Fof_and_formulaContext fof_and_formula(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Fof_and_formulaContext _localctx = new Fof_and_formulaContext(_ctx, _parentState);
		Fof_and_formulaContext _prevctx = _localctx;
		int _startState = 190;
		enterRecursionRule(_localctx, 190, RULE_fof_and_formula, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(1095);
			fof_unitary_formula();
			setState(1096);
			match(And);
			setState(1097);
			fof_unitary_formula();
			}
			_ctx.stop = _input.LT(-1);
			setState(1104);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,70,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Fof_and_formulaContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_fof_and_formula);
					setState(1099);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(1100);
					match(And);
					setState(1101);
					fof_unitary_formula();
					}
					} 
				}
				setState(1106);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,70,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Fof_unitary_formulaContext extends ParserRuleContext {
		public Fof_quantified_formulaContext fof_quantified_formula() {
			return getRuleContext(Fof_quantified_formulaContext.class,0);
		}
		public Fof_unary_formulaContext fof_unary_formula() {
			return getRuleContext(Fof_unary_formulaContext.class,0);
		}
		public Fof_atomic_formulaContext fof_atomic_formula() {
			return getRuleContext(Fof_atomic_formulaContext.class,0);
		}
		public Fof_logic_formulaContext fof_logic_formula() {
			return getRuleContext(Fof_logic_formulaContext.class,0);
		}
		public Fof_unitary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_unitary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_unitary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_unitary_formula(this);
		}
	}

	public final Fof_unitary_formulaContext fof_unitary_formula() throws RecognitionException {
		Fof_unitary_formulaContext _localctx = new Fof_unitary_formulaContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_fof_unitary_formula);
		try {
			setState(1114);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1107);
				fof_quantified_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1108);
				fof_unary_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1109);
				fof_atomic_formula();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1110);
				match(T__9);
				setState(1111);
				fof_logic_formula();
				setState(1112);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_quantified_formulaContext extends ParserRuleContext {
		public Fof_quantifierContext fof_quantifier() {
			return getRuleContext(Fof_quantifierContext.class,0);
		}
		public Fof_variable_listContext fof_variable_list() {
			return getRuleContext(Fof_variable_listContext.class,0);
		}
		public Fof_unitary_formulaContext fof_unitary_formula() {
			return getRuleContext(Fof_unitary_formulaContext.class,0);
		}
		public Fof_quantified_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_quantified_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_quantified_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_quantified_formula(this);
		}
	}

	public final Fof_quantified_formulaContext fof_quantified_formula() throws RecognitionException {
		Fof_quantified_formulaContext _localctx = new Fof_quantified_formulaContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_fof_quantified_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1116);
			fof_quantifier();
			setState(1117);
			match(T__11);
			setState(1118);
			fof_variable_list();
			setState(1119);
			match(T__12);
			setState(1120);
			match(T__13);
			setState(1121);
			fof_unitary_formula();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_variable_listContext extends ParserRuleContext {
		public List<VariableContext> variable() {
			return getRuleContexts(VariableContext.class);
		}
		public VariableContext variable(int i) {
			return getRuleContext(VariableContext.class,i);
		}
		public Fof_variable_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_variable_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_variable_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_variable_list(this);
		}
	}

	public final Fof_variable_listContext fof_variable_list() throws RecognitionException {
		Fof_variable_listContext _localctx = new Fof_variable_listContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_fof_variable_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1123);
			variable();
			setState(1128);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1124);
				match(T__1);
				setState(1125);
				variable();
				}
				}
				setState(1130);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_unary_formulaContext extends ParserRuleContext {
		public Unary_connectiveContext unary_connective() {
			return getRuleContext(Unary_connectiveContext.class,0);
		}
		public Fof_unitary_formulaContext fof_unitary_formula() {
			return getRuleContext(Fof_unitary_formulaContext.class,0);
		}
		public Fof_infix_unaryContext fof_infix_unary() {
			return getRuleContext(Fof_infix_unaryContext.class,0);
		}
		public Fof_unary_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_unary_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_unary_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_unary_formula(this);
		}
	}

	public final Fof_unary_formulaContext fof_unary_formula() throws RecognitionException {
		Fof_unary_formulaContext _localctx = new Fof_unary_formulaContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_fof_unary_formula);
		try {
			setState(1135);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Not:
				enterOuterAlt(_localctx, 1);
				{
				setState(1131);
				unary_connective();
				setState(1132);
				fof_unitary_formula();
				}
				break;
			case T__17:
			case T__18:
			case T__23:
			case T__24:
			case T__25:
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Dollar_dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
			case Distinct_object:
				enterOuterAlt(_localctx, 2);
				{
				setState(1134);
				fof_infix_unary();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_infix_unaryContext extends ParserRuleContext {
		public List<Fof_termContext> fof_term() {
			return getRuleContexts(Fof_termContext.class);
		}
		public Fof_termContext fof_term(int i) {
			return getRuleContext(Fof_termContext.class,i);
		}
		public TerminalNode Infix_inequality() { return getToken(tptp_v7_0_0_0Parser.Infix_inequality, 0); }
		public Fof_infix_unaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_infix_unary; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_infix_unary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_infix_unary(this);
		}
	}

	public final Fof_infix_unaryContext fof_infix_unary() throws RecognitionException {
		Fof_infix_unaryContext _localctx = new Fof_infix_unaryContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_fof_infix_unary);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1137);
			fof_term();
			setState(1138);
			match(Infix_inequality);
			setState(1139);
			fof_term();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_atomic_formulaContext extends ParserRuleContext {
		public Fof_plain_atomic_formulaContext fof_plain_atomic_formula() {
			return getRuleContext(Fof_plain_atomic_formulaContext.class,0);
		}
		public Fof_defined_atomic_formulaContext fof_defined_atomic_formula() {
			return getRuleContext(Fof_defined_atomic_formulaContext.class,0);
		}
		public Fof_system_atomic_formulaContext fof_system_atomic_formula() {
			return getRuleContext(Fof_system_atomic_formulaContext.class,0);
		}
		public Fof_atomic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_atomic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_atomic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_atomic_formula(this);
		}
	}

	public final Fof_atomic_formulaContext fof_atomic_formula() throws RecognitionException {
		Fof_atomic_formulaContext _localctx = new Fof_atomic_formulaContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_fof_atomic_formula);
		try {
			setState(1144);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,74,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1141);
				fof_plain_atomic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1142);
				fof_defined_atomic_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1143);
				fof_system_atomic_formula();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_plain_atomic_formulaContext extends ParserRuleContext {
		public Fof_plain_termContext fof_plain_term() {
			return getRuleContext(Fof_plain_termContext.class,0);
		}
		public Fof_plain_atomic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_plain_atomic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_plain_atomic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_plain_atomic_formula(this);
		}
	}

	public final Fof_plain_atomic_formulaContext fof_plain_atomic_formula() throws RecognitionException {
		Fof_plain_atomic_formulaContext _localctx = new Fof_plain_atomic_formulaContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_fof_plain_atomic_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1146);
			fof_plain_term();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_defined_atomic_formulaContext extends ParserRuleContext {
		public Fof_defined_plain_formulaContext fof_defined_plain_formula() {
			return getRuleContext(Fof_defined_plain_formulaContext.class,0);
		}
		public Fof_defined_infix_formulaContext fof_defined_infix_formula() {
			return getRuleContext(Fof_defined_infix_formulaContext.class,0);
		}
		public Fof_defined_atomic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_defined_atomic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_defined_atomic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_defined_atomic_formula(this);
		}
	}

	public final Fof_defined_atomic_formulaContext fof_defined_atomic_formula() throws RecognitionException {
		Fof_defined_atomic_formulaContext _localctx = new Fof_defined_atomic_formulaContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_fof_defined_atomic_formula);
		try {
			setState(1150);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,75,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1148);
				fof_defined_plain_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1149);
				fof_defined_infix_formula();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_defined_plain_formulaContext extends ParserRuleContext {
		public Fof_defined_termContext fof_defined_term() {
			return getRuleContext(Fof_defined_termContext.class,0);
		}
		public Fof_defined_plain_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_defined_plain_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_defined_plain_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_defined_plain_formula(this);
		}
	}

	public final Fof_defined_plain_formulaContext fof_defined_plain_formula() throws RecognitionException {
		Fof_defined_plain_formulaContext _localctx = new Fof_defined_plain_formulaContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_fof_defined_plain_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1152);
			fof_defined_term();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_defined_infix_formulaContext extends ParserRuleContext {
		public List<Fof_termContext> fof_term() {
			return getRuleContexts(Fof_termContext.class);
		}
		public Fof_termContext fof_term(int i) {
			return getRuleContext(Fof_termContext.class,i);
		}
		public Defined_infix_predContext defined_infix_pred() {
			return getRuleContext(Defined_infix_predContext.class,0);
		}
		public Fof_defined_infix_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_defined_infix_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_defined_infix_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_defined_infix_formula(this);
		}
	}

	public final Fof_defined_infix_formulaContext fof_defined_infix_formula() throws RecognitionException {
		Fof_defined_infix_formulaContext _localctx = new Fof_defined_infix_formulaContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_fof_defined_infix_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1154);
			fof_term();
			setState(1155);
			defined_infix_pred();
			setState(1156);
			fof_term();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_system_atomic_formulaContext extends ParserRuleContext {
		public Fof_system_termContext fof_system_term() {
			return getRuleContext(Fof_system_termContext.class,0);
		}
		public Fof_system_atomic_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_system_atomic_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_system_atomic_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_system_atomic_formula(this);
		}
	}

	public final Fof_system_atomic_formulaContext fof_system_atomic_formula() throws RecognitionException {
		Fof_system_atomic_formulaContext _localctx = new Fof_system_atomic_formulaContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_fof_system_atomic_formula);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1158);
			fof_system_term();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_plain_termContext extends ParserRuleContext {
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public FunctorContext functor() {
			return getRuleContext(FunctorContext.class,0);
		}
		public Fof_argumentsContext fof_arguments() {
			return getRuleContext(Fof_argumentsContext.class,0);
		}
		public Fof_plain_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_plain_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_plain_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_plain_term(this);
		}
	}

	public final Fof_plain_termContext fof_plain_term() throws RecognitionException {
		Fof_plain_termContext _localctx = new Fof_plain_termContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_fof_plain_term);
		try {
			setState(1166);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,76,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1160);
				constant();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1161);
				functor();
				setState(1162);
				match(T__9);
				setState(1163);
				fof_arguments();
				setState(1164);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_defined_termContext extends ParserRuleContext {
		public Defined_termContext defined_term() {
			return getRuleContext(Defined_termContext.class,0);
		}
		public Fof_defined_atomic_termContext fof_defined_atomic_term() {
			return getRuleContext(Fof_defined_atomic_termContext.class,0);
		}
		public Fof_defined_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_defined_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_defined_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_defined_term(this);
		}
	}

	public final Fof_defined_termContext fof_defined_term() throws RecognitionException {
		Fof_defined_termContext _localctx = new Fof_defined_termContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_fof_defined_term);
		try {
			setState(1170);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Real:
			case Rational:
			case Integer:
			case Distinct_object:
				enterOuterAlt(_localctx, 1);
				{
				setState(1168);
				defined_term();
				}
				break;
			case Dollar_word:
				enterOuterAlt(_localctx, 2);
				{
				setState(1169);
				fof_defined_atomic_term();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_defined_atomic_termContext extends ParserRuleContext {
		public Fof_defined_plain_termContext fof_defined_plain_term() {
			return getRuleContext(Fof_defined_plain_termContext.class,0);
		}
		public Fof_defined_atomic_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_defined_atomic_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_defined_atomic_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_defined_atomic_term(this);
		}
	}

	public final Fof_defined_atomic_termContext fof_defined_atomic_term() throws RecognitionException {
		Fof_defined_atomic_termContext _localctx = new Fof_defined_atomic_termContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_fof_defined_atomic_term);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1172);
			fof_defined_plain_term();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_defined_plain_termContext extends ParserRuleContext {
		public Defined_constantContext defined_constant() {
			return getRuleContext(Defined_constantContext.class,0);
		}
		public Defined_functorContext defined_functor() {
			return getRuleContext(Defined_functorContext.class,0);
		}
		public Fof_argumentsContext fof_arguments() {
			return getRuleContext(Fof_argumentsContext.class,0);
		}
		public Fof_defined_plain_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_defined_plain_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_defined_plain_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_defined_plain_term(this);
		}
	}

	public final Fof_defined_plain_termContext fof_defined_plain_term() throws RecognitionException {
		Fof_defined_plain_termContext _localctx = new Fof_defined_plain_termContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_fof_defined_plain_term);
		try {
			setState(1180);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1174);
				defined_constant();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1175);
				defined_functor();
				setState(1176);
				match(T__9);
				setState(1177);
				fof_arguments();
				setState(1178);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_system_termContext extends ParserRuleContext {
		public System_constantContext system_constant() {
			return getRuleContext(System_constantContext.class,0);
		}
		public System_functorContext system_functor() {
			return getRuleContext(System_functorContext.class,0);
		}
		public Fof_argumentsContext fof_arguments() {
			return getRuleContext(Fof_argumentsContext.class,0);
		}
		public Fof_system_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_system_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_system_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_system_term(this);
		}
	}

	public final Fof_system_termContext fof_system_term() throws RecognitionException {
		Fof_system_termContext _localctx = new Fof_system_termContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_fof_system_term);
		try {
			setState(1188);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1182);
				system_constant();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1183);
				system_functor();
				setState(1184);
				match(T__9);
				setState(1185);
				fof_arguments();
				setState(1186);
				match(T__10);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_argumentsContext extends ParserRuleContext {
		public List<Fof_termContext> fof_term() {
			return getRuleContexts(Fof_termContext.class);
		}
		public Fof_termContext fof_term(int i) {
			return getRuleContext(Fof_termContext.class,i);
		}
		public Fof_argumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_arguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_arguments(this);
		}
	}

	public final Fof_argumentsContext fof_arguments() throws RecognitionException {
		Fof_argumentsContext _localctx = new Fof_argumentsContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_fof_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1190);
			fof_term();
			setState(1195);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1191);
				match(T__1);
				setState(1192);
				fof_term();
				}
				}
				setState(1197);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_termContext extends ParserRuleContext {
		public Fof_function_termContext fof_function_term() {
			return getRuleContext(Fof_function_termContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Tff_conditional_termContext tff_conditional_term() {
			return getRuleContext(Tff_conditional_termContext.class,0);
		}
		public Tff_let_termContext tff_let_term() {
			return getRuleContext(Tff_let_termContext.class,0);
		}
		public Tff_tuple_termContext tff_tuple_term() {
			return getRuleContext(Tff_tuple_termContext.class,0);
		}
		public Fof_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_term(this);
		}
	}

	public final Fof_termContext fof_term() throws RecognitionException {
		Fof_termContext _localctx = new Fof_termContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_fof_term);
		try {
			setState(1203);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Dollar_dollar_word:
			case Lower_word:
			case Single_quoted:
			case Distinct_object:
				enterOuterAlt(_localctx, 1);
				{
				setState(1198);
				fof_function_term();
				}
				break;
			case Upper_word:
				enterOuterAlt(_localctx, 2);
				{
				setState(1199);
				variable();
				}
				break;
			case T__23:
				enterOuterAlt(_localctx, 3);
				{
				setState(1200);
				tff_conditional_term();
				}
				break;
			case T__24:
			case T__25:
				enterOuterAlt(_localctx, 4);
				{
				setState(1201);
				tff_let_term();
				}
				break;
			case T__17:
			case T__18:
				enterOuterAlt(_localctx, 5);
				{
				setState(1202);
				tff_tuple_term();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_function_termContext extends ParserRuleContext {
		public Fof_plain_termContext fof_plain_term() {
			return getRuleContext(Fof_plain_termContext.class,0);
		}
		public Fof_defined_termContext fof_defined_term() {
			return getRuleContext(Fof_defined_termContext.class,0);
		}
		public Fof_system_termContext fof_system_term() {
			return getRuleContext(Fof_system_termContext.class,0);
		}
		public Fof_function_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_function_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_function_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_function_term(this);
		}
	}

	public final Fof_function_termContext fof_function_term() throws RecognitionException {
		Fof_function_termContext _localctx = new Fof_function_termContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_fof_function_term);
		try {
			setState(1208);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1205);
				fof_plain_term();
				}
				break;
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Distinct_object:
				enterOuterAlt(_localctx, 2);
				{
				setState(1206);
				fof_defined_term();
				}
				break;
			case Dollar_dollar_word:
				enterOuterAlt(_localctx, 3);
				{
				setState(1207);
				fof_system_term();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_conditional_termContext extends ParserRuleContext {
		public Tff_logic_formulaContext tff_logic_formula() {
			return getRuleContext(Tff_logic_formulaContext.class,0);
		}
		public List<Fof_termContext> fof_term() {
			return getRuleContexts(Fof_termContext.class);
		}
		public Fof_termContext fof_term(int i) {
			return getRuleContext(Fof_termContext.class,i);
		}
		public Tff_conditional_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_conditional_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_conditional_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_conditional_term(this);
		}
	}

	public final Tff_conditional_termContext tff_conditional_term() throws RecognitionException {
		Tff_conditional_termContext _localctx = new Tff_conditional_termContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_tff_conditional_term);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1210);
			match(T__23);
			setState(1211);
			tff_logic_formula();
			setState(1212);
			match(T__1);
			setState(1213);
			fof_term();
			setState(1214);
			match(T__1);
			setState(1215);
			fof_term();
			setState(1216);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_let_termContext extends ParserRuleContext {
		public Tff_let_formula_defnsContext tff_let_formula_defns() {
			return getRuleContext(Tff_let_formula_defnsContext.class,0);
		}
		public Fof_termContext fof_term() {
			return getRuleContext(Fof_termContext.class,0);
		}
		public Tff_let_term_defnsContext tff_let_term_defns() {
			return getRuleContext(Tff_let_term_defnsContext.class,0);
		}
		public Tff_let_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_let_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_let_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_let_term(this);
		}
	}

	public final Tff_let_termContext tff_let_term() throws RecognitionException {
		Tff_let_termContext _localctx = new Tff_let_termContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_tff_let_term);
		try {
			setState(1230);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__24:
				enterOuterAlt(_localctx, 1);
				{
				setState(1218);
				match(T__24);
				setState(1219);
				tff_let_formula_defns();
				setState(1220);
				match(T__1);
				setState(1221);
				fof_term();
				setState(1222);
				match(T__10);
				}
				break;
			case T__25:
				enterOuterAlt(_localctx, 2);
				{
				setState(1224);
				match(T__25);
				setState(1225);
				tff_let_term_defns();
				setState(1226);
				match(T__1);
				setState(1227);
				fof_term();
				setState(1228);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_tuple_termContext extends ParserRuleContext {
		public Fof_argumentsContext fof_arguments() {
			return getRuleContext(Fof_argumentsContext.class,0);
		}
		public Tff_tuple_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_tuple_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_tuple_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_tuple_term(this);
		}
	}

	public final Tff_tuple_termContext tff_tuple_term() throws RecognitionException {
		Tff_tuple_termContext _localctx = new Tff_tuple_termContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_tff_tuple_term);
		try {
			setState(1237);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__17:
				enterOuterAlt(_localctx, 1);
				{
				setState(1232);
				match(T__17);
				}
				break;
			case T__18:
				enterOuterAlt(_localctx, 2);
				{
				setState(1233);
				match(T__18);
				setState(1234);
				fof_arguments();
				setState(1235);
				match(T__19);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_sequentContext extends ParserRuleContext {
		public List<Fof_formula_tupleContext> fof_formula_tuple() {
			return getRuleContexts(Fof_formula_tupleContext.class);
		}
		public Fof_formula_tupleContext fof_formula_tuple(int i) {
			return getRuleContext(Fof_formula_tupleContext.class,i);
		}
		public TerminalNode Gentzen_arrow() { return getToken(tptp_v7_0_0_0Parser.Gentzen_arrow, 0); }
		public Fof_sequentContext fof_sequent() {
			return getRuleContext(Fof_sequentContext.class,0);
		}
		public Fof_sequentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_sequent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_sequent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_sequent(this);
		}
	}

	public final Fof_sequentContext fof_sequent() throws RecognitionException {
		Fof_sequentContext _localctx = new Fof_sequentContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_fof_sequent);
		try {
			setState(1247);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__11:
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(1239);
				fof_formula_tuple();
				setState(1240);
				match(Gentzen_arrow);
				setState(1241);
				fof_formula_tuple();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(1243);
				match(T__9);
				setState(1244);
				fof_sequent();
				setState(1245);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_formula_tupleContext extends ParserRuleContext {
		public Fof_formula_tuple_listContext fof_formula_tuple_list() {
			return getRuleContext(Fof_formula_tuple_listContext.class,0);
		}
		public Fof_formula_tupleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_formula_tuple; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_formula_tuple(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_formula_tuple(this);
		}
	}

	public final Fof_formula_tupleContext fof_formula_tuple() throws RecognitionException {
		Fof_formula_tupleContext _localctx = new Fof_formula_tupleContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_fof_formula_tuple);
		try {
			setState(1254);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(1249);
				match(T__16);
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(1250);
				match(T__11);
				setState(1251);
				fof_formula_tuple_list();
				setState(1252);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_formula_tuple_listContext extends ParserRuleContext {
		public List<Fof_logic_formulaContext> fof_logic_formula() {
			return getRuleContexts(Fof_logic_formulaContext.class);
		}
		public Fof_logic_formulaContext fof_logic_formula(int i) {
			return getRuleContext(Fof_logic_formulaContext.class,i);
		}
		public Fof_formula_tuple_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_formula_tuple_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_formula_tuple_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_formula_tuple_list(this);
		}
	}

	public final Fof_formula_tuple_listContext fof_formula_tuple_list() throws RecognitionException {
		Fof_formula_tuple_listContext _localctx = new Fof_formula_tuple_listContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_fof_formula_tuple_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1256);
			fof_logic_formula();
			setState(1261);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1257);
				match(T__1);
				setState(1258);
				fof_logic_formula();
				}
				}
				setState(1263);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Cnf_formulaContext extends ParserRuleContext {
		public Cnf_disjunctionContext cnf_disjunction() {
			return getRuleContext(Cnf_disjunctionContext.class,0);
		}
		public Cnf_formulaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cnf_formula; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterCnf_formula(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitCnf_formula(this);
		}
	}

	public final Cnf_formulaContext cnf_formula() throws RecognitionException {
		Cnf_formulaContext _localctx = new Cnf_formulaContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_cnf_formula);
		try {
			setState(1269);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__17:
			case T__18:
			case T__23:
			case T__24:
			case T__25:
			case Not:
			case Real:
			case Rational:
			case Integer:
			case Dollar_word:
			case Dollar_dollar_word:
			case Upper_word:
			case Lower_word:
			case Single_quoted:
			case Distinct_object:
				enterOuterAlt(_localctx, 1);
				{
				setState(1264);
				cnf_disjunction(0);
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(1265);
				match(T__9);
				setState(1266);
				cnf_disjunction(0);
				setState(1267);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Cnf_disjunctionContext extends ParserRuleContext {
		public Cnf_literalContext cnf_literal() {
			return getRuleContext(Cnf_literalContext.class,0);
		}
		public Cnf_disjunctionContext cnf_disjunction() {
			return getRuleContext(Cnf_disjunctionContext.class,0);
		}
		public TerminalNode Or() { return getToken(tptp_v7_0_0_0Parser.Or, 0); }
		public Cnf_disjunctionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cnf_disjunction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterCnf_disjunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitCnf_disjunction(this);
		}
	}

	public final Cnf_disjunctionContext cnf_disjunction() throws RecognitionException {
		return cnf_disjunction(0);
	}

	private Cnf_disjunctionContext cnf_disjunction(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Cnf_disjunctionContext _localctx = new Cnf_disjunctionContext(_ctx, _parentState);
		Cnf_disjunctionContext _prevctx = _localctx;
		int _startState = 244;
		enterRecursionRule(_localctx, 244, RULE_cnf_disjunction, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(1272);
			cnf_literal();
			}
			_ctx.stop = _input.LT(-1);
			setState(1279);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Cnf_disjunctionContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_cnf_disjunction);
					setState(1274);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(1275);
					match(Or);
					setState(1276);
					cnf_literal();
					}
					} 
				}
				setState(1281);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Cnf_literalContext extends ParserRuleContext {
		public Fof_atomic_formulaContext fof_atomic_formula() {
			return getRuleContext(Fof_atomic_formulaContext.class,0);
		}
		public TerminalNode Not() { return getToken(tptp_v7_0_0_0Parser.Not, 0); }
		public Fof_infix_unaryContext fof_infix_unary() {
			return getRuleContext(Fof_infix_unaryContext.class,0);
		}
		public Cnf_literalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cnf_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterCnf_literal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitCnf_literal(this);
		}
	}

	public final Cnf_literalContext cnf_literal() throws RecognitionException {
		Cnf_literalContext _localctx = new Cnf_literalContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_cnf_literal);
		try {
			setState(1286);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1282);
				fof_atomic_formula();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1283);
				match(Not);
				setState(1284);
				fof_atomic_formula();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1285);
				fof_infix_unary();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_quantifierContext extends ParserRuleContext {
		public Fof_quantifierContext fof_quantifier() {
			return getRuleContext(Fof_quantifierContext.class,0);
		}
		public Th0_quantifierContext th0_quantifier() {
			return getRuleContext(Th0_quantifierContext.class,0);
		}
		public Th1_quantifierContext th1_quantifier() {
			return getRuleContext(Th1_quantifierContext.class,0);
		}
		public Thf_quantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_quantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_quantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_quantifier(this);
		}
	}

	public final Thf_quantifierContext thf_quantifier() throws RecognitionException {
		Thf_quantifierContext _localctx = new Thf_quantifierContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_thf_quantifier);
		try {
			setState(1291);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Forall:
			case Exists:
				enterOuterAlt(_localctx, 1);
				{
				setState(1288);
				fof_quantifier();
				}
				break;
			case Lambda:
			case Choice:
			case Description:
				enterOuterAlt(_localctx, 2);
				{
				setState(1289);
				th0_quantifier();
				}
				break;
			case TyForall:
			case TyExists:
				enterOuterAlt(_localctx, 3);
				{
				setState(1290);
				th1_quantifier();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Th0_quantifierContext extends ParserRuleContext {
		public TerminalNode Lambda() { return getToken(tptp_v7_0_0_0Parser.Lambda, 0); }
		public TerminalNode Choice() { return getToken(tptp_v7_0_0_0Parser.Choice, 0); }
		public TerminalNode Description() { return getToken(tptp_v7_0_0_0Parser.Description, 0); }
		public Th0_quantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_th0_quantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTh0_quantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTh0_quantifier(this);
		}
	}

	public final Th0_quantifierContext th0_quantifier() throws RecognitionException {
		Th0_quantifierContext _localctx = new Th0_quantifierContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_th0_quantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1293);
			_la = _input.LA(1);
			if ( !(((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (Lambda - 61)) | (1L << (Choice - 61)) | (1L << (Description - 61)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Th1_quantifierContext extends ParserRuleContext {
		public TerminalNode TyForall() { return getToken(tptp_v7_0_0_0Parser.TyForall, 0); }
		public TerminalNode TyExists() { return getToken(tptp_v7_0_0_0Parser.TyExists, 0); }
		public Th1_quantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_th1_quantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTh1_quantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTh1_quantifier(this);
		}
	}

	public final Th1_quantifierContext th1_quantifier() throws RecognitionException {
		Th1_quantifierContext _localctx = new Th1_quantifierContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_th1_quantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1295);
			_la = _input.LA(1);
			if ( !(_la==TyForall || _la==TyExists) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_pair_connectiveContext extends ParserRuleContext {
		public TerminalNode Infix_equality() { return getToken(tptp_v7_0_0_0Parser.Infix_equality, 0); }
		public TerminalNode Infix_inequality() { return getToken(tptp_v7_0_0_0Parser.Infix_inequality, 0); }
		public Binary_connectiveContext binary_connective() {
			return getRuleContext(Binary_connectiveContext.class,0);
		}
		public TerminalNode Assignment() { return getToken(tptp_v7_0_0_0Parser.Assignment, 0); }
		public Thf_pair_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_pair_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_pair_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_pair_connective(this);
		}
	}

	public final Thf_pair_connectiveContext thf_pair_connective() throws RecognitionException {
		Thf_pair_connectiveContext _localctx = new Thf_pair_connectiveContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_thf_pair_connective);
		try {
			setState(1301);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Infix_equality:
				enterOuterAlt(_localctx, 1);
				{
				setState(1297);
				match(Infix_equality);
				}
				break;
			case Infix_inequality:
				enterOuterAlt(_localctx, 2);
				{
				setState(1298);
				match(Infix_inequality);
				}
				break;
			case Iff:
			case Impl:
			case If:
			case Niff:
			case Nor:
			case Nand:
				enterOuterAlt(_localctx, 3);
				{
				setState(1299);
				binary_connective();
				}
				break;
			case Assignment:
				enterOuterAlt(_localctx, 4);
				{
				setState(1300);
				match(Assignment);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Thf_unary_connectiveContext extends ParserRuleContext {
		public Unary_connectiveContext unary_connective() {
			return getRuleContext(Unary_connectiveContext.class,0);
		}
		public Th1_unary_connectiveContext th1_unary_connective() {
			return getRuleContext(Th1_unary_connectiveContext.class,0);
		}
		public Thf_unary_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_thf_unary_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterThf_unary_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitThf_unary_connective(this);
		}
	}

	public final Thf_unary_connectiveContext thf_unary_connective() throws RecognitionException {
		Thf_unary_connectiveContext _localctx = new Thf_unary_connectiveContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_thf_unary_connective);
		try {
			setState(1305);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Not:
				enterOuterAlt(_localctx, 1);
				{
				setState(1303);
				unary_connective();
				}
				break;
			case ForallComb:
			case ExistsComb:
			case ChoiceComb:
			case DescriptionComb:
			case EqComb:
				enterOuterAlt(_localctx, 2);
				{
				setState(1304);
				th1_unary_connective();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Th1_unary_connectiveContext extends ParserRuleContext {
		public TerminalNode ForallComb() { return getToken(tptp_v7_0_0_0Parser.ForallComb, 0); }
		public TerminalNode ExistsComb() { return getToken(tptp_v7_0_0_0Parser.ExistsComb, 0); }
		public TerminalNode ChoiceComb() { return getToken(tptp_v7_0_0_0Parser.ChoiceComb, 0); }
		public TerminalNode DescriptionComb() { return getToken(tptp_v7_0_0_0Parser.DescriptionComb, 0); }
		public TerminalNode EqComb() { return getToken(tptp_v7_0_0_0Parser.EqComb, 0); }
		public Th1_unary_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_th1_unary_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTh1_unary_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTh1_unary_connective(this);
		}
	}

	public final Th1_unary_connectiveContext th1_unary_connective() throws RecognitionException {
		Th1_unary_connectiveContext _localctx = new Th1_unary_connectiveContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_th1_unary_connective);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1307);
			_la = _input.LA(1);
			if ( !(((((_la - 53)) & ~0x3f) == 0 && ((1L << (_la - 53)) & ((1L << (ForallComb - 53)) | (1L << (ExistsComb - 53)) | (1L << (ChoiceComb - 53)) | (1L << (DescriptionComb - 53)) | (1L << (EqComb - 53)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Tff_pair_connectiveContext extends ParserRuleContext {
		public Binary_connectiveContext binary_connective() {
			return getRuleContext(Binary_connectiveContext.class,0);
		}
		public TerminalNode Assignment() { return getToken(tptp_v7_0_0_0Parser.Assignment, 0); }
		public Tff_pair_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tff_pair_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTff_pair_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTff_pair_connective(this);
		}
	}

	public final Tff_pair_connectiveContext tff_pair_connective() throws RecognitionException {
		Tff_pair_connectiveContext _localctx = new Tff_pair_connectiveContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_tff_pair_connective);
		try {
			setState(1311);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Iff:
			case Impl:
			case If:
			case Niff:
			case Nor:
			case Nand:
				enterOuterAlt(_localctx, 1);
				{
				setState(1309);
				binary_connective();
				}
				break;
			case Assignment:
				enterOuterAlt(_localctx, 2);
				{
				setState(1310);
				match(Assignment);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Fof_quantifierContext extends ParserRuleContext {
		public TerminalNode Forall() { return getToken(tptp_v7_0_0_0Parser.Forall, 0); }
		public TerminalNode Exists() { return getToken(tptp_v7_0_0_0Parser.Exists, 0); }
		public Fof_quantifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fof_quantifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFof_quantifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFof_quantifier(this);
		}
	}

	public final Fof_quantifierContext fof_quantifier() throws RecognitionException {
		Fof_quantifierContext _localctx = new Fof_quantifierContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_fof_quantifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1313);
			_la = _input.LA(1);
			if ( !(_la==Forall || _la==Exists) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Binary_connectiveContext extends ParserRuleContext {
		public TerminalNode Iff() { return getToken(tptp_v7_0_0_0Parser.Iff, 0); }
		public TerminalNode Impl() { return getToken(tptp_v7_0_0_0Parser.Impl, 0); }
		public TerminalNode If() { return getToken(tptp_v7_0_0_0Parser.If, 0); }
		public TerminalNode Niff() { return getToken(tptp_v7_0_0_0Parser.Niff, 0); }
		public TerminalNode Nor() { return getToken(tptp_v7_0_0_0Parser.Nor, 0); }
		public TerminalNode Nand() { return getToken(tptp_v7_0_0_0Parser.Nand, 0); }
		public Binary_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binary_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterBinary_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitBinary_connective(this);
		}
	}

	public final Binary_connectiveContext binary_connective() throws RecognitionException {
		Binary_connectiveContext _localctx = new Binary_connectiveContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_binary_connective);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1315);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << Iff) | (1L << Impl) | (1L << If) | (1L << Niff) | (1L << Nor) | (1L << Nand))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Assoc_connectiveContext extends ParserRuleContext {
		public TerminalNode Or() { return getToken(tptp_v7_0_0_0Parser.Or, 0); }
		public TerminalNode And() { return getToken(tptp_v7_0_0_0Parser.And, 0); }
		public Assoc_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assoc_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAssoc_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAssoc_connective(this);
		}
	}

	public final Assoc_connectiveContext assoc_connective() throws RecognitionException {
		Assoc_connectiveContext _localctx = new Assoc_connectiveContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_assoc_connective);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1317);
			_la = _input.LA(1);
			if ( !(_la==Or || _la==And) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Unary_connectiveContext extends ParserRuleContext {
		public TerminalNode Not() { return getToken(tptp_v7_0_0_0Parser.Not, 0); }
		public Unary_connectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unary_connective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterUnary_connective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitUnary_connective(this);
		}
	}

	public final Unary_connectiveContext unary_connective() throws RecognitionException {
		Unary_connectiveContext _localctx = new Unary_connectiveContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_unary_connective);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1319);
			match(Not);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Type_constantContext extends ParserRuleContext {
		public Type_functorContext type_functor() {
			return getRuleContext(Type_functorContext.class,0);
		}
		public Type_constantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_constant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterType_constant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitType_constant(this);
		}
	}

	public final Type_constantContext type_constant() throws RecognitionException {
		Type_constantContext _localctx = new Type_constantContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_type_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1321);
			type_functor();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Type_functorContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public Type_functorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_functor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterType_functor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitType_functor(this);
		}
	}

	public final Type_functorContext type_functor() throws RecognitionException {
		Type_functorContext _localctx = new Type_functorContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_type_functor);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1323);
			atomic_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_typeContext extends ParserRuleContext {
		public TerminalNode Dollar_word() { return getToken(tptp_v7_0_0_0Parser.Dollar_word, 0); }
		public Defined_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_type(this);
		}
	}

	public final Defined_typeContext defined_type() throws RecognitionException {
		Defined_typeContext _localctx = new Defined_typeContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_defined_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1325);
			match(Dollar_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class System_typeContext extends ParserRuleContext {
		public Atomic_system_wordContext atomic_system_word() {
			return getRuleContext(Atomic_system_wordContext.class,0);
		}
		public System_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterSystem_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitSystem_type(this);
		}
	}

	public final System_typeContext system_type() throws RecognitionException {
		System_typeContext _localctx = new System_typeContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_system_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1327);
			atomic_system_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AtomContext extends ParserRuleContext {
		public Untyped_atomContext untyped_atom() {
			return getRuleContext(Untyped_atomContext.class,0);
		}
		public Defined_constantContext defined_constant() {
			return getRuleContext(Defined_constantContext.class,0);
		}
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAtom(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_atom);
		try {
			setState(1331);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Dollar_dollar_word:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1329);
				untyped_atom();
				}
				break;
			case Dollar_word:
				enterOuterAlt(_localctx, 2);
				{
				setState(1330);
				defined_constant();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Untyped_atomContext extends ParserRuleContext {
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public System_constantContext system_constant() {
			return getRuleContext(System_constantContext.class,0);
		}
		public Untyped_atomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_untyped_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterUntyped_atom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitUntyped_atom(this);
		}
	}

	public final Untyped_atomContext untyped_atom() throws RecognitionException {
		Untyped_atomContext _localctx = new Untyped_atomContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_untyped_atom);
		try {
			setState(1335);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1333);
				constant();
				}
				break;
			case Dollar_dollar_word:
				enterOuterAlt(_localctx, 2);
				{
				setState(1334);
				system_constant();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_propositionContext extends ParserRuleContext {
		public TerminalNode Dollar_word() { return getToken(tptp_v7_0_0_0Parser.Dollar_word, 0); }
		public Defined_propositionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_proposition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_proposition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_proposition(this);
		}
	}

	public final Defined_propositionContext defined_proposition() throws RecognitionException {
		Defined_propositionContext _localctx = new Defined_propositionContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_defined_proposition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1337);
			match(Dollar_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_predicateContext extends ParserRuleContext {
		public TerminalNode Dollar_word() { return getToken(tptp_v7_0_0_0Parser.Dollar_word, 0); }
		public Defined_predicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_predicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_predicate(this);
		}
	}

	public final Defined_predicateContext defined_predicate() throws RecognitionException {
		Defined_predicateContext _localctx = new Defined_predicateContext(_ctx, getState());
		enterRule(_localctx, 284, RULE_defined_predicate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1339);
			match(Dollar_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_infix_predContext extends ParserRuleContext {
		public TerminalNode Infix_equality() { return getToken(tptp_v7_0_0_0Parser.Infix_equality, 0); }
		public TerminalNode Assignment() { return getToken(tptp_v7_0_0_0Parser.Assignment, 0); }
		public Defined_infix_predContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_infix_pred; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_infix_pred(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_infix_pred(this);
		}
	}

	public final Defined_infix_predContext defined_infix_pred() throws RecognitionException {
		Defined_infix_predContext _localctx = new Defined_infix_predContext(_ctx, getState());
		enterRule(_localctx, 286, RULE_defined_infix_pred);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1341);
			_la = _input.LA(1);
			if ( !(_la==Infix_equality || _la==Assignment) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstantContext extends ParserRuleContext {
		public FunctorContext functor() {
			return getRuleContext(FunctorContext.class,0);
		}
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitConstant(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 288, RULE_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1343);
			functor();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunctorContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public FunctorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFunctor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFunctor(this);
		}
	}

	public final FunctorContext functor() throws RecognitionException {
		FunctorContext _localctx = new FunctorContext(_ctx, getState());
		enterRule(_localctx, 290, RULE_functor);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1345);
			atomic_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class System_constantContext extends ParserRuleContext {
		public System_functorContext system_functor() {
			return getRuleContext(System_functorContext.class,0);
		}
		public System_constantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_constant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterSystem_constant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitSystem_constant(this);
		}
	}

	public final System_constantContext system_constant() throws RecognitionException {
		System_constantContext _localctx = new System_constantContext(_ctx, getState());
		enterRule(_localctx, 292, RULE_system_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1347);
			system_functor();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class System_functorContext extends ParserRuleContext {
		public Atomic_system_wordContext atomic_system_word() {
			return getRuleContext(Atomic_system_wordContext.class,0);
		}
		public System_functorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_system_functor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterSystem_functor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitSystem_functor(this);
		}
	}

	public final System_functorContext system_functor() throws RecognitionException {
		System_functorContext _localctx = new System_functorContext(_ctx, getState());
		enterRule(_localctx, 294, RULE_system_functor);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1349);
			atomic_system_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_constantContext extends ParserRuleContext {
		public Defined_functorContext defined_functor() {
			return getRuleContext(Defined_functorContext.class,0);
		}
		public Defined_constantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_constant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_constant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_constant(this);
		}
	}

	public final Defined_constantContext defined_constant() throws RecognitionException {
		Defined_constantContext _localctx = new Defined_constantContext(_ctx, getState());
		enterRule(_localctx, 296, RULE_defined_constant);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1351);
			defined_functor();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_functorContext extends ParserRuleContext {
		public Atomic_defined_wordContext atomic_defined_word() {
			return getRuleContext(Atomic_defined_wordContext.class,0);
		}
		public Defined_functorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_functor; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_functor(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_functor(this);
		}
	}

	public final Defined_functorContext defined_functor() throws RecognitionException {
		Defined_functorContext _localctx = new Defined_functorContext(_ctx, getState());
		enterRule(_localctx, 298, RULE_defined_functor);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1353);
			atomic_defined_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Defined_termContext extends ParserRuleContext {
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public TerminalNode Distinct_object() { return getToken(tptp_v7_0_0_0Parser.Distinct_object, 0); }
		public Defined_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defined_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDefined_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDefined_term(this);
		}
	}

	public final Defined_termContext defined_term() throws RecognitionException {
		Defined_termContext _localctx = new Defined_termContext(_ctx, getState());
		enterRule(_localctx, 300, RULE_defined_term);
		try {
			setState(1357);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Real:
			case Rational:
			case Integer:
				enterOuterAlt(_localctx, 1);
				{
				setState(1355);
				number();
				}
				break;
			case Distinct_object:
				enterOuterAlt(_localctx, 2);
				{
				setState(1356);
				match(Distinct_object);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableContext extends ParserRuleContext {
		public TerminalNode Upper_word() { return getToken(tptp_v7_0_0_0Parser.Upper_word, 0); }
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitVariable(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 302, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1359);
			match(Upper_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SourceContext extends ParserRuleContext {
		public Dag_sourceContext dag_source() {
			return getRuleContext(Dag_sourceContext.class,0);
		}
		public Internal_sourceContext internal_source() {
			return getRuleContext(Internal_sourceContext.class,0);
		}
		public External_sourceContext external_source() {
			return getRuleContext(External_sourceContext.class,0);
		}
		public TerminalNode Lower_word() { return getToken(tptp_v7_0_0_0Parser.Lower_word, 0); }
		public SourcesContext sources() {
			return getRuleContext(SourcesContext.class,0);
		}
		public SourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterSource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitSource(this);
		}
	}

	public final SourceContext source() throws RecognitionException {
		SourceContext _localctx = new SourceContext(_ctx, getState());
		enterRule(_localctx, 304, RULE_source);
		try {
			setState(1369);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,98,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1361);
				dag_source();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1362);
				internal_source();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1363);
				external_source();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1364);
				match(Lower_word);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1365);
				match(T__11);
				setState(1366);
				sources();
				setState(1367);
				match(T__12);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SourcesContext extends ParserRuleContext {
		public List<SourceContext> source() {
			return getRuleContexts(SourceContext.class);
		}
		public SourceContext source(int i) {
			return getRuleContext(SourceContext.class,i);
		}
		public SourcesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sources; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterSources(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitSources(this);
		}
	}

	public final SourcesContext sources() throws RecognitionException {
		SourcesContext _localctx = new SourcesContext(_ctx, getState());
		enterRule(_localctx, 306, RULE_sources);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1371);
			source();
			setState(1376);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1372);
				match(T__1);
				setState(1373);
				source();
				}
				}
				setState(1378);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Dag_sourceContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public Inference_recordContext inference_record() {
			return getRuleContext(Inference_recordContext.class,0);
		}
		public Dag_sourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dag_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDag_source(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDag_source(this);
		}
	}

	public final Dag_sourceContext dag_source() throws RecognitionException {
		Dag_sourceContext _localctx = new Dag_sourceContext(_ctx, getState());
		enterRule(_localctx, 308, RULE_dag_source);
		try {
			setState(1381);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Integer:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1379);
				name();
				}
				break;
			case T__26:
				enterOuterAlt(_localctx, 2);
				{
				setState(1380);
				inference_record();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inference_recordContext extends ParserRuleContext {
		public Inference_ruleContext inference_rule() {
			return getRuleContext(Inference_ruleContext.class,0);
		}
		public Useful_infoContext useful_info() {
			return getRuleContext(Useful_infoContext.class,0);
		}
		public Inference_parentsContext inference_parents() {
			return getRuleContext(Inference_parentsContext.class,0);
		}
		public Inference_recordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inference_record; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInference_record(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInference_record(this);
		}
	}

	public final Inference_recordContext inference_record() throws RecognitionException {
		Inference_recordContext _localctx = new Inference_recordContext(_ctx, getState());
		enterRule(_localctx, 310, RULE_inference_record);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1383);
			match(T__26);
			setState(1384);
			inference_rule();
			setState(1385);
			match(T__1);
			setState(1386);
			useful_info();
			setState(1387);
			match(T__1);
			setState(1388);
			inference_parents();
			setState(1389);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inference_ruleContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public Inference_ruleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inference_rule; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInference_rule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInference_rule(this);
		}
	}

	public final Inference_ruleContext inference_rule() throws RecognitionException {
		Inference_ruleContext _localctx = new Inference_ruleContext(_ctx, getState());
		enterRule(_localctx, 312, RULE_inference_rule);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1391);
			atomic_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inference_parentsContext extends ParserRuleContext {
		public Parent_listContext parent_list() {
			return getRuleContext(Parent_listContext.class,0);
		}
		public Inference_parentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inference_parents; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInference_parents(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInference_parents(this);
		}
	}

	public final Inference_parentsContext inference_parents() throws RecognitionException {
		Inference_parentsContext _localctx = new Inference_parentsContext(_ctx, getState());
		enterRule(_localctx, 314, RULE_inference_parents);
		try {
			setState(1398);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(1393);
				match(T__16);
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(1394);
				match(T__11);
				setState(1395);
				parent_list();
				setState(1396);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Parent_listContext extends ParserRuleContext {
		public List<Parent_infoContext> parent_info() {
			return getRuleContexts(Parent_infoContext.class);
		}
		public Parent_infoContext parent_info(int i) {
			return getRuleContext(Parent_infoContext.class,i);
		}
		public Parent_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parent_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterParent_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitParent_list(this);
		}
	}

	public final Parent_listContext parent_list() throws RecognitionException {
		Parent_listContext _localctx = new Parent_listContext(_ctx, getState());
		enterRule(_localctx, 316, RULE_parent_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1400);
			parent_info();
			setState(1405);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1401);
				match(T__1);
				setState(1402);
				parent_info();
				}
				}
				setState(1407);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Parent_infoContext extends ParserRuleContext {
		public SourceContext source() {
			return getRuleContext(SourceContext.class,0);
		}
		public Parent_detailsContext parent_details() {
			return getRuleContext(Parent_detailsContext.class,0);
		}
		public Parent_infoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parent_info; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterParent_info(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitParent_info(this);
		}
	}

	public final Parent_infoContext parent_info() throws RecognitionException {
		Parent_infoContext _localctx = new Parent_infoContext(_ctx, getState());
		enterRule(_localctx, 318, RULE_parent_info);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1408);
			source();
			setState(1410);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__13) {
				{
				setState(1409);
				parent_details();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Parent_detailsContext extends ParserRuleContext {
		public General_listContext general_list() {
			return getRuleContext(General_listContext.class,0);
		}
		public Parent_detailsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parent_details; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterParent_details(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitParent_details(this);
		}
	}

	public final Parent_detailsContext parent_details() throws RecognitionException {
		Parent_detailsContext _localctx = new Parent_detailsContext(_ctx, getState());
		enterRule(_localctx, 320, RULE_parent_details);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1412);
			match(T__13);
			setState(1413);
			general_list();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Internal_sourceContext extends ParserRuleContext {
		public Intro_typeContext intro_type() {
			return getRuleContext(Intro_typeContext.class,0);
		}
		public Optional_infoContext optional_info() {
			return getRuleContext(Optional_infoContext.class,0);
		}
		public Internal_sourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_internal_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInternal_source(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInternal_source(this);
		}
	}

	public final Internal_sourceContext internal_source() throws RecognitionException {
		Internal_sourceContext _localctx = new Internal_sourceContext(_ctx, getState());
		enterRule(_localctx, 322, RULE_internal_source);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1415);
			match(T__27);
			setState(1416);
			intro_type();
			setState(1418);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(1417);
				optional_info();
				}
			}

			setState(1420);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Intro_typeContext extends ParserRuleContext {
		public TerminalNode Lower_word() { return getToken(tptp_v7_0_0_0Parser.Lower_word, 0); }
		public Intro_typeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intro_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterIntro_type(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitIntro_type(this);
		}
	}

	public final Intro_typeContext intro_type() throws RecognitionException {
		Intro_typeContext _localctx = new Intro_typeContext(_ctx, getState());
		enterRule(_localctx, 324, RULE_intro_type);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1422);
			match(Lower_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class External_sourceContext extends ParserRuleContext {
		public File_sourceContext file_source() {
			return getRuleContext(File_sourceContext.class,0);
		}
		public TheoryContext theory() {
			return getRuleContext(TheoryContext.class,0);
		}
		public Creator_sourceContext creator_source() {
			return getRuleContext(Creator_sourceContext.class,0);
		}
		public External_sourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_external_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterExternal_source(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitExternal_source(this);
		}
	}

	public final External_sourceContext external_source() throws RecognitionException {
		External_sourceContext _localctx = new External_sourceContext(_ctx, getState());
		enterRule(_localctx, 326, RULE_external_source);
		try {
			setState(1427);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__28:
				enterOuterAlt(_localctx, 1);
				{
				setState(1424);
				file_source();
				}
				break;
			case T__29:
				enterOuterAlt(_localctx, 2);
				{
				setState(1425);
				theory();
				}
				break;
			case T__30:
				enterOuterAlt(_localctx, 3);
				{
				setState(1426);
				creator_source();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class File_sourceContext extends ParserRuleContext {
		public File_nameContext file_name() {
			return getRuleContext(File_nameContext.class,0);
		}
		public File_infoContext file_info() {
			return getRuleContext(File_infoContext.class,0);
		}
		public File_sourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_file_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFile_source(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFile_source(this);
		}
	}

	public final File_sourceContext file_source() throws RecognitionException {
		File_sourceContext _localctx = new File_sourceContext(_ctx, getState());
		enterRule(_localctx, 328, RULE_file_source);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1429);
			match(T__28);
			setState(1430);
			file_name();
			setState(1432);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(1431);
				file_info();
				}
			}

			setState(1434);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class File_infoContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public File_infoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_file_info; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFile_info(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFile_info(this);
		}
	}

	public final File_infoContext file_info() throws RecognitionException {
		File_infoContext _localctx = new File_infoContext(_ctx, getState());
		enterRule(_localctx, 330, RULE_file_info);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1436);
			match(T__1);
			setState(1437);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TheoryContext extends ParserRuleContext {
		public Theory_nameContext theory_name() {
			return getRuleContext(Theory_nameContext.class,0);
		}
		public Optional_infoContext optional_info() {
			return getRuleContext(Optional_infoContext.class,0);
		}
		public TheoryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_theory; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTheory(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTheory(this);
		}
	}

	public final TheoryContext theory() throws RecognitionException {
		TheoryContext _localctx = new TheoryContext(_ctx, getState());
		enterRule(_localctx, 332, RULE_theory);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1439);
			match(T__29);
			setState(1440);
			theory_name();
			setState(1442);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(1441);
				optional_info();
				}
			}

			setState(1444);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Theory_nameContext extends ParserRuleContext {
		public TerminalNode Lower_word() { return getToken(tptp_v7_0_0_0Parser.Lower_word, 0); }
		public Theory_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_theory_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterTheory_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitTheory_name(this);
		}
	}

	public final Theory_nameContext theory_name() throws RecognitionException {
		Theory_nameContext _localctx = new Theory_nameContext(_ctx, getState());
		enterRule(_localctx, 334, RULE_theory_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1446);
			match(Lower_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Creator_sourceContext extends ParserRuleContext {
		public Creator_nameContext creator_name() {
			return getRuleContext(Creator_nameContext.class,0);
		}
		public Optional_infoContext optional_info() {
			return getRuleContext(Optional_infoContext.class,0);
		}
		public Creator_sourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_creator_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterCreator_source(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitCreator_source(this);
		}
	}

	public final Creator_sourceContext creator_source() throws RecognitionException {
		Creator_sourceContext _localctx = new Creator_sourceContext(_ctx, getState());
		enterRule(_localctx, 336, RULE_creator_source);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1448);
			match(T__30);
			setState(1449);
			creator_name();
			setState(1451);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(1450);
				optional_info();
				}
			}

			setState(1453);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Creator_nameContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public Creator_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_creator_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterCreator_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitCreator_name(this);
		}
	}

	public final Creator_nameContext creator_name() throws RecognitionException {
		Creator_nameContext _localctx = new Creator_nameContext(_ctx, getState());
		enterRule(_localctx, 338, RULE_creator_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1455);
			atomic_word();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Optional_infoContext extends ParserRuleContext {
		public Useful_infoContext useful_info() {
			return getRuleContext(Useful_infoContext.class,0);
		}
		public Optional_infoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optional_info; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterOptional_info(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitOptional_info(this);
		}
	}

	public final Optional_infoContext optional_info() throws RecognitionException {
		Optional_infoContext _localctx = new Optional_infoContext(_ctx, getState());
		enterRule(_localctx, 340, RULE_optional_info);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1457);
			match(T__1);
			setState(1458);
			useful_info();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Useful_infoContext extends ParserRuleContext {
		public Info_itemsContext info_items() {
			return getRuleContext(Info_itemsContext.class,0);
		}
		public General_listContext general_list() {
			return getRuleContext(General_listContext.class,0);
		}
		public Useful_infoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_useful_info; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterUseful_info(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitUseful_info(this);
		}
	}

	public final Useful_infoContext useful_info() throws RecognitionException {
		Useful_infoContext _localctx = new Useful_infoContext(_ctx, getState());
		enterRule(_localctx, 342, RULE_useful_info);
		try {
			setState(1466);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1460);
				match(T__16);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1461);
				match(T__11);
				setState(1462);
				info_items();
				setState(1463);
				match(T__12);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1465);
				general_list();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Info_itemsContext extends ParserRuleContext {
		public List<Info_itemContext> info_item() {
			return getRuleContexts(Info_itemContext.class);
		}
		public Info_itemContext info_item(int i) {
			return getRuleContext(Info_itemContext.class,i);
		}
		public Info_itemsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_info_items; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInfo_items(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInfo_items(this);
		}
	}

	public final Info_itemsContext info_items() throws RecognitionException {
		Info_itemsContext _localctx = new Info_itemsContext(_ctx, getState());
		enterRule(_localctx, 344, RULE_info_items);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1468);
			info_item();
			setState(1473);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1469);
				match(T__1);
				setState(1470);
				info_item();
				}
				}
				setState(1475);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Info_itemContext extends ParserRuleContext {
		public Formula_itemContext formula_item() {
			return getRuleContext(Formula_itemContext.class,0);
		}
		public Inference_itemContext inference_item() {
			return getRuleContext(Inference_itemContext.class,0);
		}
		public General_functionContext general_function() {
			return getRuleContext(General_functionContext.class,0);
		}
		public Info_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_info_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInfo_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInfo_item(this);
		}
	}

	public final Info_itemContext info_item() throws RecognitionException {
		Info_itemContext _localctx = new Info_itemContext(_ctx, getState());
		enterRule(_localctx, 346, RULE_info_item);
		try {
			setState(1479);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,111,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1476);
				formula_item();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1477);
				inference_item();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1478);
				general_function();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Formula_itemContext extends ParserRuleContext {
		public Description_itemContext description_item() {
			return getRuleContext(Description_itemContext.class,0);
		}
		public Iquote_itemContext iquote_item() {
			return getRuleContext(Iquote_itemContext.class,0);
		}
		public Formula_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formula_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFormula_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFormula_item(this);
		}
	}

	public final Formula_itemContext formula_item() throws RecognitionException {
		Formula_itemContext _localctx = new Formula_itemContext(_ctx, getState());
		enterRule(_localctx, 348, RULE_formula_item);
		try {
			setState(1483);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__31:
				enterOuterAlt(_localctx, 1);
				{
				setState(1481);
				description_item();
				}
				break;
			case T__32:
				enterOuterAlt(_localctx, 2);
				{
				setState(1482);
				iquote_item();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Description_itemContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public Description_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_description_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterDescription_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitDescription_item(this);
		}
	}

	public final Description_itemContext description_item() throws RecognitionException {
		Description_itemContext _localctx = new Description_itemContext(_ctx, getState());
		enterRule(_localctx, 350, RULE_description_item);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1485);
			match(T__31);
			setState(1486);
			atomic_word();
			setState(1487);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Iquote_itemContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public Iquote_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_iquote_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterIquote_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitIquote_item(this);
		}
	}

	public final Iquote_itemContext iquote_item() throws RecognitionException {
		Iquote_itemContext _localctx = new Iquote_itemContext(_ctx, getState());
		enterRule(_localctx, 352, RULE_iquote_item);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1489);
			match(T__32);
			setState(1490);
			atomic_word();
			setState(1491);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inference_itemContext extends ParserRuleContext {
		public Inference_statusContext inference_status() {
			return getRuleContext(Inference_statusContext.class,0);
		}
		public Assumptions_recordContext assumptions_record() {
			return getRuleContext(Assumptions_recordContext.class,0);
		}
		public New_symbol_recordContext new_symbol_record() {
			return getRuleContext(New_symbol_recordContext.class,0);
		}
		public RefutationContext refutation() {
			return getRuleContext(RefutationContext.class,0);
		}
		public Inference_itemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inference_item; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInference_item(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInference_item(this);
		}
	}

	public final Inference_itemContext inference_item() throws RecognitionException {
		Inference_itemContext _localctx = new Inference_itemContext(_ctx, getState());
		enterRule(_localctx, 354, RULE_inference_item);
		try {
			setState(1497);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__33:
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1493);
				inference_status();
				}
				break;
			case T__34:
				enterOuterAlt(_localctx, 2);
				{
				setState(1494);
				assumptions_record();
				}
				break;
			case T__36:
				enterOuterAlt(_localctx, 3);
				{
				setState(1495);
				new_symbol_record();
				}
				break;
			case T__35:
				enterOuterAlt(_localctx, 4);
				{
				setState(1496);
				refutation();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inference_statusContext extends ParserRuleContext {
		public Status_valueContext status_value() {
			return getRuleContext(Status_valueContext.class,0);
		}
		public Inference_infoContext inference_info() {
			return getRuleContext(Inference_infoContext.class,0);
		}
		public Inference_statusContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inference_status; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInference_status(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInference_status(this);
		}
	}

	public final Inference_statusContext inference_status() throws RecognitionException {
		Inference_statusContext _localctx = new Inference_statusContext(_ctx, getState());
		enterRule(_localctx, 356, RULE_inference_status);
		try {
			setState(1504);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__33:
				enterOuterAlt(_localctx, 1);
				{
				setState(1499);
				match(T__33);
				setState(1500);
				status_value();
				setState(1501);
				match(T__10);
				}
				break;
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 2);
				{
				setState(1503);
				inference_info();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Status_valueContext extends ParserRuleContext {
		public TerminalNode Lower_word() { return getToken(tptp_v7_0_0_0Parser.Lower_word, 0); }
		public Status_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_status_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterStatus_value(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitStatus_value(this);
		}
	}

	public final Status_valueContext status_value() throws RecognitionException {
		Status_valueContext _localctx = new Status_valueContext(_ctx, getState());
		enterRule(_localctx, 358, RULE_status_value);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1506);
			match(Lower_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Inference_infoContext extends ParserRuleContext {
		public Inference_ruleContext inference_rule() {
			return getRuleContext(Inference_ruleContext.class,0);
		}
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public General_listContext general_list() {
			return getRuleContext(General_listContext.class,0);
		}
		public Inference_infoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inference_info; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInference_info(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInference_info(this);
		}
	}

	public final Inference_infoContext inference_info() throws RecognitionException {
		Inference_infoContext _localctx = new Inference_infoContext(_ctx, getState());
		enterRule(_localctx, 360, RULE_inference_info);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1508);
			inference_rule();
			setState(1509);
			match(T__9);
			setState(1510);
			atomic_word();
			setState(1511);
			match(T__1);
			setState(1512);
			general_list();
			setState(1513);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Assumptions_recordContext extends ParserRuleContext {
		public Name_listContext name_list() {
			return getRuleContext(Name_listContext.class,0);
		}
		public Assumptions_recordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assumptions_record; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAssumptions_record(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAssumptions_record(this);
		}
	}

	public final Assumptions_recordContext assumptions_record() throws RecognitionException {
		Assumptions_recordContext _localctx = new Assumptions_recordContext(_ctx, getState());
		enterRule(_localctx, 362, RULE_assumptions_record);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1515);
			match(T__34);
			setState(1516);
			match(T__11);
			setState(1517);
			name_list();
			setState(1518);
			match(T__12);
			setState(1519);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RefutationContext extends ParserRuleContext {
		public File_sourceContext file_source() {
			return getRuleContext(File_sourceContext.class,0);
		}
		public RefutationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_refutation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterRefutation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitRefutation(this);
		}
	}

	public final RefutationContext refutation() throws RecognitionException {
		RefutationContext _localctx = new RefutationContext(_ctx, getState());
		enterRule(_localctx, 364, RULE_refutation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1521);
			match(T__35);
			setState(1522);
			file_source();
			setState(1523);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class New_symbol_recordContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public New_symbol_listContext new_symbol_list() {
			return getRuleContext(New_symbol_listContext.class,0);
		}
		public New_symbol_recordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_new_symbol_record; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterNew_symbol_record(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitNew_symbol_record(this);
		}
	}

	public final New_symbol_recordContext new_symbol_record() throws RecognitionException {
		New_symbol_recordContext _localctx = new New_symbol_recordContext(_ctx, getState());
		enterRule(_localctx, 366, RULE_new_symbol_record);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1525);
			match(T__36);
			setState(1526);
			atomic_word();
			setState(1527);
			match(T__1);
			setState(1528);
			match(T__11);
			setState(1529);
			new_symbol_list();
			setState(1530);
			match(T__12);
			setState(1531);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class New_symbol_listContext extends ParserRuleContext {
		public List<Principal_symbolContext> principal_symbol() {
			return getRuleContexts(Principal_symbolContext.class);
		}
		public Principal_symbolContext principal_symbol(int i) {
			return getRuleContext(Principal_symbolContext.class,i);
		}
		public New_symbol_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_new_symbol_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterNew_symbol_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitNew_symbol_list(this);
		}
	}

	public final New_symbol_listContext new_symbol_list() throws RecognitionException {
		New_symbol_listContext _localctx = new New_symbol_listContext(_ctx, getState());
		enterRule(_localctx, 368, RULE_new_symbol_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1533);
			principal_symbol();
			setState(1538);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1534);
				match(T__1);
				setState(1535);
				principal_symbol();
				}
				}
				setState(1540);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Principal_symbolContext extends ParserRuleContext {
		public FunctorContext functor() {
			return getRuleContext(FunctorContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public Principal_symbolContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_principal_symbol; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterPrincipal_symbol(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitPrincipal_symbol(this);
		}
	}

	public final Principal_symbolContext principal_symbol() throws RecognitionException {
		Principal_symbolContext _localctx = new Principal_symbolContext(_ctx, getState());
		enterRule(_localctx, 370, RULE_principal_symbol);
		try {
			setState(1543);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1541);
				functor();
				}
				break;
			case Upper_word:
				enterOuterAlt(_localctx, 2);
				{
				setState(1542);
				variable();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IncludeContext extends ParserRuleContext {
		public File_nameContext file_name() {
			return getRuleContext(File_nameContext.class,0);
		}
		public Formula_selectionContext formula_selection() {
			return getRuleContext(Formula_selectionContext.class,0);
		}
		public IncludeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_include; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterInclude(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitInclude(this);
		}
	}

	public final IncludeContext include() throws RecognitionException {
		IncludeContext _localctx = new IncludeContext(_ctx, getState());
		enterRule(_localctx, 372, RULE_include);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1545);
			match(T__37);
			setState(1546);
			file_name();
			setState(1548);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(1547);
				formula_selection();
				}
			}

			setState(1550);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Formula_selectionContext extends ParserRuleContext {
		public Name_listContext name_list() {
			return getRuleContext(Name_listContext.class,0);
		}
		public Formula_selectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formula_selection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFormula_selection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFormula_selection(this);
		}
	}

	public final Formula_selectionContext formula_selection() throws RecognitionException {
		Formula_selectionContext _localctx = new Formula_selectionContext(_ctx, getState());
		enterRule(_localctx, 374, RULE_formula_selection);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1552);
			match(T__1);
			setState(1553);
			match(T__11);
			setState(1554);
			name_list();
			setState(1555);
			match(T__12);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Name_listContext extends ParserRuleContext {
		public List<NameContext> name() {
			return getRuleContexts(NameContext.class);
		}
		public NameContext name(int i) {
			return getRuleContext(NameContext.class,i);
		}
		public Name_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterName_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitName_list(this);
		}
	}

	public final Name_listContext name_list() throws RecognitionException {
		Name_listContext _localctx = new Name_listContext(_ctx, getState());
		enterRule(_localctx, 376, RULE_name_list);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1557);
			name();
			setState(1562);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1558);
				match(T__1);
				setState(1559);
				name();
				}
				}
				setState(1564);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class General_termContext extends ParserRuleContext {
		public General_dataContext general_data() {
			return getRuleContext(General_dataContext.class,0);
		}
		public General_termContext general_term() {
			return getRuleContext(General_termContext.class,0);
		}
		public General_listContext general_list() {
			return getRuleContext(General_listContext.class,0);
		}
		public General_termContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_general_term; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterGeneral_term(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitGeneral_term(this);
		}
	}

	public final General_termContext general_term() throws RecognitionException {
		General_termContext _localctx = new General_termContext(_ctx, getState());
		enterRule(_localctx, 378, RULE_general_term);
		try {
			setState(1571);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,119,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1565);
				general_data();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1566);
				general_data();
				setState(1567);
				match(T__13);
				setState(1568);
				general_term();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1570);
				general_list();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class General_dataContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public General_functionContext general_function() {
			return getRuleContext(General_functionContext.class,0);
		}
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public NumberContext number() {
			return getRuleContext(NumberContext.class,0);
		}
		public TerminalNode Distinct_object() { return getToken(tptp_v7_0_0_0Parser.Distinct_object, 0); }
		public Formula_dataContext formula_data() {
			return getRuleContext(Formula_dataContext.class,0);
		}
		public General_dataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_general_data; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterGeneral_data(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitGeneral_data(this);
		}
	}

	public final General_dataContext general_data() throws RecognitionException {
		General_dataContext _localctx = new General_dataContext(_ctx, getState());
		enterRule(_localctx, 380, RULE_general_data);
		try {
			setState(1579);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,120,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1573);
				atomic_word();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1574);
				general_function();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1575);
				variable();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1576);
				number();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1577);
				match(Distinct_object);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1578);
				formula_data();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class General_functionContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public General_termsContext general_terms() {
			return getRuleContext(General_termsContext.class,0);
		}
		public General_functionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_general_function; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterGeneral_function(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitGeneral_function(this);
		}
	}

	public final General_functionContext general_function() throws RecognitionException {
		General_functionContext _localctx = new General_functionContext(_ctx, getState());
		enterRule(_localctx, 382, RULE_general_function);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1581);
			atomic_word();
			setState(1582);
			match(T__9);
			setState(1583);
			general_terms();
			setState(1584);
			match(T__10);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Formula_dataContext extends ParserRuleContext {
		public Thf_formulaContext thf_formula() {
			return getRuleContext(Thf_formulaContext.class,0);
		}
		public Tff_formulaContext tff_formula() {
			return getRuleContext(Tff_formulaContext.class,0);
		}
		public Fof_formulaContext fof_formula() {
			return getRuleContext(Fof_formulaContext.class,0);
		}
		public Cnf_formulaContext cnf_formula() {
			return getRuleContext(Cnf_formulaContext.class,0);
		}
		public Fof_termContext fof_term() {
			return getRuleContext(Fof_termContext.class,0);
		}
		public Formula_dataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formula_data; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFormula_data(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFormula_data(this);
		}
	}

	public final Formula_dataContext formula_data() throws RecognitionException {
		Formula_dataContext _localctx = new Formula_dataContext(_ctx, getState());
		enterRule(_localctx, 384, RULE_formula_data);
		try {
			setState(1606);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__38:
				enterOuterAlt(_localctx, 1);
				{
				setState(1586);
				match(T__38);
				setState(1587);
				thf_formula();
				setState(1588);
				match(T__10);
				}
				break;
			case T__39:
				enterOuterAlt(_localctx, 2);
				{
				setState(1590);
				match(T__39);
				setState(1591);
				tff_formula();
				setState(1592);
				match(T__10);
				}
				break;
			case T__40:
				enterOuterAlt(_localctx, 3);
				{
				setState(1594);
				match(T__40);
				setState(1595);
				fof_formula();
				setState(1596);
				match(T__10);
				}
				break;
			case T__41:
				enterOuterAlt(_localctx, 4);
				{
				setState(1598);
				match(T__41);
				setState(1599);
				cnf_formula();
				setState(1600);
				match(T__10);
				}
				break;
			case T__42:
				enterOuterAlt(_localctx, 5);
				{
				setState(1602);
				match(T__42);
				setState(1603);
				fof_term();
				setState(1604);
				match(T__10);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class General_listContext extends ParserRuleContext {
		public General_termsContext general_terms() {
			return getRuleContext(General_termsContext.class,0);
		}
		public General_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_general_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterGeneral_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitGeneral_list(this);
		}
	}

	public final General_listContext general_list() throws RecognitionException {
		General_listContext _localctx = new General_listContext(_ctx, getState());
		enterRule(_localctx, 386, RULE_general_list);
		try {
			setState(1613);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__16:
				enterOuterAlt(_localctx, 1);
				{
				setState(1608);
				match(T__16);
				}
				break;
			case T__11:
				enterOuterAlt(_localctx, 2);
				{
				setState(1609);
				match(T__11);
				setState(1610);
				general_terms();
				setState(1611);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class General_termsContext extends ParserRuleContext {
		public List<General_termContext> general_term() {
			return getRuleContexts(General_termContext.class);
		}
		public General_termContext general_term(int i) {
			return getRuleContext(General_termContext.class,i);
		}
		public General_termsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_general_terms; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterGeneral_terms(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitGeneral_terms(this);
		}
	}

	public final General_termsContext general_terms() throws RecognitionException {
		General_termsContext _localctx = new General_termsContext(_ctx, getState());
		enterRule(_localctx, 388, RULE_general_terms);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1615);
			general_term();
			setState(1620);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__1) {
				{
				{
				setState(1616);
				match(T__1);
				setState(1617);
				general_term();
				}
				}
				setState(1622);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameContext extends ParserRuleContext {
		public Atomic_wordContext atomic_word() {
			return getRuleContext(Atomic_wordContext.class,0);
		}
		public TerminalNode Integer() { return getToken(tptp_v7_0_0_0Parser.Integer, 0); }
		public NameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitName(this);
		}
	}

	public final NameContext name() throws RecognitionException {
		NameContext _localctx = new NameContext(_ctx, getState());
		enterRule(_localctx, 390, RULE_name);
		try {
			setState(1625);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Lower_word:
			case Single_quoted:
				enterOuterAlt(_localctx, 1);
				{
				setState(1623);
				atomic_word();
				}
				break;
			case Integer:
				enterOuterAlt(_localctx, 2);
				{
				setState(1624);
				match(Integer);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Atomic_wordContext extends ParserRuleContext {
		public TerminalNode Lower_word() { return getToken(tptp_v7_0_0_0Parser.Lower_word, 0); }
		public TerminalNode Single_quoted() { return getToken(tptp_v7_0_0_0Parser.Single_quoted, 0); }
		public Atomic_wordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atomic_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAtomic_word(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAtomic_word(this);
		}
	}

	public final Atomic_wordContext atomic_word() throws RecognitionException {
		Atomic_wordContext _localctx = new Atomic_wordContext(_ctx, getState());
		enterRule(_localctx, 392, RULE_atomic_word);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1627);
			_la = _input.LA(1);
			if ( !(_la==Lower_word || _la==Single_quoted) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Atomic_defined_wordContext extends ParserRuleContext {
		public TerminalNode Dollar_word() { return getToken(tptp_v7_0_0_0Parser.Dollar_word, 0); }
		public Atomic_defined_wordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atomic_defined_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAtomic_defined_word(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAtomic_defined_word(this);
		}
	}

	public final Atomic_defined_wordContext atomic_defined_word() throws RecognitionException {
		Atomic_defined_wordContext _localctx = new Atomic_defined_wordContext(_ctx, getState());
		enterRule(_localctx, 394, RULE_atomic_defined_word);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1629);
			match(Dollar_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Atomic_system_wordContext extends ParserRuleContext {
		public TerminalNode Dollar_dollar_word() { return getToken(tptp_v7_0_0_0Parser.Dollar_dollar_word, 0); }
		public Atomic_system_wordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atomic_system_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterAtomic_system_word(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitAtomic_system_word(this);
		}
	}

	public final Atomic_system_wordContext atomic_system_word() throws RecognitionException {
		Atomic_system_wordContext _localctx = new Atomic_system_wordContext(_ctx, getState());
		enterRule(_localctx, 396, RULE_atomic_system_word);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1631);
			match(Dollar_dollar_word);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NumberContext extends ParserRuleContext {
		public TerminalNode Integer() { return getToken(tptp_v7_0_0_0Parser.Integer, 0); }
		public TerminalNode Rational() { return getToken(tptp_v7_0_0_0Parser.Rational, 0); }
		public TerminalNode Real() { return getToken(tptp_v7_0_0_0Parser.Real, 0); }
		public NumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_number; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitNumber(this);
		}
	}

	public final NumberContext number() throws RecognitionException {
		NumberContext _localctx = new NumberContext(_ctx, getState());
		enterRule(_localctx, 398, RULE_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1633);
			_la = _input.LA(1);
			if ( !(((((_la - 74)) & ~0x3f) == 0 && ((1L << (_la - 74)) & ((1L << (Real - 74)) | (1L << (Rational - 74)) | (1L << (Integer - 74)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class File_nameContext extends ParserRuleContext {
		public TerminalNode Single_quoted() { return getToken(tptp_v7_0_0_0Parser.Single_quoted, 0); }
		public File_nameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_file_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).enterFile_name(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof tptp_v7_0_0_0Listener ) ((tptp_v7_0_0_0Listener)listener).exitFile_name(this);
		}
	}

	public final File_nameContext file_name() throws RecognitionException {
		File_nameContext _localctx = new File_nameContext(_ctx, getState());
		enterRule(_localctx, 400, RULE_file_name);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1635);
			match(Single_quoted);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 18:
			return thf_or_formula_sempred((Thf_or_formulaContext)_localctx, predIndex);
		case 19:
			return thf_and_formula_sempred((Thf_and_formulaContext)_localctx, predIndex);
		case 20:
			return thf_apply_formula_sempred((Thf_apply_formulaContext)_localctx, predIndex);
		case 42:
			return thf_xprod_type_sempred((Thf_xprod_typeContext)_localctx, predIndex);
		case 43:
			return thf_union_type_sempred((Thf_union_typeContext)_localctx, predIndex);
		case 54:
			return tff_or_formula_sempred((Tff_or_formulaContext)_localctx, predIndex);
		case 55:
			return tff_and_formula_sempred((Tff_and_formulaContext)_localctx, predIndex);
		case 85:
			return tff_xprod_type_sempred((Tff_xprod_typeContext)_localctx, predIndex);
		case 94:
			return fof_or_formula_sempred((Fof_or_formulaContext)_localctx, predIndex);
		case 95:
			return fof_and_formula_sempred((Fof_and_formulaContext)_localctx, predIndex);
		case 122:
			return cnf_disjunction_sempred((Cnf_disjunctionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean thf_or_formula_sempred(Thf_or_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean thf_and_formula_sempred(Thf_and_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean thf_apply_formula_sempred(Thf_apply_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean thf_xprod_type_sempred(Thf_xprod_typeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean thf_union_type_sempred(Thf_union_typeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean tff_or_formula_sempred(Tff_or_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 5:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean tff_and_formula_sempred(Tff_and_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 6:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean tff_xprod_type_sempred(Tff_xprod_typeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 7:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean fof_or_formula_sempred(Fof_or_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 8:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean fof_and_formula_sempred(Fof_and_formulaContext _localctx, int predIndex) {
		switch (predIndex) {
		case 9:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean cnf_disjunction_sempred(Cnf_disjunctionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 10:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3e\u0668\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k\t"+
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv\4"+
		"w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t\u0080"+
		"\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084\4\u0085"+
		"\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089\t\u0089"+
		"\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d\4\u008e"+
		"\t\u008e\4\u008f\t\u008f\4\u0090\t\u0090\4\u0091\t\u0091\4\u0092\t\u0092"+
		"\4\u0093\t\u0093\4\u0094\t\u0094\4\u0095\t\u0095\4\u0096\t\u0096\4\u0097"+
		"\t\u0097\4\u0098\t\u0098\4\u0099\t\u0099\4\u009a\t\u009a\4\u009b\t\u009b"+
		"\4\u009c\t\u009c\4\u009d\t\u009d\4\u009e\t\u009e\4\u009f\t\u009f\4\u00a0"+
		"\t\u00a0\4\u00a1\t\u00a1\4\u00a2\t\u00a2\4\u00a3\t\u00a3\4\u00a4\t\u00a4"+
		"\4\u00a5\t\u00a5\4\u00a6\t\u00a6\4\u00a7\t\u00a7\4\u00a8\t\u00a8\4\u00a9"+
		"\t\u00a9\4\u00aa\t\u00aa\4\u00ab\t\u00ab\4\u00ac\t\u00ac\4\u00ad\t\u00ad"+
		"\4\u00ae\t\u00ae\4\u00af\t\u00af\4\u00b0\t\u00b0\4\u00b1\t\u00b1\4\u00b2"+
		"\t\u00b2\4\u00b3\t\u00b3\4\u00b4\t\u00b4\4\u00b5\t\u00b5\4\u00b6\t\u00b6"+
		"\4\u00b7\t\u00b7\4\u00b8\t\u00b8\4\u00b9\t\u00b9\4\u00ba\t\u00ba\4\u00bb"+
		"\t\u00bb\4\u00bc\t\u00bc\4\u00bd\t\u00bd\4\u00be\t\u00be\4\u00bf\t\u00bf"+
		"\4\u00c0\t\u00c0\4\u00c1\t\u00c1\4\u00c2\t\u00c2\4\u00c3\t\u00c3\4\u00c4"+
		"\t\u00c4\4\u00c5\t\u00c5\4\u00c6\t\u00c6\4\u00c7\t\u00c7\4\u00c8\t\u00c8"+
		"\4\u00c9\t\u00c9\4\u00ca\t\u00ca\3\2\7\2\u0196\n\2\f\2\16\2\u0199\13\2"+
		"\3\2\3\2\3\3\3\3\5\3\u019f\n\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4\u01a8\n"+
		"\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5\u01b1\n\5\3\5\3\5\3\6\3\6\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\5\7\u01be\n\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5"+
		"\b\u01c9\n\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u01d4\n\t\3\t\3\t"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u01df\n\n\3\n\3\n\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\5\13\u01ea\n\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f"+
		"\5\f\u01f5\n\f\3\f\3\f\3\r\3\r\3\r\5\r\u01fc\n\r\3\16\3\16\3\17\3\17\5"+
		"\17\u0202\n\17\3\20\3\20\3\20\3\20\5\20\u0208\n\20\3\21\3\21\3\21\5\21"+
		"\u020d\n\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\5\23\u0216\n\23\3\24\3"+
		"\24\3\24\3\24\3\24\3\24\3\24\3\24\7\24\u0220\n\24\f\24\16\24\u0223\13"+
		"\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\25\7\25\u022d\n\25\f\25\16\25"+
		"\u0230\13\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\7\26\u023a\n\26\f"+
		"\26\16\26\u023d\13\26\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\5\27\u0249\n\27\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32"+
		"\3\32\7\32\u0257\n\32\f\32\16\32\u025a\13\32\3\33\3\33\5\33\u025e\n\33"+
		"\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\5\36"+
		"\u026d\n\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\3\37\3\37\5\37\u027f\n\37\3 \3 \3 \5 \u0284\n \3!\3!\3!\3!"+
		"\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%"+
		"\5%\u029f\n%\3&\3&\3&\3&\3\'\3\'\3\'\5\'\u02a8\n\'\3(\3(\3)\3)\3*\3*\3"+
		"*\5*\u02b1\n*\3+\3+\3+\3+\3+\3+\3+\3+\5+\u02bb\n+\3,\3,\3,\3,\3,\3,\3"+
		",\3,\7,\u02c5\n,\f,\16,\u02c8\13,\3-\3-\3-\3-\3-\3-\3-\3-\7-\u02d2\n-"+
		"\f-\16-\u02d5\13-\3.\3.\3.\3.\3.\3.\3.\3.\5.\u02df\n.\3/\3/\3/\3/\3/\3"+
		"/\3/\3/\3/\3/\5/\u02eb\n/\3\60\3\60\3\60\7\60\u02f0\n\60\f\60\16\60\u02f3"+
		"\13\60\3\61\3\61\5\61\u02f7\n\61\3\62\3\62\3\63\3\63\3\63\5\63\u02fe\n"+
		"\63\3\64\3\64\3\64\5\64\u0303\n\64\3\65\3\65\5\65\u0307\n\65\3\66\3\66"+
		"\3\66\3\66\3\67\3\67\5\67\u030f\n\67\38\38\38\38\38\38\38\38\78\u0319"+
		"\n8\f8\168\u031c\138\39\39\39\39\39\39\39\39\79\u0326\n9\f9\169\u0329"+
		"\139\3:\3:\3:\3:\3:\3:\3:\3:\3:\5:\u0334\n:\3;\3;\3;\3;\3;\3;\3;\3<\3"+
		"<\3<\7<\u0340\n<\f<\16<\u0343\13<\3=\3=\5=\u0347\n=\3>\3>\3>\3>\3?\3?"+
		"\3?\3?\5?\u0351\n?\3@\3@\3A\3A\3A\3A\3A\3A\3A\3A\3B\3B\3B\3B\3B\3B\3B"+
		"\3B\3B\3B\3B\3B\5B\u0369\nB\3C\3C\3C\3C\3C\5C\u0370\nC\3D\3D\3D\7D\u0375"+
		"\nD\fD\16D\u0378\13D\3E\3E\3E\3E\3E\3E\3E\3E\5E\u0382\nE\3F\3F\3F\3F\3"+
		"F\3F\3F\3F\5F\u038c\nF\3G\3G\3G\3G\3G\5G\u0393\nG\3H\3H\3H\7H\u0398\n"+
		"H\fH\16H\u039b\13H\3I\3I\3I\3I\3I\3I\3I\3I\5I\u03a5\nI\3J\3J\3J\3J\3J"+
		"\3J\3J\3J\5J\u03af\nJ\3K\3K\3K\3K\3K\3K\3K\3K\5K\u03b9\nK\3L\3L\3L\3L"+
		"\3L\5L\u03c0\nL\3M\3M\3M\7M\u03c5\nM\fM\16M\u03c8\13M\3N\3N\3N\3N\3N\3"+
		"N\3N\3N\5N\u03d2\nN\3O\3O\3O\3O\3P\3P\3P\3P\3P\3P\3P\5P\u03df\nP\3Q\3"+
		"Q\3Q\3Q\3Q\3Q\3Q\3R\3R\3R\3R\3R\5R\u03ed\nR\3S\3S\3S\3S\3S\5S\u03f4\n"+
		"S\3T\3T\3T\3T\3T\3T\3T\3T\5T\u03fe\nT\3U\3U\3U\7U\u0403\nU\fU\16U\u0406"+
		"\13U\3V\3V\3V\3V\3W\3W\3W\3W\3W\3W\3W\3W\7W\u0414\nW\fW\16W\u0417\13W"+
		"\3X\3X\5X\u041b\nX\3Y\3Y\5Y\u041f\nY\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3[\3[\5[\u042a"+
		"\n[\3\\\3\\\5\\\u042e\n\\\3]\3]\5]\u0432\n]\3^\3^\3^\3^\3_\3_\5_\u043a"+
		"\n_\3`\3`\3`\3`\3`\3`\3`\3`\7`\u0444\n`\f`\16`\u0447\13`\3a\3a\3a\3a\3"+
		"a\3a\3a\3a\7a\u0451\na\fa\16a\u0454\13a\3b\3b\3b\3b\3b\3b\3b\5b\u045d"+
		"\nb\3c\3c\3c\3c\3c\3c\3c\3d\3d\3d\7d\u0469\nd\fd\16d\u046c\13d\3e\3e\3"+
		"e\3e\5e\u0472\ne\3f\3f\3f\3f\3g\3g\3g\5g\u047b\ng\3h\3h\3i\3i\5i\u0481"+
		"\ni\3j\3j\3k\3k\3k\3k\3l\3l\3m\3m\3m\3m\3m\3m\5m\u0491\nm\3n\3n\5n\u0495"+
		"\nn\3o\3o\3p\3p\3p\3p\3p\3p\5p\u049f\np\3q\3q\3q\3q\3q\3q\5q\u04a7\nq"+
		"\3r\3r\3r\7r\u04ac\nr\fr\16r\u04af\13r\3s\3s\3s\3s\3s\5s\u04b6\ns\3t\3"+
		"t\3t\5t\u04bb\nt\3u\3u\3u\3u\3u\3u\3u\3u\3v\3v\3v\3v\3v\3v\3v\3v\3v\3"+
		"v\3v\3v\5v\u04d1\nv\3w\3w\3w\3w\3w\5w\u04d8\nw\3x\3x\3x\3x\3x\3x\3x\3"+
		"x\5x\u04e2\nx\3y\3y\3y\3y\3y\5y\u04e9\ny\3z\3z\3z\7z\u04ee\nz\fz\16z\u04f1"+
		"\13z\3{\3{\3{\3{\3{\5{\u04f8\n{\3|\3|\3|\3|\3|\3|\7|\u0500\n|\f|\16|\u0503"+
		"\13|\3}\3}\3}\3}\5}\u0509\n}\3~\3~\3~\5~\u050e\n~\3\177\3\177\3\u0080"+
		"\3\u0080\3\u0081\3\u0081\3\u0081\3\u0081\5\u0081\u0518\n\u0081\3\u0082"+
		"\3\u0082\5\u0082\u051c\n\u0082\3\u0083\3\u0083\3\u0084\3\u0084\5\u0084"+
		"\u0522\n\u0084\3\u0085\3\u0085\3\u0086\3\u0086\3\u0087\3\u0087\3\u0088"+
		"\3\u0088\3\u0089\3\u0089\3\u008a\3\u008a\3\u008b\3\u008b\3\u008c\3\u008c"+
		"\3\u008d\3\u008d\5\u008d\u0536\n\u008d\3\u008e\3\u008e\5\u008e\u053a\n"+
		"\u008e\3\u008f\3\u008f\3\u0090\3\u0090\3\u0091\3\u0091\3\u0092\3\u0092"+
		"\3\u0093\3\u0093\3\u0094\3\u0094\3\u0095\3\u0095\3\u0096\3\u0096\3\u0097"+
		"\3\u0097\3\u0098\3\u0098\5\u0098\u0550\n\u0098\3\u0099\3\u0099\3\u009a"+
		"\3\u009a\3\u009a\3\u009a\3\u009a\3\u009a\3\u009a\3\u009a\5\u009a\u055c"+
		"\n\u009a\3\u009b\3\u009b\3\u009b\7\u009b\u0561\n\u009b\f\u009b\16\u009b"+
		"\u0564\13\u009b\3\u009c\3\u009c\5\u009c\u0568\n\u009c\3\u009d\3\u009d"+
		"\3\u009d\3\u009d\3\u009d\3\u009d\3\u009d\3\u009d\3\u009e\3\u009e\3\u009f"+
		"\3\u009f\3\u009f\3\u009f\3\u009f\5\u009f\u0579\n\u009f\3\u00a0\3\u00a0"+
		"\3\u00a0\7\u00a0\u057e\n\u00a0\f\u00a0\16\u00a0\u0581\13\u00a0\3\u00a1"+
		"\3\u00a1\5\u00a1\u0585\n\u00a1\3\u00a2\3\u00a2\3\u00a2\3\u00a3\3\u00a3"+
		"\3\u00a3\5\u00a3\u058d\n\u00a3\3\u00a3\3\u00a3\3\u00a4\3\u00a4\3\u00a5"+
		"\3\u00a5\3\u00a5\5\u00a5\u0596\n\u00a5\3\u00a6\3\u00a6\3\u00a6\5\u00a6"+
		"\u059b\n\u00a6\3\u00a6\3\u00a6\3\u00a7\3\u00a7\3\u00a7\3\u00a8\3\u00a8"+
		"\3\u00a8\5\u00a8\u05a5\n\u00a8\3\u00a8\3\u00a8\3\u00a9\3\u00a9\3\u00aa"+
		"\3\u00aa\3\u00aa\5\u00aa\u05ae\n\u00aa\3\u00aa\3\u00aa\3\u00ab\3\u00ab"+
		"\3\u00ac\3\u00ac\3\u00ac\3\u00ad\3\u00ad\3\u00ad\3\u00ad\3\u00ad\3\u00ad"+
		"\5\u00ad\u05bd\n\u00ad\3\u00ae\3\u00ae\3\u00ae\7\u00ae\u05c2\n\u00ae\f"+
		"\u00ae\16\u00ae\u05c5\13\u00ae\3\u00af\3\u00af\3\u00af\5\u00af\u05ca\n"+
		"\u00af\3\u00b0\3\u00b0\5\u00b0\u05ce\n\u00b0\3\u00b1\3\u00b1\3\u00b1\3"+
		"\u00b1\3\u00b2\3\u00b2\3\u00b2\3\u00b2\3\u00b3\3\u00b3\3\u00b3\3\u00b3"+
		"\5\u00b3\u05dc\n\u00b3\3\u00b4\3\u00b4\3\u00b4\3\u00b4\3\u00b4\5\u00b4"+
		"\u05e3\n\u00b4\3\u00b5\3\u00b5\3\u00b6\3\u00b6\3\u00b6\3\u00b6\3\u00b6"+
		"\3\u00b6\3\u00b6\3\u00b7\3\u00b7\3\u00b7\3\u00b7\3\u00b7\3\u00b7\3\u00b8"+
		"\3\u00b8\3\u00b8\3\u00b8\3\u00b9\3\u00b9\3\u00b9\3\u00b9\3\u00b9\3\u00b9"+
		"\3\u00b9\3\u00b9\3\u00ba\3\u00ba\3\u00ba\7\u00ba\u0603\n\u00ba\f\u00ba"+
		"\16\u00ba\u0606\13\u00ba\3\u00bb\3\u00bb\5\u00bb\u060a\n\u00bb\3\u00bc"+
		"\3\u00bc\3\u00bc\5\u00bc\u060f\n\u00bc\3\u00bc\3\u00bc\3\u00bd\3\u00bd"+
		"\3\u00bd\3\u00bd\3\u00bd\3\u00be\3\u00be\3\u00be\7\u00be\u061b\n\u00be"+
		"\f\u00be\16\u00be\u061e\13\u00be\3\u00bf\3\u00bf\3\u00bf\3\u00bf\3\u00bf"+
		"\3\u00bf\5\u00bf\u0626\n\u00bf\3\u00c0\3\u00c0\3\u00c0\3\u00c0\3\u00c0"+
		"\3\u00c0\5\u00c0\u062e\n\u00c0\3\u00c1\3\u00c1\3\u00c1\3\u00c1\3\u00c1"+
		"\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2"+
		"\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2\3\u00c2"+
		"\3\u00c2\3\u00c2\5\u00c2\u0649\n\u00c2\3\u00c3\3\u00c3\3\u00c3\3\u00c3"+
		"\3\u00c3\5\u00c3\u0650\n\u00c3\3\u00c4\3\u00c4\3\u00c4\7\u00c4\u0655\n"+
		"\u00c4\f\u00c4\16\u00c4\u0658\13\u00c4\3\u00c5\3\u00c5\5\u00c5\u065c\n"+
		"\u00c5\3\u00c6\3\u00c6\3\u00c7\3\u00c7\3\u00c8\3\u00c8\3\u00c9\3\u00c9"+
		"\3\u00ca\3\u00ca\3\u00ca\2\r&(*VXnp\u00ac\u00be\u00c0\u00f6\u00cb\2\4"+
		"\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNP"+
		"RTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e"+
		"\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6"+
		"\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be"+
		"\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6"+
		"\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea\u00ec\u00ee"+
		"\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102\u0104\u0106"+
		"\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a\u011c\u011e"+
		"\u0120\u0122\u0124\u0126\u0128\u012a\u012c\u012e\u0130\u0132\u0134\u0136"+
		"\u0138\u013a\u013c\u013e\u0140\u0142\u0144\u0146\u0148\u014a\u014c\u014e"+
		"\u0150\u0152\u0154\u0156\u0158\u015a\u015c\u015e\u0160\u0162\u0164\u0166"+
		"\u0168\u016a\u016c\u016e\u0170\u0172\u0174\u0176\u0178\u017a\u017c\u017e"+
		"\u0180\u0182\u0184\u0186\u0188\u018a\u018c\u018e\u0190\u0192\2\13\5\2"+
		"??AACC\4\288==\7\2\67\67<<@@BBDD\4\2;;>>\3\2\60\65\3\2./\4\2::FF\3\2`"+
		"a\5\2LLOORR\2\u0657\2\u0197\3\2\2\2\4\u019e\3\2\2\2\6\u01a7\3\2\2\2\b"+
		"\u01a9\3\2\2\2\n\u01b4\3\2\2\2\f\u01b6\3\2\2\2\16\u01c1\3\2\2\2\20\u01cc"+
		"\3\2\2\2\22\u01d7\3\2\2\2\24\u01e2\3\2\2\2\26\u01ed\3\2\2\2\30\u01f8\3"+
		"\2\2\2\32\u01fd\3\2\2\2\34\u0201\3\2\2\2\36\u0207\3\2\2\2 \u020c\3\2\2"+
		"\2\"\u020e\3\2\2\2$\u0215\3\2\2\2&\u0217\3\2\2\2(\u0224\3\2\2\2*\u0231"+
		"\3\2\2\2,\u0248\3\2\2\2.\u024a\3\2\2\2\60\u024d\3\2\2\2\62\u0253\3\2\2"+
		"\2\64\u025d\3\2\2\2\66\u025f\3\2\2\28\u0263\3\2\2\2:\u026c\3\2\2\2<\u027e"+
		"\3\2\2\2>\u0283\3\2\2\2@\u0285\3\2\2\2B\u028d\3\2\2\2D\u0293\3\2\2\2F"+
		"\u0295\3\2\2\2H\u029e\3\2\2\2J\u02a0\3\2\2\2L\u02a7\3\2\2\2N\u02a9\3\2"+
		"\2\2P\u02ab\3\2\2\2R\u02b0\3\2\2\2T\u02ba\3\2\2\2V\u02bc\3\2\2\2X\u02c9"+
		"\3\2\2\2Z\u02de\3\2\2\2\\\u02ea\3\2\2\2^\u02ec\3\2\2\2`\u02f6\3\2\2\2"+
		"b\u02f8\3\2\2\2d\u02fd\3\2\2\2f\u0302\3\2\2\2h\u0306\3\2\2\2j\u0308\3"+
		"\2\2\2l\u030e\3\2\2\2n\u0310\3\2\2\2p\u031d\3\2\2\2r\u0333\3\2\2\2t\u0335"+
		"\3\2\2\2v\u033c\3\2\2\2x\u0346\3\2\2\2z\u0348\3\2\2\2|\u0350\3\2\2\2~"+
		"\u0352\3\2\2\2\u0080\u0354\3\2\2\2\u0082\u0368\3\2\2\2\u0084\u036f\3\2"+
		"\2\2\u0086\u0371\3\2\2\2\u0088\u0381\3\2\2\2\u008a\u038b\3\2\2\2\u008c"+
		"\u0392\3\2\2\2\u008e\u0394\3\2\2\2\u0090\u03a4\3\2\2\2\u0092\u03ae\3\2"+
		"\2\2\u0094\u03b8\3\2\2\2\u0096\u03bf\3\2\2\2\u0098\u03c1\3\2\2\2\u009a"+
		"\u03d1\3\2\2\2\u009c\u03d3\3\2\2\2\u009e\u03de\3\2\2\2\u00a0\u03e0\3\2"+
		"\2\2\u00a2\u03ec\3\2\2\2\u00a4\u03f3\3\2\2\2\u00a6\u03fd\3\2\2\2\u00a8"+
		"\u03ff\3\2\2\2\u00aa\u0407\3\2\2\2\u00ac\u040b\3\2\2\2\u00ae\u041a\3\2"+
		"\2\2\u00b0\u041e\3\2\2\2\u00b2\u0420\3\2\2\2\u00b4\u0429\3\2\2\2\u00b6"+
		"\u042d\3\2\2\2\u00b8\u0431\3\2\2\2\u00ba\u0433\3\2\2\2\u00bc\u0439\3\2"+
		"\2\2\u00be\u043b\3\2\2\2\u00c0\u0448\3\2\2\2\u00c2\u045c\3\2\2\2\u00c4"+
		"\u045e\3\2\2\2\u00c6\u0465\3\2\2\2\u00c8\u0471\3\2\2\2\u00ca\u0473\3\2"+
		"\2\2\u00cc\u047a\3\2\2\2\u00ce\u047c\3\2\2\2\u00d0\u0480\3\2\2\2\u00d2"+
		"\u0482\3\2\2\2\u00d4\u0484\3\2\2\2\u00d6\u0488\3\2\2\2\u00d8\u0490\3\2"+
		"\2\2\u00da\u0494\3\2\2\2\u00dc\u0496\3\2\2\2\u00de\u049e\3\2\2\2\u00e0"+
		"\u04a6\3\2\2\2\u00e2\u04a8\3\2\2\2\u00e4\u04b5\3\2\2\2\u00e6\u04ba\3\2"+
		"\2\2\u00e8\u04bc\3\2\2\2\u00ea\u04d0\3\2\2\2\u00ec\u04d7\3\2\2\2\u00ee"+
		"\u04e1\3\2\2\2\u00f0\u04e8\3\2\2\2\u00f2\u04ea\3\2\2\2\u00f4\u04f7\3\2"+
		"\2\2\u00f6\u04f9\3\2\2\2\u00f8\u0508\3\2\2\2\u00fa\u050d\3\2\2\2\u00fc"+
		"\u050f\3\2\2\2\u00fe\u0511\3\2\2\2\u0100\u0517\3\2\2\2\u0102\u051b\3\2"+
		"\2\2\u0104\u051d\3\2\2\2\u0106\u0521\3\2\2\2\u0108\u0523\3\2\2\2\u010a"+
		"\u0525\3\2\2\2\u010c\u0527\3\2\2\2\u010e\u0529\3\2\2\2\u0110\u052b\3\2"+
		"\2\2\u0112\u052d\3\2\2\2\u0114\u052f\3\2\2\2\u0116\u0531\3\2\2\2\u0118"+
		"\u0535\3\2\2\2\u011a\u0539\3\2\2\2\u011c\u053b\3\2\2\2\u011e\u053d\3\2"+
		"\2\2\u0120\u053f\3\2\2\2\u0122\u0541\3\2\2\2\u0124\u0543\3\2\2\2\u0126"+
		"\u0545\3\2\2\2\u0128\u0547\3\2\2\2\u012a\u0549\3\2\2\2\u012c\u054b\3\2"+
		"\2\2\u012e\u054f\3\2\2\2\u0130\u0551\3\2\2\2\u0132\u055b\3\2\2\2\u0134"+
		"\u055d\3\2\2\2\u0136\u0567\3\2\2\2\u0138\u0569\3\2\2\2\u013a\u0571\3\2"+
		"\2\2\u013c\u0578\3\2\2\2\u013e\u057a\3\2\2\2\u0140\u0582\3\2\2\2\u0142"+
		"\u0586\3\2\2\2\u0144\u0589\3\2\2\2\u0146\u0590\3\2\2\2\u0148\u0595\3\2"+
		"\2\2\u014a\u0597\3\2\2\2\u014c\u059e\3\2\2\2\u014e\u05a1\3\2\2\2\u0150"+
		"\u05a8\3\2\2\2\u0152\u05aa\3\2\2\2\u0154\u05b1\3\2\2\2\u0156\u05b3\3\2"+
		"\2\2\u0158\u05bc\3\2\2\2\u015a\u05be\3\2\2\2\u015c\u05c9\3\2\2\2\u015e"+
		"\u05cd\3\2\2\2\u0160\u05cf\3\2\2\2\u0162\u05d3\3\2\2\2\u0164\u05db\3\2"+
		"\2\2\u0166\u05e2\3\2\2\2\u0168\u05e4\3\2\2\2\u016a\u05e6\3\2\2\2\u016c"+
		"\u05ed\3\2\2\2\u016e\u05f3\3\2\2\2\u0170\u05f7\3\2\2\2\u0172\u05ff\3\2"+
		"\2\2\u0174\u0609\3\2\2\2\u0176\u060b\3\2\2\2\u0178\u0612\3\2\2\2\u017a"+
		"\u0617\3\2\2\2\u017c\u0625\3\2\2\2\u017e\u062d\3\2\2\2\u0180\u062f\3\2"+
		"\2\2\u0182\u0648\3\2\2\2\u0184\u064f\3\2\2\2\u0186\u0651\3\2\2\2\u0188"+
		"\u065b\3\2\2\2\u018a\u065d\3\2\2\2\u018c\u065f\3\2\2\2\u018e\u0661\3\2"+
		"\2\2\u0190\u0663\3\2\2\2\u0192\u0665\3\2\2\2\u0194\u0196\5\4\3\2\u0195"+
		"\u0194\3\2\2\2\u0196\u0199\3\2\2\2\u0197\u0195\3\2\2\2\u0197\u0198\3\2"+
		"\2\2\u0198\u019a\3\2\2\2\u0199\u0197\3\2\2\2\u019a\u019b\7\2\2\3\u019b"+
		"\3\3\2\2\2\u019c\u019f\5\6\4\2\u019d\u019f\5\u0176\u00bc\2\u019e\u019c"+
		"\3\2\2\2\u019e\u019d\3\2\2\2\u019f\5\3\2\2\2\u01a0\u01a8\5\f\7\2\u01a1"+
		"\u01a8\5\16\b\2\u01a2\u01a8\5\20\t\2\u01a3\u01a8\5\22\n\2\u01a4\u01a8"+
		"\5\24\13\2\u01a5\u01a8\5\26\f\2\u01a6\u01a8\5\b\5\2\u01a7\u01a0\3\2\2"+
		"\2\u01a7\u01a1\3\2\2\2\u01a7\u01a2\3\2\2\2\u01a7\u01a3\3\2\2\2\u01a7\u01a4"+
		"\3\2\2\2\u01a7\u01a5\3\2\2\2\u01a7\u01a6\3\2\2\2\u01a8\7\3\2\2\2\u01a9"+
		"\u01aa\7\3\2\2\u01aa\u01ab\5\u0188\u00c5\2\u01ab\u01ac\7\4\2\2\u01ac\u01ad"+
		"\5\32\16\2\u01ad\u01ae\7\4\2\2\u01ae\u01b0\5\n\6\2\u01af\u01b1\5\30\r"+
		"\2\u01b0\u01af\3\2\2\2\u01b0\u01b1\3\2\2\2\u01b1\u01b2\3\2\2\2\u01b2\u01b3"+
		"\7\5\2\2\u01b3\t\3\2\2\2\u01b4\u01b5\5\u00b4[\2\u01b5\13\3\2\2\2\u01b6"+
		"\u01b7\7\6\2\2\u01b7\u01b8\5\u0188\u00c5\2\u01b8\u01b9\7\4\2\2\u01b9\u01ba"+
		"\5\32\16\2\u01ba\u01bb\7\4\2\2\u01bb\u01bd\5\34\17\2\u01bc\u01be\5\30"+
		"\r\2\u01bd\u01bc\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01bf\3\2\2\2\u01bf"+
		"\u01c0\7\5\2\2\u01c0\r\3\2\2\2\u01c1\u01c2\7\7\2\2\u01c2\u01c3\5\u0188"+
		"\u00c5\2\u01c3\u01c4\7\4\2\2\u01c4\u01c5\5\32\16\2\u01c5\u01c6\7\4\2\2"+
		"\u01c6\u01c8\5`\61\2\u01c7\u01c9\5\30\r\2\u01c8\u01c7\3\2\2\2\u01c8\u01c9"+
		"\3\2\2\2\u01c9\u01ca\3\2\2\2\u01ca\u01cb\7\5\2\2\u01cb\17\3\2\2\2\u01cc"+
		"\u01cd\7\b\2\2\u01cd\u01ce\5\u0188\u00c5\2\u01ce\u01cf\7\4\2\2\u01cf\u01d0"+
		"\5\32\16\2\u01d0\u01d1\7\4\2\2\u01d1\u01d3\5d\63\2\u01d2\u01d4\5\30\r"+
		"\2\u01d3\u01d2\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d5\3\2\2\2\u01d5\u01d6"+
		"\7\5\2\2\u01d6\21\3\2\2\2\u01d7\u01d8\7\t\2\2\u01d8\u01d9\5\u0188\u00c5"+
		"\2\u01d9\u01da\7\4\2\2\u01da\u01db\5\32\16\2\u01db\u01dc\7\4\2\2\u01dc"+
		"\u01de\5\u00aeX\2\u01dd\u01df\5\30\r\2\u01de\u01dd\3\2\2\2\u01de\u01df"+
		"\3\2\2\2\u01df\u01e0\3\2\2\2\u01e0\u01e1\7\5\2\2\u01e1\23\3\2\2\2\u01e2"+
		"\u01e3\7\n\2\2\u01e3\u01e4\5\u0188\u00c5\2\u01e4\u01e5\7\4\2\2\u01e5\u01e6"+
		"\5\32\16\2\u01e6\u01e7\7\4\2\2\u01e7\u01e9\5\u00b4[\2\u01e8\u01ea\5\30"+
		"\r\2\u01e9\u01e8\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea\u01eb\3\2\2\2\u01eb"+
		"\u01ec\7\5\2\2\u01ec\25\3\2\2\2\u01ed\u01ee\7\13\2\2\u01ee\u01ef\5\u0188"+
		"\u00c5\2\u01ef\u01f0\7\4\2\2\u01f0\u01f1\5\32\16\2\u01f1\u01f2\7\4\2\2"+
		"\u01f2\u01f4\5\u00f4{\2\u01f3\u01f5\5\30\r\2\u01f4\u01f3\3\2\2\2\u01f4"+
		"\u01f5\3\2\2\2\u01f5\u01f6\3\2\2\2\u01f6\u01f7\7\5\2\2\u01f7\27\3\2\2"+
		"\2\u01f8\u01f9\7\4\2\2\u01f9\u01fb\5\u0132\u009a\2\u01fa\u01fc\5\u0156"+
		"\u00ac\2\u01fb\u01fa\3\2\2\2\u01fb\u01fc\3\2\2\2\u01fc\31\3\2\2\2\u01fd"+
		"\u01fe\7`\2\2\u01fe\33\3\2\2\2\u01ff\u0202\5\36\20\2\u0200\u0202\5Z.\2"+
		"\u0201\u01ff\3\2\2\2\u0201\u0200\3\2\2\2\u0202\35\3\2\2\2\u0203\u0208"+
		"\5 \21\2\u0204\u0208\5,\27\2\u0205\u0208\5F$\2\u0206\u0208\5J&\2\u0207"+
		"\u0203\3\2\2\2\u0207\u0204\3\2\2\2\u0207\u0205\3\2\2\2\u0207\u0206\3\2"+
		"\2\2\u0208\37\3\2\2\2\u0209\u020d\5\"\22\2\u020a\u020d\5$\23\2\u020b\u020d"+
		"\5R*\2\u020c\u0209\3\2\2\2\u020c\u020a\3\2\2\2\u020c\u020b\3\2\2\2\u020d"+
		"!\3\2\2\2\u020e\u020f\5,\27\2\u020f\u0210\5\u0100\u0081\2\u0210\u0211"+
		"\5,\27\2\u0211#\3\2\2\2\u0212\u0216\5&\24\2\u0213\u0216\5(\25\2\u0214"+
		"\u0216\5*\26\2\u0215\u0212\3\2\2\2\u0215\u0213\3\2\2\2\u0215\u0214\3\2"+
		"\2\2\u0216%\3\2\2\2\u0217\u0218\b\24\1\2\u0218\u0219\5,\27\2\u0219\u021a"+
		"\7.\2\2\u021a\u021b\5,\27\2\u021b\u0221\3\2\2\2\u021c\u021d\f\3\2\2\u021d"+
		"\u021e\7.\2\2\u021e\u0220\5,\27\2\u021f\u021c\3\2\2\2\u0220\u0223\3\2"+
		"\2\2\u0221\u021f\3\2\2\2\u0221\u0222\3\2\2\2\u0222\'\3\2\2\2\u0223\u0221"+
		"\3\2\2\2\u0224\u0225\b\25\1\2\u0225\u0226\5,\27\2\u0226\u0227\7/\2\2\u0227"+
		"\u0228\5,\27\2\u0228\u022e\3\2\2\2\u0229\u022a\f\3\2\2\u022a\u022b\7/"+
		"\2\2\u022b\u022d\5,\27\2\u022c\u0229\3\2\2\2\u022d\u0230\3\2\2\2\u022e"+
		"\u022c\3\2\2\2\u022e\u022f\3\2\2\2\u022f)\3\2\2\2\u0230\u022e\3\2\2\2"+
		"\u0231\u0232\b\26\1\2\u0232\u0233\5,\27\2\u0233\u0234\7E\2\2\u0234\u0235"+
		"\5,\27\2\u0235\u023b\3\2\2\2\u0236\u0237\f\3\2\2\u0237\u0238\7E\2\2\u0238"+
		"\u023a\5,\27\2\u0239\u0236\3\2\2\2\u023a\u023d\3\2\2\2\u023b\u0239\3\2"+
		"\2\2\u023b\u023c\3\2\2\2\u023c+\3\2\2\2\u023d\u023b\3\2\2\2\u023e\u0249"+
		"\5.\30\2\u023f\u0249\58\35\2\u0240\u0249\5:\36\2\u0241\u0249\5@!\2\u0242"+
		"\u0249\5B\"\2\u0243\u0249\5\\/\2\u0244\u0245\7\f\2\2\u0245\u0246\5\36"+
		"\20\2\u0246\u0247\7\r\2\2\u0247\u0249\3\2\2\2\u0248\u023e\3\2\2\2\u0248"+
		"\u023f\3\2\2\2\u0248\u0240\3\2\2\2\u0248\u0241\3\2\2\2\u0248\u0242\3\2"+
		"\2\2\u0248\u0243\3\2\2\2\u0248\u0244\3\2\2\2\u0249-\3\2\2\2\u024a\u024b"+
		"\5\60\31\2\u024b\u024c\5,\27\2\u024c/\3\2\2\2\u024d\u024e\5\u00fa~\2\u024e"+
		"\u024f\7\16\2\2\u024f\u0250\5\62\32\2\u0250\u0251\7\17\2\2\u0251\u0252"+
		"\7\20\2\2\u0252\61\3\2\2\2\u0253\u0258\5\64\33\2\u0254\u0255\7\4\2\2\u0255"+
		"\u0257\5\64\33\2\u0256\u0254\3\2\2\2\u0257\u025a\3\2\2\2\u0258\u0256\3"+
		"\2\2\2\u0258\u0259\3\2\2\2\u0259\63\3\2\2\2\u025a\u0258\3\2\2\2\u025b"+
		"\u025e\5\66\34\2\u025c\u025e\5\u0130\u0099\2\u025d\u025b\3\2\2\2\u025d"+
		"\u025c\3\2\2\2\u025e\65\3\2\2\2\u025f\u0260\5\u0130\u0099\2\u0260\u0261"+
		"\7\20\2\2\u0261\u0262\5L\'\2\u0262\67\3\2\2\2\u0263\u0264\5\u0102\u0082"+
		"\2\u0264\u0265\7\f\2\2\u0265\u0266\5\36\20\2\u0266\u0267\7\r\2\2\u0267"+
		"9\3\2\2\2\u0268\u026d\5<\37\2\u0269\u026d\5\u0130\u0099\2\u026a\u026d"+
		"\5\u012e\u0098\2\u026b\u026d\5> \2\u026c\u0268\3\2\2\2\u026c\u0269\3\2"+
		"\2\2\u026c\u026a\3\2\2\2\u026c\u026b\3\2\2\2\u026d;\3\2\2\2\u026e\u027f"+
		"\5\u0118\u008d\2\u026f\u0270\5\u0124\u0093\2\u0270\u0271\7\f\2\2\u0271"+
		"\u0272\5D#\2\u0272\u0273\7\r\2\2\u0273\u027f\3\2\2\2\u0274\u0275\5\u012c"+
		"\u0097\2\u0275\u0276\7\f\2\2\u0276\u0277\5D#\2\u0277\u0278\7\r\2\2\u0278"+
		"\u027f\3\2\2\2\u0279\u027a\5\u0128\u0095\2\u027a\u027b\7\f\2\2\u027b\u027c"+
		"\5D#\2\u027c\u027d\7\r\2\2\u027d\u027f\3\2\2\2\u027e\u026e\3\2\2\2\u027e"+
		"\u026f\3\2\2\2\u027e\u0274\3\2\2\2\u027e\u0279\3\2\2\2\u027f=\3\2\2\2"+
		"\u0280\u0284\5\u0100\u0081\2\u0281\u0284\5\u010c\u0087\2\u0282\u0284\5"+
		"\u0102\u0082\2\u0283\u0280\3\2\2\2\u0283\u0281\3\2\2\2\u0283\u0282\3\2"+
		"\2\2\u0284?\3\2\2\2\u0285\u0286\7\21\2\2\u0286\u0287\5\36\20\2\u0287\u0288"+
		"\7\4\2\2\u0288\u0289\5\36\20\2\u0289\u028a\7\4\2\2\u028a\u028b\5\36\20"+
		"\2\u028b\u028c\7\r\2\2\u028cA\3\2\2\2\u028d\u028e\7\22\2\2\u028e\u028f"+
		"\5,\27\2\u028f\u0290\7\4\2\2\u0290\u0291\5\34\17\2\u0291\u0292\7\r\2\2"+
		"\u0292C\3\2\2\2\u0293\u0294\5^\60\2\u0294E\3\2\2\2\u0295\u0296\5H%\2\u0296"+
		"\u0297\7\20\2\2\u0297\u0298\5L\'\2\u0298G\3\2\2\2\u0299\u029f\5:\36\2"+
		"\u029a\u029b\7\f\2\2\u029b\u029c\5\36\20\2\u029c\u029d\7\r\2\2\u029d\u029f"+
		"\3\2\2\2\u029e\u0299\3\2\2\2\u029e\u029a\3\2\2\2\u029fI\3\2\2\2\u02a0"+
		"\u02a1\5:\36\2\u02a1\u02a2\7J\2\2\u02a2\u02a3\5:\36\2\u02a3K\3\2\2\2\u02a4"+
		"\u02a8\5N(\2\u02a5\u02a8\5T+\2\u02a6\u02a8\5P)\2\u02a7\u02a4\3\2\2\2\u02a7"+
		"\u02a5\3\2\2\2\u02a7\u02a6\3\2\2\2\u02a8M\3\2\2\2\u02a9\u02aa\5,\27\2"+
		"\u02aaO\3\2\2\2\u02ab\u02ac\5*\26\2\u02acQ\3\2\2\2\u02ad\u02b1\5T+\2\u02ae"+
		"\u02b1\5V,\2\u02af\u02b1\5X-\2\u02b0\u02ad\3\2\2\2\u02b0\u02ae\3\2\2\2"+
		"\u02b0\u02af\3\2\2\2\u02b1S\3\2\2\2\u02b2\u02b3\5N(\2\u02b3\u02b4\7G\2"+
		"\2\u02b4\u02b5\5N(\2\u02b5\u02bb\3\2\2\2\u02b6\u02b7\5N(\2\u02b7\u02b8"+
		"\7G\2\2\u02b8\u02b9\5T+\2\u02b9\u02bb\3\2\2\2\u02ba\u02b2\3\2\2\2\u02ba"+
		"\u02b6\3\2\2\2\u02bbU\3\2\2\2\u02bc\u02bd\b,\1\2\u02bd\u02be\5N(\2\u02be"+
		"\u02bf\7H\2\2\u02bf\u02c0\5N(\2\u02c0\u02c6\3\2\2\2\u02c1\u02c2\f\3\2"+
		"\2\u02c2\u02c3\7H\2\2\u02c3\u02c5\5N(\2\u02c4\u02c1\3\2\2\2\u02c5\u02c8"+
		"\3\2\2\2\u02c6\u02c4\3\2\2\2\u02c6\u02c7\3\2\2\2\u02c7W\3\2\2\2\u02c8"+
		"\u02c6\3\2\2\2\u02c9\u02ca\b-\1\2\u02ca\u02cb\5N(\2\u02cb\u02cc\7I\2\2"+
		"\u02cc\u02cd\5N(\2\u02cd\u02d3\3\2\2\2\u02ce\u02cf\f\3\2\2\u02cf\u02d0"+
		"\7I\2\2\u02d0\u02d2\5N(\2\u02d1\u02ce\3\2\2\2\u02d2\u02d5\3\2\2\2\u02d3"+
		"\u02d1\3\2\2\2\u02d3\u02d4\3\2\2\2\u02d4Y\3\2\2\2\u02d5\u02d3\3\2\2\2"+
		"\u02d6\u02d7\5\\/\2\u02d7\u02d8\7K\2\2\u02d8\u02d9\5\\/\2\u02d9\u02df"+
		"\3\2\2\2\u02da\u02db\7\f\2\2\u02db\u02dc\5Z.\2\u02dc\u02dd\7\r\2\2\u02dd"+
		"\u02df\3\2\2\2\u02de\u02d6\3\2\2\2\u02de\u02da\3\2\2\2\u02df[\3\2\2\2"+
		"\u02e0\u02eb\7\23\2\2\u02e1\u02e2\7\16\2\2\u02e2\u02e3\5^\60\2\u02e3\u02e4"+
		"\7\17\2\2\u02e4\u02eb\3\2\2\2\u02e5\u02eb\7\24\2\2\u02e6\u02e7\7\25\2"+
		"\2\u02e7\u02e8\5^\60\2\u02e8\u02e9\7\26\2\2\u02e9\u02eb\3\2\2\2\u02ea"+
		"\u02e0\3\2\2\2\u02ea\u02e1\3\2\2\2\u02ea\u02e5\3\2\2\2\u02ea\u02e6\3\2"+
		"\2\2\u02eb]\3\2\2\2\u02ec\u02f1\5\36\20\2\u02ed\u02ee\7\4\2\2\u02ee\u02f0"+
		"\5\36\20\2\u02ef\u02ed\3\2\2\2\u02f0\u02f3\3\2\2\2\u02f1\u02ef\3\2\2\2"+
		"\u02f1\u02f2\3\2\2\2\u02f2_\3\2\2\2\u02f3\u02f1\3\2\2\2\u02f4\u02f7\5"+
		"b\62\2\u02f5\u02f7\5Z.\2\u02f6\u02f4\3\2\2\2\u02f6\u02f5\3\2\2\2\u02f7"+
		"a\3\2\2\2\u02f8\u02f9\5\36\20\2\u02f9c\3\2\2\2\u02fa\u02fe\5f\64\2\u02fb"+
		"\u02fe\5\u009aN\2\u02fc\u02fe\5\u0094K\2\u02fd\u02fa\3\2\2\2\u02fd\u02fb"+
		"\3\2\2\2\u02fd\u02fc\3\2\2\2\u02fee\3\2\2\2\u02ff\u0303\5h\65\2\u0300"+
		"\u0303\5r:\2\u0301\u0303\5\u009cO\2\u0302\u02ff\3\2\2\2\u0302\u0300\3"+
		"\2\2\2\u0302\u0301\3\2\2\2\u0303g\3\2\2\2\u0304\u0307\5j\66\2\u0305\u0307"+
		"\5l\67\2\u0306\u0304\3\2\2\2\u0306\u0305\3\2\2\2\u0307i\3\2\2\2\u0308"+
		"\u0309\5r:\2\u0309\u030a\5\u010a\u0086\2\u030a\u030b\5r:\2\u030bk\3\2"+
		"\2\2\u030c\u030f\5n8\2\u030d\u030f\5p9\2\u030e\u030c\3\2\2\2\u030e\u030d"+
		"\3\2\2\2\u030fm\3\2\2\2\u0310\u0311\b8\1\2\u0311\u0312\5r:\2\u0312\u0313"+
		"\7.\2\2\u0313\u0314\5r:\2\u0314\u031a\3\2\2\2\u0315\u0316\f\3\2\2\u0316"+
		"\u0317\7.\2\2\u0317\u0319\5r:\2\u0318\u0315\3\2\2\2\u0319\u031c\3\2\2"+
		"\2\u031a\u0318\3\2\2\2\u031a\u031b\3\2\2\2\u031bo\3\2\2\2\u031c\u031a"+
		"\3\2\2\2\u031d\u031e\b9\1\2\u031e\u031f\5r:\2\u031f\u0320\7/\2\2\u0320"+
		"\u0321\5r:\2\u0321\u0327\3\2\2\2\u0322\u0323\f\3\2\2\u0323\u0324\7/\2"+
		"\2\u0324\u0326\5r:\2\u0325\u0322\3\2\2\2\u0326\u0329\3\2\2\2\u0327\u0325"+
		"\3\2\2\2\u0327\u0328\3\2\2\2\u0328q\3\2\2\2\u0329\u0327\3\2\2\2\u032a"+
		"\u0334\5t;\2\u032b\u0334\5|?\2\u032c\u0334\5~@\2\u032d\u0334\5\u0080A"+
		"\2\u032e\u0334\5\u0082B\2\u032f\u0330\7\f\2\2\u0330\u0331\5f\64\2\u0331"+
		"\u0332\7\r\2\2\u0332\u0334\3\2\2\2\u0333\u032a\3\2\2\2\u0333\u032b\3\2"+
		"\2\2\u0333\u032c\3\2\2\2\u0333\u032d\3\2\2\2\u0333\u032e\3\2\2\2\u0333"+
		"\u032f\3\2\2\2\u0334s\3\2\2\2\u0335\u0336\5\u0108\u0085\2\u0336\u0337"+
		"\7\16\2\2\u0337\u0338\5v<\2\u0338\u0339\7\17\2\2\u0339\u033a\7\20\2\2"+
		"\u033a\u033b\5r:\2\u033bu\3\2\2\2\u033c\u0341\5x=\2\u033d\u033e\7\4\2"+
		"\2\u033e\u0340\5x=\2\u033f\u033d\3\2\2\2\u0340\u0343\3\2\2\2\u0341\u033f"+
		"\3\2\2\2\u0341\u0342\3\2\2\2\u0342w\3\2\2\2\u0343\u0341\3\2\2\2\u0344"+
		"\u0347\5z>\2\u0345\u0347\5\u0130\u0099\2\u0346\u0344\3\2\2\2\u0346\u0345"+
		"\3\2\2\2\u0347y\3\2\2\2\u0348\u0349\5\u0130\u0099\2\u0349\u034a\7\20\2"+
		"\2\u034a\u034b\5\u00a6T\2\u034b{\3\2\2\2\u034c\u034d\5\u010e\u0088\2\u034d"+
		"\u034e\5r:\2\u034e\u0351\3\2\2\2\u034f\u0351\5\u00caf\2\u0350\u034c\3"+
		"\2\2\2\u0350\u034f\3\2\2\2\u0351}\3\2\2\2\u0352\u0353\5\u00ccg\2\u0353"+
		"\177\3\2\2\2\u0354\u0355\7\27\2\2\u0355\u0356\5f\64\2\u0356\u0357\7\4"+
		"\2\2\u0357\u0358\5f\64\2\u0358\u0359\7\4\2\2\u0359\u035a\5f\64\2\u035a"+
		"\u035b\7\r\2\2\u035b\u0081\3\2\2\2\u035c\u035d\7\30\2\2\u035d\u035e\5"+
		"\u0084C\2\u035e\u035f\7\4\2\2\u035f\u0360\5d\63\2\u0360\u0361\7\r\2\2"+
		"\u0361\u0369\3\2\2\2\u0362\u0363\7\31\2\2\u0363\u0364\5\u008cG\2\u0364"+
		"\u0365\7\4\2\2\u0365\u0366\5d\63\2\u0366\u0367\7\r\2\2\u0367\u0369\3\2"+
		"\2\2\u0368\u035c\3\2\2\2\u0368\u0362\3\2\2\2\u0369\u0083\3\2\2\2\u036a"+
		"\u0370\5\u0088E\2\u036b\u036c\7\16\2\2\u036c\u036d\5\u0086D\2\u036d\u036e"+
		"\7\17\2\2\u036e\u0370\3\2\2\2\u036f\u036a\3\2\2\2\u036f\u036b\3\2\2\2"+
		"\u0370\u0085\3\2\2\2\u0371\u0376\5\u0088E\2\u0372\u0373\7\4\2\2\u0373"+
		"\u0375\5\u0088E\2\u0374\u0372\3\2\2\2\u0375\u0378\3\2\2\2\u0376\u0374"+
		"\3\2\2\2\u0376\u0377\3\2\2\2\u0377\u0087\3\2\2\2\u0378\u0376\3\2\2\2\u0379"+
		"\u037a\7;\2\2\u037a\u037b\7\16\2\2\u037b\u037c\5v<\2\u037c\u037d\7\17"+
		"\2\2\u037d\u037e\7\20\2\2\u037e\u037f\5\u0088E\2\u037f\u0382\3\2\2\2\u0380"+
		"\u0382\5\u008aF\2\u0381\u0379\3\2\2\2\u0381\u0380\3\2\2\2\u0382\u0089"+
		"\3\2\2\2\u0383\u0384\5\u00d8m\2\u0384\u0385\7:\2\2\u0385\u0386\5\u00e4"+
		"s\2\u0386\u038c\3\2\2\2\u0387\u0388\7\f\2\2\u0388\u0389\5\u008aF\2\u0389"+
		"\u038a\7\r\2\2\u038a\u038c\3\2\2\2\u038b\u0383\3\2\2\2\u038b\u0387\3\2"+
		"\2\2\u038c\u008b\3\2\2\2\u038d\u0393\5\u0090I\2\u038e\u038f\7\16\2\2\u038f"+
		"\u0390\5\u008eH\2\u0390\u0391\7\17\2\2\u0391\u0393\3\2\2\2\u0392\u038d"+
		"\3\2\2\2\u0392\u038e\3\2\2\2\u0393\u008d\3\2\2\2\u0394\u0399\5\u0090I"+
		"\2\u0395\u0396\7\4\2\2\u0396\u0398\5\u0090I\2\u0397\u0395\3\2\2\2\u0398"+
		"\u039b\3\2\2\2\u0399\u0397\3\2\2\2\u0399\u039a\3\2\2\2\u039a\u008f\3\2"+
		"\2\2\u039b\u0399\3\2\2\2\u039c\u039d\7;\2\2\u039d\u039e\7\16\2\2\u039e"+
		"\u039f\5v<\2\u039f\u03a0\7\17\2\2\u03a0\u03a1\7\20\2\2\u03a1\u03a2\5\u0090"+
		"I\2\u03a2\u03a5\3\2\2\2\u03a3\u03a5\5\u0092J\2\u03a4\u039c\3\2\2\2\u03a4"+
		"\u03a3\3\2\2\2\u03a5\u0091\3\2\2\2\u03a6\u03a7\5\u00ceh\2\u03a7\u03a8"+
		"\7\60\2\2\u03a8\u03a9\5r:\2\u03a9\u03af\3\2\2\2\u03aa\u03ab\7\f\2\2\u03ab"+
		"\u03ac\5\u0092J\2\u03ac\u03ad\7\r\2\2\u03ad\u03af\3\2\2\2\u03ae\u03a6"+
		"\3\2\2\2\u03ae\u03aa\3\2\2\2\u03af\u0093\3\2\2\2\u03b0\u03b1\5\u0096L"+
		"\2\u03b1\u03b2\7K\2\2\u03b2\u03b3\5\u0096L\2\u03b3\u03b9\3\2\2\2\u03b4"+
		"\u03b5\7\f\2\2\u03b5\u03b6\5\u0094K\2\u03b6\u03b7\7\r\2\2\u03b7\u03b9"+
		"\3\2\2\2\u03b8\u03b0\3\2\2\2\u03b8\u03b4\3\2\2\2\u03b9\u0095\3\2\2\2\u03ba"+
		"\u03c0\7\23\2\2\u03bb\u03bc\7\16\2\2\u03bc\u03bd\5\u0098M\2\u03bd\u03be"+
		"\7\17\2\2\u03be\u03c0\3\2\2\2\u03bf\u03ba\3\2\2\2\u03bf\u03bb\3\2\2\2"+
		"\u03c0\u0097\3\2\2\2\u03c1\u03c6\5f\64\2\u03c2\u03c3\7\4\2\2\u03c3\u03c5"+
		"\5f\64\2\u03c4\u03c2\3\2\2\2\u03c5\u03c8\3\2\2\2\u03c6\u03c4\3\2\2\2\u03c6"+
		"\u03c7\3\2\2\2\u03c7\u0099\3\2\2\2\u03c8\u03c6\3\2\2\2\u03c9\u03ca\5\u011a"+
		"\u008e\2\u03ca\u03cb\7\20\2\2\u03cb\u03cc\5\u009eP\2\u03cc\u03d2\3\2\2"+
		"\2\u03cd\u03ce\7\f\2\2\u03ce\u03cf\5\u009aN\2\u03cf\u03d0\7\r\2\2\u03d0"+
		"\u03d2\3\2\2\2\u03d1\u03c9\3\2\2\2\u03d1\u03cd\3\2\2\2\u03d2\u009b\3\2"+
		"\2\2\u03d3\u03d4\5\u011a\u008e\2\u03d4\u03d5\7J\2\2\u03d5\u03d6\5\u0118"+
		"\u008d\2\u03d6\u009d\3\2\2\2\u03d7\u03df\5\u00a6T\2\u03d8\u03df\5\u00aa"+
		"V\2\u03d9\u03df\5\u00a0Q\2\u03da\u03db\7\f\2\2\u03db\u03dc\5\u009eP\2"+
		"\u03dc\u03dd\7\r\2\2\u03dd\u03df\3\2\2\2\u03de\u03d7\3\2\2\2\u03de\u03d8"+
		"\3\2\2\2\u03de\u03d9\3\2\2\2\u03de\u03da\3\2\2\2\u03df\u009f\3\2\2\2\u03e0"+
		"\u03e1\78\2\2\u03e1\u03e2\7\16\2\2\u03e2\u03e3\5v<\2\u03e3\u03e4\7\17"+
		"\2\2\u03e4\u03e5\7\20\2\2\u03e5\u03e6\5\u00a2R\2\u03e6\u00a1\3\2\2\2\u03e7"+
		"\u03ed\5\u00a6T\2\u03e8\u03e9\7\f\2\2\u03e9\u03ea\5\u00aaV\2\u03ea\u03eb"+
		"\7\r\2\2\u03eb\u03ed\3\2\2\2\u03ec\u03e7\3\2\2\2\u03ec\u03e8\3\2\2\2\u03ed"+
		"\u00a3\3\2\2\2\u03ee\u03f4\5\u00a6T\2\u03ef\u03f0\7\f\2\2\u03f0\u03f1"+
		"\5\u00acW\2\u03f1\u03f2\7\r\2\2\u03f2\u03f4\3\2\2\2\u03f3\u03ee\3\2\2"+
		"\2\u03f3\u03ef\3\2\2\2\u03f4\u00a5\3\2\2\2\u03f5\u03fe\5\u0110\u0089\2"+
		"\u03f6\u03fe\5\u0114\u008b\2\u03f7\u03f8\5\u0112\u008a\2\u03f8\u03f9\7"+
		"\f\2\2\u03f9\u03fa\5\u00a8U\2\u03fa\u03fb\7\r\2\2\u03fb\u03fe\3\2\2\2"+
		"\u03fc\u03fe\5\u0130\u0099\2\u03fd\u03f5\3\2\2\2\u03fd\u03f6\3\2\2\2\u03fd"+
		"\u03f7\3\2\2\2\u03fd\u03fc\3\2\2\2\u03fe\u00a7\3\2\2\2\u03ff\u0404\5\u00a6"+
		"T\2\u0400\u0401\7\4\2\2\u0401\u0403\5\u00a6T\2\u0402\u0400\3\2\2\2\u0403"+
		"\u0406\3\2\2\2\u0404\u0402\3\2\2\2\u0404\u0405\3\2\2\2\u0405\u00a9\3\2"+
		"\2\2\u0406\u0404\3\2\2\2\u0407\u0408\5\u00a4S\2\u0408\u0409\7G\2\2\u0409"+
		"\u040a\5\u00a6T\2\u040a\u00ab\3\2\2\2\u040b\u040c\bW\1\2\u040c\u040d\5"+
		"\u00a4S\2\u040d\u040e\7H\2\2\u040e\u040f\5\u00a6T\2\u040f\u0415\3\2\2"+
		"\2\u0410\u0411\f\3\2\2\u0411\u0412\7H\2\2\u0412\u0414\5\u00a6T\2\u0413"+
		"\u0410\3\2\2\2\u0414\u0417\3\2\2\2\u0415\u0413\3\2\2\2\u0415\u0416\3\2"+
		"\2\2\u0416\u00ad\3\2\2\2\u0417\u0415\3\2\2\2\u0418\u041b\5\u00b0Y\2\u0419"+
		"\u041b\5\u009aN\2\u041a\u0418\3\2\2\2\u041a\u0419\3\2\2\2\u041b\u00af"+
		"\3\2\2\2\u041c\u041f\5\u00b2Z\2\u041d\u041f\5\u00f4{\2\u041e\u041c\3\2"+
		"\2\2\u041e\u041d\3\2\2\2\u041f\u00b1\3\2\2\2\u0420\u0421\7;\2\2\u0421"+
		"\u0422\7\16\2\2\u0422\u0423\5v<\2\u0423\u0424\7\17\2\2\u0424\u0425\7\20"+
		"\2\2\u0425\u0426\5\u00f4{\2\u0426\u00b3\3\2\2\2\u0427\u042a\5\u00b6\\"+
		"\2\u0428\u042a\5\u00eex\2\u0429\u0427\3\2\2\2\u0429\u0428\3\2\2\2\u042a"+
		"\u00b5\3\2\2\2\u042b\u042e\5\u00b8]\2\u042c\u042e\5\u00c2b\2\u042d\u042b"+
		"\3\2\2\2\u042d\u042c\3\2\2\2\u042e\u00b7\3\2\2\2\u042f\u0432\5\u00ba^"+
		"\2\u0430\u0432\5\u00bc_\2\u0431\u042f\3\2\2\2\u0431\u0430\3\2\2\2\u0432"+
		"\u00b9\3\2\2\2\u0433\u0434\5\u00c2b\2\u0434\u0435\5\u010a\u0086\2\u0435"+
		"\u0436\5\u00c2b\2\u0436\u00bb\3\2\2\2\u0437\u043a\5\u00be`\2\u0438\u043a"+
		"\5\u00c0a\2\u0439\u0437\3\2\2\2\u0439\u0438\3\2\2\2\u043a\u00bd\3\2\2"+
		"\2\u043b\u043c\b`\1\2\u043c\u043d\5\u00c2b\2\u043d\u043e\7.\2\2\u043e"+
		"\u043f\5\u00c2b\2\u043f\u0445\3\2\2\2\u0440\u0441\f\3\2\2\u0441\u0442"+
		"\7.\2\2\u0442\u0444\5\u00c2b\2\u0443\u0440\3\2\2\2\u0444\u0447\3\2\2\2"+
		"\u0445\u0443\3\2\2\2\u0445\u0446\3\2\2\2\u0446\u00bf\3\2\2\2\u0447\u0445"+
		"\3\2\2\2\u0448\u0449\ba\1\2\u0449\u044a\5\u00c2b\2\u044a\u044b\7/\2\2"+
		"\u044b\u044c\5\u00c2b\2\u044c\u0452\3\2\2\2\u044d\u044e\f\3\2\2\u044e"+
		"\u044f\7/\2\2\u044f\u0451\5\u00c2b\2\u0450\u044d\3\2\2\2\u0451\u0454\3"+
		"\2\2\2\u0452\u0450\3\2\2\2\u0452\u0453\3\2\2\2\u0453\u00c1\3\2\2\2\u0454"+
		"\u0452\3\2\2\2\u0455\u045d\5\u00c4c\2\u0456\u045d\5\u00c8e\2\u0457\u045d"+
		"\5\u00ccg\2\u0458\u0459\7\f\2\2\u0459\u045a\5\u00b6\\\2\u045a\u045b\7"+
		"\r\2\2\u045b\u045d\3\2\2\2\u045c\u0455\3\2\2\2\u045c\u0456\3\2\2\2\u045c"+
		"\u0457\3\2\2\2\u045c\u0458\3\2\2\2\u045d\u00c3\3\2\2\2\u045e\u045f\5\u0108"+
		"\u0085\2\u045f\u0460\7\16\2\2\u0460\u0461\5\u00c6d\2\u0461\u0462\7\17"+
		"\2\2\u0462\u0463\7\20\2\2\u0463\u0464\5\u00c2b\2\u0464\u00c5\3\2\2\2\u0465"+
		"\u046a\5\u0130\u0099\2\u0466\u0467\7\4\2\2\u0467\u0469\5\u0130\u0099\2"+
		"\u0468\u0466\3\2\2\2\u0469\u046c\3\2\2\2\u046a\u0468\3\2\2\2\u046a\u046b"+
		"\3\2\2\2\u046b\u00c7\3\2\2\2\u046c\u046a\3\2\2\2\u046d\u046e\5\u010e\u0088"+
		"\2\u046e\u046f\5\u00c2b\2\u046f\u0472\3\2\2\2\u0470\u0472\5\u00caf\2\u0471"+
		"\u046d\3\2\2\2\u0471\u0470\3\2\2\2\u0472\u00c9\3\2\2\2\u0473\u0474\5\u00e4"+
		"s\2\u0474\u0475\79\2\2\u0475\u0476\5\u00e4s\2\u0476\u00cb\3\2\2\2\u0477"+
		"\u047b\5\u00ceh\2\u0478\u047b\5\u00d0i\2\u0479\u047b\5\u00d6l\2\u047a"+
		"\u0477\3\2\2\2\u047a\u0478\3\2\2\2\u047a\u0479\3\2\2\2\u047b\u00cd\3\2"+
		"\2\2\u047c\u047d\5\u00d8m\2\u047d\u00cf\3\2\2\2\u047e\u0481\5\u00d2j\2"+
		"\u047f\u0481\5\u00d4k\2\u0480\u047e\3\2\2\2\u0480\u047f\3\2\2\2\u0481"+
		"\u00d1\3\2\2\2\u0482\u0483\5\u00dan\2\u0483\u00d3\3\2\2\2\u0484\u0485"+
		"\5\u00e4s\2\u0485\u0486\5\u0120\u0091\2\u0486\u0487\5\u00e4s\2\u0487\u00d5"+
		"\3\2\2\2\u0488\u0489\5\u00e0q\2\u0489\u00d7\3\2\2\2\u048a\u0491\5\u0122"+
		"\u0092\2\u048b\u048c\5\u0124\u0093\2\u048c\u048d\7\f\2\2\u048d\u048e\5"+
		"\u00e2r\2\u048e\u048f\7\r\2\2\u048f\u0491\3\2\2\2\u0490\u048a\3\2\2\2"+
		"\u0490\u048b\3\2\2\2\u0491\u00d9\3\2\2\2\u0492\u0495\5\u012e\u0098\2\u0493"+
		"\u0495\5\u00dco\2\u0494\u0492\3\2\2\2\u0494\u0493\3\2\2\2\u0495\u00db"+
		"\3\2\2\2\u0496\u0497\5\u00dep\2\u0497\u00dd\3\2\2\2\u0498\u049f\5\u012a"+
		"\u0096\2\u0499\u049a\5\u012c\u0097\2\u049a\u049b\7\f\2\2\u049b\u049c\5"+
		"\u00e2r\2\u049c\u049d\7\r\2\2\u049d\u049f\3\2\2\2\u049e\u0498\3\2\2\2"+
		"\u049e\u0499\3\2\2\2\u049f\u00df\3\2\2\2\u04a0\u04a7\5\u0126\u0094\2\u04a1"+
		"\u04a2\5\u0128\u0095\2\u04a2\u04a3\7\f\2\2\u04a3\u04a4\5\u00e2r\2\u04a4"+
		"\u04a5\7\r\2\2\u04a5\u04a7\3\2\2\2\u04a6\u04a0\3\2\2\2\u04a6\u04a1\3\2"+
		"\2\2\u04a7\u00e1\3\2\2\2\u04a8\u04ad\5\u00e4s\2\u04a9\u04aa\7\4\2\2\u04aa"+
		"\u04ac\5\u00e4s\2\u04ab\u04a9\3\2\2\2\u04ac\u04af\3\2\2\2\u04ad\u04ab"+
		"\3\2\2\2\u04ad\u04ae\3\2\2\2\u04ae\u00e3\3\2\2\2\u04af\u04ad\3\2\2\2\u04b0"+
		"\u04b6\5\u00e6t\2\u04b1\u04b6\5\u0130\u0099\2\u04b2\u04b6\5\u00e8u\2\u04b3"+
		"\u04b6\5\u00eav\2\u04b4\u04b6\5\u00ecw\2\u04b5\u04b0\3\2\2\2\u04b5\u04b1"+
		"\3\2\2\2\u04b5\u04b2\3\2\2\2\u04b5\u04b3\3\2\2\2\u04b5\u04b4\3\2\2\2\u04b6"+
		"\u00e5\3\2\2\2\u04b7\u04bb\5\u00d8m\2\u04b8\u04bb\5\u00dan\2\u04b9\u04bb"+
		"\5\u00e0q\2\u04ba\u04b7\3\2\2\2\u04ba\u04b8\3\2\2\2\u04ba\u04b9\3\2\2"+
		"\2\u04bb\u00e7\3\2\2\2\u04bc\u04bd\7\32\2\2\u04bd\u04be\5f\64\2\u04be"+
		"\u04bf\7\4\2\2\u04bf\u04c0\5\u00e4s\2\u04c0\u04c1\7\4\2\2\u04c1\u04c2"+
		"\5\u00e4s\2\u04c2\u04c3\7\r\2\2\u04c3\u00e9\3\2\2\2\u04c4\u04c5\7\33\2"+
		"\2\u04c5\u04c6\5\u008cG\2\u04c6\u04c7\7\4\2\2\u04c7\u04c8\5\u00e4s\2\u04c8"+
		"\u04c9\7\r\2\2\u04c9\u04d1\3\2\2\2\u04ca\u04cb\7\34\2\2\u04cb\u04cc\5"+
		"\u0084C\2\u04cc\u04cd\7\4\2\2\u04cd\u04ce\5\u00e4s\2\u04ce\u04cf\7\r\2"+
		"\2\u04cf\u04d1\3\2\2\2\u04d0\u04c4\3\2\2\2\u04d0\u04ca\3\2\2\2\u04d1\u00eb"+
		"\3\2\2\2\u04d2\u04d8\7\24\2\2\u04d3\u04d4\7\25\2\2\u04d4\u04d5\5\u00e2"+
		"r\2\u04d5\u04d6\7\26\2\2\u04d6\u04d8\3\2\2\2\u04d7\u04d2\3\2\2\2\u04d7"+
		"\u04d3\3\2\2\2\u04d8\u00ed\3\2\2\2\u04d9\u04da\5\u00f0y\2\u04da\u04db"+
		"\7K\2\2\u04db\u04dc\5\u00f0y\2\u04dc\u04e2\3\2\2\2\u04dd\u04de\7\f\2\2"+
		"\u04de\u04df\5\u00eex\2\u04df\u04e0\7\r\2\2\u04e0\u04e2\3\2\2\2\u04e1"+
		"\u04d9\3\2\2\2\u04e1\u04dd\3\2\2\2\u04e2\u00ef\3\2\2\2\u04e3\u04e9\7\23"+
		"\2\2\u04e4\u04e5\7\16\2\2\u04e5\u04e6\5\u00f2z\2\u04e6\u04e7\7\17\2\2"+
		"\u04e7\u04e9\3\2\2\2\u04e8\u04e3\3\2\2\2\u04e8\u04e4\3\2\2\2\u04e9\u00f1"+
		"\3\2\2\2\u04ea\u04ef\5\u00b6\\\2\u04eb\u04ec\7\4\2\2\u04ec\u04ee\5\u00b6"+
		"\\\2\u04ed\u04eb\3\2\2\2\u04ee\u04f1\3\2\2\2\u04ef\u04ed\3\2\2\2\u04ef"+
		"\u04f0\3\2\2\2\u04f0\u00f3\3\2\2\2\u04f1\u04ef\3\2\2\2\u04f2\u04f8\5\u00f6"+
		"|\2\u04f3\u04f4\7\f\2\2\u04f4\u04f5\5\u00f6|\2\u04f5\u04f6\7\r\2\2\u04f6"+
		"\u04f8\3\2\2\2\u04f7\u04f2\3\2\2\2\u04f7\u04f3\3\2\2\2\u04f8\u00f5\3\2"+
		"\2\2\u04f9\u04fa\b|\1\2\u04fa\u04fb\5\u00f8}\2\u04fb\u0501\3\2\2\2\u04fc"+
		"\u04fd\f\3\2\2\u04fd\u04fe\7.\2\2\u04fe\u0500\5\u00f8}\2\u04ff\u04fc\3"+
		"\2\2\2\u0500\u0503\3\2\2\2\u0501\u04ff\3\2\2\2\u0501\u0502\3\2\2\2\u0502"+
		"\u00f7\3\2\2\2\u0503\u0501\3\2\2\2\u0504\u0509\5\u00ccg\2\u0505\u0506"+
		"\7\66\2\2\u0506\u0509\5\u00ccg\2\u0507\u0509\5\u00caf\2\u0508\u0504\3"+
		"\2\2\2\u0508\u0505\3\2\2\2\u0508\u0507\3\2\2\2\u0509\u00f9\3\2\2\2\u050a"+
		"\u050e\5\u0108\u0085\2\u050b\u050e\5\u00fc\177\2\u050c\u050e\5\u00fe\u0080"+
		"\2\u050d\u050a\3\2\2\2\u050d\u050b\3\2\2\2\u050d\u050c\3\2\2\2\u050e\u00fb"+
		"\3\2\2\2\u050f\u0510\t\2\2\2\u0510\u00fd\3\2\2\2\u0511\u0512\t\3\2\2\u0512"+
		"\u00ff\3\2\2\2\u0513\u0518\7:\2\2\u0514\u0518\79\2\2\u0515\u0518\5\u010a"+
		"\u0086\2\u0516\u0518\7F\2\2\u0517\u0513\3\2\2\2\u0517\u0514\3\2\2\2\u0517"+
		"\u0515\3\2\2\2\u0517\u0516\3\2\2\2\u0518\u0101\3\2\2\2\u0519\u051c\5\u010e"+
		"\u0088\2\u051a\u051c\5\u0104\u0083\2\u051b\u0519\3\2\2\2\u051b\u051a\3"+
		"\2\2\2\u051c\u0103\3\2\2\2\u051d\u051e\t\4\2\2\u051e\u0105\3\2\2\2\u051f"+
		"\u0522\5\u010a\u0086\2\u0520\u0522\7F\2\2\u0521\u051f\3\2\2\2\u0521\u0520"+
		"\3\2\2\2\u0522\u0107\3\2\2\2\u0523\u0524\t\5\2\2\u0524\u0109\3\2\2\2\u0525"+
		"\u0526\t\6\2\2\u0526\u010b\3\2\2\2\u0527\u0528\t\7\2\2\u0528\u010d\3\2"+
		"\2\2\u0529\u052a\7\66\2\2\u052a\u010f\3\2\2\2\u052b\u052c\5\u0112\u008a"+
		"\2\u052c\u0111\3\2\2\2\u052d\u052e\5\u018a\u00c6\2\u052e\u0113\3\2\2\2"+
		"\u052f\u0530\7]\2\2\u0530\u0115\3\2\2\2\u0531\u0532\5\u018e\u00c8\2\u0532"+
		"\u0117\3\2\2\2\u0533\u0536\5\u011a\u008e\2\u0534\u0536\5\u012a\u0096\2"+
		"\u0535\u0533\3\2\2\2\u0535\u0534\3\2\2\2\u0536\u0119\3\2\2\2\u0537\u053a"+
		"\5\u0122\u0092\2\u0538\u053a\5\u0126\u0094\2\u0539\u0537\3\2\2\2\u0539"+
		"\u0538\3\2\2\2\u053a\u011b\3\2\2\2\u053b\u053c\7]\2\2\u053c\u011d\3\2"+
		"\2\2\u053d\u053e\7]\2\2\u053e\u011f\3\2\2\2\u053f\u0540\t\b\2\2\u0540"+
		"\u0121\3\2\2\2\u0541\u0542\5\u0124\u0093\2\u0542\u0123\3\2\2\2\u0543\u0544"+
		"\5\u018a\u00c6\2\u0544\u0125\3\2\2\2\u0545\u0546\5\u0128\u0095\2\u0546"+
		"\u0127\3\2\2\2\u0547\u0548\5\u018e\u00c8\2\u0548\u0129\3\2\2\2\u0549\u054a"+
		"\5\u012c\u0097\2\u054a\u012b\3\2\2\2\u054b\u054c\5\u018c\u00c7\2\u054c"+
		"\u012d\3\2\2\2\u054d\u0550\5\u0190\u00c9\2\u054e\u0550\7b\2\2\u054f\u054d"+
		"\3\2\2\2\u054f\u054e\3\2\2\2\u0550\u012f\3\2\2\2\u0551\u0552\7_\2\2\u0552"+
		"\u0131\3\2\2\2\u0553\u055c\5\u0136\u009c\2\u0554\u055c\5\u0144\u00a3\2"+
		"\u0555\u055c\5\u0148\u00a5\2\u0556\u055c\7`\2\2\u0557\u0558\7\16\2\2\u0558"+
		"\u0559\5\u0134\u009b\2\u0559\u055a\7\17\2\2\u055a\u055c\3\2\2\2\u055b"+
		"\u0553\3\2\2\2\u055b\u0554\3\2\2\2\u055b\u0555\3\2\2\2\u055b\u0556\3\2"+
		"\2\2\u055b\u0557\3\2\2\2\u055c\u0133\3\2\2\2\u055d\u0562\5\u0132\u009a"+
		"\2\u055e\u055f\7\4\2\2\u055f\u0561\5\u0132\u009a\2\u0560\u055e\3\2\2\2"+
		"\u0561\u0564\3\2\2\2\u0562\u0560\3\2\2\2\u0562\u0563\3\2\2\2\u0563\u0135"+
		"\3\2\2\2\u0564\u0562\3\2\2\2\u0565\u0568\5\u0188\u00c5\2\u0566\u0568\5"+
		"\u0138\u009d\2\u0567\u0565\3\2\2\2\u0567\u0566\3\2\2\2\u0568\u0137\3\2"+
		"\2\2\u0569\u056a\7\35\2\2\u056a\u056b\5\u013a\u009e\2\u056b\u056c\7\4"+
		"\2\2\u056c\u056d\5\u0158\u00ad\2\u056d\u056e\7\4\2\2\u056e\u056f\5\u013c"+
		"\u009f\2\u056f\u0570\7\r\2\2\u0570\u0139\3\2\2\2\u0571\u0572\5\u018a\u00c6"+
		"\2\u0572\u013b\3\2\2\2\u0573\u0579\7\23\2\2\u0574\u0575\7\16\2\2\u0575"+
		"\u0576\5\u013e\u00a0\2\u0576\u0577\7\17\2\2\u0577\u0579\3\2\2\2\u0578"+
		"\u0573\3\2\2\2\u0578\u0574\3\2\2\2\u0579\u013d\3\2\2\2\u057a\u057f\5\u0140"+
		"\u00a1\2\u057b\u057c\7\4\2\2\u057c\u057e\5\u0140\u00a1\2\u057d\u057b\3"+
		"\2\2\2\u057e\u0581\3\2\2\2\u057f\u057d\3\2\2\2\u057f\u0580\3\2\2\2\u0580"+
		"\u013f\3\2\2\2\u0581\u057f\3\2\2\2\u0582\u0584\5\u0132\u009a\2\u0583\u0585"+
		"\5\u0142\u00a2\2\u0584\u0583\3\2\2\2\u0584\u0585\3\2\2\2\u0585\u0141\3"+
		"\2\2\2\u0586\u0587\7\20\2\2\u0587\u0588\5\u0184\u00c3\2\u0588\u0143\3"+
		"\2\2\2\u0589\u058a\7\36\2\2\u058a\u058c\5\u0146\u00a4\2\u058b\u058d\5"+
		"\u0156\u00ac\2\u058c\u058b\3\2\2\2\u058c\u058d\3\2\2\2\u058d\u058e\3\2"+
		"\2\2\u058e\u058f\7\r\2\2\u058f\u0145\3\2\2\2\u0590\u0591\7`\2\2\u0591"+
		"\u0147\3\2\2\2\u0592\u0596\5\u014a\u00a6\2\u0593\u0596\5\u014e\u00a8\2"+
		"\u0594\u0596\5\u0152\u00aa\2\u0595\u0592\3\2\2\2\u0595\u0593\3\2\2\2\u0595"+
		"\u0594\3\2\2\2\u0596\u0149\3\2\2\2\u0597\u0598\7\37\2\2\u0598\u059a\5"+
		"\u0192\u00ca\2\u0599\u059b\5\u014c\u00a7\2\u059a\u0599\3\2\2\2\u059a\u059b"+
		"\3\2\2\2\u059b\u059c\3\2\2\2\u059c\u059d\7\r\2\2\u059d\u014b\3\2\2\2\u059e"+
		"\u059f\7\4\2\2\u059f\u05a0\5\u0188\u00c5\2\u05a0\u014d\3\2\2\2\u05a1\u05a2"+
		"\7 \2\2\u05a2\u05a4\5\u0150\u00a9\2\u05a3\u05a5\5\u0156\u00ac\2\u05a4"+
		"\u05a3\3\2\2\2\u05a4\u05a5\3\2\2\2\u05a5\u05a6\3\2\2\2\u05a6\u05a7\7\r"+
		"\2\2\u05a7\u014f\3\2\2\2\u05a8\u05a9\7`\2\2\u05a9\u0151\3\2\2\2\u05aa"+
		"\u05ab\7!\2\2\u05ab\u05ad\5\u0154\u00ab\2\u05ac\u05ae\5\u0156\u00ac\2"+
		"\u05ad\u05ac\3\2\2\2\u05ad\u05ae\3\2\2\2\u05ae\u05af\3\2\2\2\u05af\u05b0"+
		"\7\r\2\2\u05b0\u0153\3\2\2\2\u05b1\u05b2\5\u018a\u00c6\2\u05b2\u0155\3"+
		"\2\2\2\u05b3\u05b4\7\4\2\2\u05b4\u05b5\5\u0158\u00ad\2\u05b5\u0157\3\2"+
		"\2\2\u05b6\u05bd\7\23\2\2\u05b7\u05b8\7\16\2\2\u05b8\u05b9\5\u015a\u00ae"+
		"\2\u05b9\u05ba\7\17\2\2\u05ba\u05bd\3\2\2\2\u05bb\u05bd\5\u0184\u00c3"+
		"\2\u05bc\u05b6\3\2\2\2\u05bc\u05b7\3\2\2\2\u05bc\u05bb\3\2\2\2\u05bd\u0159"+
		"\3\2\2\2\u05be\u05c3\5\u015c\u00af\2\u05bf\u05c0\7\4\2\2\u05c0\u05c2\5"+
		"\u015c\u00af\2\u05c1\u05bf\3\2\2\2\u05c2\u05c5\3\2\2\2\u05c3\u05c1\3\2"+
		"\2\2\u05c3\u05c4\3\2\2\2\u05c4\u015b\3\2\2\2\u05c5\u05c3\3\2\2\2\u05c6"+
		"\u05ca\5\u015e\u00b0\2\u05c7\u05ca\5\u0164\u00b3\2\u05c8\u05ca\5\u0180"+
		"\u00c1\2\u05c9\u05c6\3\2\2\2\u05c9\u05c7\3\2\2\2\u05c9\u05c8\3\2\2\2\u05ca"+
		"\u015d\3\2\2\2\u05cb\u05ce\5\u0160\u00b1\2\u05cc\u05ce\5\u0162\u00b2\2"+
		"\u05cd\u05cb\3\2\2\2\u05cd\u05cc\3\2\2\2\u05ce\u015f\3\2\2\2\u05cf\u05d0"+
		"\7\"\2\2\u05d0\u05d1\5\u018a\u00c6\2\u05d1\u05d2\7\r\2\2\u05d2\u0161\3"+
		"\2\2\2\u05d3\u05d4\7#\2\2\u05d4\u05d5\5\u018a\u00c6\2\u05d5\u05d6\7\r"+
		"\2\2\u05d6\u0163\3\2\2\2\u05d7\u05dc\5\u0166\u00b4\2\u05d8\u05dc\5\u016c"+
		"\u00b7\2\u05d9\u05dc\5\u0170\u00b9\2\u05da\u05dc\5\u016e\u00b8\2\u05db"+
		"\u05d7\3\2\2\2\u05db\u05d8\3\2\2\2\u05db\u05d9\3\2\2\2\u05db\u05da\3\2"+
		"\2\2\u05dc\u0165\3\2\2\2\u05dd\u05de\7$\2\2\u05de\u05df\5\u0168\u00b5"+
		"\2\u05df\u05e0\7\r\2\2\u05e0\u05e3\3\2\2\2\u05e1\u05e3\5\u016a\u00b6\2"+
		"\u05e2\u05dd\3\2\2\2\u05e2\u05e1\3\2\2\2\u05e3\u0167\3\2\2\2\u05e4\u05e5"+
		"\7`\2\2\u05e5\u0169\3\2\2\2\u05e6\u05e7\5\u013a\u009e\2\u05e7\u05e8\7"+
		"\f\2\2\u05e8\u05e9\5\u018a\u00c6\2\u05e9\u05ea\7\4\2\2\u05ea\u05eb\5\u0184"+
		"\u00c3\2\u05eb\u05ec\7\r\2\2\u05ec\u016b\3\2\2\2\u05ed\u05ee\7%\2\2\u05ee"+
		"\u05ef\7\16\2\2\u05ef\u05f0\5\u017a\u00be\2\u05f0\u05f1\7\17\2\2\u05f1"+
		"\u05f2\7\r\2\2\u05f2\u016d\3\2\2\2\u05f3\u05f4\7&\2\2\u05f4\u05f5\5\u014a"+
		"\u00a6\2\u05f5\u05f6\7\r\2\2\u05f6\u016f\3\2\2\2\u05f7\u05f8\7\'\2\2\u05f8"+
		"\u05f9\5\u018a\u00c6\2\u05f9\u05fa\7\4\2\2\u05fa\u05fb\7\16\2\2\u05fb"+
		"\u05fc\5\u0172\u00ba\2\u05fc\u05fd\7\17\2\2\u05fd\u05fe\7\r\2\2\u05fe"+
		"\u0171\3\2\2\2\u05ff\u0604\5\u0174\u00bb\2\u0600\u0601\7\4\2\2\u0601\u0603"+
		"\5\u0174\u00bb\2\u0602\u0600\3\2\2\2\u0603\u0606\3\2\2\2\u0604\u0602\3"+
		"\2\2\2\u0604\u0605\3\2\2\2\u0605\u0173\3\2\2\2\u0606\u0604\3\2\2\2\u0607"+
		"\u060a\5\u0124\u0093\2\u0608\u060a\5\u0130\u0099\2\u0609\u0607\3\2\2\2"+
		"\u0609\u0608\3\2\2\2\u060a\u0175\3\2\2\2\u060b\u060c\7(\2\2\u060c\u060e"+
		"\5\u0192\u00ca\2\u060d\u060f\5\u0178\u00bd\2\u060e\u060d\3\2\2\2\u060e"+
		"\u060f\3\2\2\2\u060f\u0610\3\2\2\2\u0610\u0611\7\5\2\2\u0611\u0177\3\2"+
		"\2\2\u0612\u0613\7\4\2\2\u0613\u0614\7\16\2\2\u0614\u0615\5\u017a\u00be"+
		"\2\u0615\u0616\7\17\2\2\u0616\u0179\3\2\2\2\u0617\u061c\5\u0188\u00c5"+
		"\2\u0618\u0619\7\4\2\2\u0619\u061b\5\u0188\u00c5\2\u061a\u0618\3\2\2\2"+
		"\u061b\u061e\3\2\2\2\u061c\u061a\3\2\2\2\u061c\u061d\3\2\2\2\u061d\u017b"+
		"\3\2\2\2\u061e\u061c\3\2\2\2\u061f\u0626\5\u017e\u00c0\2\u0620\u0621\5"+
		"\u017e\u00c0\2\u0621\u0622\7\20\2\2\u0622\u0623\5\u017c\u00bf\2\u0623"+
		"\u0626\3\2\2\2\u0624\u0626\5\u0184\u00c3\2\u0625\u061f\3\2\2\2\u0625\u0620"+
		"\3\2\2\2\u0625\u0624\3\2\2\2\u0626\u017d\3\2\2\2\u0627\u062e\5\u018a\u00c6"+
		"\2\u0628\u062e\5\u0180\u00c1\2\u0629\u062e\5\u0130\u0099\2\u062a\u062e"+
		"\5\u0190\u00c9\2\u062b\u062e\7b\2\2\u062c\u062e\5\u0182\u00c2\2\u062d"+
		"\u0627\3\2\2\2\u062d\u0628\3\2\2\2\u062d\u0629\3\2\2\2\u062d\u062a\3\2"+
		"\2\2\u062d\u062b\3\2\2\2\u062d\u062c\3\2\2\2\u062e\u017f\3\2\2\2\u062f"+
		"\u0630\5\u018a\u00c6\2\u0630\u0631\7\f\2\2\u0631\u0632\5\u0186\u00c4\2"+
		"\u0632\u0633\7\r\2\2\u0633\u0181\3\2\2\2\u0634\u0635\7)\2\2\u0635\u0636"+
		"\5\34\17\2\u0636\u0637\7\r\2\2\u0637\u0649\3\2\2\2\u0638\u0639\7*\2\2"+
		"\u0639\u063a\5d\63\2\u063a\u063b\7\r\2\2\u063b\u0649\3\2\2\2\u063c\u063d"+
		"\7+\2\2\u063d\u063e\5\u00b4[\2\u063e\u063f\7\r\2\2\u063f\u0649\3\2\2\2"+
		"\u0640\u0641\7,\2\2\u0641\u0642\5\u00f4{\2\u0642\u0643\7\r\2\2\u0643\u0649"+
		"\3\2\2\2\u0644\u0645\7-\2\2\u0645\u0646\5\u00e4s\2\u0646\u0647\7\r\2\2"+
		"\u0647\u0649\3\2\2\2\u0648\u0634\3\2\2\2\u0648\u0638\3\2\2\2\u0648\u063c"+
		"\3\2\2\2\u0648\u0640\3\2\2\2\u0648\u0644\3\2\2\2\u0649\u0183\3\2\2\2\u064a"+
		"\u0650\7\23\2\2\u064b\u064c\7\16\2\2\u064c\u064d\5\u0186\u00c4\2\u064d"+
		"\u064e\7\17\2\2\u064e\u0650\3\2\2\2\u064f\u064a\3\2\2\2\u064f\u064b\3"+
		"\2\2\2\u0650\u0185\3\2\2\2\u0651\u0656\5\u017c\u00bf\2\u0652\u0653\7\4"+
		"\2\2\u0653\u0655\5\u017c\u00bf\2\u0654\u0652\3\2\2\2\u0655\u0658\3\2\2"+
		"\2\u0656\u0654\3\2\2\2\u0656\u0657\3\2\2\2\u0657\u0187\3\2\2\2\u0658\u0656"+
		"\3\2\2\2\u0659\u065c\5\u018a\u00c6\2\u065a\u065c\7R\2\2\u065b\u0659\3"+
		"\2\2\2\u065b\u065a\3\2\2\2\u065c\u0189\3\2\2\2\u065d\u065e\t\t\2\2\u065e"+
		"\u018b\3\2\2\2\u065f\u0660\7]\2\2\u0660\u018d\3\2\2\2\u0661\u0662\7^\2"+
		"\2\u0662\u018f\3\2\2\2\u0663\u0664\t\n\2\2\u0664\u0191\3\2\2\2\u0665\u0666"+
		"\7a\2\2\u0666\u0193\3\2\2\2\177\u0197\u019e\u01a7\u01b0\u01bd\u01c8\u01d3"+
		"\u01de\u01e9\u01f4\u01fb\u0201\u0207\u020c\u0215\u0221\u022e\u023b\u0248"+
		"\u0258\u025d\u026c\u027e\u0283\u029e\u02a7\u02b0\u02ba\u02c6\u02d3\u02de"+
		"\u02ea\u02f1\u02f6\u02fd\u0302\u0306\u030e\u031a\u0327\u0333\u0341\u0346"+
		"\u0350\u0368\u036f\u0376\u0381\u038b\u0392\u0399\u03a4\u03ae\u03b8\u03bf"+
		"\u03c6\u03d1\u03de\u03ec\u03f3\u03fd\u0404\u0415\u041a\u041e\u0429\u042d"+
		"\u0431\u0439\u0445\u0452\u045c\u046a\u0471\u047a\u0480\u0490\u0494\u049e"+
		"\u04a6\u04ad\u04b5\u04ba\u04d0\u04d7\u04e1\u04e8\u04ef\u04f7\u0501\u0508"+
		"\u050d\u0517\u051b\u0521\u0535\u0539\u054f\u055b\u0562\u0567\u0578\u057f"+
		"\u0584\u058c\u0595\u059a\u05a4\u05ad\u05bc\u05c3\u05c9\u05cd\u05db\u05e2"+
		"\u0604\u0609\u060e\u061c\u0625\u062d\u0648\u064f\u0656\u065b";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}