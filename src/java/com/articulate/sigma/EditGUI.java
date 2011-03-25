/** This code is copyrighted by Rearden Commerce (c) 2011.  It is
released under the GNU Public License &lt;http://www.gnu.org/copyleft/gpl.html&gt;.  

Users of this code also consent, by use of this code, to credit
Articulate Software in any writings, briefings, publications,
presentations, or other representations of any software which
incorporates, builds on, or uses this code.  Please cite the following
article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net.
*/

/*************************************************************************************************/
package com.articulate.sigma;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** A class that generates a GUI for elements of the ontology. */
public class EditGUI {

	ArrayList terms = new ArrayList();
	
    /** *************************************************************
	*/
	public void readConfig() {

		LineNumberReader lnr = null;
	 
	   	try {
	   		File fin  = new File("config.txt");
	   		FileReader fr = new FileReader(fin);
	   		if (fr != null) {
	   			lnr = new LineNumberReader(fr);
	   			String line = null;
	   			while ((line = lnr.readLine()) != null) {
	   				line = line.trim();
	   				terms.add(line);
	   			}
	   		}
	   	}
	   	catch (IOException ioe) {
	   		System.out.println("File error: " + ioe.getMessage());      
	   	}
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in EditGUI.readConfig()");
            }
        }
	}
	
	/** *************************************************************
	*/
	public String genRelPage(KB kb, String rel) {
		
		StringBuffer sb = new StringBuffer();

		// get classes or instances that apply for each argument
		return sb.toString();
	}
	
	/** *************************************************************
	 * Get and print all instances of a class
	*/
	public static String printInstances(KB kb, String className) {
		
		StringBuffer sb = new StringBuffer();

		TreeSet ts = kb.getAllInstances(className);
		Iterator it = ts.iterator();
		while (it.hasNext()) {
			String term = (String) it.next();
			sb.append(term);
			if (it.hasNext()) sb.append(", ");
		}
		return sb.toString();
	}
	
	/** *************************************************************
	 * get parent classes of term
	*/
	public static ArrayList<String> genParentList(KB kb, String term) {
				
		ArrayList<String> parents = new ArrayList<String>();
		ArrayList<Formula> res = kb.askWithRestriction(0,"instance",1,term);
		for (int i = 0; i < res.size(); i++) {
			Formula f = res.get(i);
			parents.add(f.getArgument(2));
		}
		return parents;
	}
	
	/** *************************************************************
	*/
	public static HashMap<String,ArrayList<ArrayList<String>>> genInstList(KB kb, String term) {
		
		HashMap<String,ArrayList<ArrayList<String>>> result = new HashMap<String,ArrayList<ArrayList<String>>>();
		
		ArrayList<String> parents = genParentList(kb,term);
		// get relations that apply
		ArrayList<String> relations = new ArrayList<String>();
		for (int i = 0; i < parents.size(); i++) {
			String parent = parents.get(i);
			ArrayList<Formula> res = kb.askWithRestriction(0,"domain",3,parent);
			//sb.append(";; res = " + Integer.toString(res.size()) + " for " +
			//		  "(domain ?X ?Y " + parent + ")");
			for (int k = 0; k < res.size(); k++) {
				Formula f = res.get(k);
				relations.add(f.getArgument(1));					
			}					
		}
		
		// get the arity of each relation
		HashMap<String,Integer> arity = new HashMap<String,Integer>();
		for (int i = 0; i < relations.size(); i++) {
			String r = relations.get(i);
			int a = kb.getValence(r);
			arity.put(r,new Integer(a));
		}
		
		for (int i = 0; i < relations.size(); i++) {
			String r = relations.get(i);
			ArrayList al = new ArrayList();
			result.put(r,al);
            int a = arity.get(r).intValue();
            for (int j = 1; j <= a; j++) {
            	ArrayList al2 = new ArrayList();
            	String className = kb.getArgType(r,j);
            	al2.addAll(kb.getAllInstances(className));
            	al.add(al2);
            }            
		}
		return result;
	}
	
	/** *************************************************************
	*/
	public static String genInstPage(KB kb, String term, String kbHref) {

		StringBuffer sb = new StringBuffer();
		HashMap<String,ArrayList<ArrayList<String>>> instList = EditGUI.genInstList(kb,term);
		ArrayList<String> parents = genParentList(kb,term);

		// show the instance and its class
		sb.append("<font size=+3><a href=\"" + kbHref + term + "\">" + term + "</a></font>:");
		for (int i = 0; i < parents.size(); i++) {
			String parent = parents.get(i);
			sb.append("<a href=\"" + kbHref + parent + "\">" + parent + "</a>,");
		}
		sb.append("<P>\n");
		
		// get relevant relations and their argument types
		Iterator it = instList.keySet().iterator();
		while (it.hasNext()) {
			String relation = (String) it.next();
			sb.append(relation + ":");
			ArrayList<ArrayList<String>> arguments = instList.get(relation);
            for (int i = 0; i < arguments.size(); i++) {
            	ArrayList<String> fillers = arguments.get(i);
           		sb.append(HTMLformatter.createMenu(relation + "--" + Integer.toString(i),"", fillers, ""));
            }
            sb.append("<P>\n");
		}
		// for each relation, get other class and instances that apply
		return sb.toString();
	}
					
    /** *************************************************************
     * A test method.
     */
    public static void main (String args[]) {
    	
        try {
            KBmanager.getMgr().initializeOnce();
            WordNet.initOnce();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB("SUMO");
    	System.out.println(genInstPage(kb,"UnitedStates",""));
    }

}
