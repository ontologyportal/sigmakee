/*  This is a grammar file for TPTP language Version 3.1.1.13
 *  Use ANTLR on this file to obtain the parser.
 *  The BNF specification of the TPTP language was provided by Geoff Sutcliffe.
 *  The grammar file is created by Andrei Tchaltsev.
 *  Date: Jan 2006
 *
 *  Intended uses of the various parts of the TPTP syntax are explained
 *  in the TPTP technical manual, linked from www.tptp.org.
 *
 *  Last revision: Apr 6, 2006, by Alexandre Riazanov, minor clean-up
 *                 and line numbers in the output.
 *  Last revision: Nov 13, 2007, by Alexandre Riazanov, package declaration
 *                 added.
 */
/*======================================================*/
/*================== PARSER ============================*/
/*======================================================*/
header  {
  package tptp_parser;

  import java.util.List;
  import java.util.LinkedList;
}

class TptpParser extends Parser;
options {
  exportVocab = Tptp;
  defaultErrorHandler=false;
}

topLevelItem [TptpParserOutput out]
  returns [TptpParserOutput.TptpInput in = null]
  : in = tptp_input[out]
  | EOF
  ;

/* ------------------------------------------------------- */
/* ----------------- FORMULAS ----------------------------- */
/* ------------------------------------------------------- */

number [TptpParserOutput out]
  returns [String str]
  : // str = sign
    ( i:INTEGER  
      { str = new String(i.getText());
      }
    | u:UNSIGNED_INTEGER  
      { str = new String(u.getText());
      }
    | a:RATIONAL
      { str = new String(a.getText());
      }
    | r:REAL
      { str = new String(r.getText());
      }
    )
  ;
//sign returns [String str]
//      : /* nothing */{str=new String("+");/* normalisation: '+' is added */}
//      | p:PLUS       {str=new String("+");}
//      | m:MINUS      {str=new String("-");}
//      ;

//---- Identifiers ----
name [TptpParserOutput out]
  returns [String str]
  : str=atomic_word[out]
  | us:UNSIGNED_INTEGER
    { str=us.getText();
    }
  ;
atomic_word  [TptpParserOutput out]
  returns [String str]
  : lw:LOWER_WORD
    { str=lw.getText();
    }
  | sq:SINGLE_QUOTED
    { str=sq.getText();
    }
  ;
atomic_defined_word [TptpParserOutput out]
  returns [String str]
  : adw:DOLLAR_WORD
    { str=adw.getText();
    }
  ;
atomic_system_word [TptpParserOutput out]
  returns [String str]
  : asw:DOLLAR_DOLLAR_WORD
    { str=asw.getText();
    }
  ;
thf_conn_term [TptpParserOutput out]
  returns [String str]
  : AND { str="&"; }
  | VLINE { str="|"; }
//----These are done in thf_atom_or_unary_formula
//  | TILDA { str="~"; }
//  | SMALLPI { str="!!"; }
//  | SMALLSIGMA { str="??"; }
  | EQUAL { str="="; }
  | IMPLICATION { str="=>"; }
  | DISEQUIVALENCE { str="<~>"; }
  | NOT_OR { str="~|"; }
  | NOT_AND { str="~&"; }
  | EQUIVALENCE { str="<="; }
  | REVERSE_IMPLICATION { str="<="; }
  ;
//----Terms
term  [TptpParserOutput out]
  returns [TptpParserOutput.Term t]
  { String var;
  }
  : t = plain_term[out]
  | t = defined_term[out]
  | t = system_term[out]
  | var = variable[out]
    { t = out.createVariableTerm(var);
    }
  ;
plain_term  [TptpParserOutput out]
  returns [TptpParserOutput.Term t]
  { String str;
    List<TptpParserOutput.Term> args;
  }
  : str = atomic_word[out]
    ( { args = null;
      }
    | LPR 
      args = arguments[out] 
      RPR
    )
    { t = out.createPlainTerm(str, args);
    }
  ;
arguments [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.Term> args]
  { TptpParserOutput.Term t;
  }
  : t = term[out]
    ( { args = new LinkedList<TptpParserOutput.Term>();
      }
    | COMMA 
      args = arguments[out]
    ) 
    { args.addFirst(t);
    }
  ;
defined_term [TptpParserOutput out]
  returns [TptpParserOutput.Term t]
  { String str;
    List<TptpParserOutput.Term> args = null;
  }
  : ( str = number[out]
    | dq:DISTINCT_OBJECT
      { str = dq.getText();
      }
    | str = atomic_defined_word[out]
      ( /* constant */
      | LPR 
        args = arguments[out] 
        RPR
      )
    ) 
  { t = out.createPlainTerm(str, args);
  }
  ;
system_term [TptpParserOutput out]
  returns [TptpParserOutput.Term t]
  { String str;
    List<TptpParserOutput.Term> args;
  }
  : str = atomic_system_word[out]
    ( /* constant */ {args = null;}
    | LPR 
      args=arguments[out] 
      RPR /* functor with arguments */
    )
  { t = out.createSystemTerm(str, args);
  }
  ;
variable [TptpParserOutput out]
  returns [String str]
  : up:UPPER_WORD   
    { str = up.getText();
    }
  ;

//----Atoms
/* This function sets polarity[0] to false if inequality is
   converted to equality and therefore negation is required.
   An array is used since I do not know how to returns two values (or pass
   a pointer to simple type)
*/
atomic_formula [TptpParserOutput out, boolean[] polarity]
  returns [TptpParserOutput.AtomicFormula af]
  { String str;
    TptpParserOutput.Term t1;
    TptpParserOutput.Term t2;
    List<TptpParserOutput.Term> args;
  }
  : /* defined atom */
    { LT(1).getText().equals("$true")
    }? DOLLAR_WORD
      { af = out.builtInTrue();
      }
  | { LT(1).getText().equals("$false")}? DOLLAR_WORD
                                        {af = out.builtInFalse();}
  | ( str = atomic_defined_word[out]
    | str = atomic_word[out]
    )
    ( /* proposition or constant */            {args = null;}
    | LPR args = arguments[out] RPR
    )
    ( /* proposition or predicate, i.e. plain atom.*/
                             {af = out.createPlainAtom(str, args);}
    | /* term !=(=) term, i.e defined atom */
                             {t1 = out.createPlainTerm(str, args);}
      ( EQUAL
      | NOTEQUAL    {polarity[0] = false;}
      )
      t2 = term[out]         {af = out.createEqualityAtom(t1, t2);}
    )
  | str = atomic_system_word[out]
    ( /* constant */ {args = null;}
    | LPR args=arguments[out] RPR /* functor with arguments */
    )
    ( /* system proposition or predicate, i.e. system atom */
                        {af = out.createSystemAtom(str, args);}
    | /* term !=(=) term, i.e defined atom */
                        {t1 = out.createSystemTerm(str, args);}
    ( EQUAL
    | NOTEQUAL          {polarity[0] = false;}
    )
    t2 = term[out]      {af = out.createEqualityAtom(t1, t2);}
    )
  /* the remaining parts of <term> and <!=(=) term> */
  | ( ( str = number[out]
      | dq:DISTINCT_OBJECT     {str = dq.getText();}
      )                   {t1 = out.createPlainTerm(str, null);}
    | str = variable[out] {t1 = out.createVariableTerm(str);}
    )
    ( EQUAL
    | NOTEQUAL            {polarity[0] = false;}
    )
    t2 = term[out]        {af = out.createEqualityAtom(t1, t2);}
  ;

//----THF formulae
thf_formula  [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula thf]
  { TptpParserOutput.BinaryConnective bc;
    TptpParserOutput.ThfFormula thf_2;
  }
  : thf = thf_unitary_formula[out]
    ( /* a unitary formula */
    | bc = thf_pair_connective 
      thf_2 = thf_unitary_formula[out]
      { thf = out.createThfBinaryFormula(thf,bc,thf_2);
      }
    | ( AND
        thf_2 = thf_unitary_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.And,thf_2);
        }
      )+
    | ( VLINE
        thf_2 = thf_unitary_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.Or,thf_2);
        }
      )+
    | ( APPLY
        thf_2 = thf_unitary_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.Apply,thf_2);
        }
      )+
    | MAP
      thf = thf_mapping_type[out,thf]
    | ( STAR
        thf_2 = thf_unitary_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.XProd,thf_2);
        }
      )+
    | ( PLUS
        thf_2 = thf_unitary_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.Union,thf_2);
        }
      )+
    | ( SUBTYPE
        thf_2 = thf_unitary_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.Subtype,thf_2);
        }
      )+
    | ( COLON
        thf_2 = thf_top_level_type[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.Type,thf_2);
        }
      )
    | ( ASSIGN
        thf_2 = thf_formula[out]
        { thf = out.createThfBinaryFormula(thf,
TptpParserOutput.BinaryConnective.Assign,thf_2);
        }
      )
//    | ( SEQUENT
//        thf_2 = thf_tuple[out]
//        { thf = out.createThfBinaryFormula(thf,
//TptpParserOutput.BinaryConnective.Sequent,thf_2);
//        }
//      )
    )
  ;

thf_pair_connective
  returns [TptpParserOutput.BinaryConnective bc]
  : EQUAL
    { bc = TptpParserOutput.BinaryConnective.Equal;
    }
  | NOTEQUAL
    { bc = TptpParserOutput.BinaryConnective.NotEqual;
    }
  | bc = binary_connective
  ;

thf_unitary_formula [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula thf]
  : thf = thf_quantified_formula[out]
  | TILDA
    thf = thf_atom_or_unary_formula[out,TptpParserOutput.UnaryConnective.Negation]
  | SMALLPI
    thf = thf_atom_or_unary_formula[out,TptpParserOutput.UnaryConnective.UnaryPi]
  | SMALLSIGMA
    thf = thf_atom_or_unary_formula[out,TptpParserOutput.UnaryConnective.UnarySigma]
  | thf = thf_atom[out]
//  | thf = thf_tuple[out]
  | LPR
    thf = thf_formula[out]
    RPR
  ;
thf_atom_or_unary_formula [TptpParserOutput out,
TptpParserOutput.UnaryConnective connective]
  returns [TptpParserOutput.ThfFormula thf]
  : LPR
    thf = thf_formula[out]
    RPR
    { thf = out.createThfUnaryOf(connective,thf);
    }
  | { thf = out.createThfPlainAtom(connective.toString(), null);
    }
  ;
thf_quantified_formula [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula quantified]
  { TptpParserOutput.Quantifier q;
    List<TptpParserOutput.ThfFormula> vars;
    TptpParserOutput.ThfFormula thf;
  }
  : q = thf_quantifier
    LSB
    vars = thf_variable_list[out] 
    RSB 
    COLON
    thf = thf_unitary_formula[out]
    { quantified = out.createThfQuantifiedFormula(q, vars, thf);
    }
  ;
thf_quantifier
  returns [TptpParserOutput.Quantifier q]
  : ALL
    { q = TptpParserOutput.Quantifier.ForAll;
    }
  | EXIST
    { q = TptpParserOutput.Quantifier.Exists;
    }
  | LAMBDA
    { q = TptpParserOutput.Quantifier.Lambda;
    }
  | BIGPI
    { q = TptpParserOutput.Quantifier.QuantifierPi;
    }
  | BIGSIGMA
    { q = TptpParserOutput.Quantifier.QuantifierSigma;
    }
  | ASSIGN
    { q = TptpParserOutput.Quantifier.Assign;
    }
  | CHOICE
    { q = TptpParserOutput.Quantifier.Choice;
    }
  | DESCRIPTION
    { q = TptpParserOutput.Quantifier.Description;
    }
  ;
thf_variable_list  [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.ThfFormula> vars]
//----Returned list is reversed
  { String var;
    TptpParserOutput.ThfFormula type;
    TptpParserOutput.ThfFormula onevar;
  }
  : var = variable[out]
    ( COLON
      type = thf_top_level_type[out]
      { onevar = out.createThfVariableAtom(var);
        onevar = out.createThfBinaryFormula(onevar,
TptpParserOutput.BinaryConnective.Type,type);
      }
    | { onevar = out.createThfVariableAtom(var);
      }
    )
    ( { vars = new LinkedList<TptpParserOutput.ThfFormula>();
      }
    | COMMA
      vars = thf_variable_list[out]
    )
    { vars.addLast(onevar);
    }
  ;
thf_atom [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula thf]
  { String str;
    TptpParserOutput.Term t1;
    TptpParserOutput.Term t2;
    List<TptpParserOutput.Term> args;
  }
  : { LT(1).getText().equals("$true")
    }? DOLLAR_WORD
      { thf = out.builtInThfTrue();
      }
  | { LT(1).getText().equals("$false")
    }? DOLLAR_WORD
      { thf = out.builtInThfFalse();
      }
  | ( str = atomic_defined_word[out]
    | str = atomic_word[out]
    )
    ( { args = null;
      }
    | LPR 
      args = arguments[out] 
      RPR
    )
    { thf = out.createThfPlainAtom(str, args);}
  | str = atomic_system_word[out]
    ( { args = null;
      }
    | LPR 
      args=arguments[out] 
      RPR
    )
    { thf = out.createThfSystemAtom(str, args);
    }
  | ( ( str = number[out]
      | dq:DISTINCT_OBJECT     
        { str = dq.getText();
        }
      ) 
      { thf = out.createThfPlainAtom(str, null);
      }
    | str = variable[out] 
      { thf = out.createThfVariableAtom(str);
      }
    )
  | ( str = thf_conn_term[out]
      { thf = out.createThfPlainAtom(str, null);
      }
    )
  ;
//thf_tuple [TptpParserOutput out]
//  returns [TptpParserOutput.ThfFormula thf]
//  { TptpParserOutput.ThfFormula formula;
//  }
//  : LSB
//    { thf = null;
//    }
//  | formula = thf_unitary_formula[out]
//    { thf = out.createThfBinaryFormula(thf,
//TptpParserOutput.BinaryConnective.TupleComma,formula);
//    }
//  | ( COMMA
//      formula = thf_unitary_formula[out]
//      { thf = out.createThfBinaryFormula(thf,
//TptpParserOutput.BinaryConnective.TupleComma,formula);
//      }
//    )+
//  RSB
//  ;
thf_top_level_type [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula thf]
  : thf = thf_formula[out]
  ;
thf_unitary_type [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula thf]
  : thf = thf_unitary_formula[out]
  ;
thf_mapping_type [TptpParserOutput out,TptpParserOutput.ThfFormula formula]
  returns [TptpParserOutput.ThfFormula thf]
  { TptpParserOutput.ThfFormula rest;
  }
  : thf = thf_unitary_formula[out]
    ( MAP
      rest = thf_mapping_type[out,thf]
      { thf = out.createThfBinaryFormula(formula,
TptpParserOutput.BinaryConnective.Map,rest);
      }
    | { thf = out.createThfBinaryFormula(formula,
TptpParserOutput.BinaryConnective.Map,thf);
      }
    )
  ;
thf_defn_constant [TptpParserOutput out]
  returns [TptpParserOutput.ThfFormula thf]
  : thf = thf_formula[out]
  ;
//thf_sequent [TptpParserOutput out]
//  returns [TptpParserOutput.ThfFormula thf]
//  { TptpParserOutput.ThfFormula lhs;
//    TptpParserOutput.ThfFormula rhs;
//  }
//  : lhs = thf_tuple[out]
//    SEQUENT
//    rhs = thf_tuple[out]
//    { thf = out.createThfBinaryFormula(lhs,
//TptpParserOutput.BinaryConnective.Sequent,rhs);
//    }
//  ;

//---- FOF formulae ----
fof_formula  [TptpParserOutput out]
  returns [TptpParserOutput.FofFormula fof]
  { TptpParserOutput.BinaryConnective bc;
    TptpParserOutput.FofFormula fof_2;
  }
  : fof = unitary_formula[out]
    ( /* a unitary formula */
    | bc = binary_connective 
      fof_2 = unitary_formula[out]
      { fof = out.createBinaryFormula(fof, bc, fof_2);
      }
    | ( AND 
        fof_2 = unitary_formula[out]
        { fof = out.createBinaryFormula(fof,
TptpParserOutput.BinaryConnective.And, fof_2);
        }
      )+
    | ( VLINE 
        fof_2 = unitary_formula[out]
        { fof = out.createBinaryFormula(fof,
TptpParserOutput.BinaryConnective.Or, fof_2);
        }
      )+
    )
  ;
//----non-associative connectives
binary_connective
  returns [TptpParserOutput.BinaryConnective bc]
  : EQUIVALENCE
    { bc = TptpParserOutput.BinaryConnective.Equivalence;
    }
  | IMPLICATION
    { bc = TptpParserOutput.BinaryConnective.Implication;
    }
  | REVERSE_IMPLICATION
    { bc = TptpParserOutput.BinaryConnective.ReverseImplication;
    }
  | DISEQUIVALENCE
    { bc = TptpParserOutput.BinaryConnective.Disequivalence;
    }
  | NOT_OR
    { bc = TptpParserOutput.BinaryConnective.NotOr;
    }
  | NOT_AND
    { bc = TptpParserOutput.BinaryConnective.NotAnd;
    }
  | SEQUENT
    { bc = TptpParserOutput.BinaryConnective.Sequent;
    }
  ;

unitary_formula  [TptpParserOutput out]
  returns [TptpParserOutput.FofFormula fof]
  {boolean[] polarity ={true};
  TptpParserOutput.AtomicFormula af;
  }
  : fof = quantified_formula[out]
  | fof = unary_formula[out]
  | LPR fof = fof_formula[out] RPR
  | af = atomic_formula[out, polarity]
           {fof = out.atomAsFormula(af);
            if (!polarity[0]) fof = out.createNegationOf(fof);
           }
  ;
//----Unary connectives bind more tightly than binary
unary_formula  [TptpParserOutput out]
  returns [TptpParserOutput.FofFormula fof]
  : TILDA fof = unitary_formula[out] {fof = out.createNegationOf(fof);}
  ;
//---- ! is universal quantification and ? is existential
//---- Formula must be closed
quantified_formula  [TptpParserOutput out]
  returns [TptpParserOutput.FofFormula fof]
  {TptpParserOutput.Quantifier q;
  List<String> vars;
  }
  : q = quantifier LSB
  vars = variable_list[out] RSB COLON
  fof = unitary_formula[out]
                   {fof = out.createQuantifiedFormula(q, vars, fof);}
  ;
quantifier
  returns [TptpParserOutput.Quantifier q]
  : ALL     {q = TptpParserOutput.Quantifier.ForAll;}
  | EXIST   {q = TptpParserOutput.Quantifier.Exists;}
  ;
variable_list  [TptpParserOutput out]
  returns [LinkedList<String> vars] /* returned list is reversed */
  {String var;}
  : var = variable[out]
  (                 {vars = new LinkedList<String>();}
  | COMMA vars = variable_list[out]
  )                 {vars.addLast(var);}
  ;

//%----CNF formulae (variables implicitly universally quantified)
cnf_formula  [TptpParserOutput out]
  returns [TptpParserOutput.CnfFormula cnf]
  {List<TptpParserOutput.Literal> lits;}
  :
  ( LPR lits = disjunction[out] RPR
  | lits = disjunction[out]
  )                          {cnf = out.createClause(lits);}
  ;
disjunction  [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.Literal> lits]
  {TptpParserOutput.Literal lit;}
  : lit = literal[out]
  (                   {lits = new LinkedList<TptpParserOutput.Literal>();}
  | VLINE lits = disjunction[out]
  )                   {lits.addFirst(lit);}

  ;
literal  [TptpParserOutput out]
  returns [TptpParserOutput.Literal lit]
  {boolean[] polarity ={true};
  TptpParserOutput.AtomicFormula af;
  }
  :
  ( af = atomic_formula[out, polarity]
  | TILDA af = atomic_formula[out, polarity]  {polarity[0] = !polarity[0];}
  )
                  {lit = out.createLiteral(new Boolean(polarity[0]), af);}
  ;

//%----Old style CNF
tptp_literals  [TptpParserOutput out]
  returns [TptpParserOutput.CnfFormula cnf]
  {TptpParserOutput.Literal lit;
  LinkedList<TptpParserOutput.Literal> lits;
  }
  : LSB
  (                   {lits = null;}
  |                   {lits = new LinkedList<TptpParserOutput.Literal>();}
    lit = tptp_literal[out]           {lits.add(lit);}
    (COMMA lit = tptp_literal[out]    {lits.add(lit);}
    )*
  )
  RSB                 {cnf = out.createClause(lits);}
  ;
tptp_literal  [TptpParserOutput out]
  returns [TptpParserOutput.Literal lit]
  {boolean[] polarity ={true};
  boolean b;
  TptpParserOutput.AtomicFormula af;
  }
  : b = tptp_sign
    af = atomic_formula[out, polarity]
           {lit = out.createLiteral(new Boolean(b == polarity[0]), af);}
  ;
tptp_sign  returns [boolean b]
  : PLUSPLUS   {b = true;}
  | MINUSMINUS {b = false;}
  ;

/* ------------------------------------------------------- */
/* -------------- ANNOTATIONS --------------------------- */
/* ------------------------------------------------------- */

//---- General purpose term --
general_term  [TptpParserOutput out]
  returns [TptpParserOutput.GeneralTerm t]
  {TptpParserOutput.GeneralTerm gt;}
  : t = general_data[out]
    (COLON gt = general_term[out] {t = out.createGeneralColon(t, gt);}
    )?
  | t = general_list[out]
  ;

general_data   [TptpParserOutput out]
  returns [TptpParserOutput.GeneralTerm t]
  {
  String str;
  List<TptpParserOutput.GeneralTerm> args;
  }
  : str=atomic_word[out]
    ( /*<constant>*/  {args = null;}
    | LPR args = general_terms[out] RPR /*<functor>*/
    )                   {t = out.createGeneralFunction(str, args);}
  | str=number[out]     {t = out.createGeneralDistinctObject(str);}
  | d:DISTINCT_OBJECT   {t = out.createGeneralDistinctObject(d.getText());}
  | str = variable[out] {t = out.createGeneralVariable(str);}
  | t = formula_data[out]
  ;

formula_data [TptpParserOutput out]
  returns [TptpParserOutput.GeneralTerm t]
  {
  TptpParserOutput.ThfFormula thf;
  TptpParserOutput.FofFormula fof;
  TptpParserOutput.CnfFormula cnf;
  TptpParserOutput.Term term;
  }
  : dw:DOLLAR_WORD
     ( {dw.getText().equals("$thf")}?  /* thf formula */
       LPR thf = thf_formula[out] RPR
                                 {t = out.createGeneralThfFormula(thf);}
     | {dw.getText().equals("$fof")}?  /* fof formula */
       LPR fof = fof_formula[out] RPR
                                 {t = out.createGeneralFofFormula(fof);}
     | {dw.getText().equals("$cnf")}?  /* cnf formula */
       LPR cnf = cnf_formula[out] RPR
                                 {t = out.createGeneralCnfFormula(cnf);}
     | {dw.getText().equals("$fot")}?  /* term formula */
       LPR term = term[out] RPR
                                 {t = out.createGeneralTerm(term);}
     | . /* error (whatever character is) */
       { throw new antlr.RecognitionException("illegal general term: '"
               + dw.getText() + "'",
             getFilename(), dw.getLine(), dw.getColumn());
       }
     )
  ;

general_terms  [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.GeneralTerm> list]
  {TptpParserOutput.GeneralTerm t;}
  : t = general_term[out]
    (          {list = new LinkedList<TptpParserOutput.GeneralTerm>();}
    | COMMA list = general_terms[out]
    )          {list.addFirst(t);}
  ;
general_list  [TptpParserOutput out]
  returns [TptpParserOutput.GeneralTerm t]
  {List<TptpParserOutput.GeneralTerm> list = null;
  }
  : LSB
    (list = general_terms[out])?
    RSB
                  {t = out.createGeneralList(list);}
  ;

//%---- Useful info fields
optional_info [TptpParserOutput out]
  returns [List<TptpParserOutput.InfoItem> list]
  :  /* null */ {list = null;}
  | COMMA list = useful_info[out]
  ;
useful_info  [TptpParserOutput out]
  returns [List<TptpParserOutput.InfoItem> list = null]
  : LSB (list = info_items[out])? RSB
  ;
info_items  [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.InfoItem> list]
  {TptpParserOutput.InfoItem infoItem;}
  : infoItem = info_item[out]
  (               {list = new LinkedList<TptpParserOutput.InfoItem>();}
  | COMMA list = info_items[out]
  )               {list.addFirst(infoItem);}
  ;
info_item  [TptpParserOutput out]
  returns [TptpParserOutput.InfoItem item]
  {String str;
  TptpParserOutput.StatusValue value;
  TptpParserOutput.Source fileSource;
  TptpParserOutput.GeneralTerm genTerm, anotherGenTerm;
  List<TptpParserOutput.GeneralTerm> args;
  List<String> nameList;
  }
  //%----Useful info for formula records
  : str=atomic_word[out]
    ( {str.equals("description")}?
      LPR str=atomic_word[out] RPR
                 {item = out.createDescriptionInfoItem(str);}
    | {str.equals("iquote")}?
      LPR str=atomic_word[out] RPR
                 {item = out.createIQuoteInfoItem(str);}
  //%----Useful info for inference records
    | {str.equals("status")}?
      LPR value = status_value[out] RPR
                 {item = out.createInferenceStatusInfoItem(value);}
    | {str.equals("assumption")}?
      LPR LSB nameList = name_list[out] RSB RPR
                 {item = out.createAssumptionRecordInfoItem(nameList);}
    | {str.equals("refutation")}?
    LPR fileSource = file_source[out] RPR
                 {item = out.createRefutationInfoItem(fileSource);}
  // ----- Inference info + General Term -----------
    |
      ( LPR
/* <inference_info> was removed from parser. Semantic part should distinguish
   <inference_info> from <general_term> and CHECK that <inference_info>
   is syntacticaly correct.
*/
        args = general_terms[out]
        RPR
          {genTerm = out.createGeneralFunction(str,args);}
      | /*<constant>*/
          {genTerm = out.createGeneralFunction(str,null);}
      )
      (COLON anotherGenTerm = general_term[out]
        {genTerm = out.createGeneralColon(genTerm, anotherGenTerm);}
      )?
      {item = out.createGeneralFunctionInfoItem(genTerm);}
    )
  |
    ( str=number[out]            {genTerm = out.createGeneralDistinctObject(str);}
    | d:DISTINCT_OBJECT     {genTerm = out.createGeneralDistinctObject(d.getText());}
    | genTerm = general_list[out]
    | str = variable[out] {genTerm = out.createGeneralVariable(str);}
    | genTerm = formula_data[out]
    )
    (COLON anotherGenTerm = general_term[out]
       {genTerm = out.createGeneralColon(genTerm, anotherGenTerm);}
    )?
    {item = out.createGeneralFunctionInfoItem(genTerm);}

  ;


//%----These are the status values from the SZS ontology
status_value   [TptpParserOutput out]
  returns [TptpParserOutput.StatusValue value]
  : id:LOWER_WORD
    { if (id.getText().equals("thm"))
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
    else {/* ERROR. Unknown <status value> string */
      throw new antlr.RecognitionException("unknown status value: '"
                  + id.getText() + "'",
             getFilename(), id.getLine(), id.getColumn());
    }
  }
  ;

//%----Formula sources
source  [TptpParserOutput out]
  returns [TptpParserOutput.Source s]
  { String str;
    String str2;
    List<TptpParserOutput.InfoItem> usefulInfo;
    List<TptpParserOutput.ParentInfo> parentList;
/*    TptpParserOutput.IntroType introType; */
    String introType;
    LinkedList<TptpParserOutput.Source> listOfSources = null;
  }
  : LSB
    listOfSources = sources[out]
    RSB
    { s = out.createSourceFromListOfSources(listOfSources);
    }
  | ( str = atomic_word[out]
      /* -- dag source -- */
      ( { str.equals("inference")}?
          LPR
          str = inference_rule[out] COMMA
          usefulInfo = useful_info[out]
          COMMA
          LSB
          parentList = parent_list[out]
          RSB
          RPR
          { s = out.createSourceFromInferenceRecord(str,usefulInfo,parentList);
          }
      /* internal source */
      | { str.equals("introduced")}?
          LPR
          introType = intro_type[out]
          usefulInfo = optional_info[out]
          RPR 
          { s = out.createInternalSource(introType, usefulInfo);
          }
      /* external source */
      | { str.equals("file")}?
          LPR
          str = file_name[out]
          str2 = file_info[out]
          RPR 
          { s = out.createSourceFromFile(str, str2);
          }
      | { str.equals("creator")}?
          LPR
          str = creator_name[out]
          usefulInfo = optional_info[out]
          RPR
          { s = out.createSourceFromCreator(str, usefulInfo);
          }
      | { str.equals("theory")}?
          LPR
          str = theory_name[out]
          usefulInfo = optional_info[out]
          RPR
          { s = out.createSourceFromTheory(str, usefulInfo);
          }
      /* unknow */
      | { str.equals("unknown")}? 
        { s = out.createSourceFromName(new String("unknown"));
        }
      | /* name */
        { s = out.createSourceFromName(str);
        }
      )
    /* name in dag-source*/
    | ui:UNSIGNED_INTEGER 
      { s = out.createSourceFromName(ui.getText());
      }
    )
  ;

sources  [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.Source> list]
  {TptpParserOutput.Source sourceItem;}
  : sourceItem = source[out]
  ( { list = new LinkedList<TptpParserOutput.Source>();
    }
  | COMMA 
    list = sources[out]
  ) 
  { list.addFirst(sourceItem);
  }
  ;

inference_rule  [TptpParserOutput out]
  returns [String str]
  : str = atomic_word[out]
  /* The parser does not check that <inference rule> correct,
     the semantic part should do that.
  */
  ;
parent_info  [TptpParserOutput out]
  returns [TptpParserOutput.ParentInfo info]
  {TptpParserOutput.Source src;
  TptpParserOutput.GeneralTerm t;
  }
  : src = source[out]
    t = parent_details[out]
                     {info = out.createParentInfo(src, t);}
  ;
parent_list  [TptpParserOutput out]
  returns [LinkedList<TptpParserOutput.ParentInfo> list]
  {TptpParserOutput.ParentInfo info;}
  :  info = parent_info[out]
     (      {list = new LinkedList<TptpParserOutput.ParentInfo>();}
     | COMMA list = parent_list[out]
     )                                   {list.addFirst(info);}
  ;
parent_details  [TptpParserOutput out]
  returns [TptpParserOutput.GeneralTerm t]
  : COLON t = general_list[out]
  | /* null */             {t = null;}
  ;

/*---- Andrei's explicit intro types
intro_type  [TptpParserOutput out]
  returns [TptpParserOutput.IntroType type]
  : str:LOWER_WORD
  ( {str.getText().equals("definition")}?
                          {type = TptpParserOutput.IntroType.Definition;}
  | {str.getText().equals("axiom_of_choice")}?
                          {type = TptpParserOutput.IntroType.AxiomOfChoice;}
  | {str.getText().equals("tautology")}?
                          {type = TptpParserOutput.IntroType.Tautology;}
  | {str.getText().equals("assumption")}?
                          {type = TptpParserOutput.IntroType.Assumption;}
  | 
    { throw new antlr.RecognitionException("unknown intro type: '"
               + str.getText() + "'",
             getFilename(), str.getLine(), str.getColumn());
    }
  )
  ;
----*/
/* my string version */
intro_type  [TptpParserOutput out]
  returns [String str]
  : str = atomic_word[out]
  ;
intro_info  [TptpParserOutput out]
  returns [List<TptpParserOutput.InfoItem> info]
  : COMMA info = useful_info[out]
  |                                {info = null;}
  ;

file_name  [TptpParserOutput out]
  returns [String str]
  : str = atomic_word[out]
  ;
file_info  [TptpParserOutput out]
  returns [String str]
  : /* nothing */ {str = null;}
  | COMMA str=name[out] /* name */
  ;
file_source  [TptpParserOutput out]
  returns [TptpParserOutput.Source src]
  {String fileName;
  String fileInfo;
  }
  :  lw : LOWER_WORD
  ( {lw.getText().equals("file")}?
     LPR
     fileName = file_name[out]
     fileInfo = file_info[out]
     RPR
                  {src = out.createSourceFromFile(fileName, fileInfo);}
   | /* error */
     { throw new antlr.RecognitionException("file source expected"
                 + "but found " + lw,
             getFilename(), lw.getLine(), lw.getColumn());
     }
   )
  ;

creator_name  [TptpParserOutput out]
  returns [String str]
  : str=atomic_word[out]
  ;

theory_name  [TptpParserOutput out]
  returns [String str]
  : lw : LOWER_WORD
  ( {lw.getText().equals("equality")}?  {str = new String("equality");}
  | {lw.getText().equals("ac")}?        {str = new String("ac");}
  | /* error */
      { throw new antlr.RecognitionException("unknown theory name: '"
                + lw.getText() + "'",
             getFilename(), lw.getLine(), lw.getColumn());
      }
  )
  ;



/* ------------------------------------------------------- */
/* ------------------ TPTP FILE ------------------------- */
/* ------------------------------------------------------- */


//%----Files.
tptp_file [TptpParserOutput out]
  returns [List<TptpParserOutput.TptpInput> list
                      = new LinkedList<TptpParserOutput.TptpInput>()]
  {TptpParserOutput.TptpInput in;
  }
  : (in = tptp_input[out]               {list.add(in);}
    )*
  ;
//%----Future languages may include ...  english | efof | tfof | mathml | ...
tptp_input [TptpParserOutput out]
  returns [TptpParserOutput.TptpInput in]
  {
   String nm;
   TptpParserOutput.FormulaRole role;
   TptpParserOutput.ThfFormula thf;
   TptpParserOutput.FofFormula fof;
   TptpParserOutput.CnfFormula cnf;
      TptpParserOutput.Annotations ann;
      List<String> formulaSelection;
  }
  : str:LOWER_WORD
    ( {str.getText().equals("thf")}?  /* fof annotated formula */
     LPR
     nm = name[out] COMMA
     role = formula_role[out] COMMA
     thf = thf_formula[out]
     ann = annotations[out]
     RPR DOT
      {in=out.createThfAnnotated(nm, role, thf, ann, str.getLine());}
  | {str.getText().equals("fof")}?  /* fof annotated formula */
     LPR
     nm = name[out] COMMA
     role = formula_role[out] COMMA
     fof = fof_formula[out]
     ann = annotations[out]
     RPR DOT
      {in=out.createFofAnnotated(nm, role, fof, ann, str.getLine());}
  | {str.getText().equals("cnf")}?  /* cnf annotated formula */
     LPR
     nm=name[out] COMMA
     role = formula_role[out] COMMA
     cnf = cnf_formula[out]
     ann = annotations[out] RPR DOT
      {in=out.createCnfAnnotated(nm, role, cnf, ann, str.getLine());}
  | {str.getText().equals("include")}?
     LPR nm = file_name[out]
     formulaSelection = formula_selection[out]
     RPR DOT
      {in=out.createIncludeDirective(nm, formulaSelection, str.getLine());}
  | {str.getText().equals("input_formula")}?  /* old-style fof formula */
     LPR
     nm = name[out] COMMA
     role = formula_role[out] COMMA
     fof = fof_formula[out] RPR DOT
      {in=out.createFofAnnotated(nm, role, fof, null, str.getLine());}
  | {str.getText().equals("input_clause")}?  /* old-style cnf formula */
       LPR
     nm = name[out] COMMA
     role = formula_role[out] COMMA
     cnf = tptp_literals[out] RPR DOT
      {in=out.createCnfAnnotated(nm, role, cnf, null, str.getLine());}
  | {true}?/* error */
      { throw new antlr.RecognitionException(
              "unexpected high level construct '" + str.getText() + "'",
            getFilename(), str.getLine(), str.getColumn());
      }
  )
  // [AT] maybe, add system-comment here. Usual comment will be white-space
  ;
formula_role  [TptpParserOutput out]
  returns [TptpParserOutput.FormulaRole role]
  : str:LOWER_WORD
    ( {str.getText().equals("axiom")}?
                    {role = TptpParserOutput.FormulaRole.Axiom;}
  | {str.getText().equals("hypothesis")}?
                    {role = TptpParserOutput.FormulaRole.Hypothesis;}
  | {str.getText().equals("definition")}?
                    {role = TptpParserOutput.FormulaRole.Definition;}
  | {str.getText().equals("assumption")}?
                    {role = TptpParserOutput.FormulaRole.Assumption;}
  | {str.getText().equals("lemma")}?
                    {role = TptpParserOutput.FormulaRole.Lemma;}
  | {str.getText().equals("theorem")}?
                    {role = TptpParserOutput.FormulaRole.Theorem;}
  | {str.getText().equals("conjecture")}?
                    {role = TptpParserOutput.FormulaRole.Conjecture;}
  | {str.getText().equals("question")}?
                    {role = TptpParserOutput.FormulaRole.Question;}
  | {str.getText().equals("negated_conjecture")}?
                    {role = TptpParserOutput.FormulaRole.NegatedConjecture;}
  | {str.getText().equals("plain")}?
                    {role = TptpParserOutput.FormulaRole.Plain;}
  | {str.getText().equals("answer")}?
                    {role = TptpParserOutput.FormulaRole.Answer;}
  | {str.getText().equals("fi_domain")}?
                    {role = TptpParserOutput.FormulaRole.FiDomain;}
  | {str.getText().equals("fi_functors")}?
                    {role = TptpParserOutput.FormulaRole.FiFunctors;}
  | {str.getText().equals("fi_predicates")}?
                    {role = TptpParserOutput.FormulaRole.FiPredicates;}
  | {str.getText().equals("type")}?
                    {role = TptpParserOutput.FormulaRole.Type;}
  | {str.getText().equals("unknown")}?
                    {role = TptpParserOutput.FormulaRole.Unknown;}
  | {/* ERROR. Unknown <type> string */
      throw new antlr.RecognitionException("unknown formula type: '"
                  + str.getText() + "'",
             getFilename(), str.getLine(), str.getColumn());}
  )
  ;
annotations  [TptpParserOutput out]
  returns [TptpParserOutput.Annotations ann]
  {TptpParserOutput.Source src;
  List<TptpParserOutput.InfoItem> usefulInfo;
  }
  : /* null */ {ann = null;}
  | COMMA
    src = source[out]
    usefulInfo = optional_info [out]
                    {ann = out.createAnnotations(src, usefulInfo);}
  ;

name_list  [TptpParserOutput out]
  returns [LinkedList<String> list]
  {String str;}
  : str = name[out]
  (                 {list = new LinkedList<String>();}
  | COMMA list = name_list[out]
  )
                    {list.addFirst(str);}
  ;
formula_selection  [TptpParserOutput out]
  returns [List<String> list]
  {String str;}
  : /* null */               {list = null;}
  | COMMA LSB list = name_list[out] RSB
  ;

/*=========================================================*/
/*===================== LEXER =============================*/
/*=========================================================*/

class TptpLexer extends Lexer;
options
  {
    k = 2;
    exportVocab = Tptp;
    testLiterals = false;
    charVocabulary = '\0'..'\377';
    defaultErrorHandler=false;
  }
  {
    public void incrLine() {newline(); /* ANTLR's function*/ }
  }

//---- COMMENTS -------
// If comments beging with %$$ or /*$$ are system comments.
ONE_LINE_COMMENT
   : '%'
   ( {LA(1) == '$' && LA(2) == '$'}? "$$" /* system comment*/
   | /* usual comment */
   )
   (~('\n'|'\r') )*    {$setType(Token.SKIP);}
   ;
MANY_LINE_COMMENT
   : "/*"
   ({LA(1) == '$' && LA(2) == '$'}? "$$" /* system comment*/
   | /* usual comment */
   )

   ( options {greedy=false;} :
     '*'
   | ("\r\n") => "\r\n"    { incrLine(); }
   | ( '\n' | '\r' )       { incrLine(); }
   | ~( '*'| '\r' | '\n' )
   )*
  "*/"       {$setType(Token.SKIP);}
  ;

//---- IDENTIFIERS -----
DOLLAR_WORD
  : "$" 'a'..'z' ('a'..'z'| 'A'..'Z' | '0'..'9' | '_')*
  ;
DOLLAR_DOLLAR_WORD
  : "$$" 'a'..'z' ('a'..'z'| 'A'..'Z' | '0'..'9' | '_')*
  ;
UPPER_WORD
  : 'A'..'Z' ('a'..'z' | '0'..'9' | 'A'..'Z'| '_')*
  ;
LOWER_WORD   options { testLiterals = true;}
  : 'a'..'z' ('a'..'z'|'0'..'9' | 'A'..'Z'| '_')*
  ;
SINGLE_QUOTED
  : "'"
  ( /* convert SINGLE_QUOTED to LOWER_WORD if possible */
    (LOWER_WORD "'") => lw:LOWER_WORD {$setToken(lw);}

  | // 047 is "'", 0134 is "\", 040-0176 (32-126) are printable
    ( '\40'..'\46' | '\50'..'\133' | '\135'..'\176'
    | '\\' ('\\'|"'")  // "\" can be followed by "\" or "'" only.
    )*
  )
  "'"
  ;

DISTINCT_OBJECT
  : '"'
  (   // 042 is '"', 0134 is '\', 040-0176 are printable
    '\40' .. '\41' | '\43'..'\133' | '\135'..'\176'
      // '\' can be followed by '\' or '"' only.
  | '\\' ('\\'|'"')
      // additional characters:
      // 011 (09) - horizontal tab \t,
      // 012 (0A) - line feed \n,
      // 013 (0B) - vertical tab \v,
      // 014 (0C) - form feed \f,
      // 015 (0D) - carriage return \r,
      // 033 (1B) - escape  \e
      // 0177 - 0377 (7E-FF) - extended ASCII
  | '\11' | '\013' | '\014' | '\033' | '\177'..'\377'
  | ('\n'
    | (("\r\n") => "\r\n" | '\r' )
    )      { incrLine();}
  )*
  '"'           //"
  ;
// -------- NUMBERS----------

UNSIGNED_INTEGER
   :
   ( '+'! {$setType(INTEGER);/* it is unsigned integer. "+" is removed */ }
   | '-'  {$setType(INTEGER);/* it is signed integer */ }
   )?
   ( '0'
   | '1'..'9' ('0'..'9')*
   )
   ( /* rational */
    '/' ('0'..'9')+ {$setType(RATIONAL);}
   )?
   ( /* real */
    '.' ('0'..'9')+ {$setType(REAL);}
   )?
   ( /* real with exponent-part */
     ('e' | 'E')
     ('+'! {/* normalization: "+" is removed */} | '-')?
     ( '0'
     | '1'..'9' ('0'..'9')*
     )
     {$setType(REAL);}
   )?
   ;

protected
INTEGER:;

protected
REAL:;

// --------  SPECIAL CHARACTERS ----------

COMMA:    ",";
LPR:      "(";
RPR:      ")";
LSB:      "[";
RSB:      "]";
AND:      "&";
VLINE:    "|";
APPLY:    "@";
LAMBDA:    "^";
MAP:      ">";
SUBTYPE:  "<<";
STAR:     "*";
//PLUS:     "+"; /* commented out because of + and - in UNSIGNED_INTEGER */
//MINUS:    "-"; /* commented out because of + and - in UNSIGNED_INTEGER */
TILDA:    "~";
EQUAL:    "=";
NOTEQUAL:  "!=";
PLUSPLUS:  "++";
SEQUENT:   "--" ('>'
                | {$setType(MINUSMINUS);}
                );
protected MINUSMINUS:;
//TRUE:      "$true";   /* commented out becuase of DOLLAR_WORD */
//FALSE:     "$false";  /* commented out becuase of DOLLAR_WORD */
ALL:       "!";
EXIST:     "?";
SMALLPI:   "!!";
SMALLSIGMA: "??";
BIGPI:     "!>";
BIGSIGMA:  "?*";
CHOICE:    "@+";
DESCRIPTION: "@-";
COLON:     ":";
ASSIGN:    ":=";
DOT:       ".";

IMPLICATION:     "=>";
DISEQUIVALENCE:  "<~>";
NOT_OR:          "~|";
NOT_AND:         "~&";
EQUIVALENCE:     "<=" ('>'
                 | {$setType(REVERSE_IMPLICATION);}
                 );
protected REVERSE_IMPLICATION:;


WHITESPACE
  : ( ( '\003'..'\010'
      | '\t'
      | '\013' //it is '\v'
      | '\f'
      | '\016'.. '\037'
      | '\177'..'\377'
      | ' '
      )
    | ("\r\n" | '\n' | '\r' )          { incrLine();}
    )          {$setType(Token.SKIP);}
  ;
//-----------------------------------------------------------------------------
