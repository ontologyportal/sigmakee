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
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

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
        validatePictureList();
    }
}

