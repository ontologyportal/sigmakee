
package com.articulate.celt;

import java.io.*;
import java.util.*;

import com.articulate.sigma.*;

/** *************************************************************
 * This code is copyright Articulate Software (c) 2003.  
 * This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
 * Users of this code also consent, by use of this code, to credit Articulate Software
 * in any writings, briefings, publications, presentations, or 
 * other representations of any software which incorporates, builds on, or uses this 
 * code.  Please cite the following article in any publication with references:
 * 
 * Pease, A., (2003). The Sigma Ontology Development Environment, 
 * in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 * August 9, Acapulco, Mexico.
 * 
 * Holds the information about each node in the grammar.
 */
public class GrammarNode {

     /** Name of the node.  "WordNet" as the first characters of the name signifies it's WordNet node. */
    public String nodeName;
     /** The KIF template that will be filled in based on which child nodes match. Note the information
      about the syntax of meta-information in this variable, which is described in @see CELTinterpreter. */
    public String KIFtemplate;
     /** The template with its variables filled in. */
    public String KIFexpression;
     /** The SUMO term equivalent to this word.  Only applicable if this is a single word or concept. */
    public String SUMOterm;
     /** The filename of the lexicon that comprises this node. */
    public String filename;
     /** Whether this is an or node, meaning that only one child may be selected in a given parse, or,
      *  if false, this signifies an and node that may have any or all of the children selected. */
    public boolean orNode;
     /** Whether this node is repeated zero or more, one or more times, or is optional. 
      * Static variables are provided below to make setting this value simpler. */
    public int repeat;
     /** Whether this node should be visible in the GrammarTree GUI. */
    public boolean visible;
     /** Whether this node has been selected during parsing. */
    public boolean selected;
     /** The index of the last child of this node visited during parsing.  -1 signifies that
      * either there are no children, or node has been visited. */
    public int lastChildVisited;
     /** A word matched to this node during parsing. */
    public String matchedWord;
     /** The root form, if applicable, of the word matched to this node during parsing. */
    public String rootWord;
     /** The tense of a verb.  Only meaningful if this is a verb. */
    public int tense;
     /** The number of a noun, singular or plural.  Only meaningful if this is a noun. */
    public int number;
     /** The part of speech. */
    public int POS = -1;

    /** Number of times a grammar node may be repeated */
    public static final int NO_REPEAT = 0;
    public static final int ZERO_TO_MANY = 1;
    public static final int ONE_TO_MANY = 2;
    public static final int OPTIONAL = 3;

    public static final int NOT_A_VERB = 0;
    public static final int PAST_TENSE = 1;               // he went
    public static final int PRESENT_TENSE = 2;            // he goes
    public static final int FUTURE_TENSE = 3;             // he will go
    public static final int PRESENT_IMPERFECT_TENSE = 4;  // he is going
    public static final int PAST_IMPERFECT_TENSE = 5;     // he was going

    public static final int NOT_A_NOUN = 0;
    public static final int SINGULAR = 1;                 // the dog
    public static final int PLURAL = 2;                   // the dogs

    /** *************************************************************
     */
    public GrammarNode(String name, String template, boolean orNodeInput, String fname, int rep) {

        nodeName = name;
        KIFtemplate = template;
        orNode = orNodeInput;
        filename = fname;
        repeat = rep;
        visible = false;
        selected = false;
        lastChildVisited = -1;
    }

    /** *************************************************************
     */
    public GrammarNode(String name, String template, boolean orNodeInput, String fname, int rep, boolean vis) {

        visible = vis;
        nodeName = name;
        KIFtemplate = template;
        orNode = orNodeInput;
        filename = fname;
        repeat = rep;
        selected = false;
        lastChildVisited = -1;
    }

    /** *************************************************************
     */
    public GrammarNode(GrammarNode node) {

        visible = node.visible;
        nodeName = new String(node.nodeName);
        if (node.KIFtemplate != null) 
            KIFtemplate = new String(node.KIFtemplate);
        else 
            KIFtemplate = null;
        orNode = node.orNode;
        if (node.filename != null) 
            filename = new String(node.filename);
        else
            filename = null;
        repeat = node.repeat;
        selected = false;
        lastChildVisited = -1;
    }

     /** ************************************************************* 
      * Write the information about a node.  Count on save() or
      * saveChild() in GrammarTree.save() to write the closing tag.
      */
    public void save(PrintWriter pw, boolean visibleNode, int childCount) {

        pw.print("<node ");
        if (nodeName != null && nodeName.length() > 0) 
            pw.print("name=\"" + nodeName + "\" ");
        if (KIFtemplate != null && KIFtemplate.length() > 0) 
            pw.print("kif=\"" + KIFtemplate + "\" ");
        if (filename != null && filename.length() > 0) 
            pw.print("filename=\"" + filename + "\" ");
        pw.print("children='");
        pw.print(childCount);
        pw.print("' ");
        pw.print("repeat='");
        pw.print(repeat);
        pw.print("' ");
        if (visibleNode) 
            pw.print("visible=\"true\" ");
        if (orNode) 
            pw.println("orNode=\"true\">");
        else
            pw.println("orNode=\"false\">");
    }

    /** *************************************************************
     * Reset the node after parsing.  Clears any values that aren't part
     * of the grammar itself.
     */
    public void reset() {

        lastChildVisited = -1;
        selected = false;
        KIFexpression = null;
        SUMOterm = null;
    }

    /** *************************************************************
     */
    public String toString() {

        StringBuffer result = new StringBuffer();
         
        if (matchedWord != null && matchedWord.length() > 0) 
            result = result.append(nodeName + "-\"" + matchedWord + "\"");
        else
            result = result.append(nodeName);
        if (KIFexpression != null && KIFexpression.length() > 0) 
            result = result.append(": " + KIFtemplate + ": " + KIFexpression);
        else {
            if (KIFtemplate != null && KIFtemplate.length() > 0) 
                result = result.append(": " + KIFtemplate);
        }
        if (SUMOterm != null && SUMOterm.length() > 0) 
            result = result.append("- " + SUMOterm);
        if (selected) 
            result = result.append("-*");
        return result.toString();
    }
}
