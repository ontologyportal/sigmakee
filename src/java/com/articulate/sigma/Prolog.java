package com.articulate.sigma;

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
     * @param fname - the name of the file to write, including full path.
     */
    public static String writePrologFile(String fname) {

        File file = null;
        PrintWriter pr = null;
        String result = null;

        try {
            file = new File(fname);
            if ((WordNet.wn != null) && WordNet.wn.wordFrequencies.isEmpty())
                WordNet.wn.readWordFrequencies();
            pr = new PrintWriter(new FileWriter(file));
            pr.println("% Copyright (c) 2006-2009 Articulate Software Incorporated");
            pr.println("% This software released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.");
            pr.println("% This is a very lossy translation to prolog of the KIF ontologies available at www.ontologyportal.org\n");

            pr.println("% subAttribute");
            writePrologFormulas(kb.ask("arg",0,"subAttribute"),pr);
            pr.println("\n% subrelation");
            writePrologFormulas(kb.ask("arg",0,"subrelation"),pr);
            pr.println("\n% disjoint");
            writePrologFormulas(kb.ask("arg",0,"disjoint"),pr);
            pr.println("\n% partition");
            writePrologFormulas(kb.ask("arg",0,"partition"),pr);
            pr.println("\n% instance");
            writePrologFormulas(kb.ask("arg",0,"instance"),pr);
            pr.println("\n% subclass");
            writePrologFormulas(kb.ask("arg",0,"subclass"),pr);
            System.out.println(" ");

            pr.flush();
            result = file.getCanonicalPath();
        }
        catch (Exception e) {
            System.out.println("Error in KB.writePrologFile(): " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            try {
                if (pr != null) pr.close();
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
          KB kb = KBmanager.getMgr().getKB("pTest");
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
