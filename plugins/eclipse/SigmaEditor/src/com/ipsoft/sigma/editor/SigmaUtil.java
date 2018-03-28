package com.articulate.ipsoft.editor;

/** This code is copyright Articulate Software (c) 2003.  Some portions
 copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.SortedSet;

import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;

/** ***************************************************************
 */
public class SigmaUtil {

	private static KB kb;
	private boolean isLoadSUMO = true;
	private static String[] cnfterms;
	private static String[] sumoterms;

    /** ***************************************************************
     */
	public static String[] getCNFTerms() {

		if (cnfterms == null)
			cnfterms = readTerms("CNFTerms.txt");
		return cnfterms;
	}

    /** ***************************************************************
     */
	public static String[] getSUMOTerms() {

		if (sumoterms == null)
			sumoterms = readTerms("sumoterms.txt");
		return sumoterms;
	}

    /** ***************************************************************
     */
	public static String[] readTerms(String filename) {

		Scanner in;
		HashSet<String> terms = new HashSet<String>();
		
		Reader paramReader = new InputStreamReader(SigmaUtil.class.getResourceAsStream("/sigmaeditor/editors/" + filename));
		if (paramReader == null) SigmaLogger.log("resouce reader is null");
		in = new Scanner(paramReader);
		while (in.hasNextLine()) {
			String k = in.nextLine();
			terms.add(k);
		}
		try {
			paramReader.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
		return terms.toArray(new String[terms.size()]);
	}

    /** ***************************************************************
     */
	public static KB getKB(){

		try {
            KBmanager.getMgr().initializeOnce();
        } 
        catch (Exception e) {
            SigmaLogger.log(e.getMessage());
        }
        KB kb = KBmanager.getMgr().getKB(KBmanager.getMgr().getPref("sumokbname"));
        return kb;
	}

    /** ***************************************************************
     */
	public static String[] getSUMOtermsWithSigma() {

		File f = new File("sumoterms.txt");
		SortedSet<String> hs = null;
		if (!f.exists()) {
			KB kb = getKB();
			hs = kb.terms;
			return hs.toArray(new String[hs.size()]);
		}
		else {
			String[] res = getTermsFromFile();
			if (res == null) SigmaLogger.log("Something happened when loading sumo terms.");
			printAllSUMOtermsToFile(res);
			return res;
		}
		
	}

    /** ***************************************************************
     */
	public static boolean printAllSUMOtermsToFile(String[] terms) {

		File f = new File("sumoterms.txt");
		if (!f.exists()) f.setWritable(true);
		try (PrintWriter pw = new PrintWriter(f)) {
			for (String k : terms)
				pw.println(k.substring(0,k.lastIndexOf('\n')));
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		try {
			SigmaLogger.log(f.getCanonicalPath());
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

    /** ***************************************************************
     */
	public static String[] getTermsFromFile() {

		File f = new File("sumoterms.txt");
		if (!f.exists()) return null;
		ArrayList<String> res = new ArrayList<String>();
		try (Scanner in = new Scanner(f)) {
			while (in.hasNextLine())
				res.add(in.nextLine());
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return res.toArray(new String[res.size()]);
	}

    /** ***************************************************************
     */
	public static void main(String[] args){
		//SigmaUtil.printAllSUMOtermsToFile();
	}
}
