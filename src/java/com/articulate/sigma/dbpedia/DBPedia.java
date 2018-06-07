/** This code is copyright Articulate Software (c) 2003-2007.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net

 Authors:
 Adam Pease
 Infosys LTD.
 */

package com.articulate.sigma.dbpedia;

import com.articulate.sigma.*;
import com.articulate.sigma.wordNet.MultiWords;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** ***************************************************************
 *  This program finds and displays SUMO terms that are related in meaning to the English
 *  expressions that are entered as input.  Note that this program uses four WordNet data
 *  files, "NOUN.EXC", "VERB.EXC" etc, as well as four WordNet to SUMO
 *  mappings files called "WordNetMappings-nouns.txt", "WordNetMappings-verbs.txt" etc
 *  The main part of the program prompts the user for an English term and then
 *  returns associated SUMO concepts.  The two primary public methods are initOnce() and page().
 *  @author Ian Niles
 *  @author Adam Pease
 */
public class DBPedia implements Serializable {

    public static boolean debug = false;
    public static DBPedia dbp  = new DBPedia();


    /**  This array contains all of the regular expression strings that
     * will be compiled to Pattern objects for use in the methods in
     * this file. */
    private static final String[] regexPatternStrings =
    {
        // 0: DBpediaStrings.ttl
        // ^(dbp:)(.*)(rdfs:.*)(")([\S ]*)(")(.*\.)$
        "^(dbp:)(.*)(rdfs:.*)(\")([\\S ]*)(\")(.*\\.)$",
        
        // 1: DBPediaSUMO.ttl
        // ^(dbp:)(.*)(rdf:.*)(sumo:)(.*)(\.)$
        "^(dbp:)(.*)(rdf:.*)(sumo:)(.*)(\\.)$"
    };
    
    /** This array contains all of the compiled Pattern objects that
     * will be used by methods in this file. */
    private static Pattern[] regexPatterns = null;
    private transient Matcher m;
    private static HashMap<String,String> wnFilenames = new HashMap<>();
    public MultiWords multiWords = new MultiWords();
    public Hashtable<String, String> dbpSUMOSenseKeys = new Hashtable<String, String>();

    public MultiWords getMultiWords() {

        return multiWords;
    }

    /** ***************************************************************
     */
    private void makeFileMap() {

    	/* DBPedia related, these files are read like any other files using getWnFile(),
    	 so the files should be placed under WordNet.baseDir */
    	wnFilenames.put("dbpedia_words",    "DBpediaStrings.ttl");
    	wnFilenames.put("dbpedia_SUMO",     "DBPediaSUMO.ttl");
    	
     }

    /** ***************************************************************
     * This method compiles all of the regular expression pattern
     * strings in regexPatternStrings and puts the resulting compiled
     * Pattern objects in the Pattern[] regexPatterns.
     */
    private void compileRegexPatterns() {
        
        System.out.println("INFO in WordNet.compileRegexPatterns(): compiling patterns");
        regexPatterns = new Pattern[regexPatternStrings.length];
        for (int i = 0; i < regexPatternStrings.length; i++) {
            regexPatterns[i] = Pattern.compile(regexPatternStrings[i]);
            if (!(regexPatterns[i] instanceof Pattern)) 
                System.out.println("ERROR in WordNet.compileRegexPatterns(): could not compile \""
                        + regexPatternStrings[i] + "\"");            
        }
        return;
    }

    /** ***************************************************************
     * Returns the WordNet File object corresponding to key.
     *
     * @param key A descriptive literal String that maps to a regular
     * expression pattern used to obtain a WordNet file.
     *
     * @return A File object
     */
    public File getWnFile(String key, String override) {
        
        File theFile = null;
        try {
            if (override != null)
                theFile = new File(override);
            else if ((key != null) && (KBmanager.getMgr().getPref("dbpediaSrcDir") != null))
                theFile = new File(KBmanager.getMgr().getPref("dbpediaSrcDir") + File.separator + wnFilenames.get(key));
            if (theFile == null || !theFile.exists())
                System.out.println("Error in WordNet.getWnFile(): no such file: " + theFile.getAbsolutePath());
        }
        catch (Exception ex) {
            System.out.println("Error in WordNet.getWnFile(): key: " + key);
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
        return theFile;
    }

    private void readDBPedia() throws IOException {
    	System.out.println("INFO in WordNet.readDBPedia(): Reading DBpediaStrings.ttl");
    	try (LineNumberReader lr = new LineNumberReader(new FileReader(getWnFile("dbpedia_words", null)))) {
    		lr.readLine();
    		lr.readLine();
    		lr.readLine();
    		lr.readLine();
    		String line;
    		while ((line = lr.readLine()) != null) {
    			m = regexPatterns[0].matcher(line);
    			if ((m.matches()) && (m.group(2).indexOf("_") >= 0))
    				multiWords.addDBPediaMultiWord(m.group(2).trim());
    		}
    	}
    }

    private void readDBPediaSUMO() throws IOException {
    	System.out.println("INFO in WordNet.readDBPedia(): Reading DBPediaSUMO.ttl");
    	try (LineNumberReader lr = new LineNumberReader(new FileReader(getWnFile("dbpedia_SUMO", null)))) {
    		lr.readLine();
    		lr.readLine();
    		lr.readLine();
    		lr.readLine();
    		String line;
    		while ((line = lr.readLine()) != null) {
    			m = regexPatterns[1].matcher(line);
    			if (m.matches())
    				this.dbpSUMOSenseKeys.put(m.group(2).trim(), m.group(5).trim());
    		}
    	}
    }
    
    public static void initOnce() {
    	try {
    		dbp.makeFileMap();
            dbp.compileRegexPatterns();
	    	dbp.readDBPedia();
	    	dbp.readDBPediaSUMO();
    	}
    	catch (Exception ex) {
            System.out.println("Error in WordNet.initOnce(): ");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
