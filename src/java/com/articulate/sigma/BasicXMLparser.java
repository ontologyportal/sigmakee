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

package com.articulate.sigma;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;

/** ***************************************************************
 * Parses simple XML into a hierarchy of BasicXMLelement (s).  Used
 * instead of SAX because that class is so complex.  The right thing
 * to do is probably to subclass SAX or create some simpler utility
 * class that makes using it easier.
 * This class assumes that each XML tag is on its own line and that
 * every attribute value is enclosed in single or double quotes.
 */
public class BasicXMLparser {

     /** An ArrayList of BasicXMLelement(s). */
    public ArrayList<BasicXMLelement> elements = new ArrayList<BasicXMLelement>();

    /** ***************************************************************
     * Constructor that parses an XML-formatted string, with one tag per
     * line, into an ArrayList of BasicXMLelement (s).
     */
    public BasicXMLparser(String xml) {

        StringReader sr = new StringReader(xml);
        LineNumberReader lnr = new LineNumberReader(sr);
        try {
            BasicXMLelement el = new BasicXMLelement();
            parse(lnr,el);
            elements = el.subelements;
        }
        catch (ParseException pe) {
            System.out.print("Error in BasicXMLparser(): " + pe.getMessage() + " At line:");
            System.out.println(pe.getErrorOffset());
            elements = null;
        }
        catch (IOException ioe) {
            System.out.println("Error in BasicXMLparser(): " + ioe.getMessage());
            elements = null;
        }
    }

    /** ***************************************************************
     * Parse an XML formatted string into a hierarchy of BasicXMLelement (s).
     * Assume that each line has only one tag.
     */
    private void parse(LineNumberReader lnr, BasicXMLelement element) throws ParseException, IOException {
        
        String newTag = null;
        boolean inQuote = false;
        while (lnr.ready()) {
            String line = lnr.readLine();
            inQuote = false;
            if (line == null) {
                System.out.println("INFO in BasicXMLparser.parse: Exiting with line = null.");
                return;
            }
            line = line.trim();
            int tagStart = line.indexOf('<');
            if (tagStart != 0) {                        // It's not a tag
                element.contents = element.contents + (new String(line.trim()));
                System.out.println("INFO in BasicXMLparser.parse: Adding contents: " + line);
                continue;
            }
            if (line.charAt(tagStart+1) == '/') {      // Found a closing tag
                String endTagString = line.substring(tagStart+2,line.indexOf('>'));
                if (element.tagname == null)
                    throw new ParseException("Error in BasicXMLparser.parse(): Closing tag " + line + " without open tag, at line: ", lnr.getLineNumber());                
                if (endTagString.equalsIgnoreCase(element.tagname)) 
                    return;                
                else
                    throw new ParseException("Error in BasicXMLparser.parse(): Close tag " + endTagString + " doesn't match open tag " + element.tagname, lnr.getLineNumber());
            }
            else {                                    // An opening or combined open/close tag - like <foo/>
                int tagEnd = tagStart + 1;
                while (tagEnd < line.length() && Character.isJavaIdentifierPart(line.charAt(tagEnd))) 
                    tagEnd++;
                newTag = line.substring(line.indexOf('<') + 1,tagEnd);
                BasicXMLelement newElement = new BasicXMLelement();
                newElement.tagname = newTag;
                if (line.charAt(tagEnd) == ' ') {     // The tag has attributes
                    do {
                        tagEnd++;
                        int name = tagEnd;
                        while (tagEnd < line.length() && Character.isJavaIdentifierPart(line.charAt(tagEnd))) 
                            tagEnd++;
                        String nameString = line.substring(name,tagEnd);
                        if (line.charAt(tagEnd) != '=') 
                            throw new ParseException("Error in BasicXMLparser.parse(): Name without value: " + nameString, lnr.getLineNumber());
                        tagEnd++;
                        int value = tagEnd;
                        char valueEnd = ' ';
                        if (line.charAt(value) == '\'') {
                            value++;
                            tagEnd++;
                            valueEnd = '\'';
                            inQuote = !inQuote;
                        }
                        if (line.charAt(value) == '\"') {
                            value++;
                            tagEnd++;
                            valueEnd = '\"';
                            inQuote = !inQuote;
                        }
                        while (tagEnd < line.length() && line.charAt(tagEnd) != valueEnd && 
                               (line.charAt(tagEnd) != '>' || inQuote)) {
                            tagEnd++;
                            if (line.charAt(tagEnd) == '\"' || line.charAt(tagEnd) == '\'') 
                                inQuote = !inQuote;
                        }
                        String valueString = line.substring(value,tagEnd);

                        if (line.charAt(tagEnd) == valueEnd) 
                            tagEnd++;
                        newElement.attributes.put(nameString,valueString);
                    } while (line.charAt(tagEnd) == ' ');

                }
                if (line.charAt(tagEnd) == '>') {
                    parse(lnr,newElement);
                    element.subelements.add(newElement);
                }
                else if (line.charAt(tagEnd) == '/' && 
                         (line.charAt(tagEnd + 1) == '>' && !inQuote)) {
                    newTag = "";
                    element.subelements.add(newElement);
                }
            }
        }
        return;
    }
   
    /** ***************************************************************
     * Convert the XML hierarchy to a String.
     */
    public String toString() {

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < elements.size(); i++) {
            BasicXMLelement element = elements.get(i);
            result = result.append(element.toString());
        }
        return result.toString();
    }

    /** ***************************************************************
     * Test method.
     */
    public static void main(String[] args) {

        //String xml = "<queryResponse>\n<answer result='yes' number='1'>\n<bindingSet type='definite'>\n<binding>\n<var name='?X' value='<=>'/>\n</binding>\n</bindingSet>\n<proof>\n<proofStep>\n<premises>\n</premises>\n<conclusion>\n<formula number='1531'>\n(<=> (holds subclass ?X108 ?X14) (and (holds instance ?X108 Class) (holds instance ?X14 Class) (forall ( ?X15) (=> (holds instance ?X15 ?X108) (holds instance ?X15 ?X14)))))\n</formula>\n</conclusion>\n</proofStep>\n</proof>\n</answer>\n</queryResponse>\n";
        // String xml = "<queryResponse>\n<answer result='yes' number='1'>\n</answer>\n</queryResponse>\n";
        String xml = "<preference key='showcached' value='yes'/>\n" +
            "<preference key='eprover' value='/home/user/Programs/E/bin/eprover'/>\n" +
            "<preference key='testOutputDir' value='/home/user/Sigma/tests'/>\n"+
            "<preference key='prolog' value='/home/user'/>\n<preference key='sumokbname' value='sumo2'/>\n"+
            "<preference key='kbDir' value='/home/user/Sigma/KBs'/>\n"+
            "<preference key='celtdir' value='/home/user/2004-01-05-CELTv2.65-3b'/>\n"+
            "<preference key='hostname' value='localhost'/>\n"+
            "<preference key='cache' value='no'/>\n"+
            "<preference key='inferenceTestDir' value='/home/user/tests'/>\n"+
            "<preference key='PLDir' value='/home/user/Programs/pl-5.2.10/bin'/>\n"+
            "<preference key='loadCELT' value='yes'/>\n"+
            "<kb name='sumo2'>\n"+
              "<constituent filename='/home/user/Sigma/KBs/english_format.kif'/>\n"+
              "<constituent filename='/home/user/Sigma/KBs/Mid-level-ontology.txt'/>\n"+
              "<constituent filename='/home/user/Sigma/KBs/Merge.kif'/>\n"+
            "</kb>\n";

        BasicXMLparser bp = new BasicXMLparser(xml);
        System.out.print("Parse completed.  Number of elements: ");
        System.out.println(bp.elements.size());
        System.out.println(bp.toString());

        /* 
    <queryResponse>
      <answer result='yes' number='1'>
        <bindingSet type='definite'>
          <binding>
            <var name='?X' value='AbsoluteValueFn'/>
          </binding>
        </bindingSet>
        <proof>
          <proofStep>
            <premises>
            </premises>
            <conclusion>
              <formula number='1531'>
                (holds instance AbsoluteValueFn TotalValuedRelation)
              </formula>
            </conclusion>
          </proofStep>
        </proof>
      </answer>
    </queryResponse>
          */
    }

}
