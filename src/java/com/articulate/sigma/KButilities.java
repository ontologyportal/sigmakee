/** This code is copyright Articulate Software (c) 2003.  Some
portions copyright Teknowledge (c) 2003 and reused under the termsof the GNU
license.  This software is released under the GNU Public License
<http://www.gnu.org/copyleft/gpl.html>.  Users of this code also consent,
by use of this code, to credit Articulate Software and Teknowledge in any
writings, briefings, publications, presentations, or other representations
of any software which incorporates, builds on, or uses this code.  Please
cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. see also 
http://sigmakee.sourceforge.net 
*/

/*************************************************************************************************/
package com.articulate.sigma;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.articulate.sigma.KB;

/** *****************************************************************
 *  Contains utility methods for KBs
 */
public class KButilities {

    /** *************************************************************
     */
    public static boolean isRelation(KB kb, String term) {
        
        return kb.isInstanceOf(term,"Relation");
    }

    /** *************************************************************
     */
    public static boolean isFunction(KB kb, String term) {
        return kb.isInstanceOf(term,"Function");        
    }
    
    /** *************************************************************
     */
    public static boolean isAttribute(KB kb, String term) {
        return kb.isInstanceOf(term,"Attribute");
    }
    
    /** *************************************************************
     */
    public static boolean isClass(KB kb, String term) {
        return kb.isInstanceOf(term,"Class");
    }
    
    /** *************************************************************
     */
    public static boolean isInstance(KB kb, String term) {
        return !kb.isInstanceOf(term,"Class");
    }
    
    /** *************************************************************
     * Get all formulas that contain both terms. 
     */
    public static ArrayList<Formula> termIntersection(KB kb, String term1, String term2) {
  	
    	ArrayList<Formula> ant1 = kb.ask("ant",0,term1);
    	ArrayList<Formula> ant2 = kb.ask("ant",0,term2);
        ArrayList<Formula> cons1 = kb.ask("cons",0,term1);
        ArrayList<Formula> cons2 = kb.ask("cons",0,term2);
        HashSet<Formula> hrule1 = new HashSet<Formula>();
        hrule1.addAll(ant1);
        hrule1.addAll(cons1);
        HashSet<Formula> hrule2 = new HashSet<Formula>();
        hrule2.addAll(ant2);
        hrule2.addAll(cons2);
        ArrayList<Formula> result = new ArrayList<Formula>();
        result.addAll(hrule1);
        result.retainAll(hrule2);
        ArrayList<Formula> stmt1 = kb.ask("stmt",0,term1);
        ArrayList<Formula> stmt2 = kb.ask("stmt",0,term2);
        stmt1.retainAll(stmt2);
        result.addAll(stmt1);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
            	if (j != i) {
                    ArrayList<Formula> stmt = kb.askWithRestriction(i,term1,j,term2);
                    result.addAll(stmt);
            	}
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static void countRelations(KB kb) {

        System.out.println("Relations: " + kb.getCountRelations());
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList al = kb.ask("arg",0,term);
            if (al != null && al.size() > 0) {
                System.out.println(term + " " + al.size());
            }
        }
    }

    /** *************************************************************
     */
    private static boolean uRLexists(String URLName){

        try {
            HttpURLConnection.setFollowRedirects(false);
            // note : you may also need
            //        HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con =
                (HttpURLConnection) new URL(URLName).openConnection();
            con.setRequestMethod("HEAD");
            return(con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** *************************************************************
     */
    public static void checkURLs(KB kb) {

        URL u = null;
        ArrayList<Formula> results = kb.ask("arg",0,"externalImage");
        for (int i = 0; i < results.size(); i++) {
            Formula f = (Formula) results.get(i);
            String url = StringUtil.removeEnclosingQuotes(f.getArgument(2));
            if (!uRLexists(url)) 
                System.out.println(f + " doesn't exist");
        }
    }


    /** *************************************************************
     */
    public static void validatePictureList() {

        // (externalImage WaterVehicle "http://upload.wikimedia.org/wikipedia/commons/1/12/2003_LWGO_ubt.JPG") 
        // http://commons.wikimedia.org/wiki/File:2003_LWGO_ubt.JPG
        // http://upload.wikimedia.org/wikipedia/commons/f/ff/Fishing_boat_ORL-3_Gdynia_Poland_2003_ubt.JPG
        // http://en.wikipedia.org/wiki/File:Reef.jpg
        // (externalImage Reef "http://upload.wikimedia.org/wikipedia/en/3/33/Reef.jpg") 
        // http://upload.wikimedia.org/wikipedia/commons/3/33/Reef.jpg
        // 
        URL u = null;
        String line = null;

        FileReader fr = null;
        LineNumberReader lr = null;

        try {
            fr = new FileReader("pictureList.kif");
            lr = new LineNumberReader(fr);
            Pattern p = Pattern.compile("([^ ]+) ([^ ]+) \"([^\"]+)\"\\)");
            while ((line = lr.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String url = StringUtil.removeEnclosingQuotes(m.group(3));
                    //System.out.println("the url: " + url);
                    if (!uRLexists(url)) 
                        System.out.println(";; " + line);
                    else
                        System.out.println(line);
                }
                else
                    System.out.println(line);
            }
        }
        catch (java.io.IOException e) {
            System.out.println("Error reading pictureList.kif\n" + e.getMessage());
        }
        finally {
            try {
                if (lr != null) 
                    lr.close();            
                if (fr != null) 
                    fr.close(); 
            }
            catch (Exception ex) {
            }
        }
    }
    
    /** *************************************************************
     *  Turn SUMO into a semantic network by extracting all ground
     *  binary relations, turning all higher arity relations into a
     *  set of binary relations, and making all term co-occurring in
     *  an axiom to be related with a general "link" relation. Also
     *  use the subclass hierarchy to relate all parents of terms in
     *  domain statements, through the relation itself but with a
     *  suffix designating it as a separate relation. Convert SUMO
     *  terms to WordNet synsets.
     */
    private static Set<String> generateSemanticNetwork(KB kb) {

        TreeSet<String> resultSet = new TreeSet<String>();
        Iterator<String> it = kb.formulaMap.keySet().iterator();
        while (it.hasNext()) {          // look at all formulas in the KB
            String formula = it.next();
            Formula f = new Formula();
            f.read(formula);
            if (!f.isSimpleClause() || !f.isGround()) {                
                Set<String> terms = f.collectTerms();
                for (String term1 : terms) {
                    if (Formula.isLogicalOperator(term1) || Formula.isVariable(term1))
                        continue;                
                    for (String term2 : terms) {
                        if (Formula.isLogicalOperator(term2) || Formula.isVariable(term2))
                            continue;  
                        //resultSet.add("(link " + term1 + " " + term2 + ")");
                        if (!term1.equals(term2))
                            resultSet.add(term1 + " link " +  term2);
                    }
                }
            }
            else {
                String predicate = f.getArgument(0);            
                ArrayList<String> args = f.argumentsToArrayList(1);
                if (args != null && args.size() == 2) { // could have a function which would return null
                    String arg1 = f.getArgument(1);
                    String arg2 = f.getArgument(2);  
                    if (!Formula.isVariable(arg1) && !Formula.isVariable(arg1))
                        resultSet.add(arg1 + " " + predicate + " " +  arg2);
                }
            }
        }
        return resultSet;
    }

    /** *************************************************************
     *  Find all cases of where (instance A B) (instance B C) as
     *  well as all cases of where (instance A B) (instance B C)
     *  (instance C D).  Report true if any such cases are found,
     *  false otherwise.
     */
    public static boolean instanceOfInstanceP(KB kb) {

        boolean result = false;
        Iterator it = kb.terms.iterator();
        while (it.hasNext()) {
            String term = (String) it.next();
            ArrayList<Formula> al = kb.askWithRestriction(0,"instance",1,term);
            for (int i = 0; i < al.size(); i++) {
                Formula f = (Formula) al.get(i);
                String term2 = f.getArgument(2);
                if (Formula.atom(term2)) {
                    ArrayList<Formula> al2 = kb.askWithRestriction(0,"instance",1,term2);
                    if (al2.size() > 0)
                        result = true;
                    for (int j = 0; j < al2.size(); j++) {
                        Formula f2 = (Formula) al2.get(j);
                        String term3 = f2.getArgument(2);
                        if (Formula.atom(term3)) {
                            ArrayList<Formula> al3 = kb.askWithRestriction(0,"instance",1,term3);
                            for (int k = 0; k < al3.size(); k++) {
                                Formula f3 = (Formula) al3.get(k);
                                String term4 = f3.getArgument(2);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static void writeDisplayText(KB kb, String displayFormatPredicate, String displayTermPredicate, 
            String language, String fname) throws IOException {
        
        PrintWriter pr = null;
        try {
            pr = new PrintWriter(new FileWriter(fname, false));            
            //get all formulas that have the display predicate as the predicate           
            ArrayList<Formula> formats = kb.askWithRestriction(0, displayFormatPredicate, 1, language);
            ArrayList<Formula> terms = kb.askWithRestriction(0, displayTermPredicate, 1, language);            
            HashMap<String,String> termMap = new HashMap<String,String>();            
            for (int i = 0; i < terms.size(); i++) {
                Formula term = terms.get(i);                
                String key = term.getArgument(2);
                String value = term.getArgument(3);                
                if (key != "" && value != "") 
                    termMap.put(key, value);                
            }            
            for (int i = 0; i < formats.size(); i++) {
                Formula format = formats.get(i);                
                // This is the current predicate whose format we are keeping track of. 
                String key = format.getArgument(2);
                String value = format.getArgument(3);                
                if (key != "" && value != "") {                
                    // This basically gets all statements that use the current predicate in the 0 position
                    ArrayList<Formula> predInstances = kb.ask("arg", 0, key);                    
                    for(int j=0; j < predInstances.size(); j++) {
                        StringBuilder sb = new StringBuilder();
                        String displayText = String.copyValueOf(value.toCharArray());                        
                        Formula f = predInstances.get(j);
                        ArrayList arguments = f.argumentsToArrayList(0);      
                        sb.append(key);
                        sb.append(",");           
                        // check if each of the arguments for the statements is to be replaced in its
                        // format statement.
                        for (int k = 1; k < arguments.size(); k++) {
                            String argName = f.getArgument(k);
                            String term = (String) termMap.get(argName);
                            term = StringUtil.removeEnclosingQuotes(term);
                            String argNum = "%" + String.valueOf(k);
                        
                            // also, add the SUMO Concept that is replaced in the format
                            if (displayText.contains(argNum)) {
                                sb.append(argName);
                                sb.append(",");
                                displayText = displayText.replace(argNum, term);                                
                            }                                                                
                        }                                             
                        sb.append(displayText);                                               
                        // resulting line will be something like:
                        // <predicate>, <argument_0>, ..., <argument_n>, <display_text>
                        // note: argument_0 to argument_n is only placed there if their 
                        // termFormat is used in the display_text.
                        pr.println(sb.toString());                        
                    }                    
                }
            }            
            
        }
        catch (java.io.IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (pr != null) 
                pr.close();            
        }
    }

    /** *************************************************************
     */
    public static void generateTPTPTestAssertions() {
        
        try {
            int counter = 0;
            System.out.println("INFO in KB.generateTPTPTestAssertions()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println("INFO in KB.generateTPTPTestAssertions(): printing predicates");
            Iterator<String> it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = it.next();
                if (Character.isLowerCase(term.charAt(0)) && kb.kbCache.valences.get(term) <= 2) {
                    /*
                    ArrayList<Formula> forms = kb.askWithRestriction(0,"domain",1,term);
                    for (int i = 0; i < forms.size(); i++) {
                        String argnum = forms.get(i).getArgument(2);
                        String type = forms.get(i).getArgument(3);
                        if (argnum.equals("1"))
                            System.out.print("(instance Foo " + type + "),");
                        if (argnum.equals("2"))
                            System.out.print("(instance Bar " + type + ")");
                    }
                    */
                    String argType1 = kb.getArgType(term,1);
                    String argType2 = kb.getArgType(term,2);
                    if (argType1 != null && argType2 != null) {
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__" + term + "(s__Foo,s__Bar))).|");
                        System.out.print("fof(local_" + counter++ + ",axiom,(s__instance(s__Foo,s__" + argType1 + "))).|");
                        System.out.println("fof(local_" + counter++ + ",axiom,(s__instance(s__Bar,s__" + argType2 + "))).");
                    }                    
                }
            }
        } 
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /** *************************************************************
     * Note this simply assumes that initial lower case terms are relations.
     */
    public static void generateRelationList() {
        
        try {
            System.out.println("INFO in KB.generateRelationList()");
            KBmanager.getMgr().initializeOnce();
            KB kb = KBmanager.getMgr().getKB("SUMO");
            System.out.println("INFO in KB.generateRelationList(): printing predicates");
            Iterator<String> it = kb.terms.iterator();
            while (it.hasNext()) {
                String term = it.next();
                if (Character.isLowerCase(term.charAt(0)))
                    System.out.println(term);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /** *************************************************************
     * Count the number of words in all the strings in a knowledge base
     */
    public static void countStringWords(KB kb) {

        int total = 0;
        System.out.println("INFO in KB.countStringWords(): counting words");
        Iterator<String> it = kb.formulas.keySet().iterator();
        while (it.hasNext()) {
            String s = it.next();
            Pattern p = Pattern.compile("\"(.+)\"");
            Matcher m = p.matcher(s);
            boolean b = m.find();
            if (b) {
                String quoted = m.group(1);
                String[] ar = quoted.split(" ");
                for (int i = 0; i < ar.length-1; i++) {
                    if (ar[i].matches("\\w+"))
                        total++;
                }
                System.out.println(quoted);
                System.out.println(ar.length);
            }
        }
        System.out.println(total);
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        try {
            KBmanager.getMgr().initializeOnce();
            // WordNet.initOnce();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");
        //countRelations(kb);
        //checkURLs(kb);
        //validatePictureList();
        //for (String s : generateSemanticNetwork(kb))
        //    System.out.println(s);
        countStringWords(kb);
    }
}

