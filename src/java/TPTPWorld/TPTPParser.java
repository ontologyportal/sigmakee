package TPTPWorld;

import java.util.*;
import java.io.*;
import tptp_parser.*;

public class TPTPParser {

    public Hashtable<String,TPTPFormula> ftable;
    public Vector<SimpleTptpParserOutput.TopLevelItem> Items;

    /** ***************************************************************
     */
    public static class Symbol {

        String text;
        int arity;
        Symbol (String text, int arity) {
            this.text = text;
            this.arity = arity;
        }
        public String toString () {
            return "[" + text + " |  " + arity + "]";
        }
    }

    /** ***************************************************************
     */
    public static class SymbolComparator implements Comparator {

        public int compare(Object obj1, Object obj2) {
            String s1 = ((Symbol)obj1).text;
            String s2 = ((Symbol)obj2).text;
            //      System.out.println("comparing s1: " + s1 + " to s2: " + s2);
            return s1.compareTo(s2);
        }
    }

    /** ***************************************************************
     */
    public TPTPParser (BufferedReader reader) throws Exception {

        StringBuffer result = new StringBuffer();
        TptpLexer lexer = new TptpLexer(reader);
        TptpParser parser = new TptpParser(lexer);
        SimpleTptpParserOutput outputManager = new SimpleTptpParserOutput();		
        this.ftable = new Hashtable();
        this.Items = new Vector();

        int i = 0;
        for (SimpleTptpParserOutput.TopLevelItem item = 
                 (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager);
             item != null;
             item = (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager)) {
            TPTPFormula formula = new TPTPFormula(item, i);
            String name = getName(formula.item);
            this.ftable.put(name, formula);
            this.Items.add(item);
            i++;
        }

        // add parents to tptp formula info
        Set<String> set = this.ftable.keySet();	
        Iterator<String> itr = set.iterator();
        Vector<String> parents = new Vector();
        while (itr.hasNext()) {
            String str = itr.next();
            TPTPFormula formula = this.ftable.get(str);
            SimpleTptpParserOutput.Source source = formula.source;
            if (source != null) {
                if (source.getKind() == SimpleTptpParserOutput.Source.Kind.Inference) {
                    gatherParents(source, parents);
                    for (String parent : parents) {
                        formula.addParent((TPTPFormula)this.ftable.get(parent));
                    }
                } else {
                    String sourceInfo = source.toString();
                    if (!sourceInfo.contains("(") && !sourceInfo.contains(")")) {
                        formula.addParent((TPTPFormula)this.ftable.get(sourceInfo));
                    }       
                }
            }
            parents.clear();
        }
    }

    /** ***************************************************************
     */
    public static void checkArguments (String args[]) {

        // has to have at least one argument (for filename or stdin)
        if (args.length < 1) {
            System.out.println("%ERROR: Please supply filename or -- for stdin");
            //Necessary?
            //System.exit(0);
        }
    }

    /** ***************************************************************
     */
    public static BufferedReader createReader (String arg) throws Exception {

        BufferedReader reader;
        if (arg.equals("--")) {
            // read from stdin
            InputStreamReader sr = new InputStreamReader(System.in);
            reader = new BufferedReader(sr);
        } else {
            // read from file
            if (!(new File(arg)).exists()) {
                System.out.println("%ERROR: Not a valid file: " + arg);
                //Necessary?
                //System.exit(0);
            }
            FileReader fr = new FileReader(arg);
            reader = new BufferedReader(fr);
        }
        return reader;
    }

    /** ***************************************************************
     */
    public static TPTPParser parse (BufferedReader reader) throws Exception {
        return new TPTPParser(reader);
    }

    /** ***************************************************************
     */
    public static void gatherParents (SimpleTptpParserOutput.Source source, 
                                      Vector<String> parents) {

        for (SimpleTptpParserOutput.ParentInfo p 
                 : ((SimpleTptpParserOutput.Source.Inference)source).getParentInfoList()) {
            SimpleTptpParserOutput.Source psource = p.getSource();
            if (psource.getKind() == SimpleTptpParserOutput.Source.Kind.Inference) {
                gatherParents(psource, parents);
            } else if (!(p.toString()).contains("(") && !(p.toString()).contains(")")){

                // System.out.println("##### Adding Parent: "+ p.toString());

                parents.add(p.toString());
            } else if (p.toString().contains(":")) {
                String[] ps;
                ps = p.toString().split(":");
                if (!ps[0].contains("(")) {
                    parents.add(ps[0]);
                    // System.out.println("##### Also adding Parent: "+ p.toString());
                }
            } // else System.out.println("##### Not Adding Parent: "+ p.toString());
        }
    }

    /** ***************************************************************
     */
    public static String getType (SimpleTptpParserOutput.TopLevelItem item) {

        if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
            SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
            return AF.getRole().toString();
        } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
            SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
            return AC.getRole().toString();
        } else {
            return "";
        }
    }

    /** ***************************************************************
     */
    public static String getName (SimpleTptpParserOutput.TopLevelItem item) {

        if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
            SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
            return AF.getName();
        } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
            SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
            return AC.getName();
        } else {
            return null;
        }
    }

    /** ***************************************************************
     */
    public static ArrayList<String> identifyTermVariables (SimpleTptpParserOutput.Formula.Atomic 
                                                           atom,
                                                           ArrayList<String> variables) {

        // if arguments.size() > 1, then definetly is not a variable
        for (int j = 0; j < atom.getNumberOfArguments(); j++) {
            SimpleTptpParserOutput.Term term = 
                (SimpleTptpParserOutput.Term) ((LinkedList)atom.getArguments()).get(j);
            if (term.getTopSymbol().isVariable()) {          
                String variable = term.getTopSymbol().toString();
                boolean unique = true;
                for (String oldvariable : variables) {
                    if (oldvariable.equals(variable)) {
                        unique = false;
                    }
                }
                if (unique) {
                    variables.add(variable);
                }
            }
        }
        return variables;
    }

    /** ***************************************************************
     */
    // given an fof, identify all quantified variables in order from start of
    public static ArrayList<String> identifyQuantifiedVariables (SimpleTptpParserOutput.Formula 
                                                                 formula,
                                                                 ArrayList<String> variables) {

        switch(formula.getKind()) {
        case Atomic:
            // no more quantified variables
            break;
        case Negation:
            variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Negation)
                                                     formula).getArgument(), 
                                                    variables);
            break;
        case Binary:
            variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Binary)
                                                     formula).getLhs(), 
                                                    variables);
            variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Binary)
                                                     formula).getRhs(), 
                                                    variables);
            break;
        case Quantified:
            variables.add(((SimpleTptpParserOutput.Formula.Quantified)formula).getVariable());
            variables = identifyQuantifiedVariables(((SimpleTptpParserOutput.Formula.Quantified)
                                                     formula).getMatrix(), 
                                                    variables);
            break;
        default:
            break;
        }
        return variables;    
    }

    /** ***************************************************************
     */
    public static ArrayList<String> identifyFormulaVariables (SimpleTptpParserOutput.Formula 
                                                              formula,
                                                              ArrayList<String> variables) {

        switch(formula.getKind()) {
        case Atomic:
            variables = identifyTermVariables((SimpleTptpParserOutput.Formula.Atomic)formula, 
                                              variables);
            break;
        case Negation:
            variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(), variables);
            break;
        case Binary:
            variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(), variables);
            variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(), variables);
            break;
        case Quantified:
            variables = identifyFormulaVariables(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(), variables);
            break;
        default:
            break;
        }
        return variables;
    }

    /** ***************************************************************
     */
    public static ArrayList<String> identifyClauseVariables (SimpleTptpParserOutput.Clause clause,
                                                             ArrayList<String> variables) {

        LinkedList<SimpleTptpParserOutput.Literal> literals = (LinkedList)clause.getLiterals();
        if (literals == null) {
            return variables;
        }
        for (int i = 0; i < literals.size(); i++) {        
            SimpleTptpParserOutput.Literal literal = literals.get(i);
            variables = identifyTermVariables((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom(), variables);
        }
        return variables;
    }

    /** ***************************************************************
     */
    // identify variables in the formula, store as ArrayList of Strings
    public static ArrayList<String> identifyVariables (TPTPFormula formula) {

        ArrayList<String> variables = new ArrayList();
        SimpleTptpParserOutput.TopLevelItem item = formula.item;
        if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
            SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
            variables = identifyFormulaVariables(AF.getFormula(), variables);
        } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
            SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
            variables = identifyClauseVariables(AC.getClause(), variables);
        } 
        return variables;     
    }

    /** ***************************************************************
     */
    private static TreeSet<Symbol> getSymbolList (SimpleTptpParserOutput.Term term, TreeSet set) {

        //    System.out.println("Looking at term: " + term.toString());
        LinkedList<SimpleTptpParserOutput.Term> arguments = (LinkedList)term.getArguments();
        if (arguments == null) {
            //      System.out.println("Term has no more arguments");
            return set;
        }
        for (int i = 0; i < arguments.size(); i++) {
            if (!((SimpleTptpParserOutput.Term)arguments.get(i)).getTopSymbol().isVariable()) {
                SimpleTptpParserOutput.Term newTerm = (SimpleTptpParserOutput.Term)arguments.get(i);
                int arity = (newTerm.getArguments() != null) ? ((LinkedList)newTerm.getArguments()).size() : 0;
                Symbol symbol = new Symbol(newTerm.getTopSymbol().toString(), arity);
                set.add(symbol);
                set = getSymbolList(newTerm, set);
            }
        }
        return set;
    }

    /** ***************************************************************
     */
    private static TreeSet<Symbol> getSymbolList (SimpleTptpParserOutput.Formula.Atomic atom, TreeSet set) {

        //        System.out.println("Looking at atom: " + atom.toString());
        LinkedList<SimpleTptpParserOutput.Term> arguments = (LinkedList)atom.getArguments();
        if (arguments == null) {
            //      System.out.println("Atom has no more arguments");
            return set; 
        }
        for (int i = 0; i < arguments.size(); i++) {
            //      System.out.println("looking at: " + arguments.get(i).toString());
            if (!((SimpleTptpParserOutput.Term)arguments.get(i)).getTopSymbol().isVariable()) {
                //        System.out.println("This is a not a variable: " + ((SimpleTptpParserOutput.Term)arguments.get(i)).getTopSymbol().toString());
                SimpleTptpParserOutput.Term newTerm = (SimpleTptpParserOutput.Term)arguments.get(i);
                int arity = (newTerm.getArguments() != null) ? ((LinkedList)newTerm.getArguments()).size() : 0;
                Symbol symbol = new Symbol(newTerm.getTopSymbol().toString(), arity);
                set.add(symbol);
                set = getSymbolList(newTerm, set);
            } else {
                //        System.out.println("This is a variable: " + ((SimpleTptpParserOutput.Term)arguments.get(i)).getTopSymbol().toString());
            }
        }
        return set;
    }

    /** ***************************************************************
     */
    private static TreeSet<Symbol> getSymbolList (SimpleTptpParserOutput.Formula formula, TreeSet set) {

        //    System.out.println("Looking at formula: " + formula.toString());
        switch(formula.getKind()) {
        case Atomic:
            set = getSymbolList((SimpleTptpParserOutput.Formula.Atomic)formula, set);
            break;
        case Negation:
            set = getSymbolList(((SimpleTptpParserOutput.Formula.Negation)formula).getArgument(), set);
            break;
        case Binary:
            set = getSymbolList(((SimpleTptpParserOutput.Formula.Binary)formula).getLhs(), set);
            set = getSymbolList(((SimpleTptpParserOutput.Formula.Binary)formula).getRhs(), set);
            break;
        case Quantified:
            set = getSymbolList(((SimpleTptpParserOutput.Formula.Quantified)formula).getMatrix(), set);
            break;
        default:
            break;
        }
        return set;
    }

    /** ***************************************************************
     */
    private static TreeSet<Symbol> getSymbolList (SimpleTptpParserOutput.Literal literal, TreeSet set) {
        set = getSymbolList((SimpleTptpParserOutput.Formula.Atomic)literal.getAtom(), set);
        return set;
    }

    /** ***************************************************************
     */
    private static TreeSet<Symbol> getSymbolList (SimpleTptpParserOutput.Clause clause, TreeSet set) {

        //    System.out.println("Looking at clause: " + clause.toString());
        LinkedList<SimpleTptpParserOutput.Literal> literals = (LinkedList)clause.getLiterals();
        if (literals == null) {
            return set;
        }
        assert !literals.isEmpty();
        for (int i = 0; i < literals.size(); i++) {
            set = getSymbolList(literals.get(i), set);
        }
        return set;
    }

    /** ***************************************************************
     */
    public static TreeSet<Symbol> getSymbolList (BufferedReader reader) throws Exception {

        TreeSet<Symbol> set = new TreeSet(new TPTPParser.SymbolComparator());
        TPTPParser parser = TPTPParser.parse(reader);
        for (int i = 0; i < parser.Items.size(); i++) {
            SimpleTptpParserOutput.TopLevelItem item = parser.Items.get(i);
            if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
                SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
                set = getSymbolList(AF.getFormula(),set);
            } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
                SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
                set = getSymbolList(AC.getClause(),set);
            }
        }
        return set;
    }

    /** ***************************************************************
     */
    public static TreeSet<Symbol> getSymbolList (String filename) throws Exception {

        TreeSet<Symbol> result = null;
        try {
            BufferedReader reader = TPTPParser.createReader(filename);
            result = TPTPParser.getSymbolList(reader);
        }
        catch (Exception ex) {
            System.out.println("ERROR reading " + filename);
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
        return result;
    }

    /** ***************************************************************
     */
    // given a list of bindings, get list of symbols
    public static TreeSet<Symbol> getSymbolList (ArrayList<Binding> bindings) throws Exception {

        String temp = "";
        int count = 0;
        for (Binding bind : bindings) {
            temp += "fof(dummy__formula_" + count + ", axiom, dummy_predicate(" + bind.binding +  ")).\n"; 
        }
        BufferedReader reader = new BufferedReader(new StringReader(temp));
        return getSymbolList(reader);
    }

    /** ***************************************************************
     */
    public static void main (String args[]) throws Exception {

         TPTPParser.checkArguments(args);
        // assumption: filename is args[0] or "--" for stdin
       
        BufferedReader reader = TPTPParser.createReader(args[0]);

        TreeSet<Symbol> set = TPTPParser.getSymbolList(reader);
        Iterator it = set.iterator();
        int count = 0;
        while (it.hasNext()) {
            System.out.println("[" + count + "]: " + it.next());
            count++;
        }
    }
}
