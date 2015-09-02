package com.articulate.sigma.nlp.corpora;

import com.articulate.sigma.SimpleDOMParser;
import com.articulate.sigma.SimpleElement;

import java.util.ArrayList;

/**
 *  This code is copyright IPsoft 2015.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 Users of this code also consent, by use of this code, to credit Articulate Software
 and Teknowledge in any writings, briefings, publications, presentations, or
 other representations of any software which incorporates, builds on, or uses this
 code.  Please cite the following article in any publication with references:

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico.

 * Created by apease on 9/1/15.
 */
public class Coinco {

    public class Document {
        ArrayList<Sentence> sentences = new ArrayList<>();
    }

    public ArrayList<Document> documents = new ArrayList<>();

    public class Sentence {

        public String MASCfile = "";
        public String MASCsentID = "";
        public String preContext = "";
        public String targetSentence = "";
        public String postContext = "";
        public ArrayList<Token> tokens = new ArrayList<>();
    }

    public class Token {

        public String id = "";
        public String wordform = "";
        public String lemma = "";
        public String posMASC = "";
        public String posTT = "";
        public String problematic = "";
        public ArrayList<Substitution> substitutions = new ArrayList<>();
    }

    public class Substitution {

        public String lemma = "";
        public String pos = "";
        public String freq = "";
    }

    /** ***************************************************************
     */
    public void extract(SimpleElement bxp) {

        if (bxp.getChildElements() == null || bxp.getChildElements().size() < 1) {
            System.out.println("Error in Coinco.extract(): no top level element in " + bxp);
            return;
        }
        Document d = new Document();
        documents.add(d);
        for (SimpleElement sentXML : bxp.getChildElements()) {
            if (!sentXML.getTagName().equals("sent")) {
                System.out.println("Error in Coinco.extract(): bad tag where 'sent' expected " + sentXML);
                return;
            }
            Sentence s = new Sentence();
            d.sentences.add(s);
            if (sentXML.getAttributeNames().contains("MASCfile"))
                s.MASCfile = sentXML.getAttribute("MASCfile");
            if (sentXML.getAttributeNames().contains("MASCsentID"))
                s.MASCsentID = sentXML.getAttribute("MASCsentID");
            for (SimpleElement subXML : sentXML.getChildElements()) {
                if (subXML.getTagName().equals("precontext"))
                    s.preContext = subXML.getText();
                if (subXML.getTagName().equals("postcontext"))
                    s.postContext = subXML.getText();
                if (subXML.getTagName().equals("tokens")) {
                    for (SimpleElement tokXML : subXML.getChildElements()) {
                        Token t = new Token();
                        s.tokens.add(t);
                        if (tokXML.getAttributeNames().contains("id"))
                            t.id = tokXML.getAttribute("id");
                        if (tokXML.getAttributeNames().contains("wordform"))
                            t.wordform = tokXML.getAttribute("wordform");
                        if (tokXML.getAttributeNames().contains("lemma"))
                            t.lemma = tokXML.getAttribute("lemma");
                        if (tokXML.getAttributeNames().contains("posMASC"))
                            t.posMASC = tokXML.getAttribute("posMASC");
                        if (tokXML.getAttributeNames().contains("posTT"))
                            t.posTT = tokXML.getAttribute("posTT");
                        if (tokXML.getAttributeNames().contains("problematic"))
                            t.problematic = tokXML.getAttribute("problematic");
                        if (tokXML.getChildElements() != null && tokXML.getChildElements().size() > 0) {
                            SimpleElement substs = tokXML.getChildElements().get(0);
                            if (!substs.getTagName().equals("substitutions")) {
                                System.out.println("Error in Coinco.extract(): bad tag where 'substitutions' expected " + substs);
                                return;
                            }
                            if (substs.getChildElements() != null && substs.getChildElements().size() > 0) {
                                for (SimpleElement subst : substs.getChildElements()) {
                                    Substitution sub = new Substitution();
                                    t.substitutions.add(sub);
                                    if (!subst.getTagName().equals("subst")) {
                                        System.out.println("Error in Coinco.extract(): bad tag where 'subst' expected " + subst);
                                        return;
                                    }
                                    if (subst.getAttributeNames().contains("lemma"))
                                        sub.lemma = subst.getAttribute("lemma");
                                    if (subst.getAttributeNames().contains("pos"))
                                        sub.pos = subst.getAttribute("pos");
                                    if (subst.getAttributeNames().contains("freq"))
                                        sub.freq = subst.getAttribute("freq");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** ***************************************************************
     */
    public void parse(String filename) {

        try {
            SimpleDOMParser sdp = new SimpleDOMParser();
            SimpleElement se = sdp.readFile(filename);
            extract(se);
        }
        catch (Exception e) {
            System.out.println("Error in BasicXMLparser(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        Coinco c = new Coinco();
        c.parse("/home/apease/IPsoft/corpora/coinco/coinco.xml");
    }
}
