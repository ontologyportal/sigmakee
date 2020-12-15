/** This code is copyright Krystof Hoder and Articulate Software
2009. This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  

Please cite the following when describing SInE
Hoder, K., (2008) The SUMO Inference Engine (SInE).  Master's thesis,
Charles University, Prague.  See also http://www.cs.man.ac.uk/~hoderk/sine/
                                           
Please cite the following article in any publication with references
when addressing Sigma in general:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net 
*/

package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Krystof Hoder - Minor changes by Adam Pease
 */
public class SInE extends InferenceEngine {

    static boolean kifFormat = true;

    /** *************************************************************
     */
    public static SInE getNewInstance(String kbFileName) {

        SInE res = null;
        try {
            res = new SInE(kbFileName);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return res;
    }
    
    /** *************************************************************
     */
    public static class SInEFactory extends EngineFactory {

        @Override
        public InferenceEngine createWithFormulas(Iterable<String> formulaSource) {
                return new SInE(formulaSource);
        }

        @Override
        public InferenceEngine createFromKBFile(String kbFileName) {
                return SInE.getNewInstance(kbFileName);
        }      
    }
    
    /** *************************************************************
     */
    public static EngineFactory getFactory() {
            return new SInEFactory();
    }
    
    static Pattern symbolPattern=Pattern.compile("(?<!\\w|\\?|\\@)[a-zA-Z][_a-zA-Z0-9-]*");
    static Pattern quotedPattern=Pattern.compile("\"[^\"]*\"");
    
    static Set<String> nonSymbols=new HashSet<String>();
    static {
        nonSymbols.add("and");
        nonSymbols.add("or");
        nonSymbols.add("not");
        nonSymbols.add("forall");
        nonSymbols.add("exists");
    }    

    ArrayList<String> formulas;
    /***
     * These formulas will be always selected.
     * Actually formulas which contain no symbols are put here.
     */
    ArrayList<String> mandatoryFormulas;
    Hashtable<String, Set<String>> formSymbols;
    Hashtable<String, Integer> degrees;
    
    /***
     * Relation between symbols and formulas which represents the fact that 
     * a formula (axiom) defines meaning of a symbol.
     * 
     * The meaning of strings is following:<br>
     * Map<Symbol, List<Formula>>
     */
    Map<String, List<String>> requirements;
    EngineFactory underlyingEngineFactory;
    
    /** *************************************************************
     */
    private SInE(String kbFileName, EngineFactory ef) throws Exception {

        underlyingEngineFactory = ef;
        formSymbols = new Hashtable<String, Set<String>>();
        formulas = new ArrayList<String>();
        mandatoryFormulas = new ArrayList<String>();
        degrees = new Hashtable<String, Integer>();
        requirements = new Hashtable<String, List<String>>();
    
        String error = null;
        //System.out.println("INFO in SInE(): initializing");
        File kbFile = null;
        if (error == null) {
            kbFile = new File(kbFileName);
            if (!kbFile.exists() ) {
                error = ("The file " + kbFileName + " does not exist");
                System.out.println("INFO in SInE(): " + error);
                KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                             + "\n<br/>" + error + "\n<br/>");
            }
        }
        
        if (error == null) {
            if (!kifFormat) {
                //System.out.println("INFO in SInE(): non KIF");
                ArrayList<String> forms = loadNonKif(kbFile);
                if (!forms.isEmpty()) {
                    formulas.ensureCapacity(forms.size());
                    loadFormulas(forms);
                }
            }
            else {
                KIF kif = new KIF();
                kif.setParseMode(KIF.RELAXED_PARSE_MODE);
                kif.readFile(kbFile.getCanonicalPath());
                if (!kif.formulaMap.keySet().isEmpty()) {
                    formulas.ensureCapacity(kif.formulaMap.keySet().size());
                    loadFormulas(kif.formulaMap.keySet());
                }
            }
        }
    }

    /** *************************************************************
     */
    private SInE(String kbFileName) throws Exception {
         //this(kbFileName, Vampire.getFactory());
    }

    /** *************************************************************
     */
    public SInE(Iterable<String> formulaSource) { 

        //underlyingEngineFactory = Vampire.getFactory();
        formSymbols = new Hashtable<String, Set<String>>();
        formulas = new ArrayList<String>();
        mandatoryFormulas = new ArrayList<String>();
        degrees = new Hashtable<String, Integer>();
        requirements = new Hashtable<String, List<String>>();
    
        loadFormulas(formulaSource);
    }

    /** *************************************************************
     * Loads formulas from given source.
     */
    public ArrayList<String> loadNonKif(File kbFile) {

        ArrayList<String> result = new ArrayList<>();
        LineNumberReader lr = null;
        String line = null;
        try {
            FileReader fr = new FileReader(kbFile);
            lr = new LineNumberReader(fr);
            while ((line = lr.readLine()) != null) {
                if (!StringUtil.emptyString(line) && !line.startsWith("%")) {
                    result.add(line);
                    //System.out.println(line);
                }
            }
        }
        catch (Exception ex1) {
            System.out.println("ERROR in ENTER SInE.loadNonKif(" + kbFile + ")");
            System.out.println("  approx. line # == " + lr.getLineNumber());
            System.out.println("  line == " + line);
            ex1.printStackTrace();
        }
        System.out.println("SInE.loadNonKif(): loaded " + result.size() + " formulas ");
        return result;
    }

    /** *************************************************************
     * Loads formulas from given source.
     * 
     * @param formulaSource Iterable object that contains strings representing formulas.
     */
    public void loadFormulas(Iterable<String> formulaSource) {

        Iterator it = formulaSource.iterator();
    
        // First we go through all formulas, and compute degree of each symbol.
        while (it.hasNext()) {
            String f = (String) it.next();            
            formulas.add(f);            
            for (String sym : getSymbols(f)) {
                Integer prev = degrees.get(sym);
                if (prev != null) 
                    degrees.put(sym, prev+1);
                else 
                    degrees.put(sym, 1);                
            }
        }
        //System.out.println("SInE.loadFormulas(): stored " + formulas.size() + " formulas ");
        
        // Now we associate each formula with its lowest-degree symbols.
        for (String form : formulas) {
            Set<String> symbols = getSymbols(form);
            if (symbols.size() == 0) {
                mandatoryFormulas.add(form);
                continue;
            }
                        
            int minDeg = 5000000;
            ArrayList<String> minDegSyms = new ArrayList<String>();
            for (String sym : symbols) {
                int deg = degrees.get(sym);
                if (deg < minDeg) {
                    minDeg = deg;
                    minDegSyms.clear();
                    minDegSyms.add(sym);
                }
                else if (deg == minDeg) {
                    minDegSyms.add(sym);
                }
            }
            //System.out.println("SInE.loadFormulas(): minDegSyms: " + minDegSyms);
            for (String sym : minDegSyms) {
                List<String> reqForms = requirements.get(sym);
                if (reqForms == null) {
                    reqForms = new ArrayList<String>();
                    reqForms.add(form);
                    requirements.put(sym, reqForms);
                }
                else {
                    reqForms.add(form);
                }
            }
        }
        //System.out.println("SInE.loadFormulas(): computed " + requirements.size() + " formulas ");
    }
      
    /** *************************************************************
     * Returns all symbols occurring in given formula.
     * 
     * @param form Formula to get symbols from.
     * @return Symbols occurring in given formula.
     */
    public Set<String> getSymbols(String form) {

        if (form.startsWith("tff") || form.startsWith("tptp") || form.startsWith("thf") || form.startsWith("fof"))
            form = form.substring(form.indexOf('(',5),form.length());
        //System.out.println("SInE.getSymbols(): form: " + form);
        Set<String> res = formSymbols.get(form);
        if (res == null) {
            res = new HashSet<String>();            
            String quotesCollapsedForm = quotedPattern.matcher(form).replaceAll("\"\"");            
            Matcher symMatch = symbolPattern.matcher(quotesCollapsedForm);
            while (symMatch.find()) 
                res.add(symMatch.group());           
            res.removeAll(nonSymbols);            
            formSymbols.put(form, res);
        }
        //System.out.println("SInE.getSymbols(): symbols: " + res);
        return res;
    }

    /** *************************************************************
     */
    public Set<String> getSymbols(Collection<String> forms) {

        //System.out.println("SInE.getSymbols(): forms " + forms.size());
        Set<String> syms = new HashSet<String>();
        for (String form : forms) 
            syms.addAll(getSymbols(form));
        //System.out.println("SInE.getSymbols(): symbols " + syms.size());
        return syms; 
    }
    
    /** *************************************************************
     * Returns formulas that are directly required by given symbols
     * (in the sense of requirements map).   
     * 
     * @param symbols Symbols whose required formulas will be found.
     * @return Formulas required by symbols.
     */
    public Set<String> get1RequiredFormulas(Collection<String> symbols) {

        //System.out.println("SInE.get1RequiredFormulas(): symbols: " + symbols);
        Set<String> reqForms = new HashSet<String>();
        for (String sym : symbols) {
            List<String> symReqForms = requirements.get(sym);
            if (symReqForms == null)
                continue;
            for (String form : symReqForms) 
                reqForms.add(form);            
        }
        return reqForms;
    }
    
    /** *************************************************************
     * Returns all symbols transitively required by given symbols
     * (in the sense of requirements map).
     * 
     * @param symbols Collection of symbols to be closed under requirements relation.
     * @return Closure of given collection of symbols under requirements relation.
     */
    public Set<String> getRequiredSymbols(Collection<String> symbols) {

        Set<String> reqSyms = new HashSet<String>(symbols);            
        int prevSize = 0;
        do {
            prevSize = reqSyms.size();                    
            Set<String> reqForms = get1RequiredFormulas(reqSyms);                    
            reqSyms.addAll(getSymbols(reqForms));
        } while (reqSyms.size() > prevSize);
        System.out.println("SInE.getRequiredSymbols(): " + reqSyms.size());
        return reqSyms;
    }
    
    /** *************************************************************
     * Returns formulas that are transitively required by given symbols
     * (in the sense of requirements map).
     * 
     * @param symbols Symbols whose required formulas will be found.
     * @return Formulas transitively required by symbols.
     */
    public Set<String> getRequiredFormulas(Collection<String> symbols) {

        Set<String> reqSyms = getRequiredSymbols(symbols);
        return get1RequiredFormulas(reqSyms);
    }

    /** *************************************************************
     * Performs axiom selection for given query.
     * 
     * @param form Formula, according to which axioms will be selected.
     * @return Selected formulas.
     */
    public Set<String> performSelection(String form) {

        Set<String> symbols = getSymbols(form);
        //System.out.println("performSelection(): form: " + form);
        //System.out.println("performSelection(): symbols: " + symbols);
        symbols.addAll(getSymbols(mandatoryFormulas));            
        Set<String> res = getRequiredFormulas(symbols);            
        res.addAll(mandatoryFormulas);
        
        return res;
    }
    
    /** *************************************************************
     */
    @Override
    public String submitQuery(String formula, int timeLimit, int bindingsLimit)
                    throws IOException {
        
        long t1 = System.currentTimeMillis();
        Set<String> selectedFormulas = performSelection(formula);        
        long t_elapsed = (System.currentTimeMillis() - t1);

        System.out.println("INFO in SInE.submitQuery(): "
                           + (t_elapsed / 1000.0)
                           + " seconds to perform axiom selection");
        System.out.println("INFO in SInE.submitQuery(): "
                + selectedFormulas.size() + " formula(s) selected out of " + formulas.size()); 

        // print all selected formulas for debugging
        Iterator it = selectedFormulas.iterator();
        while (it.hasNext()) {
            String f = (String) it.next();
            System.out.println(f);
        }

        InferenceEngine eng = underlyingEngineFactory.createWithFormulas(selectedFormulas);           
        String res = eng.submitQuery(formula, timeLimit, bindingsLimit);            
        eng.terminate();
        
        return res;
    }

    /** *************************************************************
     */
    @Override
    public String assertFormula(String formula) {

        formulas.add(formula);
        //Formulas asserted through this method will always be used.
        mandatoryFormulas.add(formula);
        
        return null;
    }

    /** *************************************************************
     *  A simple test to load a KB file and pose a query, which are
     *  the first and second item, respectively, given on the
     *  command line.
     */
    public static void test(String[] args) {

        String kbFileName = args[0];
        if (!kbFileName.endsWith(".kif"))
            kifFormat = false;
        String query = args[1];
        System.out.println("% Selecting from " + kbFileName);

        SInE sine = SInE.getNewInstance(kbFileName);
        System.out.println("test(): object degrees: " + sine.degrees.get("s__Object"));
        System.out.println("test(): subclass degrees: " + sine.degrees.get("s__subclass"));
        System.out.println("test(): object requirements: " + sine.requirements.get("s__Object"));
        System.out.println("test(): subclass requirements: " + sine.requirements.get("s__subclass"));
        System.out.println("test(): symbols: " + sine.getSymbols(query));
        Set<String> selectedFormulas = sine.performSelection(query);
        System.out.println("test(): "  + selectedFormulas.size() + " formulas");
        for (String form : selectedFormulas)
            System.out.println(form);
       // sine.terminate();
    }

    /** *************************************************************
     *  A simple test to load a KB file and pose a query, which are
     *  the first and second item, respectively, given on the
     *  command line.
     */
    public static void main (String[] args) throws Exception {

        test(args);
    }
}
