// created by Alexander Steen (a.steen@fu-berlin.de)
// and Tobias Gleißner (tobias.gleissner@fu-berlin.de)

// #INFO is about sections or where the parse tree has been flattened according to ANTLR idiomatics
// #ALT alternative grammar formulation for some parts
// #RES where no further restrictions are applied e.g. in the case of defined_functor
// any dollar word is allowed instead of only the predefined functors

grammar tptp_v7_0_0_0;

// #INFO HERE COME THE LEXER RULES

fragment Do_char : [\u0020-\u0021\u0023-\u005B\u005D-\u007E] | '\\'["\\];
fragment Sq_char : [\u0020-\u0026\u0028-\u005B\u005D-\u007E] | '\\'['\\];
fragment Sign : [+-];
fragment Exponent : [Ee];
fragment Non_zero_numeric : [1-9];
fragment Numeric : [0-9];
fragment Lower_alpha : [a-z];
fragment Upper_alpha : [A-Z];
fragment Alpha_numeric : Lower_alpha | Upper_alpha | Numeric | '_';

Or: '|';
And: '&';
Iff : '<=>';
Impl : '=>';
If: '<=';
Niff: '<~>';
Nor: '~|';
Nand: '~&';
Not: '~';
ForallComb: '!!';
TyForall: '!>';
Infix_inequality : '!=';
Infix_equality : '=';
Forall: '!';
ExistsComb: '??';
TyExists: '?*';
Exists: '?';
Lambda: '^';
ChoiceComb: '@@+';
Choice: '@+';
DescriptionComb: '@@-';
Description: '@-';
EqComb: '@=';
App: '@';
Assignment: ':=';
Arrow: '>';
Star: '*';
Plus: '+';
Subtype_sign: '<<';
Gentzen_arrow            : '-->';

Real : Signed_real | Unsigned_real;
Signed_real : Sign Unsigned_real;
Unsigned_real : Decimal_fraction|Decimal_exponent;
Rational: Signed_rational | Unsigned_rational;
Signed_rational: Sign Unsigned_rational;
Unsigned_rational: Decimal '/' Positive_decimal;
Integer : Signed_integer | Unsigned_integer;
Signed_integer: Sign Unsigned_integer;
Unsigned_integer: Decimal;
Decimal : '0' | Positive_decimal;
Positive_decimal : Non_zero_numeric Numeric*;
Decimal_exponent : (Decimal|Decimal_fraction) Exponent Exp_integer;
Decimal_fraction : Decimal Dot_decimal;
Dot_decimal : '.' Numeric Numeric*;
Exp_integer : Signed_exp_integer|Unsigned_exp_integer;
Signed_exp_integer : Sign Unsigned_exp_integer;
Unsigned_exp_integer : Numeric Numeric*;

Dollar_word : '$' Lower_word;
Dollar_dollar_word : '$$' Lower_word;
Upper_word : Upper_alpha Alpha_numeric*;
Lower_word : Lower_alpha Alpha_numeric*;

Single_quoted : '\'' Sq_char+ '\'';
Distinct_object : '"' Do_char+ '"';

WS : [ \r\t\n]+ -> skip ;
Line_comment : '%' ~[\r\n]* -> skip;
Block_comment : '/*' .*? '*/' -> skip;



//%----Top of Page---------------------------------------------------------------
//%----Rules from here on down are for defining tokens (terminal symbols) of the
//%----grammar, assuming they will be recognized by a lexical scanner.
//%----A ::- rule defines a token, a ::: rule defines a macro that is not a
//%----token. Usual regexp notation is used. Single characters are always placed
//%----in []s to disable any special meanings (for uniformity this is done to
//%----all characters, not only those with special meanings).
//
//%----These are tokens that appear in the syntax rules above. No rules
//%----defined here because they appear explicitly in the syntax rules,
//%----except that <vline>, <star>, <plus> denote "|", "*", "+", respectively.
//%----Keywords:    fof cnf thf tff include
//%----Punctuation: ( ) , . [ ] :
//%----Operators:   ! ? ~ & | <=> => <= <~> ~| ~& * +
//%----Predicates:  = != $true $false
//
//%----For lex/yacc there cannot be spaces on either side of the | here
//<comment>              ::- <comment_line>|<comment_block>
//<comment_line>         ::- [%]<printable_char>*
//<comment_block>        ::: [/][*]<not_star_slash>[*][*]*[/]
//<not_star_slash>       ::: ([^*]*[*][*]*[^/*])*[^*]*
//%----Defined comments are a convention used for annotations that are used as
//%----additional input for systems. They look like comments, but start with %$
//%----or /*$. A wily user of the syntax can notice the $ and extract information
//%----from the "comment" and pass that on as input to the system. They are
//%----analogous to pragmas in programming languages. To extract these separately
//%----from regular comments, the rules are:
//%----  <defined_comment>    ::- <def_comment_line>|<def_comment_block>
//%----  <def_comment_line>   ::: [%]<dollar><printable_char>*
//%----  <def_comment_block>  ::: [/][*]<dollar><not_star_slash>[*][*]*[/]
//%----A string that matches both <defined_comment> and <comment> should be
//%----recognized as <defined_comment>, so put these before <comment>.
//%----Defined comments that are in use include:
//%----    TO BE ANNOUNCED
//%----System comments are a convention used for annotations that may used as
//%----additional input to a specific system. They look like comments, but start
//%----with %$$ or /*$$. A wily user of the syntax can notice the $$ and extract
//%----information from the "comment" and pass that on as input to the system.
//%----The specific system for which the information is intended should be
//%----identified after the $$, e.g., /*$$Otter 3.3: Demodulator */
//%----To extract these separately from regular comments, the rules are:
//%----  <system_comment>     ::- <sys_comment_line>|<sys_comment_block>
//%----  <sys_comment_line>   ::: [%]<dollar><dollar><printable_char>*
//%----  <sys_comment_block>  ::: [/][*]<dollar><dollar><not_star_slash>[*][*]*[/]
//%----A string that matches both <system_comment> and <defined_comment> should
//%----be recognized as <system_comment>, so put these before <defined_comment>.
//
//<single_quoted>        ::- <single_quote><sq_char><sq_char>*<single_quote>
//%----<single_quoted>s contain visible characters. \ is the escape character for
//%----' and \, i.e., \' is not the end of the <single_quoted>.
//%----The token does not include the outer quotes, e.g., 'cat' and cat are the
//%----same. See <atomic_word> for information about stripping the quotes.
//
//<distinct_object>      ::- <double_quote><do_char>*<double_quote>
//%---Space and visible characters upto ~, except " and \
//%----<distinct_object>s contain visible characters. \ is the escape character
//%----for " and \, i.e., \" is not the end of the <distinct_object>.
//%----<distinct_object>s are different from (but may be equal to) other tokens,
//%----e.g., "cat" is different from 'cat' and cat. Distinct objects are always
//%----interpreted as themselves, so if they are different they are unequal,
//%----e.g., "Apple" != "Microsoft" is implicit.
//
//<dollar_word>          ::- <dollar><lower_word>
//<dollar_dollar_word>   ::- <dollar><dollar><lower_word>
//<upper_word>           ::- <upper_alpha><alpha_numeric>*
//<lower_word>           ::- <lower_alpha><alpha_numeric>*
//
//%----Tokens used in syntax, and cannot be character classes
//<vline>                ::- [|]
//<star>                 ::- [*]
//<plus>                 ::- [+]
//<arrow>                ::- [>]
//<less_sign>            ::- [<]
//
//%----Numbers. Signs are made part of the same token here.
//<real>                 ::- (<signed_real>|<unsigned_real>)
//<signed_real>          ::- <sign><unsigned_real>
//<unsigned_real>        ::- (<decimal_fraction>|<decimal_exponent>)
//<rational>             ::- (<signed_rational>|<unsigned_rational>)
//<signed_rational>      ::- <sign><unsigned_rational>
//<unsigned_rational>    ::- <decimal><slash><positive_decimal>
//<integer>              ::- (<signed_integer>|<unsigned_integer>)
//<signed_integer>       ::- <sign><unsigned_integer>
//<unsigned_integer>     ::- <decimal>
//<decimal>              ::- (<zero_numeric>|<positive_decimal>)
//<positive_decimal>     ::- <non_zero_numeric><numeric>*
//<decimal_exponent>     ::- (<decimal>|<decimal_fraction>)<exponent><exp_integer>
//<decimal_fraction>     ::- <decimal><dot_decimal>
//<dot_decimal>          ::- <dot><numeric><numeric>*
//<exp_integer>          ::- (<signed_exp_integer>|<unsigned_exp_integer>)
//<signed_exp_integer>   ::- <sign><unsigned_exp_integer>
//<unsigned_exp_integer> ::- <numeric><numeric>*
//
//%----Character classes
//<percentage_sign>      ::: [%]
//<double_quote>         ::: ["]
//<do_char>              ::: ([\40-\41\43-\133\135-\176]|[\\]["\\])
//<single_quote>         ::: [']
//%---Space and visible characters upto ~, except ' and \
//<sq_char>              ::: ([\40-\46\50-\133\135-\176]|[\\]['\\])
//<sign>                 ::: [+-]
//<dot>                  ::: [.]
//<exponent>             ::: [Ee]
//<slash>                ::: [/]
//<zero_numeric>         ::: [0]
//<non_zero_numeric>     ::: [1-9]
//<numeric>              ::: [0-9]
//<lower_alpha>          ::: [a-z]
//<upper_alpha>          ::: [A-Z]
//<alpha_numeric>        ::: (<lower_alpha>|<upper_alpha>|<numeric>|[_])
//<dollar>               ::: [$]
//<printable_char>       ::: .
//%----<printable_char> is any printable ASCII character, codes 32 (space) to 126
//%----(tilde). <printable_char> does not include tabs, newlines, bells, etc. The
//%----use of . does not not exclude tab, so this is a bit loose.
//<viewable_char>        ::: [.\n]
//%----Top of Page---------------------------------------------------------------




// #INFO HERE COMES THE GRAMMAR

//%----v7.0.0.0 (TPTP version.internal development number)
//%------------------------------------------------------------------------------
//%----README ... this header provides important meta- and usage information
//%----
//%----Intended uses of the various parts of the TPTP syntax are explained
//%----in the TPTP technical manual, linked from www.tptp.org.
//%----
//%----Four kinds of separators are used, to indicate different types of rules:
//%----  ::= is used for regular grammar rules, for syntactic parsing.
//%----  :== is used for semantic grammar rules. These define specific values
//%----      that make semantic sense when more general syntactic rules apply.
//%----  ::- is used for rules that produce tokens.
//%----  ::: is used for rules that define character classes used in the
//%----       construction of tokens.
//%----
//%----White space may occur between any two tokens. White space is not specified
//%----in the grammar, but there are some restrictions to ensure that the grammar
//%----is compatible with standard Prolog: a <TPTP_file> should be readable with
//%----read/1.
//%----
//%----The syntax of comments is defined by the <comment> rule. Comments may
//%----occur between any two tokens, but do not act as white space. Comments
//%----will normally be discarded at the lexical level, but may be processed
//%----by systems that understand them (e.g., if the system comment convention
//%----is followed).
//%----
//%----Multiple languages are defined. Depending on your need, you can implement
//%----just the one(s) you need. The common rules for atoms, terms, etc, come
//%----after the definitions of the languages, and mostly all needed for all the
//%----languages.
//%----Top of Page---------------------------------------------------------------

//%----Files. Empty file is OK.
//<TPTP_file>            ::= <TPTP_input>*
//<TPTP_input>           ::= <annotated_formula> | <include>
tptp_file               : tptp_input* EOF;
tptp_input              : annotated_formula | include;

//%----Formula records
//<annotated_formula>    ::= <thf_annotated> | <tfx_annotated> | <tff_annotated> |
//                           <tcf_annotated> | <fof_annotated> | <cnf_annotated> |
//                           <tpi_annotated>
//%----Future languages may include ...  english | efof | tfof | mathml | ...
//<tpi_annotated>        ::= tpi(<name>,<formula_role>,<tpi_formula><annotations>).
//<tpi_formula>          ::= <fof_formula>
//<thf_annotated>        ::= thf(<name>,<formula_role>,<thf_formula>
//                           <annotations>).
//<tfx_annotated>        ::= tfx(<name>,<formula_role>,<tfx_formula>
//                           <annotations>).
//<tff_annotated>        ::= tff(<name>,<formula_role>,<tff_formula>
//                           <annotations>).
//<tcf_annotated>        ::= tcf(<name>,<formula_role>,<tcf_formula>
//                           <annotations>).
//<fof_annotated>        ::= fof(<name>,<formula_role>,<fof_formula>
//                           <annotations>).
//<cnf_annotated>        ::= cnf(<name>,<formula_role>,<cnf_formula>
//                           <annotations>).
//<annotations>          ::= ,<source><optional_info> | <null>
annotated_formula       : thf_annotated | tfx_annotated | tff_annotated
                        | tcf_annotated | fof_annotated | cnf_annotated
                        | tpi_annotated;
tpi_annotated           : 'tpi(' name ',' formula_role ',' tpi_formula annotations? ').';
tpi_formula             : fof_formula;
thf_annotated           : 'thf(' name ',' formula_role ',' thf_formula annotations? ').';
tfx_annotated           : 'tfx(' name ',' formula_role ',' tfx_formula annotations? ').';
tff_annotated           : 'tff(' name ',' formula_role ',' tff_formula annotations? ').';
tcf_annotated           : 'tcf(' name ',' formula_role ',' tcf_formula annotations? ').';
fof_annotated           : 'fof(' name ',' formula_role ',' fof_formula annotations? ').';
cnf_annotated           : 'cnf(' name ',' formula_role ',' cnf_formula annotations? ').';
annotations             : ',' source optional_info?;

//%----In derivations the annotated formulae names must be unique, so that
//%----parent references (see <inference_record>) are unambiguous.

//%----Types for problems.
//%----Note: The previous <source_type> from ...
//%----   <formula_role> ::= <user_role>-<source>
//%----... is now gone. Parsers may choose to be tolerant of it for backwards
//%----compatibility.
//<formula_role>         ::= <lower_word>
//<formula_role>         :== axiom | hypothesis | definition | assumption |
//                           lemma | theorem | corollary | conjecture |
//                           negated_conjecture | plain | type |
//                           fi_domain | fi_functors | fi_predicates | unknown
formula_role            : Lower_word; // #RES no restrictions

//%----"axiom"s are accepted, without proof. There is no guarantee that the
//%----axioms of a problem are consistent.
//%----"hypothesis"s are assumed to be true for a particular problem, and are
//%----used like "axiom"s.
//%----"definition"s are intended to define symbols. They are either universally
//%----quantified equations, or universally quantified equivalences with an
//%----atomic lefthand side. They can be treated like "axiom"s.
//%----"assumption"s can be used like axioms, but must be discharged before a
//%----derivation is complete.
//%----"lemma"s and "theorem"s have been proven from the "axiom"s. They can be
//%----used like "axiom"s in problems, and a problem containing a non-redundant
//%----"lemma" or theorem" is ill-formed. They can also appear in derivations.
//%----"theorem"s are more important than "lemma"s from the user perspective.
//%----"conjecture"s are to be proven from the "axiom"(-like) formulae. A problem
//%----is solved only when all "conjecture"s are proven.
//%----"negated_conjecture"s are formed from negation of a "conjecture" (usually
//%----in a FOF to CNF conversion).
//%----"plain"s have no specified user semantics.
//%----"fi_domain", "fi_functors", and "fi_predicates" are used to record the
//%----domain, interpretation of functors, and interpretation of predicates, for
//%----a finite interpretation.
//%----"type" defines the type globally for one symbol; treat as $true.
//%----"unknown"s have unknown role, and this is an error situation.

//%----Top of Page---------------------------------------------------------------
//%----THF formulae.
//<thf_formula>          ::= <thf_logic_formula> | <thf_sequent>
//<thf_logic_formula>    ::= <thf_binary_formula> | <thf_unitary_formula> |
//                           <thf_type_formula> | <thf_subtype>
//<thf_binary_formula>   ::= <thf_binary_pair> | <thf_binary_tuple> |
//                           <thf_binary_type>
thf_formula : thf_logic_formula | thf_sequent;
thf_logic_formula       : thf_binary_formula | thf_unitary_formula
                        | thf_type_formula | thf_subtype;
thf_binary_formula      : thf_binary_pair | thf_binary_tuple
                        | thf_binary_type;

//%----Only some binary connectives can be written without ()s.
//%----There's no precedence among binary connectives
//<thf_binary_pair>      ::= <thf_unitary_formula> <thf_pair_connective>
//                           <thf_unitary_formula>
//<thf_binary_tuple>     ::= <thf_or_formula> | <thf_and_formula> |
//                           <thf_apply_formula>
//<thf_or_formula>       ::= <thf_unitary_formula> <vline> <thf_unitary_formula> |
//                           <thf_or_formula> <vline> <thf_unitary_formula>
//<thf_and_formula>      ::= <thf_unitary_formula> & <thf_unitary_formula> |
//                           <thf_and_formula> & <thf_unitary_formula>
//%----@ (denoting apply) is left-associative and lambda is right-associative.
//%----^ [X] : ^ [Y] : f @ g (where f is a <thf_apply_formula> and g is a
//%----<thf_unitary_formula>) should be parsed as: (^ [X] : (^ [Y] : f)) @ g.
//%----That is, g is not in the scope of either lambda.
//<thf_apply_formula>    ::= <thf_unitary_formula> @ <thf_unitary_formula> |
//                           <thf_apply_formula> @ <thf_unitary_formula>
thf_binary_pair         : thf_unitary_formula thf_pair_connective thf_unitary_formula;
thf_binary_tuple        : thf_or_formula | thf_and_formula
                        | thf_apply_formula;
thf_or_formula          : thf_unitary_formula Or thf_unitary_formula
                        | thf_or_formula Or thf_unitary_formula;
thf_and_formula         : thf_unitary_formula And thf_unitary_formula
                        | thf_and_formula And thf_unitary_formula;
thf_apply_formula       : thf_unitary_formula App thf_unitary_formula
                        | thf_apply_formula App thf_unitary_formula;

//%----<thf_unitary_formula> are in ()s or do not have a <binary_connective> at
//%----the top level. Essentially, any lambda expression that "has enough ()s" to
//%----be used inside a larger lambda expression. However, lambda notation might
//%----not be used.
//<thf_unitary_formula>  ::= <thf_quantified_formula> | <thf_unary_formula> |
//                           <thf_atom> | <thf_conditional> | <thf_let> |
//                           <thf_tuple> | (<thf_logic_formula>)
thf_unitary_formula     : thf_quantified_formula | thf_unary_formula
                        | thf_atom | thf_conditional | thf_let
                        | thf_tuple | '(' thf_logic_formula ')';

//%----All variables must be quantified
//<thf_quantified_formula> ::= <thf_quantification> <thf_unitary_formula>
//<thf_quantification>   ::= <thf_quantifier> [<thf_variable_list>] :
//<thf_variable_list>    ::= <thf_variable> | <thf_variable>,<thf_variable_list>
//<thf_variable>         ::= <thf_typed_variable> | <variable>
//<thf_typed_variable>   ::= <variable> : <thf_top_level_type>
thf_quantified_formula  : thf_quantification thf_unitary_formula;
thf_quantification      : thf_quantifier '[' thf_variable_list ']' ':';
thf_variable_list       : thf_variable (',' thf_variable)*; // #INFO thf_variable_list flattened
//thf_variable_list       : thf_variable | thf_variable ',' thf_variable_list; // #ALT flattened to thf_variable_list
thf_variable            : thf_typed_variable | variable;
thf_typed_variable      : variable ':' thf_top_level_type;
//thf_variable            : variable (':' thf_top_level_type)?; // #ALT to thf_variable (more condensed)

//%----Unary connectives bind more tightly than binary. The negated formula
//%----must be ()ed because a ~ is also a term.
//<thf_unary_formula>    ::= <thf_unary_connective> (<thf_logic_formula>)
//<thf_atom>             ::= <thf_function> | <variable> | <defined_term> |
//                           <thf_conn_term>
thf_unary_formula       : thf_unary_connective '(' thf_logic_formula ')';
thf_atom                : thf_function | variable | defined_term
                        | thf_conn_term;

//%----Defined terms have TPTP specific interpretations. Note that <thf_atom>
//%----allows <defined_type>s as terms, which will fail type checking. The
//%----user must take care with this liberal syntax!
//<thf_function>         ::= <atom> | <functor>(<thf_arguments>) |
//                           <defined_functor>(<thf_arguments>) |
//                           <system_functor>(<thf_arguments>)
thf_function            : atom | functor '(' thf_arguments ')'
                        | defined_functor '(' thf_arguments ')'
                        | system_functor '(' thf_arguments ')';

// #ALT to thf_function
// Splitted rules of <thf_function> to avoid using <atom> here:
// We use conditional arguments, i.e.
// the atoms are included (= thf_arguments is empty).
//thf_function: thf_plain_term | thf_defined_term | thf_system_term;
//thf_plain_term : functor  ('(' thf_arguments ')')?;
//thf_defined_term : defined_functor ('(' thf_arguments ')')?;
//thf_system_term : system_functor ('(' thf_arguments ')')?;

//%----| <defined_type> | <defined_prop>, but they are captured by <atom> as
//%----<defined_constant> as <atomic_defined_word>.
//<thf_conn_term>        ::= <thf_pair_connective> | <assoc_connective> |
//                           <thf_unary_connective>
thf_conn_term           : thf_pair_connective | assoc_connective
                        | thf_unary_connective;

//%----Note that syntactically this allows (p @ =), but for = the first
//%----argument must be known to infer the type of =, so that's not
//%----allowed, i.e., only (= @ p).
//<thf_conditional>      ::= $ite(<thf_logic_formula>,<thf_logic_formula>,
//                            <thf_logic_formula>)
thf_conditional         : '$ite(' thf_logic_formula ',' thf_logic_formula ',' thf_logic_formula ')';

//%----<thf_let> is about to be changed. Don't trust anything here.
//%----The LHS of a term or formula binding must be a non-variable term that
//%----is flat with pairwise distinct variable arguments, and the variables in
//%----the LHS must be exactly those bound in the universally quantified variable
//%----list, in the same order. Let definitions are not recursive: a non-variable
//%----symbol introduced in the LHS of a let definition cannot occur in the RHS.
//%----If a symbol with the same signature as the one in the LHS of the binding
//%----is declared above the let expression (at the top level or in an
//%----encompassing let) then it can be used in the RHS of the binding, but it is
//%----not accessible in the term or formula of the let expression. Let
//%----expressions can be eliminated by a simple definition expansion.
//<thf_let>              ::= $let(<thf_unitary_formula>,<thf_formula>)
//<thf_let>              :== $let(<thf_let_defns>,<thf_formula>)
//<thf_let_defns>        :== <thf_let_defn> | [<thf_let_defn_list>]
//<thf_let_defn_list>    :== <thf_let_defn> | <thf_let_defn>,<thf_let_defn_list>
//<thf_let_defn>         :== <thf_let_quantified_defn> | <thf_let_plain_defn>
//<thf_let_quantified_defn> :== <thf_quantification> (<thf_let_plain_defn>)
//<thf_let_plain_defn>   :== <thf_let_defn_LHS> <assignment> <thf_formula>
//<thf_let_defn_LHS>     :== <constant> | <functor>(<fof_arguments>) |
//                           <thf_tuple>
thf_let                 : '$let(' thf_unitary_formula ',' thf_formula ')';
// TODO nothing since it is about to be changed

//%----The <fof_arguments> must all be <variable>s, and the <thf_tuple> may
//%----contain only <constant>s and <functor>(<fof_arguments>)s
//
//%----Arguments recurse back up to formulae (this is the THF world here)
//<thf_arguments>        ::= <thf_formula_list>
thf_arguments           : thf_formula_list;


//%----A <thf_type_formula> is an assertion that the formula is in this type.
//<thf_type_formula>     ::= <thf_typeable_formula> : <thf_top_level_type>
//<thf_typeable_formula> ::= <thf_atom> | (<thf_logic_formula>)
//<thf_subtype>          ::= <thf_atom> <subtype_sign> <thf_atom>
thf_type_formula        : thf_typeable_formula ':' thf_top_level_type;
thf_typeable_formula    : thf_atom | '(' thf_logic_formula ')';
thf_subtype             : thf_atom Subtype_sign thf_atom;

//%----In a formula with role 'type', <thf_type_formula> is a global declaration
//%----that <constant> is in this thf_top_level_type>, i.e., the rule is ...
//<thf_type_formula>     :== <constant> : <thf_top_level_type>
// #INFO the previous thf_type_formula leads to constant on the left side of the :
//thf_type_formula        : constant ':' thf_top_level_type;

//%----<thf_top_level_type> appears after ":", where a type is being specified
//%----for a term or variable. <thf_unitary_type> includes <thf_unitary_formula>,
//%----so the syntax allows just about any lambda expression with "enough"
//%----parentheses to serve as a type. The expected use of this flexibility is
//%----parametric polymorphism in types, expressed with lambda abstraction.
//%----Mapping is right-associative: o > o > o means o > (o > o).
//%----Xproduct is left-associative: o * o * o means (o * o) * o.
//%----Union is left-associative: o + o + o means (o + o) + o.
//<thf_top_level_type>   ::= <thf_unitary_type> | <thf_mapping_type> |
//                           <thf_apply_type>
thf_top_level_type      : thf_unitary_type | thf_mapping_type | thf_apply_type;

//%----Removed along with adding <thf_binary_type> to <thf_binary_formula>, for
//%----TH1 polymorphic types with binary after quantification.
//%----      | (<thf_binary_type>)
//<thf_unitary_type>     ::= <thf_unitary_formula>
//<thf_apply_type>       ::= <thf_apply_formula>
//<thf_binary_type>      ::= <thf_mapping_type> | <thf_xprod_type> |
//                           <thf_union_type>
//<thf_mapping_type>     ::= <thf_unitary_type> <arrow> <thf_unitary_type> |
//                           <thf_unitary_type> <arrow> <thf_mapping_type>
//<thf_xprod_type>       ::= <thf_unitary_type> <star> <thf_unitary_type> |
//                           <thf_xprod_type> <star> <thf_unitary_type>
//<thf_union_type>       ::= <thf_unitary_type> <plus> <thf_unitary_type> |
//                           <thf_union_type> <plus> <thf_unitary_type>
thf_unitary_type        : thf_unitary_formula;
thf_apply_type          : thf_apply_formula;
thf_binary_type         : thf_mapping_type | thf_xprod_type
                        | thf_union_type;
thf_mapping_type        : thf_unitary_type Arrow thf_unitary_type
                        | thf_unitary_type Arrow thf_mapping_type;
thf_xprod_type          : thf_unitary_type Star thf_unitary_type
                        | thf_xprod_type Star thf_unitary_type;
thf_union_type          : thf_unitary_type Plus thf_unitary_type
                        | thf_union_type Plus thf_unitary_type;

//%----Sequents using the Gentzen arrow
//<thf_sequent>          ::= <thf_tuple> <gentzen_arrow> <thf_tuple> |
//                           (<thf_sequent>)
thf_sequent             : thf_tuple Gentzen_arrow thf_tuple
                        | '(' thf_sequent ')';

//%----By convention, []s are used for tuple of statements, {}s for tuples of
//%----objects. The convention matches the requirements for <tff_tuple_formula>s
//%----and <tff_tuple_term>s. Mixed THF tuples should use []s.
//<thf_tuple>            ::= [] | [<thf_formula_list>] |
//                           {} | {<thf_formula_list>}
//<thf_formula_list>     ::= <thf_logic_formula> |
//                           <thf_logic_formula>,<thf_formula_list>
thf_tuple               : '[]' | '[' thf_formula_list ']'
                        | '{}' | '{' thf_formula_list '}';
thf_formula_list        : thf_logic_formula (',' thf_logic_formula)*;

//%----New material for modal logic semantics, not integrated yet
//<logic_defn_rule>      :== <logic_defn_LHS> <assignment> <logic_defn_RHS>
//<logic_defn_LHS>       :== <logic_defn_value> | <thf_top_level_type>  | <name>
//<logic_defn_LHS>       :== $constants | $quantification | $consequence |
//                           $modalities
//logic_defn_rule          : logic_defn_lhs Assignment logic_defn_rhs;
//logic_defn_lhs           : logic_defn_value         // TODO dunno why this is there
//                         | thf_function             // for constant selection
//                         | thf_top_level_type       // for domain selection
//                         | name                     // for axioms selection
//                         | Dollar_word; // #RES no restrictions
                         //| '$modal'              // enables modal logical options
                         //| '$constants'          // modal logic: alter constants
                         //| '$quantification'     // modal logic: alter quantification
                         //| '$consequence'        // modal logic: alter consequence
                         //| '$modalities';        // modal logic: alter modality operators

//%----The $constants, $quantification, and $consequence apply to all of the
//%----$modalities. Each of these may be specified only once, but not necessarily
//%----all in a single annotated formula.
//<logic_defn_RHS>       :== <logic_defn_value> | <thf_unitary_formula>
//<logic_defn_value>     :== <defined_constant>
//<logic_defn_value>     :== $rigid | $flexible |
//                           $constant | $varying | $cumulative | $decreasing |
//                           $local | $global |
//                           $modal_system_K | $modal_system_T | $modal_system_D |
//                           $modal_system_S4 | $modal_system_S5 |
//                           $modal_axiom_K | $modal_axiom_T | $modal_axiom_B |
//                           $modal_axiom_D | $modal_axiom_4 | $modal_axiom_5
//logic_defn_rhs           : logic_defn_value
//                         | thf_unitary_formula;    // TODO dunno why this is there
//logic_defn_value         : Dollar_word; // #RES no restrictions
//                           '$rigid'              // modal logic: consequence option
//                         | '$flexible'           // modal logic: consequence option
//                         | '$constant'           // modal logic: quantification option
//                         | '$varying'            // modal logic: quantification option
//                         | '$cumulative'         // modal logic: quantification option
//                         | '$decreasing'         // modal logic: quantification option
//                         | '$local'              // modal logic: consequence option
//                         | '$global'             // modal logic: consequence option
//                         | '$modal_system_K'     // modal logic: Axiom: K
//                         | '$modal_system_T'     // modal logic: Axiom: K + T
//                         | '$modal_system_D'     // modal logic: Axiom: K + D
//                         | '$modal_system_S4'    // modal logic: Axiom K + T + 4
//                         | '$modal_system_S5'    // modal logic: Axiom K + T + 5
//                         | '$modal_axiom_K'      // modal logic: box(s -> t) -> (box s -> box t)
//                         | '$modal_axiom_T'      // modal logic: box s -> s
//                         | '$modal_axiom_B'      // modal logic: s -> box dia s
//                         | '$modal_axiom_D'      // modal logic: box s -> dia s
//                         | '$modal_axiom_4'      // modal logic: box s -> box box s
//                         | '$modal_axiom_5'      // modal logic: dia s -> box dia s
//                         | '$modal_axiom_CD'     // modal logic: dia s -> box s
//                         | '$modal_axiom_BOXM'   // modal logic: box (box s -> s)
//                         | '$modal_axiom_C4'     // modal logic: box box s -> box s
//                         | '$modal_axiom_C';     // modal logic: dia box s -> box dia s

//%----Top of Page---------------------------------------------------------------
//%----TFX formulae
//<tfx_formula>          ::= <tfx_logic_formula> | <thf_sequent>
//<tfx_logic_formula>    ::= <thf_logic_formula>
//% <tfx_logic_formula>    ::= <thf_binary_formula> | <thf_unitary_formula> |
//%                            <tff_typed_atom> | <tff_subtype>
tfx_formula              : tfx_logic_formula | thf_sequent;
tfx_logic_formula        : thf_logic_formula;

//%----Top of Page---------------------------------------------------------------
//%----TFF formulae.
//<tff_formula>          ::= <tff_logic_formula> | <tff_typed_atom> |
//                           <tff_sequent>
//<tff_logic_formula>    ::= <tff_binary_formula> | <tff_unitary_formula> |
//                           <tff_subtype>
//<tff_binary_formula>   ::= <tff_binary_nonassoc> | <tff_binary_assoc>
//<tff_binary_nonassoc>  ::= <tff_unitary_formula> <binary_connective>
//                           <tff_unitary_formula>
//<tff_binary_assoc>     ::= <tff_or_formula> | <tff_and_formula>
//<tff_or_formula>       ::= <tff_unitary_formula> <vline> <tff_unitary_formula> |
//                           <tff_or_formula> <vline> <tff_unitary_formula>
//<tff_and_formula>      ::= <tff_unitary_formula> & <tff_unitary_formula> |
//                           <tff_and_formula> & <tff_unitary_formula>
//<tff_unitary_formula>  ::= <tff_quantified_formula> | <tff_unary_formula> |
//                           <tff_atomic_formula> | <tff_conditional> |
//                           <tff_let> | (<tff_logic_formula>)
tff_formula             : tff_logic_formula | tff_typed_atom
                        | tff_sequent;
tff_logic_formula       : tff_binary_formula | tff_unitary_formula
                        | tff_subtype;
tff_binary_formula      : tff_binary_nonassoc | tff_binary_assoc;
tff_binary_nonassoc     : tff_unitary_formula binary_connective tff_unitary_formula;
tff_binary_assoc        : tff_or_formula | tff_and_formula;
tff_or_formula          : tff_unitary_formula Or tff_unitary_formula
                        | tff_or_formula Or tff_unitary_formula;
tff_and_formula         : tff_unitary_formula And tff_unitary_formula
                        | tff_and_formula And tff_unitary_formula;
tff_unitary_formula     : tff_quantified_formula | tff_unary_formula
                        | tff_atomic_formula | tff_conditional
                        | tff_let | '(' tff_logic_formula ')';

//%----All variables must be quantified
//<tff_quantified_formula> ::= <fof_quantifier> [<tff_variable_list>] :
//                           <tff_unitary_formula>
//<tff_variable_list>    ::= <tff_variable> | <tff_variable>,<tff_variable_list>
//<tff_variable>         ::= <tff_typed_variable> | <variable>
//<tff_typed_variable>   ::= <variable> : <tff_atomic_type>
//<tff_unary_formula>    ::= <unary_connective> <tff_unitary_formula> |
//                           <fof_infix_unary>
//<tff_atomic_formula>   ::= <fof_atomic_formula>
tff_quantified_formula  : fof_quantifier '[' tff_variable_list ']' ':' tff_unitary_formula;
tff_variable_list       : tff_variable (',' tff_variable)*; // #INFO tff_variable_list flattened
//tff_variable_list       : tff_variable | tff_variable ',' tff_variable_list; // # ALT to tff_variable_list
tff_variable            : tff_typed_variable | variable;
// tff_variable : variable (':' tff_atomic_type)?; // #ALT to tff_variable (more condensed)
tff_typed_variable      : variable ':' tff_atomic_type;
tff_unary_formula       : unary_connective tff_unitary_formula
                        | fof_infix_unary;
tff_atomic_formula      : fof_atomic_formula;

//<tff_conditional>      ::= $ite_f(<tff_logic_formula>,<tff_logic_formula>,
//                           <tff_logic_formula>)
//<tff_let>              ::= $let_tf(<tff_let_term_defns>,<tff_formula>) |
//                           $let_ff(<tff_let_formula_defns>,<tff_formula>)
//%----See the commentary for <thf_let>.
//<tff_let_term_defns>   ::= <tff_let_term_defn> | [<tff_let_term_list>]
//<tff_let_term_list>    ::= <tff_let_term_defn> |
//                           <tff_let_term_defn>,<tff_let_term_list>
//<tff_let_term_defn>    ::= ! [<tff_variable_list>] : <tff_let_term_defn> |
//                           <tff_let_term_binding>
//<tff_let_term_binding> ::= <fof_plain_term> = <fof_term> |
//                           (<tff_let_term_binding>)
//<tff_let_formula_defns> ::= <tff_let_formula_defn> | [<tff_let_formula_list>]
//<tff_let_formula_list> ::= <tff_let_formula_defn> |
//                           <tff_let_formula_defn>,<tff_let_formula_list>
//<tff_let_formula_defn> ::= ! [<tff_variable_list>] : <tff_let_formula_defn> |
//                           <tff_let_formula_binding>
//<tff_let_formula_binding> ::= <fof_plain_atomic_formula> <=>
//                           <tff_unitary_formula> | (<tff_let_formula_binding>)
tff_conditional         : '$ite_f(' tff_logic_formula ',' tff_logic_formula ',' tff_logic_formula ')';
tff_let                 : '$let_tf(' tff_let_term_defns ',' tff_formula ')'
                        | '$let_ff(' tff_let_formula_defns ',' tff_formula ')';
tff_let_term_defns      : tff_let_term_defn | '[' tff_let_term_list ']';
tff_let_term_list       : tff_let_term_defn (',' tff_let_term_defn)*;
tff_let_term_defn       : Forall '[' tff_variable_list ']' ':' tff_let_term_defn
                        | tff_let_term_binding;
tff_let_term_binding    : fof_plain_term Infix_equality fof_term
                        | '(' tff_let_term_binding ')';
tff_let_formula_defns   : tff_let_formula_defn | '[' tff_let_formula_list ']';
tff_let_formula_list    : tff_let_formula_defn (',' tff_let_formula_defn)*;
tff_let_formula_defn    : Forall '[' tff_variable_list ']' ':' tff_let_formula_defn
                        | tff_let_formula_binding;
tff_let_formula_binding : fof_plain_atomic_formula Iff tff_unitary_formula
                        | '(' tff_let_formula_binding ')';

//<tff_sequent>          ::= <tff_formula_tuple> <gentzen_arrow>
//                           <tff_formula_tuple> | (<tff_sequent>)
//<tff_formula_tuple>    ::= [] | [<tff_formula_tuple_list>]
//<tff_formula_tuple_list> ::= <tff_logic_formula> |
//                           <tff_logic_formula>,<tff_formula_tuple_list>
tff_sequent             : tff_formula_tuple Gentzen_arrow tff_formula_tuple
                        | '(' tff_sequent ')';
tff_formula_tuple       : '[]' | '[' tff_formula_tuple_list ']';
tff_formula_tuple_list  : tff_logic_formula (',' tff_logic_formula)*;

//%----<tff_typed_atom> can appear only at top level
//<tff_typed_atom>       ::= <untyped_atom> : <tff_top_level_type> |
//                           (<tff_typed_atom>)
//
//<tff_subtype>          ::= <untyped_atom> <subtype_sign> <atom>
tff_typed_atom          : untyped_atom ':' tff_top_level_type
                        | '(' tff_typed_atom ')';
tff_subtype             : untyped_atom Subtype_sign atom;

//%----See <thf_top_level_type> for commentary.
//<tff_top_level_type>   ::= <tff_atomic_type> | <tff_mapping_type> |
//                           <tf1_quantified_type> | (<tff_top_level_type>)
//<tf1_quantified_type>  ::= !> [<tff_variable_list>] : <tff_monotype>
//<tff_monotype>         ::= <tff_atomic_type> | (<tff_mapping_type>)
//<tff_unitary_type>     ::= <tff_atomic_type> | (<tff_xprod_type>)
//<tff_atomic_type>      ::= <type_constant> | <defined_type> |
//                           <type_functor>(<tff_type_arguments>) | <variable>
//<tff_type_arguments>   ::= <tff_atomic_type> |
//                           <tff_atomic_type>,<tff_type_arguments>
//%----For consistency with <thf_unitary_type> (the analogue in thf),
//%----<tff_atomic_type> should also allow (<tff_atomic_type>), but that causes
//%----ambiguity.
//<tff_mapping_type>     ::= <tff_unitary_type> <arrow> <tff_atomic_type>
//<tff_xprod_type>       ::= <tff_unitary_type> <star> <tff_atomic_type> |
//                           <tff_xprod_type> <star> <tff_atomic_type>
tff_top_level_type      : tff_atomic_type | tff_mapping_type
                        | tf1_quantified_type | '(' tff_top_level_type ')';
tf1_quantified_type     : '!>' '[' tff_variable_list ']' ':' tff_monotype;
tff_monotype            : tff_atomic_type | '(' tff_mapping_type ')';
tff_unitary_type        : tff_atomic_type | '(' tff_xprod_type ')';
tff_atomic_type         : type_constant | defined_type
                        | type_functor '(' tff_type_arguments ')' | variable;
// tff_atomic_type : defined_type | type_functor ('(' tff_type_arguments ')')? | variable; // #ALT to tff_atomic_type (more condensed)
tff_type_arguments      : tff_atomic_type (',' tff_atomic_type)*;
tff_mapping_type        : tff_unitary_type Arrow tff_atomic_type;
tff_xprod_type          : tff_unitary_type Star tff_atomic_type
                        | tff_xprod_type Star tff_atomic_type;

//%----Top of Page---------------------------------------------------------------
//%----TCF formulae.
//<tcf_formula>          ::= <tcf_logic_formula> | <tff_typed_atom>
//<tcf_logic_formula>    ::= <tcf_quantified_formula> | <cnf_formula>
//<tcf_quantified_formula> ::= ! [<tff_variable_list>] : <cnf_formula>
tcf_formula             : tcf_logic_formula | tff_typed_atom;
tcf_logic_formula       : tcf_quantified_formula | cnf_formula;
tcf_quantified_formula  : Forall '[' tff_variable_list ']' ':' cnf_formula;

//%----Top of Page---------------------------------------------------------------
//%----FOF formulae.
//<fof_formula>          ::= <fof_logic_formula> | <fof_sequent>
//<fof_logic_formula>    ::= <fof_binary_formula> | <fof_unitary_formula>
//%----Future answer variable ideas | <answer_formula>
//<fof_binary_formula>   ::= <fof_binary_nonassoc> | <fof_binary_assoc>
//%----Only some binary connectives are associative
//%----There's no precedence among binary connectives
//<fof_binary_nonassoc>  ::= <fof_unitary_formula> <binary_connective>
//                           <fof_unitary_formula>
//%----Associative connectives & and | are in <binary_assoc>
//<fof_binary_assoc>     ::= <fof_or_formula> | <fof_and_formula>
//<fof_or_formula>       ::= <fof_unitary_formula> <vline> <fof_unitary_formula> |
//                           <fof_or_formula> <vline> <fof_unitary_formula>
//<fof_and_formula>      ::= <fof_unitary_formula> & <fof_unitary_formula> |
//                           <fof_and_formula> & <fof_unitary_formula>
//%----<fof_unitary_formula> are in ()s or do not have a <binary_connective> at
//%----the top level.
//<fof_unitary_formula>  ::= <fof_quantified_formula> | <fof_unary_formula> |
//                           <fof_atomic_formula> | (<fof_logic_formula>)
//%----All variables must be quantified
//<fof_quantified_formula> ::= <fof_quantifier> [<fof_variable_list>] :
//                           <fof_unitary_formula>
//<fof_variable_list>    ::= <variable> | <variable>,<fof_variable_list>
//<fof_unary_formula>    ::= <unary_connective> <fof_unitary_formula> |
//                           <fof_infix_unary>
fof_formula             : fof_logic_formula | fof_sequent;
fof_logic_formula       : fof_binary_formula | fof_unitary_formula;
fof_binary_formula      : fof_binary_nonassoc | fof_binary_assoc;
fof_binary_nonassoc     : fof_unitary_formula binary_connective fof_unitary_formula;
fof_binary_assoc        : fof_or_formula | fof_and_formula;
fof_or_formula          : fof_unitary_formula Or fof_unitary_formula
                        | fof_or_formula Or fof_unitary_formula;
fof_and_formula         : fof_unitary_formula And fof_unitary_formula
                        | fof_and_formula And fof_unitary_formula;
fof_unitary_formula     : fof_quantified_formula | fof_unary_formula
                        | fof_atomic_formula | '(' fof_logic_formula ')';
fof_quantified_formula  : fof_quantifier '[' fof_variable_list ']' ':' fof_unitary_formula;
fof_variable_list       : variable (',' variable)*;
fof_unary_formula       : unary_connective fof_unitary_formula | fof_infix_unary;

//%----<fof_term> != <fof_term> is equivalent to ~ <fof_term> = <fof_term>
//<fof_infix_unary>      ::= <fof_term> <infix_inequality> <fof_term>
//<fof_atomic_formula>   ::= <fof_plain_atomic_formula> |
//                           <fof_defined_atomic_formula> |
//                           <fof_system_atomic_formula>
//<fof_plain_atomic_formula> ::= <fof_plain_term>
//<fof_plain_atomic_formula> :== <proposition> | <predicate>(<fof_arguments>)
//<fof_defined_atomic_formula> ::= <fof_defined_plain_formula> |
//                           <fof_defined_infix_formula>
//<fof_defined_plain_formula> ::= <fof_defined_plain_term>
//<fof_defined_plain_formula> :== <defined_proposition> |
//                           <defined_predicate>(<fof_arguments>)
//<fof_defined_infix_formula> ::= <fof_term> <defined_infix_pred> <fof_term>
//%----System terms have system specific interpretations
//<fof_system_atomic_formula> ::= <fof_system_term>
//%----<fof_system_atomic_formula>s are used for evaluable predicates that are
//%----available in particular tools. The predicate names are not controlled by
//%----the TPTP syntax, so use with due care. Same for <fof_system_term>s.
fof_infix_unary             : fof_term Infix_inequality fof_term;
fof_atomic_formula          : fof_plain_atomic_formula
                            | fof_defined_atomic_formula
                            | fof_system_atomic_formula;
fof_plain_atomic_formula    : fof_plain_term;
fof_defined_atomic_formula  : fof_defined_plain_formula | fof_defined_infix_formula;
fof_defined_plain_formula   : fof_defined_term;
fof_defined_infix_formula   : fof_term defined_infix_pred fof_term;
fof_system_atomic_formula   : fof_system_term;

//%----FOF terms.
//<fof_plain_term>       ::= <constant> | <functor>(<fof_arguments>)
//%----Defined terms have TPTP specific interpretations
//<fof_defined_term>     ::= <defined_term> | <fof_defined_atomic_term>
//<fof_defined_atomic_term>  ::= <fof_defined_plain_term>
//%----None yet             | <defined_infix_term>
//%----None yet <defined_infix_term> ::= <fof_term> <defined_infix_func> <fof_term>
//%----None yet <defined_infix_func> ::=
//<fof_defined_plain_term>   ::= <defined_constant> |
//                           <defined_functor>(<fof_arguments>)
//%----System terms have system specific interpretations
//<fof_system_term>      ::= <system_constant> | <system_functor>(<fof_arguments>)
fof_plain_term           : constant
                        | functor '(' fof_arguments ')';
fof_defined_term        : defined_term | fof_defined_atomic_term;
fof_defined_atomic_term : fof_defined_plain_term;
fof_defined_plain_term  : defined_constant
                        | defined_functor '(' fof_arguments ')';
fof_system_term         : system_constant
                        | system_functor '(' fof_arguments ')';

// #ALT alternatives for these terms
//fof_plain_term: functor ('(' fof_arguments ')')?; // contracted for easier handling
//fof_defined_term: defined_functor ('(' fof_arguments ')')?; // contracted for easier handling
//fof_system_term: system_functor ('(' fof_arguments ')')?; // contracted for easier handling


//%----Arguments recurse back to terms (this is the FOF world here)
//<fof_arguments>        ::= <fof_term> | <fof_term>,<fof_arguments>
//%----These are terms used as arguments. Not the entry point for terms because
//%----<fof_plain_term> is also used as <fof_plain_atomic_formula>. The <tff_
//%----options are for only TFF, but are here because <fof_plain_atomic_formula>
//%----is used in <fof_atomic_formula>, which is also used as
//%----<tff_atomic_formula>.
//<fof_term>             ::= <fof_function_term> | <variable> |
//                           <tff_conditional_term> | <tff_let_term> |
//                           <tff_tuple_term>
//<fof_function_term>    ::= <fof_plain_term> | <fof_defined_term> |
//                           <fof_system_term>
fof_arguments           : fof_term (',' fof_term)*;
fof_term                : fof_function_term | variable
                        | tff_conditional_term | tff_let_term
                        | tff_tuple_term;
fof_function_term       : fof_plain_term | fof_defined_term
                        | fof_system_term;

//%----Conditional terms should be used by only TFF.
//<tff_conditional_term> ::= $ite_t(<tff_logic_formula>,<fof_term>,<fof_term>)
//%----Let terms should be used by only TFF. $let_ft is for use when there is
//%----a $ite_t in the <fof_term>. See the commentary for $let_tf and $let_ff.
//<tff_let_term>         ::= $let_ft(<tff_let_formula_defns>,<fof_term>) |
//                           $let_tt(<tff_let_term_defns>,<fof_term>)
//%----<tff_tuple_term> uses {}s to disambiguate from tuples of formulae in []s.
//<tff_tuple_term>       ::= {} | {<fof_arguments>}
tff_conditional_term    : '$ite_t(' tff_logic_formula ',' fof_term ',' fof_term ')';
tff_let_term            : '$let_ft(' tff_let_formula_defns ',' fof_term ')'
                        | '$let_tt(' tff_let_term_defns ',' fof_term ')';
tff_tuple_term          : '{}' | '{' fof_arguments '}';

//%----Top of Page---------------------------------------------------------------
//%----This section is the FOFX syntax. Not yet in use.
//% <fof_let>            ::= := [<fof_let_list>] : <fof_unitary_formula>
//% <fof_let_list>       ::= <fof_defined_var> |
//%                          <fof_defined_var>,<fof_let_list>
//% <fof_defined_var>    ::= <variable> := <fof_logic_formula> |
//%                          <variable> :- <fof_term> | (<fof_defined_var>)
//%
//% <fof_conditional>    ::= $ite_f(<fof_logic_formula>,<fof_logic_formula>,
//%                          <fof_logic_formula>)
//%
//% <fof_conditional_term> ::= $ite_t(<fof_logic_formula>,<fof_term>,<fof_term>)
// #INFO not yet in use therefore not implemented

//<fof_sequent>          ::= <fof_formula_tuple> <gentzen_arrow>
//                           <fof_formula_tuple> | (<fof_sequent>)
//
//<fof_formula_tuple>    ::= [] | [<fof_formula_tuple_list>]
//<fof_formula_tuple_list> ::= <fof_logic_formula> |
//                           <fof_logic_formula>,<fof_formula_tuple_list>
fof_sequent             : fof_formula_tuple Gentzen_arrow fof_formula_tuple
                        | '(' fof_sequent ')';
fof_formula_tuple       : '[]' | '[' fof_formula_tuple_list ']';
fof_formula_tuple_list  : fof_logic_formula (',' fof_logic_formula)*;

//%----Top of Page---------------------------------------------------------------
//%----CNF formulae (variables implicitly universally quantified)
//<cnf_formula>          ::= <disjunction> | (<disjunction>)
//<disjunction>          ::= <literal> | <disjunction> <vline> <literal>
//<literal>              ::= <fof_atomic_formula> | ~ <fof_atomic_formula> |
//                           <fof_infix_unary>
cnf_formula             : cnf_disjunction | '(' cnf_disjunction ')';
cnf_disjunction         : cnf_literal | cnf_disjunction Or cnf_literal;
cnf_literal             : fof_atomic_formula | Not fof_atomic_formula
                        | fof_infix_unary;

//%----Top of Page---------------------------------------------------------------
//%----Connectives - THF
//<thf_quantifier>       ::= <fof_quantifier> | <th0_quantifier> |
//                           <th1_quantifier>
//%----TH0 quantifiers are also available in TH1
//<th1_quantifier>       ::= !> | ?*
//<th0_quantifier>       ::= ^ | @+ | @-
//<thf_pair_connective>  ::= <infix_equality> | <infix_inequality> |
//                           <binary_connective> | <assignment>
//<thf_unary_connective> ::= <unary_connective> | <th1_unary_connective>
//<th1_unary_connective> ::= !! | ?? | @@+ | @@- | @=
thf_quantifier          : fof_quantifier | th0_quantifier
                        | th1_quantifier;
th0_quantifier          : Lambda | Choice | Description;
th1_quantifier          : TyForall | TyExists;
thf_pair_connective     : Infix_equality | Infix_inequality
                        | binary_connective | Assignment ;
thf_unary_connective    : unary_connective | th1_unary_connective;
th1_unary_connective    : ForallComb | ExistsComb | ChoiceComb | DescriptionComb | EqComb;

//%----Connectives - THF and TFF
//<subtype_sign>         ::= <<
// #INFO See Lexer rules

//%----Connectives - TFF
//% <tff_pair_connective>  ::= <binary_connective> | <assignment>
tff_pair_connective     : binary_connective | Assignment;

//%----Connectives - FOF
//<fof_quantifier>       ::= ! | ?
//<binary_connective>    ::= <=> | => | <= | <~> | ~<vline> | ~&
//<assoc_connective>     ::= <vline> | &
//<unary_connective>     ::= ~
fof_quantifier: Forall | Exists;
binary_connective: Iff | Impl | If | Niff | Nor | Nand;
assoc_connective : Or | And;
unary_connective : Not;

//%----The seqent arrow
//<gentzen_arrow>        ::= -->
//<assignment>           ::= :=
// #INFO See Lexer rules for definitions

//%----Types for THF and TFF
//<type_constant>        ::= <type_functor>
//<type_functor>         ::= <atomic_word>
//<defined_type>         ::= <atomic_defined_word>
//<defined_type>         :== $oType | $o | $iType | $i | $tType |
//                           $real | $rat | $int
//%----$oType/$o is the Boolean type, i.e., the type of $true and $false.
//%----$iType/$i is non-empty type of individuals, which may be finite or
//%----infinite. $tType is the type of all types. $real is the type of <real>s.
//%----$rat is the type of <rational>s. $int is the type of <signed_integer>s
//%----and <unsigned_integer>s.
//<system_type>          :== <atomic_system_word>
type_constant           : type_functor;
type_functor            : atomic_word;
// #PREDEF
defined_type            : Dollar_word; // #RES no restrictions //Defined_type;
//Defined_type            : '$oType' | '$o' | '$iType' | '$i' | '$tType' |
//                          '$real' | '$rat' | '$int';
// #UNDEF
//defined_type            : atomic_defined_word;
system_type             : atomic_system_word;

//%----For all language types
//<atom>                 ::= <untyped_atom> | <defined_constant>
//<untyped_atom>         ::= <constant> | <system_constant>
atom                    : untyped_atom | defined_constant;
untyped_atom            : constant | system_constant;

//<defined_proposition>  :== <atomic_defined_word>
//<defined_proposition>  :== $true | $false
//<defined_predicate>    :== <atomic_defined_word>
//<defined_predicate>    :== $distinct |
//                           $less | $lesseq | $greater | $greatereq |
//                           $is_int | $is_rat |
//                           $box_P | $box_i | $box_int | $box |
//                           $dia_P | $dia_i | $dia_int | $dia
// #PREDEF
defined_proposition     : Dollar_word; // #RES // Defined_proposition;
// Defined_proposition     : '$true' | '$false';
defined_predicate       : Dollar_word; // #RES // Defined_predicate;
//Defined_predicate       : '$distinct'
//                        | '$less' | '$lesseq' | '$greater' | '$greatereq'
//                        | '$is_int' | '$is_rat'
//                        | '$box_P' | '$box_i' | '$box_int' | '$box'
//                        | '$dia_P' | '$dia_i' | '$dia_int' | '$dia';
//# UNDEF
// defined_proposition     : atomic_defined_word;
// defined_predicate       : atomic_defined_word;

//%----$distinct means that each of it's constant arguments are pairwise !=. It
//%----is part of the TFF syntax. It can be used only as a fact, not under any
//%----connective.
//<defined_infix_pred>   ::= <infix_equality> | <assignment>
//<infix_equality>       ::= =
//<infix_inequality>     ::= !=
defined_infix_pred      : Infix_equality | Assignment;
//# INFO See lexer rules for definitions


//<constant>             ::= <functor>
//<functor>              ::= <atomic_word>
constant                : functor;
functor                 : atomic_word;

//<system_constant>      ::= <system_functor>
//<system_functor>       ::= <atomic_system_word>
system_constant         : system_functor;
system_functor          : atomic_system_word;

//<defined_constant>     ::= <defined_functor>
//<defined_functor>      ::= <atomic_defined_word>
//<defined_functor>      :== $uminus | $sum | $difference | $product |
//                           $quotient | $quotient_e | $quotient_t | $quotient_f |
//                           $remainder_e | $remainder_t | $remainder_f |
//                           $floor | $ceiling | $truncate | $round |
//                           $to_int | $to_rat | $to_real
defined_constant        : defined_functor;
defined_functor         : atomic_defined_word;

//<defined_term>         ::= <number> | <distinct_object>
//<variable>             ::= <upper_word>
defined_term            : number | Distinct_object;
variable                : Upper_word;

//%----Top of Page---------------------------------------------------------------
//%----Formula sources
//<source>               ::= <general_term>
//<source>               :== <dag_source> | <internal_source> |
//                           <external_source> | unknown | [<sources>]
//%----Alternative sources are recorded like this, thus allowing representation
//%----of alternative derivations with shared parts.
//<sources>              :== <source> | <source>,<sources>
//%----Only a <dag_source> can be a <name>, i.e., derived formulae can be
//%----identified by a <name> or an <inference_record>
//<dag_source>           :== <name> | <inference_record>
source                  : dag_source | internal_source
                        | external_source
                        | Lower_word // #RES | 'unknown'
                        | '[' sources ']';
sources                 : source ( ',' source )*; // #INFO flattened
//sources                 : source | source ',' sources; // #ALT to flattened sources
dag_source              : name | inference_record;

//<inference_record>     :== inference(<inference_rule>,<useful_info>,
//                           <inference_parents>)
//<inference_rule>       :== <atomic_word>
inference_record        : 'inference(' inference_rule ',' useful_info ',' inference_parents ')';
inference_rule          : atomic_word;

//%----Examples are          deduction | modus_tollens | modus_ponens | rewrite |
//%                          resolution | paramodulation | factorization |
//%                          cnf_conversion | cnf_refutation | ...
//%----<inference_parents> can be empty in cases when there is a justification
//%----for a tautologous theorem. In case when a tautology is introduced as
//%----a leaf, e.g., for splitting, then use an <internal_source>.
//<inference_parents>    :== [] | [<parent_list>]
//<parent_list>          :== <parent_info> | <parent_info>,<parent_list>
//<parent_info>          :== <source><parent_details>
//<parent_details>       :== :<general_list> | <null>
//<internal_source>      :== introduced(<intro_type><optional_info>)
//<intro_type>           :== definition | axiom_of_choice | tautology | assumption
inference_parents       : '[]' | '[' parent_list ']';
parent_list             : parent_info ( ',' parent_info )*; // #INFO flattened
//parent_list             : parent_info | parent_info ',' parent_list; // #ALT to flattened parent_list
parent_info             : source parent_details?; // #INFO ? because parent_details may be empty
parent_details          : ':' general_list;
internal_source         : 'introduced(' intro_type optional_info? ')';
intro_type              : Lower_word; // #RES Intro_type;
//Intro_type              : 'definition' | 'axiom_of_choice' | 'tautology' | 'assumption';

//%----This should be used to record the symbol being defined, or the function
//%----for the axiom of choice
//<external_source>      :== <file_source> | <theory> | <creator_source>
//<file_source>          :== file(<file_name><file_info>)
//<file_info>            :== ,<name> | <null>
//<theory>               :== theory(<theory_name><optional_info>)
//<theory_name>          :== equality | ac
//%----More theory names may be added in the future. The <optional_info> is
//%----used to store, e.g., which axioms of equality have been implicitly used,
//%----e.g., theory(equality,[rst]). Standard format still to be decided.
//<creator_source>       :== creator(<creator_name><optional_info>)
//<creator_name>         :== <atomic_word>
external_source          : file_source | theory | creator_source;
file_source             : 'file(' file_name file_info? ')'; // #INFO ? because file_info may be empty
file_info               : ',' name;
theory                  : 'theory(' theory_name optional_info? ')'; // #INFO ? because optional_info may be empty
theory_name             : Lower_word; // #RES Theory_name;
//Theory_name             : 'equality' | 'ac';
creator_source          : 'creator(' creator_name optional_info? ')'; // #INFO ? because optional_info may be empty
creator_name            : atomic_word;

//%----Useful info fields
//<optional_info>        ::= ,<useful_info> | <null>
//<useful_info>          ::= <general_list>
//<useful_info>          :== [] | [<info_items>]
//<info_items>           :== <info_item> | <info_item>,<info_items>
//<info_item>            :== <formula_item> | <inference_item> |
//                           <general_function>
optional_info           : ',' useful_info;
useful_info             : '[]' | '[' info_items ']' | general_list;
info_items              : info_item ( ',' info_item )*; // #INFO flattened
info_item               : formula_item | inference_item
                        | general_function;

//%----Useful info for formula records
//<formula_item>         :== <description_item> | <iquote_item>
//<description_item>     :== description(<atomic_word>)
//<iquote_item>          :== iquote(<atomic_word>)
//%----<iquote_item>s are used for recording exactly what the system output about
//%----the inference step. In the future it is planned to encode this information
//%----in standardized forms as <parent_details> in each <inference_record>.
//%----Useful info for inference records
formula_item            : description_item | iquote_item;
description_item        : 'description(' atomic_word ')';
iquote_item             : 'iquote(' atomic_word ')';

//<inference_item>       :== <inference_status> | <assumptions_record> |
//                           <new_symbol_record> | <refutation>
//<inference_status>     :== status(<status_value>) | <inference_info>
//%----These are the success status values from the SZS ontology. The most
//%----commonly used values are:
//%----  thm - Every model of the parent formulae is a model of the inferred
//%----        formula. Regular logical consequences.
//%----  cth - Every model of the parent formulae is a model of the negation of
//%----        the inferred formula. Used for negation of conjectures in FOF to
//%----        CNF conversion.
//%----  esa - There exists a model of the parent formulae iff there exists a
//%----        model of the inferred formula. Used for Skolemization steps.
//%----For the full hierarchy see the SZSOntology file distributed with the TPTP.
//<status_value>         :== suc | unp | sap | esa | sat | fsa | thm | eqv | tac |
//                           wec | eth | tau | wtc | wth | cax | sca | tca | wca |
//                           cup | csp | ecs | csa | cth | ceq | unc | wcc | ect |
//                           fun | uns | wuc | wct | scc | uca | noc
//%----<inference_info> is used to record standard information associated with an
//%----arbitrary inference rule. The <inference_rule> is the same as the
//%----<inference_rule> of the <inference_record>. The <atomic_word> indicates
//%----the information being recorded in the <general_list>. The <atomic_word>
//%----are (loosely) set by TPTP conventions, and include esplit, sr_split, and
//%----discharge.
//<inference_info>       :== <inference_rule>(<atomic_word>,<general_list>)
inference_item          : inference_status | assumptions_record
                        | new_symbol_record | refutation;
inference_status        : 'status(' status_value ')' | inference_info;
status_value            : Lower_word; // #RES Status_value;
//Status_value            : 'suc' | 'unp' | 'sap' | 'esa' | 'sat' | 'fsa' | 'thm' | 'eqv' | 'tac'
//                        | 'wec' | 'eth' | 'tau' | 'wtc' | 'wth' | 'cax' | 'sca' | 'tca' | 'wca'
//                        | 'cup' | 'csp' | 'ecs' | 'csa' | 'cth' | 'ceq' | 'unc' | 'wcc' | 'ect'
//                        | 'fun' | 'uns' | 'wuc' | 'wct' | 'scc' | 'uca' | 'noc';
inference_info          : inference_rule '(' atomic_word ',' general_list ')';

//%----An <assumptions_record> lists the names of assumptions upon which this
//%----inferred formula depends. These must be discharged in a completed proof.
//<assumptions_record>   :== assumptions([<name_list>])
//%----A <refutation> record names a file in which the inference recorded here
//%----is recorded as a proof by refutation.
//<refutation>           :== refutation(<file_source>)
//%----A <new_symbol_record> provides information about a newly introduced symbol.
//<new_symbol_record>    :== new_symbols(<atomic_word>,[<new_symbol_list>])
//<new_symbol_list>      :== <principal_symbol> |
//                           <principal_symbol>,<new_symbol_list>
//%----Principal symbols are predicates, functions, variables
//<principal_symbol>   :== <functor> | <variable>
assumptions_record      : 'assumptions(' '[' name_list ']' ')';
refutation              : 'refutation(' file_source ')';
new_symbol_record       : 'new_symbols(' atomic_word ',' '[' new_symbol_list ']' ')';
new_symbol_list         : principal_symbol ( ',' principal_symbol )*; // #INFO flattened
//new_symbol_list         : principal_symbol | principal_symbol ',' new_symbol_list; //#ALT to flattened new_symbol_list
principal_symbol        : functor | variable;

//%----Include directives
//<include>              ::= include(<file_name><formula_selection>).
//<formula_selection>    ::= ,[<name_list>] | <null>
//<name_list>            ::= <name> | <name>,<name_list>
include                 : 'include(' file_name formula_selection? ').'; // #INFO ? because formula_selection may be empty
formula_selection       : ',' '[' name_list ']';
name_list               : name (',' name)*;

//%----Non-logical data
//<general_term>         ::= <general_data> | <general_data>:<general_term> |
//                           <general_list>
//<general_data>         ::= <atomic_word> | <general_function> |
//                           <variable> | <number> | <distinct_object> |
//                           <formula_data>
//<general_function>     ::= <atomic_word>(<general_terms>)
//%----A <general_data> bind() term is used to record a variable binding in an
//%----inference, as an element of the <parent_details> list.
//<general_data>         :== bind(<variable>,<formula_data>)
//<formula_data>         ::= $thf(<thf_formula>) | $tff(<tff_formula>) |
//                           $fof(<fof_formula>) | $cnf(<cnf_formula>) |
//                           $fot(<fof_term>)
//<general_list>         ::= [] | [<general_terms>]
//<general_terms>        ::= <general_term> | <general_term>,<general_terms>
general_term            : general_data | general_data ':' general_term
                        | general_list;
general_data            : atomic_word | general_function
                        | variable | number | Distinct_object
                        | formula_data;
general_function        : atomic_word '(' general_terms ')';
formula_data            : '$thf(' thf_formula ')' | '$tff(' tff_formula ')'
                        | '$fof(' fof_formula ')' | '$cnf(' cnf_formula ')'
                        | '$fot(' fof_term ')';
general_list            : '[]' | '[' general_terms ']';
general_terms           : general_term (',' general_term)*; // #INFO flattened
//general_terms           : general_term | general_term ',' general_terms; // #ALT to flattened general_terms

//%----General purpose
//<name>                 ::= <atomic_word> | <integer>
//%----Integer names are expected to be unsigned
//<atomic_word>          ::= <lower_word> | <single_quoted>
//%----<single_quoted> tokens do not include their outer quotes, therefore the
//%----<lower_word> <atomic_word> cat and the <single_quoted> <atomic_word> 'cat'
//%----are the same. Quotes must be removed from a <single_quoted> <atomic_word>
//%----if doing so produces a <lower_word> <atomic_word>. Note that <numbers>s
//%----and <variable>s are not <lower_word>s, so '123' and 123, and 'X' and X,
//%----are different.
//<atomic_defined_word>  ::= <dollar_word>
//<atomic_system_word>   ::= <dollar_dollar_word>
//<number>               ::= <integer> | <rational> | <real>
//%----Numbers are always interpreted as themselves, and are thus implicitly
//%----distinct if they have different values, e.g., 1 != 2 is an implicit axiom.
//%----All numbers are base 10 at the moment.
//<file_name>            ::= <single_quoted>
name : atomic_word | Integer;
atomic_word : Lower_word | Single_quoted;
atomic_defined_word : Dollar_word;
atomic_system_word : Dollar_dollar_word;
number : Integer | Rational | Real;
file_name : Single_quoted;

//<null>                 ::=

// TOKEN RULES SEE ABOVE











































































