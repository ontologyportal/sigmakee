package com.ipsoft.amelia.sigma;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.*;
import com.intellij.psi.TokenType;

%%

%class RewriteRuleLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+

WORD=[a-zA-Z0-9\*\-\_]+
VARIABLE=[?@][A-Za-z0-9\-]*
WS=[\s\t]*
STRING=\"[^\"]*\"
COMMENT=";"[^\r\n]*
CRLF=\n\r
SEPERATOR=("==>")|("\?=>")

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return com.intellij.psi.TokenType.WHITE_SPACE; }

  "("                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.LEFTPARN; }
  ")"                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.RIGHTPARN; }
  "+"                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.ADD; }
  "/-"                 { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.PERMANENTSEPERATOR; }
  "{"                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.MIDLEFTPARN; }
  "}"                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.MIDRIGHTPARN; }
  ","                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.COMMA; }
  "."                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.DOT; }
  "-"                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.NEG; }
  "!"                  { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.TAN; }
  "stop"               { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.STOP; }
  "not"                { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.NOT; }
  "and"                { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.AND; }
  "forall"             { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.FORALL; }
  "exists"             { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.EXISTS; }
  "=>"                 { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.IMPLIES; }
  "<=>"                { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.EQUALSTO; }
  "or"                 { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.OR; }

  {WORD}               { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.WORD; }
  {VARIABLE}           { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.VARIABLE; }
  {WS}                 { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.WS; }
  {STRING}             { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.STRING; }
  {COMMENT}            { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.COMMENT; }
  {CRLF}               { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.CRLF; }
  {SEPERATOR}          { return com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.SEPERATOR; }

  [^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
