package com.articulate.sigma;

/**
 *  Abstract syntax for TPTP. Most of the features declared here
 *  are used by the TPTP parser to communicate the results of parsing. 
 *  The user of the parser is supposed to provide implementations 
 *  of the interfaces declared here (for many users the default implementation
 *  in SimpleTptpParserOutput.java may be good enough).
 *
 *  Such approach is taken to nicely isolate the grammar sources from 
 *  the environment. It makes the parser highly alienable. A new user 
 *  only has to provide an implementation of these interfaces in order to link
 *  the parser to his/her code. No matter how the data returned from 
 *  the parser is processed, no modification of the parser code is 
 *  required. The requirements imposed by the interface are quite
 *  liberal, so that writing modules glueing the parser with practically
 *  any application is easy.
 *
 *  @author Alexandre Riazanov
 *  @since Feb 02, 2006
 */

public interface TptpParserOutput {

    /** Must be implemented by a class representing instances of 
     *  &#60TPTP input&#62 in the BNF grammar (except comments), ie top level input 
     *  items, such as annotated formulas and include directives.
     */
    public static interface TptpInput {
      
      enum Kind {
          Formula, 
          Clause,
          Include
      };

    } // interface TptpInput
    
    /** Must be implemented by a class representing formula structures
     *  corresponding to instances of &#60fof formula&#62 in the BNF grammar.
     */
    public static interface FofFormula {
      // nothing here
    }; // static interface FofFormula

    /** Must be implemented by a class representing clause structures
     *  corresponding to instances of &#60cnf formula&#62 in the BNF grammar.
     */
    public static interface CnfFormula {
      // nothing here
    }; // static interface CnfFormula

    /** Must be implemented by a class representing structures
     *  corresponding to instances of &#60atomic formula&#62 in the BNF grammar.
     */
    public static interface AtomicFormula {
      // nothing here
    }; // static interface AtomicFormula

    /** Must be implemented by a class representing structures
     *  corresponding to instances of &#60literal&#62 in the BNF grammar.
     */
    public static interface Literal {
      // nothing here
    }; // static interface Literal

    
    /** Must be implemented by a class representing
     *      &#60term&#62 in the BNF grammar.
     */
    public static interface Term {
      // nothing here
    }; // static interface Term
    


    /** Reprsents all binary connectives, both associative (&,|) and nonassociative
     *  (admissible instances of &#60binary connective&#62).
     */
    public static enum BinaryConnective {

      And,
      Or,
      
      Equivalence, /* <=> */
      Implication, /* => */
      ReverseImplication, /* <= */
      Disequivalence, /* <~> */
      NotOr, /* ~| */
      NotAnd; /* ~& */

      public String toString() {
          switch (this) 
          {
            case And: return new String("&");
            case Or: return new String("|");
            case Equivalence: return new String("<=>");
            case Implication: return new String("=>");
            case ReverseImplication: return new String("<=");
            case Disequivalence: return new String("<~>");
            case NotOr: return new String("~|");
            case NotAnd: return new String("~&");      
          };
          assert false;
          return null;
      } // toString()

    }; // enum BinaryConnective


    /** Reprsents all quantifiers. */
    public static enum Quantifier {
      
      ForAll, /* ! */
      Exists;  /* ? */
      
      public String toString() {
          if (this == ForAll) return new String("!");
          assert (this == Exists);
          return new String("?");
      }

    }; // enum BinaryConnective

    
    

    /** Reprsents all admissible instances of &#60formula role&#62
     *  (see the BNF grammar).
     */
    public static enum FormulaRole {
      
      Axiom, 
      Hypothesis,
      Definition, 
      Assumption, 
      Lemma,
      Theorem,
      Conjecture,
      NegatedConjecture,
      Plain,
      FiDomain,
      FiFunctors,
      FiPredicates,
      Type,
      Unknown;

      public String toString() {
          switch (this) {
            case Axiom: return "axiom";
            case Hypothesis: return "hypothesis";
            case Definition: return "definition"; 
            case Assumption: return "assumption"; 
            case Lemma: return "lemma";
            case Theorem: return "theorem";
            case Conjecture: return "conjecture";
            case NegatedConjecture: return "negated_conjecture";
            case Plain: return "plain";
            case FiDomain: return "fi_domain";
            case FiFunctors: return "fi_functors";
            case FiPredicates: return "fi_predicates";
            case Type: return "type";
            case Unknown: return "unknown";
          };
          assert false;
          return null;
      }

    }; // enum FormulaRole

    /** Must be implemented by a class representing annotations,
     *      ie instances of &#60annotations&#62 in the BNF grammar.
     */
    public static interface Annotations {
      // nothing here
    }; // static interface Annotations


    /** Must be implemented by a class representing instances of &#60source&#62 
     *  in the BNF grammar.
     */
    public static interface Source {
      // nothing here
    }; // static interface Source

    /** Must be implemented by a class representing instances of &#60info item&#62 
     *  in the BNF grammar.
     */
    public static interface InfoItem {
      // nothing here
    }; // static interface InfoItem



    /** Reprsents all admissible instances of &#60intro type&#62
     *  (see the BNF grammar).
     */
    public static enum IntroType {
      
      Definition,
      AxiomOfChoice,
      Tautology,
      Assumption;
      
      public String toString() {
          switch (this) 
          {
            case Definition:    return new String("definition");
            case AxiomOfChoice: return new String("axiom_of_choice");
            case Tautology:     return new String("tautology");
            case Assumption:    return new String("assumption");
          };
          assert false;
          return null;
      } // toString()
    }; // enum IntroType

    /** Reprsents all admissible instances of &#60status value&#62
     *  (see the BNF grammar).
     */
    public static enum StatusValue {
      
      Tau, Tac, Eqv, Thm, Sat, Cax, Noc, Csa, Cth, Ceq, 
        Unc, Uns, Sab, Sam, Sar, Sap, Csp, Csr, Csm, Csb;
      
      public String toString() {
          switch (this) 
          {
            case Tau: return new String("tau");
            case Tac: return new String("tac");
            case Eqv: return new String("eqv");
            case Thm: return new String("thm");
            case Sat: return new String("sat");
            case Cax: return new String("cax");
            case Noc: return new String("noc");
            case Csa: return new String("csa");
            case Cth: return new String("cth");
            case Ceq: return new String("ceq");
            
            case Unc: return new String("unc");
            case Uns: return new String("uns");
            case Sab: return new String("sab");
            case Sam: return new String("sam");
            case Sar: return new String("sar");
            case Sap: return new String("sap");
            case Csp: return new String("csp");
            case Csr: return new String("csr");
            case Csm: return new String("csm");
            case Csb: return new String("csb");
          };
          assert false;
          return null;
      } // toString()

    }; // enum StatusValue



    /** Must be implemented by a class representing
     *      &#60general term&#62 in the BNF grammar.
     */
    public static interface GeneralTerm {
      // nothing here
    }; // static interface GeneralTerm
      
    
    /** Must be implemented by a class representing
     *      &#60parent info&#62 in the BNF grammar.
     */
    public static interface ParentInfo {
      // nothing here
    }; // static interface ParentInfo

    

    /*==================================================================
     *                      Top level items.                           *
     *=================================================================*/


    /** A correct implementation must return a TptpInput object representing
     *  <strong> formula </strong> wrapped in the corresponding annotation.
     *  @param name != null
     *  @param role != null 
     *  @param formula != null
     *  @param annotations can be null
     *  @param lineNumber location in the input
     */
    public 
    TptpInput
    createFofAnnotated(String name,
                   FormulaRole role,
                   FofFormula formula,
                   Annotations annotations,
                   int lineNumber);

    
    /** A correct implementation must return a TptpInput object representing
     *  <strong> clause </strong> wrapped in the corresponding annotation.
     *  @param name != null
     *  @param role != null 
     *  @param clause != null
     *  @param annotations can be null
     *  @param lineNumber location in the input
     */
    public 
    TptpInput
    createCnfAnnotated(String name,
                   FormulaRole role,
                   CnfFormula clause,
                   Annotations annotations,
                   int lineNumber);


    /** A correct implementation must return a TptpInput object representing
     *  the instance of &#60include&#62 with the specified parameters.
     *  @param fileName != null
     *  @param formulaSelection satisfies
     *         (formulaSelection == null || formulaSelection.iterator().hasNext())
     *  @param lineNumber location in the input
     */
    public 
    TptpInput
    createIncludeDirective(String fileName,
                     Iterable<String> formulaSelection,
                     int lineNumber);




    /*==================================================================
     *          Formulas, clauses, literals and terms.                 *
     *=================================================================*/

    /** A correct implementation must return an object representing
     *  the binary formula obtained by applying <strong> connective </strong>
     *  to <strong> lhs </strong> and <strong> rhs </strong>.
     *  Note that the method must work with conjunction and disjunction,
     *  as well as with nonassociative connectives.
     *  @param lhs != null
     *  @param connective != null 
     *  @param rhs != null
     */
    public 
    FofFormula 
    createBinaryFormula(FofFormula lhs,
                  BinaryConnective connective,
                  FofFormula rhs);

    /** A correct implementation must return an object representing
     *  the formula obtained by applying the negation connective
     *  to <strong> formula </strong>.
     *  @param formula != null
     */
    public 
    FofFormula
    createNegationOf(FofFormula formula);


    /** A correct implementation must return an object representing
     *  the formula obtained by applying the quantifier
     *  <strong> quantifier </strong> to <strong> formula </strong>.
     *  @param quantifier != null
     *  @param variableList != null && variableList.iterator().hasNext()
     *  @param formula != null
     */
    public 
    FofFormula
    createQuantifiedFormula(Quantifier quantifier,
                      Iterable<String> variableList,
                      FofFormula formula);


    /** A correct implementation must return an object representing
     *  the clause made of <strong> literals </strong>.
     *  <strong> literals </strong> will always be null if an empty
     *  clause has to be created.
     *  @param literals satisfies 
     *                   (literals == null || literals.iterator().hasNext())
     */
    public CnfFormula createClause(Iterable<Literal> literals);
    

    /** A correct implementation must return an object of the class implementing FofFormula,
     *  representing the atomic formula represented by the object
     *  <strong> atom </strong>. 
     *  @param atom != null
     */
    public FofFormula atomAsFormula(AtomicFormula atom);
    
    /** A correct implementation must return an object representing the literal
     *  with <strong> atom </strong> and the polarity determined by 
     *  <strong> positive </strong>.
     *  @param positive == true iff the literal has to be positive
     *  @param atom != null
     */
    public Literal createLiteral(boolean positive,AtomicFormula atom);
    
    
    /** A correct implementation must return an object representing
     *  the atomic formula obtained by applying 
     *  <strong> predicate </strong> to
     *  <strong> arguments </strong>. <strong> arguments </strong> 
     *  will always be null if there are no arguments.
     *  @param predicate != null
     *  @param arguments satisfies 
     *                   (arguments == null || arguments.iterator().hasNext())
     */
    public 
    AtomicFormula
    createPlainAtom(String predicate,Iterable<Term> arguments);
    
    /** A correct implementation must return an object representing
     *  the atomic formula obtained by applying 
     *  <strong> predicate </strong> to
     *  <strong> arguments </strong>. <strong> arguments </strong> 
     *  will always be null if there are no arguments.
     *  @param predicate != null
     *  @param arguments satisfies 
     *                   (arguments == null || arguments.iterator().hasNext())
     */
    public 
    AtomicFormula
    createSystemAtom(String predicate,Iterable<Term> arguments);
    
    /** A correct implementation must return an object representing
     *  the atomic formula obtained by applying 
     *  the equality predicate to the terms.
     */
    public
    AtomicFormula
    createEqualityAtom(Term lhs,Term rhs);
    
    /* 
       Nothing for disequality here: lhs != lhs is handled with
       createNegationOf(createEqualityAtom(lhs,rhs)) 
    */

    /** A correct implementation must return a representation for $true. */
    public AtomicFormula builtInTrue();

    /** A correct implementation must return a representation for $false. */
    public AtomicFormula builtInFalse();

    
    /** A correct implementation must return an object representing 
     *  <strong> variable </strong> as a term.
     *  @param variable != null
     */
    public Term createVariableTerm(String variable);
    
    
    /** A correct implementation must return an object representing
     *  the non-variable term obtained by applying 
     *  <strong> function </strong> to
     *  <strong> arguments </strong>. <strong> arguments </strong> 
     *  will always be null if there are no arguments, ie 
     *  when the term is an individual constant.
     *  @param function != null
     *  @param arguments satisfies 
     *                   (arguments == null || arguments.iterator().hasNext())
     */
    public Term createPlainTerm(String function,Iterable<Term> arguments);
                           
    
    /** A correct implementation must return an object representing
     *  the atomic formula obtained by applying 
     *  <strong> function </strong> to
     *  <strong> arguments </strong>. <strong> arguments </strong> 
     *  will always be null if there are no arguments, ie 
     *  when the term is an individual constant.
     *  @param function != null
     *  @param arguments satisfies 
     *                   (arguments == null || arguments.iterator().hasNext())
     */
    public Term createSystemTerm(String function,Iterable<Term> arguments);



    

    /*==================================================================
     *                     Annotations for top level items.            *
     *=================================================================*/



    /** A correct implementation must return an object representing
     *  an instance of &#60annotations&#62 composed of 
     *  <strong> source </strong> and <strong> usefulInfo </strong>.
     *  @param source != null
     *  @param usefulInfo satisfies 
     *                    (usefulInfo == null || usefulInfo.iterator().hasNext())
     */
    public Annotations createAnnotations(Source source,
                               Iterable<InfoItem> usefulInfo);



    /*==================================================================
     *  Various kinds of source descriptors (see <source> in the BNF). *
     *=================================================================*/

    
    /** A correct implementation must return an object representing
     *  an instance of &#60source&#62 corresponding to 
     *  <strong> name </strong>.
     *  @param name != null
     */
    public Source createSourceFromName(String name);

    
    
    /** A correct implementation must return an object representing
     *  an instance of &#60source&#62 corresponding to 
     *  an instance of &#60inference record&#62 with the specified constituents.
     *  @param inferenceRule != null
     *  @param usefulInfo satisfies 
     *                    (usefulInfo == null || usefulInfo.iterator().hasNext())
     *  @param parentInfoList nonempty
     */
    public 
    Source 
    createSourceFromInferenceRecord(String inferenceRule,
                            Iterable<InfoItem> usefulInfo,
                            Iterable<ParentInfo> parentInfoList);


    /** A correct implementation must return an object representing
     *  an instance of &#60source&#62 corresponding to 
     *  an instance of &#60internal source&#62 with the specified constituents.
     *  @param introType != null
     *  @param introInfo satisfies 
     *                    (usefulInfo == null || usefulInfo.iterator().hasNext())
     */
    public Source createInternalSource(IntroType introType,
                               Iterable<InfoItem> introInfo);

    /** A correct implementation must return an object representing
     *  an instance of &#60source&#62 corresponding to the specified
     *  location in a file.
     *  @param fileName != null
     *  @param fileInfo (may be null)
     */
    public Source createSourceFromFile(String fileName,
                                       String fileInfo);


    /** A correct implementation must return an object representing
     *  an instance of &#60source&#62 corresponding to the specified
     *  creator.
     *  @param creatorName != null
     *  @param usefulInfo satisfies 
     *         (usefulInfo == null || usefulInfo.iterator().hasNext())
     */
    public Source createSourceFromCreator(String creatorName,
                                          Iterable<InfoItem> usefulInfo);
    
    /** A correct implementation must return an object representing
     *  an instance of &#60source&#62 corresponding to the specified
     *  theory.
     *  @param theoryName != null
     *  @param usefulInfo satisfies 
     *         (usefulInfo == null || usefulInfo.iterator().hasNext())
     */
    public Source createSourceFromTheory(String theoryName,
                                         Iterable<InfoItem> usefulInfo);


    /*==================================================================
     *  Various kinds of info items (see <info> in the BNF).           *
     *=================================================================*/


    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to an instance
     *  of &#60description item&#62 with the specified parameter.
     *  @param singleQuoted != null
     */
    public InfoItem createDescriptionInfoItem(String singleQuoted);


    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to an instance
     *  of &#60iquote item&#62 with the specified parameter.
     *  @param singleQuoted != null
     */
    public InfoItem createIQuoteInfoItem(String singleQuoted);


    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to an instance
     *  of  status(&#60status value&#62) with the specified parameter.
     *  @param statusValue != null
     */
    public InfoItem createInferenceStatusInfoItem(StatusValue statusValue);


    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to an instance
     *  of &#60assumption record&#62 with a list &#60name list&#62 as the parameter.
     *  @param nameList != null
     */
    public InfoItem createAssumptionRecordInfoItem(Iterable<String> nameList);


    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to an instance
     *  of  &#60inference info&#62 with the specified constituents.
     *  @param inferenceRule != null
     *  @param inferenceId != null
     *  @param attributes satisfies (attributes == null || attributes.iterator().hasNext())
     */
    public InfoItem createInferenceRuleInfoItem(String inferenceRule,
                                    String inferenceId,
                                    Iterable<GeneralTerm> attributes);



    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to an instance
     *  of &#60refutation&#62 with the specified parameter.
     *  @param fileSource returned by a call to createSourceFromFile(..)
     */
    public InfoItem createRefutationInfoItem(Source fileSource);

    

    /** A correct implementation must return an object representing
     *  an instance of &#60info item&#62 corresponding to the specified 
     *  instance of &#60general function&#62.
     *  @param generalFunction returned by a call to 
     *         createGeneralFunction(..)
     */
    public InfoItem createGeneralFunctionInfoItem(GeneralTerm generalFunction);


    /*==================================================================
     *        General terms (see <general term> in the BNF).           *
     *=================================================================*/

    
    
    /** A correct implementation must return an object representing
     *  the non-list term obtained by applying 
     *  <strong> function </strong> to
     *  <strong> arguments </strong>. <strong> arguments </strong> 
     *  will always be null if there are no arguments, ie 
     *  when the term is an individual constant.
     *  @param function != null
     *  @param arguments satisfies 
     *                   (arguments == null || arguments.iterator().hasNext())
     */
    public GeneralTerm createGeneralFunction(String function,
                                   Iterable<GeneralTerm> arguments);


    /** A correct implementation must return an object representing
     *  the general term constructed as a list.
     *  @param list satisfies 
     *              (list == null || list.iterator().hasNext())
     */
    public GeneralTerm createGeneralList(Iterable<GeneralTerm> list);


    /** A correct implementation must return an object representing
     *  a general term obtained by applying ':' (colon) binary operator 
     *  to two general terms. Left operand should be not a list.
     *  @param left should not be a general term presenting a list 
     *  @param righ
     */
    public GeneralTerm createGeneralColon(GeneralTerm left, GeneralTerm right);


    /** A correct implementation must return an object representing
     *  a general term constructed as a number or a double-quoted string
     *  (both represented as a string)
     *  @param str
     */
    public GeneralTerm createGeneralDistinctObject(String str);


    /** A correct implementation must return an object representing
     *  a general term constructed as a variable
     *  (represented as a string)
     *  @param str
     */
    public GeneralTerm createGeneralVariable(String var);


    /** A correct implementation must return an object representing
     *  a formula
     *  @param formula
     */
    public GeneralTerm createGeneralFormula(FofFormula formula);


    /*==================================================================
     *          Parent info items (see <parent info> in the BNF).      *
     *=================================================================*/

    /** A correct implementation must return an object representing
     *  the parent info item with the specified parameters.
     *  @param source != null
     *  @param parentDetails if nonnull, corresponds to &#60single quoted&#62 
     *         in the -&#60single quoted&#62 option in the BNF rule for &#60parent details&#62;
     *         when parentDetails = null, it corresponds to the &#60null&#62 option
     *         in that rule
     */
    public ParentInfo createParentInfo(Source source,String parentDetails);



} // interface TptpParserOutput
