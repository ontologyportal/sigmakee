

package tptp_parser;


import java.util.LinkedList;
import java.util.Iterator;

import java.util.Hashtable;

/**
*  A simple implementation of the interface TptpParserOutput.
*  This implementation is likely to be sufficient for all simple uses 
*  of the parser. 
*  @author Alexandre Riazanov
*  @author Andrei Tchaltsev
*  @since Feb 02, 2006                 
*  @since Apr 06, 2006
*  @since Nov 13, 2007 ("package tptp_parser" added by AR)
*/



public class SimpleTptpParserOutput 
implements TptpParserOutput 
{
  
  /** A common base for the classes AnnotatedFormula, AnnotatedClause
  *  and IncludeDirective.
  */
  public static class TopLevelItem implements TptpParserOutput.TptpInput {
    
    public Kind getKind() { return _kind; }
    
    public int getLineNumber() { return _lineNumber; }
    
    public String toString() { return toString(0); }
    
    /** @param indent - indicate indentation for pretty-printing. if indent <= 0
     *   then no indentation and not pretty-printing is used.
     *  The current indentation is expected to NOT be already output.
     */
    public String toString(int indent) {
      switch (_kind) {
      case Formula: return ((AnnotatedFormula)this).toString(indent);
      case Clause: return ((AnnotatedClause)this).toString(indent);
      case Include: return ((IncludeDirective)this).toString(indent);
      };
      assert false;
      return null;
    }
    

    protected Kind _kind;
    protected int _lineNumber; 
    
  } // class TopLevelItem
  
  
  /** Represents instances of &#60fof annotated&#62 in the BNF grammar. */
  public static class AnnotatedFormula extends TopLevelItem {
    
    public AnnotatedFormula(String name,
                            TptpParserOutput.FormulaRole role,
                            TptpParserOutput.FofFormula formula,
                            TptpParserOutput.Annotations annotations,
                            int lineNumber)
    {
      _kind = TopLevelItem.Kind.Formula;
      _name = name;
      _role = role;
      _formula = (Formula)formula;
      _annotations = (Annotations)annotations;
      _lineNumber = lineNumber;
    }
    
    public String getName() { return _name; }

    public TptpParserOutput.FormulaRole getRole() { return _role; }
     
    public Formula getFormula() { return _formula; }
    
    public Annotations getAnnotations() { return _annotations; }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = spaces(indent) + "fof(" + _name + "," + _role + ",";

      if (indent >= 0) { /* pretty printing is on */
        if (_formula.getKind() == Formula.Kind.Binary) {
          res += "\n" + spaces(indent+4) +"( ";
          res += _formula.toString(indent + 6);
          res += " )";
        }
        else {
          res += "(\n" + spaces(indent+4);
          res += _formula.toString(indent + 4);
          res += " )";
       }
      } 
      else { /* pretty printing is off */
        res += "( " + _formula.toString(-1) + " )";
      }

      if (_annotations != null) {
        res += "," + (indent >= 0 ? "\n" + spaces(indent+4) : "") + 
          _annotations.toString(indent >= 0 ? indent + 4 : -1);
      }
      res += ").\n";
      return res;
    }
    
    private String _name;
    private TptpParserOutput.FormulaRole _role;
    private Formula _formula;
    private Annotations _annotations;
  } // class AnnotatedFormula
  
  /** Represents instances of &#60cnf annotated&#62 in the BNF grammar. */
  public static class AnnotatedClause extends TopLevelItem {
    
    public AnnotatedClause(String name,
                           TptpParserOutput.FormulaRole role,
                           TptpParserOutput.CnfFormula clause,
                           TptpParserOutput.Annotations annotations,
                           int lineNumber)
    {
      _kind = TopLevelItem.Kind.Clause;
      _name = name;
      _role = role;
      _clause = (Clause)clause;
      _annotations = (Annotations)annotations;
      _lineNumber = lineNumber;
    }
    
    public String getName() { return _name; }

    public TptpParserOutput.FormulaRole getRole() { return _role; } 

    public Clause getClause() { return _clause; }
    
    public Annotations getAnnotations() { return _annotations; }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = spaces(indent) + "cnf(" + _name + "," + _role + ",";

      res += (indent >= 0 ? "\n" + spaces(indent+4) : "") + "( ";
      res += _clause.toString(indent >= 0 ? indent + 6 : -1);
      res += " )";

      if (_annotations != null) {
        res += "," + (indent >= 0 ? "\n" + spaces(indent+4) : " ")
          + _annotations.toString(indent >= 0 ? indent + 4 : -1);
      }
      res += ").\n";
      return res;
    }
    
    private String _name;
    private TptpParserOutput.FormulaRole _role;
    private Clause _clause;
    private Annotations _annotations;
  } // class AnnotatedClause
  
  
  /** Represents instances of &#60include&#62 in the BNF grammar. */
  public static class IncludeDirective extends TopLevelItem {
    
    public IncludeDirective(String fileName,
                            Iterable<String> formulaSelection,
                            int lineNumber)
    {
      _kind = TopLevelItem.Kind.Include;
      _fileName = fileName;
      if (formulaSelection != null) {
        _formulaSelection = new LinkedList<String>();
        for (String name : formulaSelection) 
          _formulaSelection.add(name);
      };
      _lineNumber = lineNumber;
    }
    
    public String getFileName() { return _fileName; }
    
    public Iterable<String> getFormulaSelection() { return _formulaSelection; }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = spaces(indent) + "include(" + _fileName;
      if (_formulaSelection != null) {
        res += "," + "[";
        for (int n = 0; n < _formulaSelection.size(); ++n) {
          if (n != 0) res += ",";
          res += _formulaSelection.get(n);
        };
        res += "]";
      };
      res += ").";
      return res;
    }
    
    private String _fileName;
    private LinkedList<String> _formulaSelection = null;
  } // class IncludeDirective
  
  


  public static class Formula 
  implements TptpParserOutput.FofFormula
  {
    public static enum Kind 
    {
      Atomic,
      Negation,
      Binary,
      Quantified
    }
    


    public static class Atomic extends Formula
    implements TptpParserOutput.AtomicFormula
    {
      
      public Atomic(String predicate,
                    Iterable<TptpParserOutput.Term> arguments) 
      {
        _kind = Formula.Kind.Atomic;
        _predicate = predicate;
        if (arguments != null) {
          _arguments = new LinkedList<Term>();
          for (TptpParserOutput.Term arg : arguments) {
            _arguments.add((Term)arg);
          };
        };
      }
      
      
      public String getPredicate() { return _predicate; }
      
      public int getNumberOfArguments() { 
        return (_arguments == null)? 0 : _arguments.size();
      }
      
      public Iterable<Term> getArguments() { return _arguments; }
      
      /** @param obj must be convertible to Atomic, can be null */
      public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        return _kind == ((Formula)obj)._kind && 
        _predicate.equals(((Atomic)obj)._predicate) &&
        ( _arguments == null 
        ? ((Atomic)obj)._arguments == null
        : _arguments.equals(((Atomic)obj)._arguments));
      }
      
      
      public int hashCode() {
        int res = _kind.hashCode();
        res = 31 * res + _predicate.hashCode();
        res = 31 * res;
        if (_arguments != null) res += _arguments.hashCode();
        return res;
      }
      
      
      public String toString() { return toString(0); }
      
      public String toString(int indent) {
        String res = new String();

        if (_predicate.equals("=")) { /* equality infix opertor */
          assert _arguments != null && _arguments.size() == 2;
          res += _arguments.get(0).toString();
          res += " = ";
          res += _arguments.get(1).toString();
          /* NB: special case of negated equality is processed in class Negation */
        }
        else { /* usual predicate */
          res += _predicate;
          if (_arguments != null) {
            res += "(" + _arguments.get(0).toString();
            for (int n = 1; n < _arguments.size(); ++n) {
              res += "," + _arguments.get(n).toString();
            };
          res += ")";
          };
        }
        return res;
      }
      
      
      private String _predicate;
      
      private LinkedList<Term> _arguments = null;
      
    } // class Atomic
    
    


    public static class Negation extends Formula {
      
      public Negation(TptpParserOutput.FofFormula argument) {
        _kind = Formula.Kind.Negation;
        _argument = (Formula)argument;
      }
      
      /** Returns the formula under the negation. */
      public Formula getArgument() {
        return _argument;
      }
      
      /** @param obj must be convertible to Negation, can be null */
      public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        return _kind == ((Formula)obj)._kind && 
        _argument.equals(((Negation)obj)._argument);
      }
      
      public int hasCode() { return 31 * _kind.hashCode() + _argument.hashCode(); }
      
      public String toString() { return toString(0); }
      
      public String toString(int indent) {
        String res = new String();

        /* special case -- the argument is equality => output inequality */
        if (_argument.getKind() == Kind.Atomic &&
            ((Atomic)_argument).getPredicate().equals("=")) {
          assert ((Atomic)_argument).getNumberOfArguments() == 2;
          Iterator<Term> arguments = ((Atomic)_argument).getArguments().iterator();

          res += arguments.next().toString();
          res += " != ";
          res += arguments.next().toString();
        }
        else {
          res += "~ ";
          if (_argument.getKind() == Kind.Binary) {
            res += ((Binary)_argument).toStringWithParantheses(indent+2);
          }
          else {
            res += _argument.toString(indent+2);
          }
        }
        return res;
      }
      
      
      private Formula _argument;
    } // class Negation
    


    
    public static class Binary extends Formula {
      
      public Binary(TptpParserOutput.FofFormula lhs,
                    TptpParserOutput.BinaryConnective connective,
                    TptpParserOutput.FofFormula rhs)
      {
        _kind = Formula.Kind.Binary;
        _lhs = (Formula)lhs;
        _connective = connective;
        _rhs = (Formula)rhs;
      }
      
      public TptpParserOutput.BinaryConnective getConnective() {
        return _connective;
      }
      
      public Formula getLhs() { return _lhs; }
      
      public Formula getRhs() { return _rhs; }
      
      /** @param obj must be convertible to Binary, can be null */
      public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        return _kind == ((Formula)obj)._kind && 
        _connective == ((Binary)obj)._connective &&
        _lhs.equals(((Binary)obj)._lhs) &&
        _rhs.equals(((Binary)obj)._rhs);
      }
      
      public int hashCode() {
        int res = _kind.hashCode();
        res = 31 * res + _connective.hashCode();
        res = 31 * res + _lhs.hashCode();
        res = 31 * res + _rhs.hashCode();
        return res;
      }
      
      
      public String toString() { return toString(0, null); }
      
      public String toString(int indent) { return toString(indent, null); }

      public String toStringWithParantheses(int indent) {
        /* provide some connective to the second argument but not AND or OR */
        return toString(indent, TptpParserOutput.BinaryConnective.Equivalence); 
      }

      public String toString(int indent,
                             TptpParserOutput.BinaryConnective connective) {
        /* this function has additional argument "connective" to detect cases
           when it is or it is not necessary to add parantheses around binary
           expression.
           "connective" may have 4 value:
             null - parantheses are not required in any case
             AND - parantheses are not required only if the exp is AND
             OR - parantheses are not required only if the exp is OR
             anything else - parantheses are always required.
        */
        String res = new String();
        boolean areParanthesesRequired;
        int connectiveIndent;

        /* parantheses ARE NOT required */
        if (connective == null ||
            ((connective == BinaryConnective.And || 
              connective == BinaryConnective.Or) && 
             connective == _connective)) {
          areParanthesesRequired = false;
          connectiveIndent = indent - _connective.toString().length() - 1;
        }
        /* parantheses ARE required */
        else {
          areParanthesesRequired = true;
          res += "( ";
          connectiveIndent = indent - _connective.toString().length() + 1;
          indent += 2;
       }

        if (_lhs.getKind() == Kind.Binary) {
          res += ((Binary)_lhs).toString(indent, _connective);
        }
        else {
          res += _lhs.toString(indent);
        }
        
        res += indent >= 0 ? "\n" + spaces(connectiveIndent) : " ";
        res += _connective + " ";

        if (_rhs.getKind() == Kind.Binary) {
          res += ((Binary)_rhs).toString(indent, _connective);
        }
        else {
          res += _rhs.toString(indent);
        }

        if (areParanthesesRequired) {
          res += " )";
        }
        return res;
      }
      
      
      private Formula _lhs;
      private TptpParserOutput.BinaryConnective _connective;
      private Formula _rhs;
    } // class Binary
    
    
    
    
    
    public static class Quantified extends Formula {
      
      public Quantified(TptpParserOutput.Quantifier quantifier,
                        String variable,
                        TptpParserOutput.FofFormula matrix)
      {
        _kind = Formula.Kind.Quantified;
        _quantifier = quantifier;
        _variable = variable;
        _matrix = (Formula)matrix;
      }
      
      public TptpParserOutput.Quantifier getQuantifier() {
        return _quantifier;
      }
      
      public String getVariable() { return _variable; }
      
      public Formula getMatrix() { return _matrix; }
      
      /** @param obj must be convertible to Quantified, can be null */
      public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        return _kind == ((Formula)obj)._kind && 
        _quantifier == ((Quantified)obj)._quantifier &&
        _variable.equals(((Quantified)obj)._variable) &&
        _matrix.equals(((Quantified)obj)._matrix);
      }
      
      public int hashCode() {
        int res = _kind.hashCode();
        res = 31 * res + _quantifier.hashCode();
        res = 31 * res + _variable.hashCode();
        res = 31 * res + _matrix.hashCode();
        return res;
      }
      
      
      public String toString() { return toString(0, true); }
      
      public String toString(int indent) { return toString(indent, true); }

      public String toString(int indent, boolean outputQuantifier) {
        /* additional parameter "outputQuantifier" is used to output nested 
           quantifiers of the same kind. Possible values are
           true -- the quantifier must be output now.
           false -- the quantifier was already output and may be not output now.
        */
        String res = new String();
        if (outputQuantifier) {
          res += _quantifier + " [" + _variable;
        }
        else {
          res += "," + _variable; /* quantifier has been already output */
        }

        /* the exp is the same quantifier */
        if (indent >= 0 && 
            _matrix.getKind() == Kind.Quantified &&
            getQuantifier() == ((Quantified)_matrix).getQuantifier()) {
          res += ((Quantified)_matrix).toString(indent, false);
        }
        /* the exp is not the same quantifier */
        else {
          /* this is the last variable in the list of the same quantifiers =>
             close the list and output the expression */
          res += "] : ";          
          
          switch (_matrix.getKind()) {
          case Atomic:
          case Negation:
            res += _matrix.toString(indent);
            break;

          case Binary:
            if (indent >= 0) {
              indent += 2;
              res += "\n" + spaces(indent);
            }
            /* binary expressions require parantheses */
            res += ((Binary)_matrix).toStringWithParantheses(indent);
            break;

          case Quantified:
            res += indent >= 0 ? "\n" + spaces(indent) : "";
            res += _matrix.toString(indent);
            break;
          }
        }
        
        return res;
      }
      
      
      private TptpParserOutput.Quantifier _quantifier;
      private String _variable;
      private Formula _matrix;
    } // class Quantified
    
    
    
    
    public Kind getKind() { return _kind; }
    
    /** @param obj must be convertible to Formula, can be null */
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      if (_kind != ((Formula)obj)._kind) return false;
      switch (_kind) 
      {
        case Atomic: 
        return ((Atomic)this).equals((Atomic)obj);
        case Negation:
        return ((Negation)this).equals((Negation)obj);
        case Binary:
        return ((Binary)this).equals((Binary)obj);
        case Quantified:
        return ((Quantified)this).equals((Quantified)obj);
      };
      assert false;
      return false;
    } // equals(Object obj)
    
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      switch (_kind) 
      {
        case Atomic: return ((Atomic)this).toString(indent);
        case Negation: return ((Negation)this).toString(indent);
        case Binary: return ((Binary)this).toString(indent);
        case Quantified: return ((Quantified)this).toString(indent);
      };
      assert false;
      return null;
    }
    
    
    
    //================== Attributes: =========================
    
    protected Kind _kind;
    
  }; //  class Formula
  
  
  
  
  
  public static class Clause 
  implements TptpParserOutput.CnfFormula
  {
    public Clause(Iterable<TptpParserOutput.Literal> literals) {
      if (literals != null) {
        _literals = new LinkedList<Literal>();
        for (TptpParserOutput.Literal lit : literals) {
          _literals.add((Literal)lit);
        };
      };
    }
    
    public Iterable<Literal> getLiterals() { return _literals; } 
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      /* old style literal is converted to false clause*/
      if (_literals == null) return "$false";
      
      assert !_literals.isEmpty();
      String res = _literals.get(0).toString(indent);
      for (int n = 1; n < _literals.size(); ++n)
        res += (indent >= 0 ? "\n" + spaces(indent-3) : "") 
          + " | " +  _literals.get(n).toString(indent);
      return res;
    }
    
    
    
    private LinkedList<Literal> _literals = null;
    
  }; //  class Clause
  
  
  
  
  
  public static class Literal 
  implements TptpParserOutput.Literal
  {
    
    public Literal(boolean positive,
                   TptpParserOutput.AtomicFormula atom)
    {
      _isPositive = positive;
      _atom = (Formula.Atomic)atom;
    }
    
    public boolean isPositive() { return _isPositive; }
    
    public Formula.Atomic getAtom() { return _atom; }
    
    /** @param obj must be convertible to Literal, can be null */
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      return _isPositive == ((Literal)obj)._isPositive &&
      _atom.equals(((Literal)obj)._atom);
      
    }
    
    public int hashCode() {
      return 31 * _atom.hashCode() + (_isPositive? 1 : 0);
    }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = new String();

      if (_isPositive) {
        res += _atom.toString(indent);
      } 
      else {
        if (_atom.getPredicate().equals("=")) {
          assert _atom.getNumberOfArguments() == 2;
          Iterator<Term> arguments = _atom.getArguments().iterator();

          res += arguments.next().toString();
          res += " != ";
          res += arguments.next().toString();
        } 
        else {
          res+= "~ " + _atom.toString(indent);
        }
      }
      return res;
    }
    
    
    
    private boolean _isPositive;
    private Formula.Atomic _atom;
    
  }; //  class Literal
  
  
  
  
  public static class Term 
  implements TptpParserOutput.Term
  {  
    
    public Term(Symbol topSymbol,
                Iterable<TptpParserOutput.Term> arguments)
    {
      _topSymbol = topSymbol;
      if (arguments != null) {
        _arguments = new LinkedList<Term>();
        for (TptpParserOutput.Term arg : arguments) {
          _arguments.add((Term)arg);
        };
      };
    }
    
    public Symbol getTopSymbol() { return _topSymbol; }
    
    public int getNumberOfArguments() { 
      return (_arguments == null)? 0 : _arguments.size();
    }
    
    public Iterable<Term> getArguments() { return _arguments; }
    
    /** @param obj must be convertible to Term, can be null */
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (this == obj) return true;
      return _topSymbol.equals(((Term)obj)._topSymbol) &&
      (_arguments == null? 
       ((Term)obj)._arguments == null
       :
       _arguments.equals(((Term)obj)._arguments));
    }
    
    public int hashCode() {
      return 31 * _topSymbol.hashCode() + 
      ((_arguments == null)? 0 : _arguments.hashCode());
    }
    
    public String toString() {
      String res = _topSymbol.toString();

      if (_arguments != null) 
      {
        assert !_arguments.isEmpty();
        res += "(" + _arguments.get(0).toString();
        for (int n = 1; n < _arguments.size(); ++n)
          res += "," + _arguments.get(n).toString();
        res += ")";
      };
      return res;
    }
    
    
    
    private Symbol _topSymbol;
    
    private LinkedList<Term> _arguments = null;
    
  }; // class Term
  
  
  
  public static class Symbol {
    
    public Symbol(String text,boolean isVariable) {
      _text = text;
      _isVariable = isVariable;
    }
    
    public boolean isVariable() { return _isVariable; }
    
    public String getText() { return _text; }
    
    /** @param obj must be convertible to Symbol, can be null */
    public boolean equals(Object obj) {
      if (obj == null) return false;
      return isVariable() == ((Symbol)obj).isVariable() &&
      _text.equals(((Symbol)obj)._text);
    }
    
    public int hashCode() {
      return 31 * _text.hashCode() + ((_isVariable)? 1 : 0);
    }
    
    public String toString() {
      return _text;
    }
    
    
    private String _text; 
    
    private boolean _isVariable;
    
  } // class Symbol
  
  
  
  public static class Annotations 
  implements TptpParserOutput.Annotations
  {
    public Annotations(TptpParserOutput.Source source,
                       Iterable<TptpParserOutput.InfoItem> usefulInfo)
    {
      assert source != null;
      _source = (Source)source;
      if (usefulInfo != null) {
        _usefulInfo = new LinkedList<InfoItem>();
        for (TptpParserOutput.InfoItem item : usefulInfo)
          _usefulInfo.add((InfoItem)item);
      };
    }
    
    public Source getSource() { return _source; }
    
    public Iterable<InfoItem> usefulInfo() { return _usefulInfo; }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = _source.toString(indent);
      if (_usefulInfo != null)
      {
        assert !_usefulInfo.isEmpty();
        res += "," + (indent >= 0 ? "\n" + spaces(indent) : " ") + "[";
        res += _usefulInfo.get(0).toString(indent);
        for (int n = 1; n < _usefulInfo.size(); ++n) 
          res += "," + _usefulInfo.get(n).toString(indent);
        res += "]";
      };
      return res;
    }
    
    
    private Source _source;
    private LinkedList<InfoItem> _usefulInfo = null;
    
  }; //  class Annotations
  
  
  public static class Source
  implements TptpParserOutput.Source
  {  
    public static enum Kind 
    {
      Name,
      Inference,
      Internal,
      File,
      Creator,
      Theory
    }
    
    public Kind getKind() { return _kind; }

    public static class Name extends Source {
      
      public Name(String text) {
        _kind = Kind.Name;
        _text = text;
      }
      
      String getText() { return _text; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) { return _text; }
      
      private String _text;
      
    } // class Name 
    
    public static class Inference extends Source {
      
      public 
      Inference(String inferenceRule,
                Iterable<TptpParserOutput.InfoItem> usefulInfo,
                Iterable<TptpParserOutput.ParentInfo> parentInfoList)
      {
        _kind = Kind.Inference;        
        _inferenceRule = inferenceRule;
        if (usefulInfo != null) {
          _usefulInfo = new LinkedList<InfoItem>();
          for (TptpParserOutput.InfoItem item : usefulInfo) 
            _usefulInfo.add((InfoItem)item);
        };
        if (parentInfoList != null) {
          _parentInfoList = new LinkedList<ParentInfo>();
          for (TptpParserOutput.ParentInfo par : parentInfoList)
            _parentInfoList.add((ParentInfo)par);
        };
      }
      
      public String getInferenceRule() { return _inferenceRule; }
      
      /** May return null. */
      public Iterable<InfoItem> getUsefulInfo() { return  _usefulInfo; }
      
      public Iterable<ParentInfo> getParentInfoList() { return _parentInfoList; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        String res = "inference(" + _inferenceRule + ",[";
        if (_usefulInfo != null) {
          assert !_usefulInfo.isEmpty();
          res += _usefulInfo.get(0).toString(indent);
          for (int n = 1; n < _usefulInfo.size(); ++n)
            res += ", " + _usefulInfo.get(n).toString(indent);
        };
        res += "],[";
        if (_parentInfoList != null) {
          res += _parentInfoList.get(0).toString(indent);
          for (int n = 1; n < _parentInfoList.size(); ++n)
            res += "," + _parentInfoList.get(n).toString(indent);
        };
        res += "])";
        return res;
      }
      
      
      private String _inferenceRule;
      
      private LinkedList<InfoItem> _usefulInfo = null;
      
      private LinkedList<ParentInfo> _parentInfoList = null;
      
    } // class Inference 
    
    
    public static class Internal extends Source {
      
      public Internal(TptpParserOutput.IntroType introType,
                      Iterable<TptpParserOutput.InfoItem> introInfo) {
        _kind = Kind.Internal;
        _introType = (IntroType)introType;
        if (introInfo != null) {
          _introInfo = new LinkedList<InfoItem>();
          for (TptpParserOutput.InfoItem item : introInfo)
            _introInfo.add((InfoItem)item);
        }
        else _introInfo = null;
        ;
      }
      
      public IntroType getIntroType() { return _introType; }
      
      public Iterable<InfoItem> getIntroInfo() { return _introInfo; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        String res = "introduced(" + _introType;
        if (_introInfo != null) {
          assert !_introInfo.isEmpty();
          res += ",[" + _introInfo.get(0).toString(indent);
          for (int n = 1; n < _introInfo.size(); ++n) 
            res += "," + _introInfo.get(n).toString(indent);
          res += "]";
        };
        res += ")";
        return res;
      }
      
      
      
      private IntroType _introType;
      
      private LinkedList<InfoItem> _introInfo = null;
      
    } // class Internal
    
    public static class File extends Source {
      
      public File(String fileName,String fileInfo) {
        _kind = Kind.File;
        _fileName = fileName;
        _fileInfo = fileInfo;
      }
      
      public String getFileName() { return _fileName; }
      
      public String getFileInfo() { return _fileInfo; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        String res = "file(" + _fileName;
        if (_fileInfo != null) res += "," + _fileInfo;
        res += ")";
        return res;
      }
      
      
      
      private String _fileName;
      
      /** If null, corresponds to the value 'unknown'. */
      private String _fileInfo;
      
    } // class File
    
    
    
    public static class Creator extends Source {
      
      public Creator(String creatorName,
                     Iterable<TptpParserOutput.InfoItem> usefulInfo) 
      {
        _kind = Kind.Creator;
        _creatorName = creatorName;
        if (usefulInfo != null) {
          _usefulInfo = new LinkedList<InfoItem>();
          for (TptpParserOutput.InfoItem item : usefulInfo)
            _usefulInfo.add((InfoItem)item);
        }
      }
      
      public String getCreatorName() { return _creatorName; }
      
      public Iterable<InfoItem> getUsefulInfo() { return _usefulInfo; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) { 
        String res = "creator(" + _creatorName;
        if (_usefulInfo != null) {
          assert !_usefulInfo.isEmpty();
          res += ",[" + _usefulInfo.get(0).toString(indent);
          for (int n = 1; n < _usefulInfo.size(); ++n)
            res += "," + _usefulInfo.get(n).toString(indent);
          res += "]";
        };
        res += ")";
        return res;
      }
      
      
      private String _creatorName;
      
      private LinkedList<InfoItem> _usefulInfo = null;
      
    } // class Creator
    
    
    
    
    public static class Theory extends Source {
      
      public Theory(String theoryName,
                    Iterable<TptpParserOutput.InfoItem> usefulInfo) {
        _kind = Kind.Theory;
        _theoryName = theoryName;
        if (usefulInfo != null) {
          _usefulInfo = new LinkedList<InfoItem>();
          for (TptpParserOutput.InfoItem item : usefulInfo)
            _usefulInfo.add((InfoItem)item);
        }
      }
      
      public String getTheoryName() { return _theoryName; }

      public Iterable<InfoItem> getUsefulInfo() { return _usefulInfo; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        String res = "theory(" + _theoryName;
        if (_usefulInfo != null) {
          assert !_usefulInfo.isEmpty();
          res += ",[" + _usefulInfo.get(0).toString(indent);
          for (int n = 1; n < _usefulInfo.size(); ++n)
            res += "," + _usefulInfo.get(n).toString(indent);
          res += "]";
        };
        res += ")";
        return res;
      }
      
      
      
      
      private String _theoryName;
      
      private LinkedList<InfoItem> _usefulInfo = null;
    } // class Theory
    
    
    
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      switch (_kind) 
      {
        case Name: return ((Name)this).toString(indent);
        case Inference: return ((Inference)this).toString(indent);
        case Internal: return ((Internal)this).toString(indent);
        case File: return ((File)this).toString(indent);
        case Creator: return ((Creator)this).toString(indent);
        case Theory: return ((Theory)this).toString(indent);
      };
      assert false;
      return null;
    }
    
    
    
    
    protected Kind _kind;
    
  }; //  class Source
  
  
  
  
  
  public static class InfoItem
  implements TptpParserOutput.InfoItem
  {  
    public static enum Kind 
    {
      Description,
      IQuote,
      InferenceStatus,
      InferenceRule,
      AssumptionRecord,
      Refutation,
      GeneralFunction
    }

    public Kind getKind() { return _kind; }
    
    public static class Description extends InfoItem {
      
      public Description(String description) {
        _kind = Kind.Description;
        _description = description;
      }
      
      public String getDescription() { return _description; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        return "description(" + _description + ")";
      }
      
      
      private String _description;
      
    } // class Description
    
    
    
    public static class IQuote extends InfoItem {
      
      public IQuote(String text) {
        _kind = Kind.IQuote;
        _text = text;
      }
      
      public String getIQuoteText() { return _text; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        return "iquote(" + _text + ")";
      }
      
      
      
      private String _text;
      
    } // class IQuote
    
    
    
    public static class InferenceStatus extends InfoItem {
      
      public 
      InferenceStatus(TptpParserOutput.StatusValue statusValue) {
        _kind = Kind.InferenceStatus;
        _statusValue = statusValue;
      }
      
      public TptpParserOutput.StatusValue getStatusValue() {
        return _statusValue;
      }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        return "status(" + _statusValue + ")";
      }
      
      
      
      private TptpParserOutput.StatusValue _statusValue;
      
    } // class InferenceStatus
    
    
    
    public static class InferenceRule extends InfoItem {
      
      public 
      InferenceRule(String inferenceRule,
                    String inferenceId,
                    Iterable<TptpParserOutput.GeneralTerm> attributes) {
        _kind = Kind.InferenceRule;
        _inferenceRule = inferenceRule;
        _inferenceId = inferenceId;
        if (attributes != null) {
          _attributes = 
          new LinkedList<GeneralTerm>();
          for (TptpParserOutput.GeneralTerm term : attributes)
            _attributes.add((GeneralTerm)term);
        };
      }
      
      
      public String getIinferenceRule() { return _inferenceRule; }
      
      public String getInferenceId() { return _inferenceId; }
      
      public Iterable<GeneralTerm> getAttributes() { return _attributes; } 
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        String res = _inferenceRule + "(" + _inferenceId + ",[";
        if (_attributes != null) {
          assert !_attributes.isEmpty();
          res += _attributes.get(0).toString(indent);
          for (int n = 1; n < _attributes.size(); ++n)
            res += "," + _attributes.get(n).toString(indent);
        };
        res += "])";
        return res;
      }
      
      
      private String _inferenceRule;
      
      private String _inferenceId;
      
      private 
      LinkedList<GeneralTerm> _attributes = null;
      
    } // class InferenceRule



    public static class AssumptionRecord extends InfoItem {
      
      public 
      AssumptionRecord(Iterable<String> nameList) {
        assert nameList != null;

        _nameList = new LinkedList<String>();
        for (String name : nameList)
          _nameList.add(name);

        assert !_nameList.isEmpty();
      }
      
      public Iterable<String> getNameList() { return _nameList; }
      
      public String toString() { return toString(0); }
    
      public String toString(int indent) {
        String res = "assumption([";
        assert !_nameList.isEmpty();
        res += _nameList.get(0);
        for (int n = 1; n < _nameList.size(); ++n)
          res += "," + _nameList.get(n);
        res += "])";
        return res;
      }

      private 
      LinkedList<String> _nameList = null;
      
    } // class AssumptionRecord
      
    
    
    
    public static class Refutation extends InfoItem {
      
      public Refutation(TptpParserOutput.Source fileSource) {
        _kind = Kind.Refutation;
        _fileSource = (Source)fileSource;
      }
      
      public Source getFileSource() { return _fileSource; }
      
      public String toString() { return toString(0); }
    
    public String toString(int indent) {
        return "refutation(" + _fileSource + ")";
      }
       
      
      private Source _fileSource;
      
    } // class Refutation
    
    
    
    public static class GeneralFunction extends InfoItem {
      
      public 
      GeneralFunction(TptpParserOutput.GeneralTerm generalFunction) {
        _kind = Kind.GeneralFunction;
        _generalFunction = (GeneralTerm)generalFunction;
      }
      
      
      public GeneralTerm getGeneralFunction() { 
        return _generalFunction;
      }
      
      public String toString() { return toString(0); }
    
    public String toString(int indent) {
        return _generalFunction.toString(indent);
      }
      
      
      private GeneralTerm _generalFunction;
      
    } // class GeneralFunction
    
    
    
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      switch (_kind)
      {
        case Description: return ((Description)this).toString(indent);
        case IQuote: return ((IQuote)this).toString(indent);
        case InferenceStatus: return ((InferenceStatus)this).toString(indent);
        case InferenceRule: return ((InferenceRule)this).toString(indent);
        case AssumptionRecord: return ((AssumptionRecord)this).toString(indent);
        case Refutation: return ((Refutation)this).toString(indent);
        case GeneralFunction: return ((GeneralFunction)this).toString(indent);
      };
      assert false;
      return null;
    }
    
    
    protected Kind _kind;
    
  }; //  class InfoItem
  
  
  public static class GeneralTerm
  implements TptpParserOutput.GeneralTerm
  {
    public GeneralTerm(String function,
                       Iterable<TptpParserOutput.GeneralTerm> arguments) {
      assert function != null;
      _kind = GeneralTermKind.Function;
      _str = function;
      if (arguments != null) {
        _arguments = new LinkedList<GeneralTerm>();
        for (TptpParserOutput.GeneralTerm arg : arguments)
          _arguments.add((GeneralTerm)arg);
      };
      _left = null;
      _right = null;
      _fof_formula = null;
      _cnf_formula = null;
      _term = null;
    }
    
    public GeneralTerm(Iterable<TptpParserOutput.GeneralTerm> elements) {
      _kind = GeneralTermKind.List;
      if (elements != null) {
        _arguments = new LinkedList<GeneralTerm>();
        for (TptpParserOutput.GeneralTerm el : elements)
          _arguments.add((GeneralTerm)el);
      };
      _str = null;
      _left = null;
      _right = null;
      _fof_formula = null;
      _cnf_formula = null;
      _term = null;
    }

    public GeneralTerm(TptpParserOutput.GeneralTerm left,
                       TptpParserOutput.GeneralTerm right) {
      _kind = GeneralTermKind.Colon;
      assert left != null;
      assert right != null;
      _left = (GeneralTerm)left;
      _right = (GeneralTerm)right;
      _str = null;
      _arguments = null;
      _fof_formula = null;
      _cnf_formula = null;
      _term = null;
    }
    
    public GeneralTerm(String str) {
      _kind = GeneralTermKind.DistinctObject;
      assert str != null;
      _str = str;
      _left = null;
      _right = null;
      _arguments = null;
      _fof_formula = null;
      _cnf_formula = null;
      _term = null;
    }

    public GeneralTerm(TptpParserOutput.FofFormula formula) {
      _kind = GeneralTermKind.FofFormula;
      _str = null;
      _left = null;
      _right = null;
      _arguments = null;
      _cnf_formula = null;
      _term = null;

      assert formula != null;
      _fof_formula = (Formula)formula;
   }

    public GeneralTerm(TptpParserOutput.CnfFormula formula) {
      _kind = GeneralTermKind.CnfFormula;
      _str = null;
      _left = null;
      _right = null;
      _arguments = null;
      _fof_formula = null;
      _term = null;

      assert formula != null;
      _cnf_formula = (Clause)formula;
    }

    public GeneralTerm(TptpParserOutput.Term term) {
      _kind = GeneralTermKind.Term;
      _str = null;
      _left = null;
      _right = null;
      _arguments = null;
      _fof_formula = null;
      _cnf_formula = null;
      assert term != null;
      _term = (Term)term;
    }

    public boolean isFunction() { return _kind == GeneralTermKind.Function; }
    public boolean isList() { return _kind == GeneralTermKind.List; }
    public boolean isColon() { return _kind == GeneralTermKind.Colon; }
    public boolean isDistinctObject() 
                          { return _kind == GeneralTermKind.DistinctObject; }
    public boolean isFofFormula() { return _kind == GeneralTermKind.FofFormula; }
    public boolean isCnfFormula() { return _kind == GeneralTermKind.CnfFormula; }
    public boolean isTerm()       { return _kind == GeneralTermKind.Term; }
    public boolean isVariable() { return _kind == GeneralTermKind.Variable; }
    
    /** Precondition: isFunction(). */
    public String getFunction() {
      assert isFunction();
      return _str;
    }
    
    /** Precondition: isFunction(). */
    public Iterable<GeneralTerm> getArguments() {
      assert isFunction();
      return _arguments;
    }
    
    /** Precondition: isList(). */
    public Iterable<GeneralTerm> getListElements() {
      assert isList();
      return _arguments;
    }
    
    /** Precondition: isColon(). */
    public GeneralTerm getLeftColonOperand() {
      assert isColon();
      return _left;
    }

    /** Precondition: isColon(). */
    public GeneralTerm getRightColonOperand() {
      assert isColon();
      return _right;
    }
    
    /** Precondition: isDistinctObject(). */
    public String getDistinctObject() {
      assert isDistinctObject();
      return _str;
    }
    
    /** Precondition: isFofFormula(). */
    public Formula getFofFormula() {
      assert isFofFormula();
      return _fof_formula;
    }
    
    /** Precondition: isCnfFormula(). */
    public Clause getCnfFormula() {
      assert isCnfFormula();
      return _cnf_formula;
    }

    /** Precondition: isTerm(). */
    public Term getTerm() {
      assert isTerm();
      return _term;
    }

    /** Precondition: isVariable(). */
    public String getVariable() {
      assert isVariable();
      return _str;
    }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = new String();
      switch (_kind) {
      case Function:
        res += _str;
        if (_arguments == null) return res; /* this is a <constant> */
        assert !_arguments.isEmpty();
        res += "(";
        res += _arguments.get(0).toString(indent);
        for (int n = 1; n < _arguments.size(); ++n) 
          res += "," + _arguments.get(n).toString(indent);
        res += ")";
        break;
      case List:
        /* this is empty <general list> */
        if (_arguments == null) return res + "[]";
        res += "[";
        res += _arguments.get(0).toString(indent);
        for (int n = 1; n < _arguments.size(); ++n) 
          res += "," + _arguments.get(n).toString(indent);
        res += "]";
        break;
      case Colon:
        res += _left.toString(indent);
        res += ":";
        res += _right.toString(indent);
        break;
      case DistinctObject:
        res += _str;
        break;
      case FofFormula:
        res += "$fof(" + _fof_formula.toString(indent) + ")";
        break;
      case CnfFormula:
        res += "$cnf(" + _cnf_formula.toString(indent) + ")";
        break;
      case Term:
        res += "$fot(" + _term.toString() + ")";
        break;
      case Variable:
        res += _str;
        break;
      }
      return res;
    }
    
    private enum GeneralTermKind {
      Function,
      List,
      Colon,
      DistinctObject,
      FofFormula,
      CnfFormula,
      Term,
      Variable
    };
    
    private GeneralTermKind _kind;
    
    private String _str;
    private LinkedList<GeneralTerm> _arguments = null;
    private GeneralTerm _left;
    private GeneralTerm _right;
    private Formula _fof_formula;
    private Clause _cnf_formula;
    private Term _term;
  }; //  class GeneralTerm
  
  
  
  public static class ParentInfo implements TptpParserOutput.ParentInfo {
    
    public ParentInfo(TptpParserOutput.Source source,
                      TptpParserOutput.GeneralTerm parentDetails) {
      _source = (Source)source;
      _parentDetails = (GeneralTerm)parentDetails;
    }
    
    public Source getSource() { return _source; }
    
    public GeneralTerm getParentDetails() { return _parentDetails; }
    
    public String toString() { return toString(0); }
    
    public String toString(int indent) {
      String res = _source.toString(indent);
      if (_parentDetails != null)
        res += ":" + _parentDetails;
      return res;
    }
    
    
    private Source _source;
    
    private GeneralTerm _parentDetails;
    
  }; // class ParentInfo
  
  
  /*------------------------------------------------------------*/
  /*    Methods prescribed by the interface TptpParserOutput:   */
  /*------------------------------------------------------------*/
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.TptpInput
  createFofAnnotated(String name,
                     TptpParserOutput.FormulaRole role,
                     TptpParserOutput.FofFormula formula,
                     TptpParserOutput.Annotations annotations,
                     int lineNumber)
  {
    return 
    (TptpParserOutput.TptpInput)(new AnnotatedFormula(sharedCopyOf(name),
                                                      role,
                                                      formula,
                                                      annotations,
                                                      lineNumber));
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.TptpInput
  createCnfAnnotated(String name,
                     TptpParserOutput.FormulaRole role,
                     TptpParserOutput.CnfFormula clause,
                     TptpParserOutput.Annotations annotations,
                     int lineNumber)
  {
    
    return 
    (TptpParserOutput.TptpInput)(new AnnotatedClause(sharedCopyOf(name),
                                                     role,
                                                     clause,
                                                     annotations,
                                                     lineNumber));
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpInput
  createIncludeDirective(String fileName,
                         Iterable<String> formulaSelection,
                         int lineNumber) {
    // strings in formulaSelection don't get shared,
    // there seems to be no practical need for this
    return 
    (TptpParserOutput.TptpInput)(new IncludeDirective(sharedCopyOf(fileName),
                                                      formulaSelection,
                                                      lineNumber));
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.FofFormula 
  createBinaryFormula(TptpParserOutput.FofFormula lhs,
                      TptpParserOutput.BinaryConnective connective,
                      TptpParserOutput.FofFormula rhs) 
  {
    Formula key = new Formula.Binary(lhs,connective,rhs);
    Formula res = _formulaTable.get(key);
    if (res == null) {
      _formulaTable.put(key,key);
      return key;
    }
    else
      return res;   
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.FofFormula
  createNegationOf(TptpParserOutput.FofFormula formula) {
    Formula key = new Formula.Negation(formula);
    Formula res = _formulaTable.get(key);
    if (res == null) {
      _formulaTable.put(key,key);
      return key;
    }
    else
      return res;   
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput.
      For this implementation the list of variables is expected to be reversed */
  public 
  TptpParserOutput.FofFormula
  createQuantifiedFormula(TptpParserOutput.Quantifier quantifier,
                          Iterable<String> variableList,
                          TptpParserOutput.FofFormula formula) 
  {
    assert variableList != null && variableList.iterator().hasNext();
    TptpParserOutput.FofFormula key = formula;
    for (String var : variableList) {
      key = 
      (TptpParserOutput.FofFormula)(new Formula.Quantified(quantifier,
                                                           sharedCopyOf(var),
                                                           key));
    };
    
    // Here the key is fully formed
    
    Formula res = _formulaTable.get((Formula)key);
    if (res == null) {
      _formulaTable.put((Formula)key,(Formula)key);
      return key;
    }
    else
      return res;
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.CnfFormula 
  createClause(Iterable<TptpParserOutput.Literal> literals) {
    return (TptpParserOutput.CnfFormula)(new Clause(literals));
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.FofFormula 
  atomAsFormula(TptpParserOutput.AtomicFormula atom) {
    return (TptpParserOutput.FofFormula)((Formula.Atomic)atom);
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Literal 
  createLiteral(boolean positive,TptpParserOutput.AtomicFormula atom) {
    Literal key = new Literal(positive,atom);
    Literal res = _literalTable.get(key);
    if (res == null) {
      _literalTable.put(key,key);
      return key;
    }
    else
      return res;   
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.AtomicFormula
  createPlainAtom(String predicate,
                  Iterable<TptpParserOutput.Term> arguments) 
  {
    assert arguments == null || arguments.iterator().hasNext();
    Formula key = 
    new Formula.Atomic(sharedCopyOf(predicate),arguments);
    Formula res = _formulaTable.get(key);
    if (res == null) {
      _formulaTable.put(key,key);
      return (TptpParserOutput.AtomicFormula)key;
    }
    else
      return (TptpParserOutput.AtomicFormula)res;   
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.AtomicFormula
  createSystemAtom(String predicate,
                   Iterable<TptpParserOutput.Term> arguments) 
  {
    assert arguments == null || arguments.iterator().hasNext();
    Formula key = 
    new Formula.Atomic(sharedCopyOf(predicate),arguments);
    Formula res = _formulaTable.get(key);
    if (res == null) {
      _formulaTable.put(key,key);
      return (TptpParserOutput.AtomicFormula)key;
    }
    else
      return (TptpParserOutput.AtomicFormula)res;
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public
  TptpParserOutput.AtomicFormula
  createEqualityAtom(TptpParserOutput.Term lhs, TptpParserOutput.Term rhs) {
    String predicate = new String("=");
    LinkedList<TptpParserOutput.Term> arguments = 
    new LinkedList<TptpParserOutput.Term>();
    arguments.add(lhs);
    arguments.add(rhs);
    return createPlainAtom(predicate,arguments);
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public TptpParserOutput.AtomicFormula builtInTrue() {
    return createPlainAtom(new String("$true"),null);
  }
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public TptpParserOutput.AtomicFormula builtInFalse() {
    return createPlainAtom(new String("$false"),null);
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Term 
  createVariableTerm(String variable) 
  {
    Symbol sym = new Symbol(sharedCopyOf(variable),true);
    Term key = new Term(sym,null);
    Term res = _termTable.get(key);
    if (res == null) {
      _termTable.put(key,key);
      return key;
    }
    else
      return res;      
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Term 
  createPlainTerm(String function,
                  Iterable<TptpParserOutput.Term> arguments) 
  {
    Symbol sym = new Symbol(sharedCopyOf(function),false);
    Term key = new Term(sym,arguments);
    Term res = _termTable.get(key);
    if (res == null) {
      _termTable.put(key,key);
      return key;
    }
    else
      return res;
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Term 
  createSystemTerm(String function,
                   Iterable<TptpParserOutput.Term> arguments) 
  {
    Symbol sym = new Symbol(sharedCopyOf(function),false);
    Term key = new Term(sym,arguments);
    Term res = _termTable.get(key);
    if (res == null) {
      _termTable.put(key,key);
      return key;
    }
    else
      return res;
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Annotations 
  createAnnotations(TptpParserOutput.Source source,
                    Iterable<TptpParserOutput.InfoItem> usefulInfo) {
    return new Annotations(source,usefulInfo);
  }
  
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public TptpParserOutput.Source createSourceFromName(String name) {
    return new Source.Name(sharedCopyOf(name));
  }
  
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Source 
  createSourceFromInferenceRecord(String inferenceRule,
                                  Iterable<TptpParserOutput.InfoItem> usefulInfo,
                                  Iterable<TptpParserOutput.ParentInfo> parentInfoList)
  {
    return new Source.Inference(sharedCopyOf(inferenceRule),
                                usefulInfo,
                                parentInfoList);
  }
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Source 
  createInternalSource(TptpParserOutput.IntroType introType,
                       Iterable<TptpParserOutput.InfoItem> introInfo) {
    return new Source.Internal(introType,introInfo);
  }
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Source createSourceFromFile(String fileName,
                                               String fileInfo) {
    return 
    new Source.File(sharedCopyOf(fileName),
                    fileInfo != null ? sharedCopyOf(fileInfo) : null);
  }
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Source 
  createSourceFromCreator(String creatorName,
                          Iterable<TptpParserOutput.InfoItem> usefulInfo)
  {
    return 
    new Source.Creator(sharedCopyOf(creatorName), usefulInfo);
  }
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.Source
  createSourceFromTheory(String theoryName,
                         Iterable<TptpParserOutput.InfoItem> usefulInfo) {
    return new Source.Theory(sharedCopyOf(theoryName), usefulInfo);
  }
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem 
  createDescriptionInfoItem(String singleQuoted) {
    return new InfoItem.Description(sharedCopyOf(singleQuoted));
  }
  
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem createIQuoteInfoItem(String singleQuoted) {
    return new InfoItem.IQuote(sharedCopyOf(singleQuoted));
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem 
  createInferenceStatusInfoItem(TptpParserOutput.StatusValue statusValue)
  {
    return new InfoItem.InferenceStatus(statusValue);
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem 
  createInferenceRuleInfoItem(String inferenceRule,
                              String inferenceId,
                              Iterable<TptpParserOutput.GeneralTerm> attributes)
  {
    return 
    new InfoItem.InferenceRule(sharedCopyOf(inferenceRule),
                               sharedCopyOf(inferenceId),
                               attributes);
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem 
  createAssumptionRecordInfoItem(Iterable<String> nameList)
  {
    return new InfoItem.AssumptionRecord(nameList);
  }

  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem 
  createRefutationInfoItem(TptpParserOutput.Source fileSource)
  {
    return new InfoItem.Refutation(fileSource);
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.InfoItem 
  createGeneralFunctionInfoItem(TptpParserOutput.GeneralTerm generalFunction)
  {
    return new InfoItem.GeneralFunction(generalFunction);
  }
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm 
  createGeneralFunction(String function,
                        Iterable<TptpParserOutput.GeneralTerm> arguments)
  {
    return new GeneralTerm(sharedCopyOf(function),arguments);
  } 
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm 
  createGeneralList(Iterable<TptpParserOutput.GeneralTerm> list)
  {
    return new GeneralTerm(list);
  } 
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm
  createGeneralColon(TptpParserOutput.GeneralTerm left,
                     TptpParserOutput.GeneralTerm right)
  {
    return new GeneralTerm(left, right);
  } 


  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm
  createGeneralDistinctObject(String str)
  {
    return new GeneralTerm(sharedCopyOf(str));
  } 
  

  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm 
  createGeneralVariable(String var)
  {
    return new GeneralTerm(sharedCopyOf(var));
  } 
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm 
  createGeneralFofFormula(TptpParserOutput.FofFormula formula)
  {
    return new GeneralTerm(formula);
  } 

  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm 
  createGeneralCnfFormula(TptpParserOutput.CnfFormula formula)
  {
    return new GeneralTerm(formula);
  } 

  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.GeneralTerm 
  createGeneralTerm(TptpParserOutput.Term term)
  {
    return new GeneralTerm(term);
  } 
  
  
  /** Implements the corresponding spec in TptpParserOutput. */
  public 
  TptpParserOutput.ParentInfo 
  createParentInfo(TptpParserOutput.Source source,
                   TptpParserOutput.GeneralTerm parentDetails) 
  {
    return new ParentInfo(source, parentDetails);
  }
  
  
  /*-------------------------------------------------*/
  /*    Methods to be called by the client code:     */
  /*-------------------------------------------------*/
  
  public SimpleTptpParserOutput() {
    _stringTable = new Hashtable<String,String>();
    _termTable = new Hashtable<Term,Term>();
    _literalTable = new Hashtable<Literal,Literal>(); 
    _formulaTable = new Hashtable<Formula,Formula>();
  } // SimpleTptpParserOutput()
  
  
  
  /** Reinitialises everything. Note that after a call to reset() 
  *  objects created by various method calls prior to that call 
  *  to reset(), are considered invalid and should not be used
  *  in any way.
  */
  public void reset() {
    _stringTable.clear();
    _termTable.clear();
    _literalTable.clear();
    _formulaTable.clear();
  }
  
  
  /*-------------------------------------------------*/
  /*               Private methods:                  */
  /*-------------------------------------------------*/
  
  /** Returns the copy of <strong> str </strong> stored in _stringTable;
  *  the copy is created if necessary.
  */
  private String sharedCopyOf(String str) {
    assert str != null;
    
    String res = _stringTable.get(str);
    
    if (res == null) {
      _stringTable.put(str,str);
      return str;
    }
    else
      return res;
  }
  
  /** Returns a string of "num" number of spaces. If num is less then 0
      then empty string is returned.
      @param num - number of white spaces to be returned. 
   */
  private static String spaces(int num) { 
    String res = new String();
    for (;num>0;--num) {
      res += " ";
    }
    return res;
  }


  /*-------------------------------------------------*/
  /*                   Attributes:                   */
  /*-------------------------------------------------*/
  
  
  /** Maintains sharing of String objects. */
  private Hashtable<String,String> _stringTable;
  
  /** Maintains sharing of Term objects. 
  *  @see methods for creating different kinds of Term objects
  */
  private Hashtable<Term,Term> _termTable;
  
  /** Maintains sharing of Literal objects.
  *  @see createLiteral(boolean positive,TptpParserOutput.AtomicFormula atom)
  */
  private Hashtable<Literal,Literal> _literalTable;
  
  
  /** Maintains sharing of Formula objects.
  *  @see methods for creating different kinds of Formula objects
  */
  private Hashtable<Formula,Formula> _formulaTable;
  
  
} // class SimpleParserOutput
