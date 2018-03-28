package com.articulate.sigma;

import com.articulate.sigma.wordNet.WordNet;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class Prolog {

    public static KB kb = null;
    
    /** *************************************************************
     */
    private static void writePrologFormulas(ArrayList<Formula> forms, PrintWriter pw) {

        TreeSet<Formula> ts = new TreeSet<Formula>();
        ts.addAll(forms);
        if (forms != null) {
            Formula formula = null;
            String result = null;
            Iterator<Formula> it = ts.iterator(); 
            while (it.hasNext()) {
                formula = it.next();
                result = formula.toProlog();
                if (result != null && result != "")
                    pw.println(result);
            }
        }
        return;
    }

    /** *************************************************************
     */
    private static void writeOneHornClause(Formula f, PrintWriter pw) {
        
        System.out.println("INFO in Prolog.writeOneHornClause(): formula: " + f);
        StringBuffer sb = new StringBuffer();
        Formula antecedent = f.cdrAsFormula().carAsFormula();
        System.out.println("INFO in Prolog.writeOneHornClause(): antecedent: " + antecedent);
        sb.append(" :- ");
        if (antecedent.isSimpleClause(kb)) {
            String clause = antecedent.toProlog();
            if (clause == null)
                return;
            sb.append(clause);
        }
        if (antecedent.car().equals("and")) {
            Formula consList = antecedent.cdrAsFormula();
            System.out.println("INFO in Prolog.writeOneHornClause(): consList: " + consList);
            while (!consList.empty()) {
                Formula car = consList.carAsFormula();     
                String clause = car.toProlog();
                if (clause == null)
                    return;
                sb.append(clause);
                consList = consList.cdrAsFormula();
                System.out.println("INFO in Prolog.writeOneHornClause(): consList: " + consList);
                if (!consList.empty())
                    sb.append(", ");
            }
        }

        Formula consequent = f.cdrAsFormula().cdrAsFormula().carAsFormula();
        System.out.println("INFO in Prolog.writeOneHornClause(): consequent: " + consequent);
        if (consequent.isSimpleClause(kb)) {
            String clause = consequent.toProlog();
            if (clause == null)
                return;
            pw.println(clause + sb.toString() + ".");
        }
        if (consequent.car().equals("and")) {
              Formula consList = consequent.cdrAsFormula();
              boolean first = true;
              while (!consList.empty()) {
                  Formula car = consList.carAsFormula();                  
                  consList = consList.cdrAsFormula();
                  String carst = car.toProlog();  
                  if (carst == null)
                      return;
                  pw.println(carst + sb.toString() + ".");
              }
        }
    }
    
    /** *************************************************************
     */
    private static void writeClauses(PrintWriter pw) {
        
        Iterator<Formula> it = kb.formulaMap.values().iterator();
        while (it.hasNext()) {
            Formula f = it.next();
            if (f.isRule() && f.isHorn(kb) && !f.theFormula.contains("exists") &&
                !f.theFormula.contains("forall")) 
                writeOneHornClause(f,pw);  
            else if (f.isSimpleClause(kb))
                pw.println(f.toProlog() + ".");
        }
    }
    
    /** *************************************************************
     * @param fname - the name of the file to write, including full path.
     */
    public static String writePrologFile(String fname) {

        File file = null;
        PrintWriter pw = null;
        String result = null;

        try {
            file = new File(fname);
            if ((WordNet.wn != null) && WordNet.wn.wordCoFrequencies.isEmpty())
                WordNet.wn.readWordCoFrequencies();
            pw = new PrintWriter(new FileWriter(file));
            pw.println("% Copyright (c) 2006-2009 Articulate Software Incorporated");
            pw.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pw.println("% This is a very lossy translation to prolog of the KIF ontologies available at www.ontologyportal.org\n");
            writeClauses(pw);
            pw.flush();
            result = file.getCanonicalPath();
        }
        catch (Exception e) {
            System.out.println("Error in KB.writePrologFile(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pw != null) pw.close();
            }
            catch (Exception e1) {
            }
        }
        return result;
    }
    /** *************************************************************
     */
    public static void main(String[] args) {

       String prologFile = null;
       File plFile = null;
       String pfcp = null;
       try {
          KBmanager.getMgr().initializeOnce();
          KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
          plFile = new File(KBmanager.getMgr().getPref("kbDir") + File.separator + kb.name + ".pl");
          pfcp = plFile.getCanonicalPath();
          Prolog.kb = kb;
          Prolog.writePrologFile(pfcp);
       } 
       catch (Exception pfe) {
           pfe.printStackTrace();
       }
       String result = ((StringUtil.isNonEmptyString(prologFile) && plFile.canRead())
                 ? ("Wrote the Prolog file " + prologFile)
                 : "Could not write a Prolog file");
    }
}
