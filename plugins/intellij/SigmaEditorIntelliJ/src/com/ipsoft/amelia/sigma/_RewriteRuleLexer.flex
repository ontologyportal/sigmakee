package com.ipsoft.amelia.sigma;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static com.ipsoft.amelia.sigma.psi.RewriteRuleTypes.*;

%%

%{
  public _RewriteRuleLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _RewriteRuleLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL="\r"|"\n"|"\r\n"
LINE_WS=[\ \t\f]
WHITE_SPACE=({LINE_WS}|{EOL})+

WORD=[a-zA-Z0-9\*\-\_]+
VARIABLE=[?@][A-Za-z0-9\-]*
WS=[\s\t]*
STRING=\"[^\"]*\"
COMMENT=;[^\r\n]*
CRLF=\n\r
SEPERATOR=(==>)|(\?=>)

%%
<YYINITIAL> {
  {WHITE_SPACE}       { return com.intellij.psi.TokenType.WHITE_SPACE; }

  "("                 { return LEFTPARN; }
  ")"                 { return RIGHTPARN; }
  "+"                 { return ADD; }
  "/-"                { return PERMANENTSEPERATOR; }
  "{"                 { return MIDLEFTPARN; }
  "}"                 { return MIDRIGHTPARN; }
  ","                 { return COMMA; }
  "."                 { return DOT; }
  "-"                 { return NEG; }
  "!"                 { return TAN; }
  "stop"              { return STOP; }
  "not"               { return NOT; }
  "and"               { return AND; }
  "forall"            { return FORALL; }
  "exists"            { return EXISTS; }
  "=>"                { return IMPLIES; }
  "<=>"               { return EQUALSTO; }
  "leftparnkifkey"    { return LEFTPARNKIFKEY; }
  "or"                { return OR; }

  {WORD}              { return WORD; }
  {VARIABLE}          { return VARIABLE; }
  {WS}                { return WS; }
  {STRING}            { return STRING; }
  {COMMENT}           { return COMMENT; }
  {CRLF}              { return CRLF; }
  {SEPERATOR}         { return SEPERATOR; }

  [^] { return com.intellij.psi.TokenType.BAD_CHARACTER; }
}
