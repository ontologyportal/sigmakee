// This is a generated file. Not intended for manual editing.
package com.ipsoft.amelia.sigma.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.ipsoft.amelia.sigma.psi.impl.*;

public interface RewriteRuleTypes {

  IElementType CLAUSE = new RewriteRuleElementType("CLAUSE");
  IElementType CLAUSE_PATTERN = new RewriteRuleElementType("CLAUSE_PATTERN");
  IElementType KEY = new RewriteRuleElementType("KEY");
  IElementType KIF = new RewriteRuleElementType("KIF");
  IElementType KIFKEY = new RewriteRuleElementType("KIFKEY");
  IElementType KIFVALUE = new RewriteRuleElementType("KIFVALUE");
  IElementType LHS = new RewriteRuleElementType("LHS");
  IElementType LOGSENT = new RewriteRuleElementType("LOGSENT");
  IElementType QUANTSENT = new RewriteRuleElementType("QUANTSENT");
  IElementType RELSENT = new RewriteRuleElementType("RELSENT");
  IElementType RHS = new RewriteRuleElementType("RHS");
  IElementType RULE = new RewriteRuleElementType("RULE");
  IElementType VALUE = new RewriteRuleElementType("VALUE");

  IElementType ADD = new RewriteRuleTokenType("+");
  IElementType AND = new RewriteRuleTokenType("and");
  IElementType COMMA = new RewriteRuleTokenType(",");
  IElementType COMMENT = new RewriteRuleTokenType("comment");
  IElementType CRLF = new RewriteRuleTokenType("crlf");
  IElementType DOT = new RewriteRuleTokenType(".");
  IElementType EQUALSTO = new RewriteRuleTokenType("<=>");
  IElementType EXISTS = new RewriteRuleTokenType("exists");
  IElementType FORALL = new RewriteRuleTokenType("forall");
  IElementType IMPLIES = new RewriteRuleTokenType("=>");
  IElementType LEFTPARN = new RewriteRuleTokenType("(");
  IElementType MIDLEFTPARN = new RewriteRuleTokenType("{");
  IElementType MIDRIGHTPARN = new RewriteRuleTokenType("}");
  IElementType NEG = new RewriteRuleTokenType("-");
  IElementType NOT = new RewriteRuleTokenType("not");
  IElementType OR = new RewriteRuleTokenType("or");
  IElementType PERMANENTSEPERATOR = new RewriteRuleTokenType("/-");
  IElementType RIGHTPARN = new RewriteRuleTokenType(")");
  IElementType SEPERATOR = new RewriteRuleTokenType("seperator");
  IElementType STOP = new RewriteRuleTokenType("stop");
  IElementType STRING = new RewriteRuleTokenType("string");
  IElementType TAN = new RewriteRuleTokenType("!");
  IElementType VARIABLE = new RewriteRuleTokenType("variable");
  IElementType WORD = new RewriteRuleTokenType("word");
  IElementType WS = new RewriteRuleTokenType("ws");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == CLAUSE) {
        return new RewriteRuleClauseImpl(node);
      }
      else if (type == CLAUSE_PATTERN) {
        return new RewriteRuleClausePatternImpl(node);
      }
      else if (type == KEY) {
        return new RewriteRuleKeyImpl(node);
      }
      else if (type == KIF) {
        return new RewriteRuleKifImpl(node);
      }
      else if (type == KIFKEY) {
        return new RewriteRuleKifkeyImpl(node);
      }
      else if (type == KIFVALUE) {
        return new RewriteRuleKifvalueImpl(node);
      }
      else if (type == LHS) {
        return new RewriteRuleLhsImpl(node);
      }
      else if (type == LOGSENT) {
        return new RewriteRuleLogsentImpl(node);
      }
      else if (type == QUANTSENT) {
        return new RewriteRuleQuantsentImpl(node);
      }
      else if (type == RELSENT) {
        return new RewriteRuleRelsentImpl(node);
      }
      else if (type == RHS) {
        return new RewriteRuleRhsImpl(node);
      }
      else if (type == RULE) {
        return new RewriteRuleRuleImpl(node);
      }
      else if (type == VALUE) {
        return new RewriteRuleValueImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
