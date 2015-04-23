// This is a generated file. Not intended for manual editing.
package com.ipsoft.amelia.sigma.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class RewriteRuleParser implements PsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == CLAUSE) {
      r = clause(b, 0);
    }
    else if (t == CLAUSE_PATTERN) {
      r = clausePattern(b, 0);
    }
    else if (t == KEY) {
      r = key(b, 0);
    }
    else if (t == KIF) {
      r = kif(b, 0);
    }
    else if (t == KIFKEY) {
      r = kifkey(b, 0);
    }
    else if (t == KIFVALUE) {
      r = kifvalue(b, 0);
    }
    else if (t == LHS) {
      r = lhs(b, 0);
    }
    else if (t == LOGSENT) {
      r = logsent(b, 0);
    }
    else if (t == QUANTSENT) {
      r = quantsent(b, 0);
    }
    else if (t == RELSENT) {
      r = relsent(b, 0);
    }
    else if (t == RHS) {
      r = rhs(b, 0);
    }
    else if (t == RULE) {
      r = rule(b, 0);
    }
    else if (t == VALUE) {
      r = value(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return rewriteRuleFile(b, l + 1);
  }

  /* ********************************************************** */
  // !(leftparn) key (ws)? leftparn (ws)? value (ws)? comma (ws)? value  (ws)?rightparn
  public static boolean clause(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause")) return false;
    if (!nextTokenIs(b, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = clause_0(b, l + 1);
    r = r && key(b, l + 1);
    r = r && clause_2(b, l + 1);
    r = r && consumeToken(b, LEFTPARN);
    r = r && clause_4(b, l + 1);
    r = r && value(b, l + 1);
    r = r && clause_6(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && clause_8(b, l + 1);
    r = r && value(b, l + 1);
    r = r && clause_10(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, CLAUSE, r);
    return r;
  }

  // !(leftparn)
  private static boolean clause_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_, null);
    r = !consumeToken(b, LEFTPARN);
    exit_section_(b, l, m, null, r, false, null);
    return r;
  }

  // (ws)?
  private static boolean clause_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_2")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean clause_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_4")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean clause_6(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_6")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean clause_8(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_8")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean clause_10(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clause_10")) return false;
    consumeToken(b, WS);
    return true;
  }

  /* ********************************************************** */
  // ((add)?clause((ws)?comma(ws)?)?)+
  public static boolean clausePattern(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern")) return false;
    if (!nextTokenIs(b, "<clause pattern>", ADD, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<clause pattern>");
    r = clausePattern_0(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!clausePattern_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "clausePattern", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, l, m, CLAUSE_PATTERN, r, false, null);
    return r;
  }

  // (add)?clause((ws)?comma(ws)?)?
  private static boolean clausePattern_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = clausePattern_0_0(b, l + 1);
    r = r && clause(b, l + 1);
    r = r && clausePattern_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (add)?
  private static boolean clausePattern_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern_0_0")) return false;
    consumeToken(b, ADD);
    return true;
  }

  // ((ws)?comma(ws)?)?
  private static boolean clausePattern_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern_0_2")) return false;
    clausePattern_0_2_0(b, l + 1);
    return true;
  }

  // (ws)?comma(ws)?
  private static boolean clausePattern_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = clausePattern_0_2_0_0(b, l + 1);
    r = r && consumeToken(b, COMMA);
    r = r && clausePattern_0_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean clausePattern_0_2_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern_0_2_0_0")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean clausePattern_0_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "clausePattern_0_2_0_2")) return false;
    consumeToken(b, WS);
    return true;
  }

  /* ********************************************************** */
  // rule|comment|crlf
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = rule(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    if (!r) r = consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // word
  public static boolean key(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "key")) return false;
    if (!nextTokenIs(b, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WORD);
    exit_section_(b, m, KEY, r);
    return r;
  }

  /* ********************************************************** */
  // (leftparn kifkey (ws)? (kifvalue (ws)?)* rightparn)|relsent|logsent|quantsent
  public static boolean kif(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kif")) return false;
    if (!nextTokenIs(b, LEFTPARN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = kif_0(b, l + 1);
    if (!r) r = relsent(b, l + 1);
    if (!r) r = logsent(b, l + 1);
    if (!r) r = quantsent(b, l + 1);
    exit_section_(b, m, KIF, r);
    return r;
  }

  // leftparn kifkey (ws)? (kifvalue (ws)?)* rightparn
  private static boolean kif_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kif_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFTPARN);
    r = r && kifkey(b, l + 1);
    r = r && kif_0_2(b, l + 1);
    r = r && kif_0_3(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean kif_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kif_0_2")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (kifvalue (ws)?)*
  private static boolean kif_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kif_0_3")) return false;
    int c = current_position_(b);
    while (true) {
      if (!kif_0_3_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "kif_0_3", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // kifvalue (ws)?
  private static boolean kif_0_3_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kif_0_3_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = kifvalue(b, l + 1);
    r = r && kif_0_3_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean kif_0_3_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kif_0_3_0_1")) return false;
    consumeToken(b, WS);
    return true;
  }

  /* ********************************************************** */
  // word
  public static boolean kifkey(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kifkey")) return false;
    if (!nextTokenIs(b, WORD)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, WORD);
    exit_section_(b, m, KIFKEY, r);
    return r;
  }

  /* ********************************************************** */
  // kif|value
  public static boolean kifvalue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "kifvalue")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<kifvalue>");
    r = kif(b, l + 1);
    if (!r) r = value(b, l + 1);
    exit_section_(b, l, m, KIFVALUE, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (clausePattern)|(leftparn (ws)? clausePattern (ws)?'|'(ws)? clausePattern (ws)? rightparn)|(neg(ws)? lhs)
  public static boolean lhs(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<lhs>");
    r = lhs_0(b, l + 1);
    if (!r) r = lhs_1(b, l + 1);
    if (!r) r = lhs_2(b, l + 1);
    exit_section_(b, l, m, LHS, r, false, null);
    return r;
  }

  // (clausePattern)
  private static boolean lhs_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = clausePattern(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // leftparn (ws)? clausePattern (ws)?'|'(ws)? clausePattern (ws)? rightparn
  private static boolean lhs_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFTPARN);
    r = r && lhs_1_1(b, l + 1);
    r = r && clausePattern(b, l + 1);
    r = r && lhs_1_3(b, l + 1);
    r = r && consumeToken(b, "|");
    r = r && lhs_1_5(b, l + 1);
    r = r && clausePattern(b, l + 1);
    r = r && lhs_1_7(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean lhs_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_1_1")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean lhs_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_1_3")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean lhs_1_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_1_5")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean lhs_1_7(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_1_7")) return false;
    consumeToken(b, WS);
    return true;
  }

  // neg(ws)? lhs
  private static boolean lhs_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NEG);
    r = r && lhs_2_1(b, l + 1);
    r = r && lhs(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean lhs_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "lhs_2_1")) return false;
    consumeToken(b, WS);
    return true;
  }

  /* ********************************************************** */
  // leftparn not kif rightparn | leftparn and kif+ rightparn | leftparn or kif+ rightparn | leftparn implies kif kif rightparn | leftparn equalsto kif kif rightparn
  public static boolean logsent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent")) return false;
    if (!nextTokenIs(b, LEFTPARN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = logsent_0(b, l + 1);
    if (!r) r = logsent_1(b, l + 1);
    if (!r) r = logsent_2(b, l + 1);
    if (!r) r = logsent_3(b, l + 1);
    if (!r) r = logsent_4(b, l + 1);
    exit_section_(b, m, LOGSENT, r);
    return r;
  }

  // leftparn not kif rightparn
  private static boolean logsent_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, NOT);
    r = r && kif(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // leftparn and kif+ rightparn
  private static boolean logsent_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, AND);
    r = r && logsent_1_2(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // kif+
  private static boolean logsent_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = kif(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!kif(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logsent_1_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // leftparn or kif+ rightparn
  private static boolean logsent_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, OR);
    r = r && logsent_2_2(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // kif+
  private static boolean logsent_2_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_2_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = kif(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!kif(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "logsent_2_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // leftparn implies kif kif rightparn
  private static boolean logsent_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, IMPLIES);
    r = r && kif(b, l + 1);
    r = r && kif(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // leftparn equalsto kif kif rightparn
  private static boolean logsent_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "logsent_4")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, EQUALSTO);
    r = r && kif(b, l + 1);
    r = r && kif(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // leftparn forall leftparn variable+ rightparn kif+ rightparn  | leftparn exists leftparn variable+rightparn kif+rightparn
  public static boolean quantsent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent")) return false;
    if (!nextTokenIs(b, LEFTPARN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = quantsent_0(b, l + 1);
    if (!r) r = quantsent_1(b, l + 1);
    exit_section_(b, m, QUANTSENT, r);
    return r;
  }

  // leftparn forall leftparn variable+ rightparn kif+ rightparn
  private static boolean quantsent_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, FORALL, LEFTPARN);
    r = r && quantsent_0_3(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    r = r && quantsent_0_5(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // variable+
  private static boolean quantsent_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent_0_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VARIABLE);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, VARIABLE)) break;
      if (!empty_element_parsed_guard_(b, "quantsent_0_3", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // kif+
  private static boolean quantsent_0_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent_0_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = kif(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!kif(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "quantsent_0_5", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // leftparn exists leftparn variable+rightparn kif+rightparn
  private static boolean quantsent_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LEFTPARN, EXISTS, LEFTPARN);
    r = r && quantsent_1_3(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    r = r && quantsent_1_5(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // variable+
  private static boolean quantsent_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent_1_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VARIABLE);
    int c = current_position_(b);
    while (r) {
      if (!consumeToken(b, VARIABLE)) break;
      if (!empty_element_parsed_guard_(b, "quantsent_1_3", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // kif+
  private static boolean quantsent_1_5(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quantsent_1_5")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = kif(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!kif(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "quantsent_1_5", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // leftparn kifkey value+rightparn
  public static boolean relsent(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "relsent")) return false;
    if (!nextTokenIs(b, LEFTPARN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFTPARN);
    r = r && kifkey(b, l + 1);
    r = r && relsent_2(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, RELSENT, r);
    return r;
  }

  // value+
  private static boolean relsent_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "relsent_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = value(b, l + 1);
    int c = current_position_(b);
    while (r) {
      if (!value(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "relsent_2", c)) break;
      c = current_position_(b);
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // item_*
  static boolean rewriteRuleFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rewriteRuleFile")) return false;
    int c = current_position_(b);
    while (true) {
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "rewriteRuleFile", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // (leftparn(ws)? clausePattern (ws)?rightparn)|(midleftparn kif midrightparn)|tan|stop
  public static boolean rhs(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rhs")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<rhs>");
    r = rhs_0(b, l + 1);
    if (!r) r = rhs_1(b, l + 1);
    if (!r) r = consumeToken(b, TAN);
    if (!r) r = consumeToken(b, STOP);
    exit_section_(b, l, m, RHS, r, false, null);
    return r;
  }

  // leftparn(ws)? clausePattern (ws)?rightparn
  private static boolean rhs_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rhs_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LEFTPARN);
    r = r && rhs_0_1(b, l + 1);
    r = r && clausePattern(b, l + 1);
    r = r && rhs_0_3(b, l + 1);
    r = r && consumeToken(b, RIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean rhs_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rhs_0_1")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean rhs_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rhs_0_3")) return false;
    consumeToken(b, WS);
    return true;
  }

  // midleftparn kif midrightparn
  private static boolean rhs_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rhs_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, MIDLEFTPARN);
    r = r && kif(b, l + 1);
    r = r && consumeToken(b, MIDRIGHTPARN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // (lhs (ws)? seperator (ws)? rhs dot)|(permanentseperator (ws)? clause (ws)? dot)
  public static boolean rule(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<rule>");
    r = rule_0(b, l + 1);
    if (!r) r = rule_1(b, l + 1);
    exit_section_(b, l, m, RULE, r, false, null);
    return r;
  }

  // lhs (ws)? seperator (ws)? rhs dot
  private static boolean rule_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = lhs(b, l + 1);
    r = r && rule_0_1(b, l + 1);
    r = r && consumeToken(b, SEPERATOR);
    r = r && rule_0_3(b, l + 1);
    r = r && rhs(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean rule_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule_0_1")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean rule_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule_0_3")) return false;
    consumeToken(b, WS);
    return true;
  }

  // permanentseperator (ws)? clause (ws)? dot
  private static boolean rule_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, PERMANENTSEPERATOR);
    r = r && rule_1_1(b, l + 1);
    r = r && clause(b, l + 1);
    r = r && rule_1_3(b, l + 1);
    r = r && consumeToken(b, DOT);
    exit_section_(b, m, null, r);
    return r;
  }

  // (ws)?
  private static boolean rule_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule_1_1")) return false;
    consumeToken(b, WS);
    return true;
  }

  // (ws)?
  private static boolean rule_1_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "rule_1_3")) return false;
    consumeToken(b, WS);
    return true;
  }

  /* ********************************************************** */
  // variable|word|string
  public static boolean value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, "<value>");
    r = consumeToken(b, VARIABLE);
    if (!r) r = consumeToken(b, WORD);
    if (!r) r = consumeToken(b, STRING);
    exit_section_(b, l, m, VALUE, r, false, null);
    return r;
  }

}
