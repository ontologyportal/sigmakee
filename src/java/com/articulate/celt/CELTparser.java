
/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following articles in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
  
(the following article need be cited only if using CELT)
Pease, A., and Murray, W., (2003).  An English to Logic Translator for
Ontology-based Knowledge Representation Languages.  In Proceedings of
the 2003 IEEE International Conference on Natural Language Processing
and Knowledge Engineering, Beijing, China, pp 777-783.

*/

package com.articulate.celt;

import java.io.*;
import java.util.*;

import javax.swing.tree.*;

import com.articulate.sigma.*;

/**
 * A Java reimplementation of the CELT prolog system.
 */
public class CELTparser {

     public DefaultTreeModel grammarTree = null;
      /** An ArrayList of words in the sentence, minus punctuation.  This is
       * set in parse() and examined in match(). */
     private ArrayList words = null;

    /** ***************************************************************
     * This routine sets up the StreamTokenizer so that it parses English.
     * = < > are treated as word characters, as are normal alphanumerics.
     * ; is the line comment character and " is the quote character.
     */
    private void setupStreamTokenizer(StreamTokenizer st) {

        st.whitespaceChars(0,32);
        st.ordinaryChars(33,44);   // !"#$%&'()*+,
        st.wordChars(45,45);       // -
        st.ordinaryChars(46,46);   // .
        st.ordinaryChar(47);       // /
        st.wordChars(48,57);       // 0-9
        st.ordinaryChars(58,59);   // :;
        st.wordChars(60,62);       // <=>
        st.ordinaryChars(63,64);   // ?
        st.wordChars(64,64);       // @
        st.wordChars(65,90);       // A-Z
        st.ordinaryChars(91,94);   // [\]^
        st.wordChars(95,95);       // _
        st.ordinaryChar(96);       // `
        st.wordChars(97,122);      // a-z
        st.ordinaryChars(123,127); // {|}~
        // st.parseNumbers();
        st.quoteChar('"');
        st.commentChar(';');
        st.eolIsSignificant(true);
    }

    /** *************************************************************
     * Whether the word matches the leaf node at the end of the path.
     * Has the side effect of setting nInfo.selected to true and nInfo.SUMOterm
     * to the appropriate SUMO term if there is a match.
     */
    private boolean match(TreePath path, String word) {

        String rootWord = null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        GrammarNode nInfo = (GrammarNode) node.getUserObject();
        if (word.equalsIgnoreCase(nInfo.nodeName)) {
            System.out.println("INFO in CELTparser.match(): Found match: " + word + " with " + nInfo.nodeName);        
            return true;
        }
        if (nInfo.nodeName.equalsIgnoreCase("WordNetNoun")) {
            System.out.println("INFO in CELTparser.match(): checking noun " + word + " and " + word.toLowerCase());
            rootWord = WordNet.wn.nounRootForm(word,word.toLowerCase());
            System.out.println("INFO in CELTparser.match(): checking noun " + rootWord);
            if (rootWord != null && WordNet.wn.containsWord(rootWord,WordNet.NOUN)) {
                nInfo.selected = true;
                nInfo.rootWord = rootWord;
                nInfo.POS = WordNet.NOUN;
                //nInfo.SUMOterm = WordNet.wn.findSUMOWordSense(rootWord,words,WordNet.NOUN);
                System.out.println("INFO in CELTparser.match(): Found match: " + word + " with " + nInfo.nodeName + " and SUMO term " + nInfo.SUMOterm);
                return true;
            }
        }
        if (nInfo.nodeName.equalsIgnoreCase("WordNetVerb")) {
            rootWord = WordNet.wn.verbRootForm(word,word.toLowerCase());
            System.out.println("INFO in CELTparser.match(): checking verb " + rootWord);
            if (rootWord != null && WordNet.wn.containsWord(rootWord,WordNet.VERB)) {
                nInfo.selected = true;
                nInfo.rootWord = rootWord;
                nInfo.POS = WordNet.VERB;
                //nInfo.SUMOterm = WordNet.wn.findSUMOWordSense(rootWord,words,WordNet.VERB);
                System.out.println("INFO in CELTparser.match(): Found match: " + word + " with " + nInfo.nodeName + " and SUMO term " + nInfo.SUMOterm);
                return true;
            }
        }
        if (nInfo.nodeName.equalsIgnoreCase("WordNetAdjective")) {
            System.out.println("INFO in CELTparser.match(): checking adjective " + word);
            if (WordNet.wn.containsWord(word,WordNet.ADJECTIVE)) {
                nInfo.selected = true;
                nInfo.POS = WordNet.ADJECTIVE;
                //nInfo.SUMOterm = WordNet.wn.findSUMOWordSense(word,words,WordNet.ADJECTIVE);
                System.out.println("INFO in CELTparser.match(): Found match: " + word + " with " + nInfo.nodeName + " and SUMO term " + nInfo.SUMOterm);
                return true;
            }
        }
        if (nInfo.nodeName.equalsIgnoreCase("WordNetAdverb")) {
            System.out.println("INFO in CELTparser.match(): checking adverb " + word);
            if (WordNet.wn.containsWord(word,WordNet.ADVERB)) {
                nInfo.selected = true;
                nInfo.POS = WordNet.ADVERB;
                //nInfo.SUMOterm = WordNet.wn.findSUMOWordSense(word,words,WordNet.ADVERB);
                System.out.println("INFO in CELTparser.match(): Found match: " + word + " with " + nInfo.nodeName + " and SUMO term " + nInfo.SUMOterm);
                return true;
            }
        }
        return false;
    }

    /** *************************************************************
     * Whether the path terminates at a leaf node.
     */
    private boolean isLeaf(TreePath path) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return grammarTree.isLeaf(node);
    }

    /** *************************************************************
     * Mark all the nodes in the path as having been part of a selection.
     */
    private void markPathSelected(TreePath path) {

        DefaultMutableTreeNode node;
        GrammarNode nInfo;

        for (int i = 0; i < path.getPathCount(); i++) {   
            node = (DefaultMutableTreeNode) path.getPathComponent(i);
            nInfo = (GrammarNode) node.getUserObject();
            nInfo.selected = true;
            grammarTree.nodeChanged(node);
        }       
    }

    /** *************************************************************
     * Does a depth first search of the tree trying to find word.
     */
    private TreePath findMatch(TreePath path, String word) {
        
        DefaultMutableTreeNode node = null;
        GrammarNode nInfo = null;
        TreePath newPath = null;

        if (path == null) {
            //System.out.println("INFO in CELTparser.findMatch(): Path is null with word: " + word);
            TreePath rootPath = new TreePath(grammarTree.getRoot());
            System.out.println("INFO in CELTparser.findMatch(): Root path: " + rootPath.toString());
            newPath = findMatch(rootPath,word);
            if (newPath != null) {
                node = (DefaultMutableTreeNode) newPath.getLastPathComponent();
                nInfo = (GrammarNode) node.getUserObject();
            }
        }
        else {
            node = (DefaultMutableTreeNode) path.getLastPathComponent();
            nInfo = (GrammarNode) node.getUserObject();
            //System.out.println("INFO in CELTparser.findMatch(): Looking at node " + nInfo.nodeName);
            if (isLeaf(path)) {
                //System.out.println("INFO in CELTparser.findMatch(): Its a leaf " + nInfo.nodeName);
                if (match(path,word)) {
                    nInfo.matchedWord = word;
                    markPathSelected(path);
                    return path;
                }
                else {
                    newPath = findMatch(path.getParentPath(),word);
                }
            }
            else {
                if (nInfo.lastChildVisited >= node.getChildCount() - 1) {   // last element in the list = size - 1
                    if (node.getParent() == null)  // If at root and no more children to try, fail.
                        return null;
                    newPath = findMatch(path.getParentPath(),word);
                }
                else {
                    //System.out.println("INFO in CELTparser.findMatch(): Trying a child of node " + nInfo.nodeName);
                    if (!nInfo.selected || !nInfo.orNode) {
                        //System.out.println("INFO in CELTparser.findMatch(): Not selected, or its not an or node");
                        nInfo.lastChildVisited++;
                        //System.out.println("INFO in CELTparser.findMatch(): Incremented " + nInfo.nodeName);
                        //System.out.print("Last child visited: ");
                        //System.out.println(nInfo.lastChildVisited);
                        DefaultMutableTreeNode child = (DefaultMutableTreeNode) grammarTree.getChild(node,nInfo.lastChildVisited);
                        newPath = findMatch(path.pathByAddingChild(child),word);
                    }
                    else {
                        newPath = findMatch(path.getParentPath(),word);
                    }
                }
            }
        }

        //System.out.println("INFO in CELTparser.findMatch(): Unwinding");
        //if (newPath != null) 
            //System.out.println("INFO in CELTparser.findMatch(): path: " + newPath.toString());
        return newPath;
    }

    /** *************************************************************
     * Decrement the lastChildVisited counters of all nodes in the path
     * other than the leaf and its parent.  This is done so that when
     * searching for a match in the tree for the next word, that the
     * depth first search algoritm in findMatch() will be able to examine
     * the siblings of the found word.
     */
    private void resetChildCounters(TreePath path) {
        
        DefaultMutableTreeNode node;
        GrammarNode nInfo;

        for (int i = 0; i < path.getPathCount() - 2; i++) {   // Last element is size - 1, and then we skip 
                                                              // the leaf and its parent.  Root is element 0.
            node = (DefaultMutableTreeNode) path.getPathComponent(i);
            nInfo = (GrammarNode) node.getUserObject();
            //System.out.println("INFO in CELTparser.resetChildCounters(): Resetting " + nInfo.nodeName);
            if (nInfo.lastChildVisited > -1) { 
                if (nInfo.repeat == GrammarNode.ONE_TO_MANY || nInfo.repeat == GrammarNode.ZERO_TO_MANY) 
                    nInfo.lastChildVisited = -1;
                else
                    nInfo.lastChildVisited--;
            }
        }       
    }

    /** *************************************************************
     */
    private boolean punctuation(String input) {

        String punct = "?!,.:;\"'";
        if (punct.indexOf(input) == -1) 
            return false;
        else
            return true;
    }

    /** *************************************************************
     * Read words from a String one at a time and attempt to find a match in the parse tree.
     * The method's primary result is in marking nodes as "selected".  This is done in 
     * findMatch(), and uses the NodeInfo.selected boolean member variable. The routine
     * may also return a String error message if no parse is found.
     */
    public String parse(String input) {

        StringReader r = new StringReader(input);
        StreamTokenizer st = new StreamTokenizer(r);
        ArrayList paths = new ArrayList();           // An ArrayList of TreePaths, one path for each word.
        CELTinterpreter celt = new CELTinterpreter(grammarTree);
        setupStreamTokenizer(st);
        TreePath path = null;
        words = new ArrayList();
        try {                                               // Add each word in the sentence to an ArrayList.
            do {
                int lastVal = st.ttype;
                st.nextToken();
                if (st.ttype == st.TT_NUMBER) {
                    String number = st.sval;
                }
                if (st.ttype == st.TT_WORD) { 
                    String word = st.sval;
                    if (punctuation(word)) 
                        continue;
                    System.out.println("INFO in CELTparser.parse(): Matching word " + word);
                    words.add(word);
                }
            }
            while (st.ttype != st.TT_EOF);
        }
        catch (IOException ioe) {
            System.out.println("Error in CELTparser.parse(): IOException: " + ioe.getMessage());
        } 
        for (int i = 0; i < words.size(); i++) {            // Parse the sentence.
            String word = (String) words.get(i);
            path = findMatch(null,word);
            System.out.println("INFO in CELTparser.parse(): Returning from matching word " + word);
            // System.out.println("INFO in CELTparser.parse(): path " + path);
            if (path == null) {
                System.out.println("INFO in CELTparser.parse(): " + word + " not found.");
                return word + " not found.";
            }
            else {
                paths.add(path);
                resetChildCounters(path);
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                GrammarNode nInfo = (GrammarNode) node.getUserObject();
                if (nInfo.rootWord != null) 
                    word = nInfo.rootWord;
            }
        }
        for (int i = 0; i < paths.size(); i++) {            // Disambiguate word senses.
            path = (TreePath) paths.get(i);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            GrammarNode nInfo = (GrammarNode) node.getUserObject();
            if (nInfo.POS != -1) {
                String rootWord = null;
                if (nInfo.rootWord != null)
                    rootWord = nInfo.rootWord;
                else
                    rootWord = nInfo.matchedWord;
                nInfo.SUMOterm = WordNet.wn.findSUMOWordSense(rootWord,words,nInfo.POS);
                System.out.println("INFO in CELTparser.parse(): SUMO term " + nInfo.SUMOterm + " for word " + rootWord);
                grammarTree.nodeChanged(node);
            }
        }
        Formula f = new Formula();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) grammarTree.getRoot();   
          // This reflects a problem that really needs to be fixed in GrammarTree.load().  For some reason,
          // the real grammar root is being made a child of a root node called "Empty".  We get the only
          // child of the root here as a hack around this problem.
        f.read(celt.interpretNode((DefaultMutableTreeNode) root.getChildAt(0),null));
        System.out.println("INFO in CELTparser.parse(): Result: \n" + f.toString());
        return null;
    }

    /** *************************************************************
     */
    public static void main(String args[]) {

        CELTparser cp = new CELTparser();
        System.out.println(cp.parse("John kicks the cart."));
    }
}


