/*
 * @(#)SimpleDOMParser.java
 * From DevX
 * http://www.devx.com/xml/Article/10114
 * Further modified for Articulate Software by Adam Pease 12/2005
 *   modified 2017 to allow contents in a parent element after the close of a child element
 *
 * Note that consecutive whitespace characters in a tag body are removed
 */
package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

/** *****************************************************************   
 * <code>SimpleDOMParser</code> is a highly-simplified XML DOM
 * parser.
 */
public class SimpleDOMParser {

    private static final int[] cdata_start = {'<', '!', '[', 'C', 'D', 'A', 'T', 'A', '['};
    private static final int[] cdata_end = {']', ']', '>'};

    private Reader reader;
    private Stack elements;
    private SimpleElement currentElement;
    private boolean skipProlog = true;
    public void setSkipProlog(boolean b) { skipProlog = b; }

    /** *****************************************************************
    */
    public SimpleDOMParser() {

        elements = new Stack();
        currentElement = null;
    }

    /** *****************************************************************
     * Read the full path of an XML file and returns the SimpleElement 
     * object that corresponds to its parsed format.
    */
    public static SimpleElement readFile (String filename) {

        SimpleElement result = null;
        File f = new File(filename);
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(f));
            SimpleDOMParser sdp = new SimpleDOMParser();
            result = sdp.parse(br);
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
            System.out.println("Error in SimpleDOMParser.readFile(): IO exception parsing file " + 
                               filename + "\n" + e.getMessage());
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                }
                catch (Exception ex) {
                    System.out.println("Error in SimpleDOMParser.readFile(): IO exception parsing file " + 
                                       filename + "\n" + ex.getMessage());
                }
            }
        }
        return result;
    }

    /** *****************************************************************
    */
    public SimpleElement parse(Reader reader) throws IOException {

        this.reader = reader;
        if (skipProlog) skipPrologs();          // skip xml declaration or DocTypes
        while (true) {
            int index;
            String tagName;
            
            String currentTag = null;
            while (currentTag == null || currentTag.startsWith("<!--")) {        // ignore comments
                currentTag = readTag();                                  // remove the prepend or trailing white spaces
                //System.out.println("SimpleDOMParse.parse() currentTag: '" + currentTag + "'");
                //System.out.println("SimpleDOMParse.parse() currentElement: '" + currentElement + "'");
                //if (currentElement != null)
                //    System.out.println("SimpleDOMParse.parse() currentelement text: " + currentElement.getText());
                if (currentTag.length() > 1 && currentTag.contains("<"))
                    currentTag = currentTag.trim();
                if (currentTag.charAt(0) != '<' && currentElement != null) { // don't allow consecutive whitespace
                     if (currentElement.getText().length() == 0 || !(Character.isWhitespace(currentTag.charAt(0)) &&
                             Character.isWhitespace(currentElement.getText().charAt(currentElement.getText().length()-1)))) {
                        currentElement.setText(currentElement.getText() + currentTag.charAt(0));
                    }
                    currentTag = null;
                }
            }
            //System.out.println("SimpleDOMParse.parse() currentTag 2: " + currentTag);
            if (currentTag.startsWith("</")) {                                  // close tag
                tagName = currentTag.substring(2, currentTag.length()-1).trim();
                if (currentElement == null)                                     // no open tag
                    throw new IOException("Got close tag '" + tagName +
                                    "' without open tag.");                
                if (!tagName.equals(currentElement.getTagName()))               // close tag does not match with open tag
                    throw new IOException("Expected close tag for '" +
                                    currentElement.getTagName() + "' but got '" +
                                    tagName + "' while parsing '" + currentTag + "'.");
                if (elements.empty()) 
                    return currentElement;                                      // document processing is over
                else                                                            // pop up the previous open tag
                    currentElement = (SimpleElement) elements.pop();
            }
            else {                                                              // open tag or tag with both open and close tags                        
                index = currentTag.indexOf(" ");
                if (index < 0) {                                                // tag with no attributes                    
                    if (currentTag.endsWith("/>")) {                            // close tag as well                        
                        tagName = currentTag.substring(1, currentTag.length()-2).trim();
                        currentTag = "/>";
                    }
                    else {                                                    // open tag
                        tagName = currentTag.substring(1, currentTag.length()-1).trim();
                        currentTag = "";
                    }
                } 
                else {                                                          // tag with attributes                        
                    tagName = currentTag.substring(1, index).trim();
                    currentTag = currentTag.substring(index+1).trim();
                }              
                SimpleElement element = new SimpleElement(tagName.trim());             // create new element
                
                boolean isTagClosed = false;                                    // parse the attributes
                while (currentTag.length() > 0) {                               // remove the prepend or trailing white spaces                    
                    currentTag = currentTag.trim();
                    //System.out.println(currentTag);
                    if (currentTag.equals("/>")) {                              // close tag                                
                        isTagClosed = true;
                        break;
                    } 
                    else 
                        if (currentTag.equals(">"))                           // open tag                                
                            break;                        
                    index = currentTag.indexOf("=");
                    if (index < 0) 
                        throw new IOException("Invalid attribute for tag '" +
                                            tagName + "'.  With current tag=" + currentTag);                        
                    
                    String attributeName = currentTag.substring(0, index).trim();    // get attribute name
                    currentTag = currentTag.substring(index+1).trim();
                    
                    String attributeValue;                                    // get attribute value
                    boolean isQuoted = true;
                    if (currentTag.startsWith("\"")) {
                        index = currentTag.indexOf('"', 1);
                    } 
                    else 
                        if (currentTag.startsWith("'")) {
                            index = currentTag.indexOf('\'', 1);
                        } 
                        else {
                            isQuoted = false;
                            index = currentTag.indexOf(' ');
                            if (index < 0) {
                                index = currentTag.indexOf('>');
                                if (index < 0) 
                                    index = currentTag.indexOf('/');                                
                            }
                        }
                    if (index < 0)
                            throw new IOException("Invalid attribute for tag '" +
                                            tagName + "'.  With current tag=" + currentTag);                        
                    if (isQuoted)
                        attributeValue = currentTag.substring(1, index).trim();
                    else
                        attributeValue = currentTag.substring(0, index).trim();

                    element.setAttribute(attributeName, attributeValue);      // add attribute to the new element
                    currentTag = currentTag.substring(index+1).trim();
                }
                
                if (!isTagClosed)                                  // read the text between the open and close tag
                    element.setText(readText());                                                         
                if (currentElement != null)                        // add new element as a child element of the current element
                    currentElement.addChildElement(element);
                if (!isTagClosed) {
                    if (currentElement != null) 
                        elements.push(currentElement);                            
                    currentElement = element;
                } 
                else 
                    if (currentElement == null)                    // only has one tag in the document                            
                        return element;                    
            }
        }
    }

    /** *****************************************************************
    */
    private int peek() throws IOException {

        reader.mark(1);
        int result = reader.read();
        reader.reset();

        return result;
    }

    /** *****************************************************************
    */
    private void peek(int[] buffer) throws IOException {

        reader.mark(buffer.length);
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = reader.read();
        }
        reader.reset();
    }

    /** *****************************************************************
    */
    private void skipWhitespace() throws IOException {

        while (Character.isWhitespace((char) peek())) {
            reader.read();
        }
    }

    /** *****************************************************************
    */
    private void skipProlog() throws IOException {
        
        reader.skip(2);                        // skip "<?" or "<!"
        while (true) {
            int next = peek();
            if (next == '>') {
                reader.read();
                break;
            }
            else if (next == '<')            // nesting prolog
                skipProlog();
            else
                reader.read();
        }
    }

    /** *****************************************************************
    */
    private void skipPrologs() throws IOException {

        while (true) {
            skipWhitespace();
            int[] next = new int[2];
            peek(next);
            if (next[0] != '<') 
                throw new IOException("SimpleDOMParser.skipPrologs(): Expected '<' but got '" + (char)next[0] + "'.");
            if ((next[1] == '?') || (next[1] == '!')) 
                skipProlog();
            else
                break;            
        }
    }

    /** *****************************************************************
    */
    private String readTag() throws IOException {

        //skipWhitespace();
        StringBuffer sb = new StringBuffer();
        int next = peek();
        if (next != '<') {
            //throw new IOException("SimpleDOMParser.readTag(): Expected < but got " + (char) next);
            next = (char) reader.read();
            return Character.toString((char) next);
        }
        sb.append((char)reader.read());
        while (peek() != '>') {
            char c = (char)reader.read();
            if (Character.isWhitespace(c)) 
                c = ' ';            
            sb.append(c);        
        }
        sb.append((char)reader.read());

        //System.out.println("Tag: " + sb.toString());
        return sb.toString();
    }

    /** ***************************************************************** 
     * Convert ampersand character elements to reserved characters.
     */
    public static String convertToReservedCharacters(String input) {

        if (StringUtil.emptyString(input))
            return "";
        input = input.replaceAll("&gt;",">");
        input = input.replaceAll("&lt;","<");
        return input;
    }

    /** ***************************************************************** 
     * Convert reserved characters to ampersand character elements.
     */
    public static String convertFromReservedCharacters(String input) {

        if (StringUtil.emptyString(input)) 
            return "";
        input = input.replaceAll(">","&gt;");
        input = input.replaceAll("<","&lt;");
        return input;
    }

    /** *****************************************************************
    */
    private String readText() throws IOException {

        StringBuffer sb = new StringBuffer();
        int[] next = new int[cdata_start.length];
        peek(next);
        if (compareIntArrays(next, cdata_start) == true) {      // CDATA            
            reader.skip(next.length);
            int[] buffer = new int[cdata_end.length];
            while (true) {
                peek(buffer);
                if (compareIntArrays(buffer, cdata_end) == true) {
                    reader.skip(buffer.length);
                    break;
                } 
                else 
                    sb.append((char)reader.read());               
            }
        }
        else {
            while (peek() != '<') 
                sb.append((char)reader.read());                
        }
        return sb.toString();
    }

    /** *****************************************************************
    */
    private boolean compareIntArrays(int[] a1, int[] a2) {

        if (a1.length != a2.length)
            return false;
        for (int i=0; i<a1.length; i++) {
            if (a1[i] != a2[i]) 
                return false;               
        }
        return true;
    }

    /** *****************************************************************
    */
    public static void main(String[] args) {

        SimpleDOMParser sdp = new SimpleDOMParser();
        String fname = "";
        try {
            //String _projectFileName = "projects-energy.xml";
            //fname = KBmanager.getMgr().getPref("baseDir") + File.separator + _projectFileName;
            fname = System.getProperty("user.home") + "/corpora/timebank_1_2/data/extra/wsj_0991.tml";
            System.out.println(fname);
            File f = new File(fname);
            if (!f.exists()) 
                return;
            BufferedReader br = new BufferedReader(new FileReader(fname));

            SimpleElement se = sdp.parse(br);
            System.out.println(se.toString());            
        } 
        catch (java.io.IOException e) {
            System.out.println("Error in main(): IO exception parsing file " + fname);
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.out.println("elements: " + sdp.elements);
            System.out.println("current element: " + sdp.currentElement);
        }
        /* String test = "<P>hi<P>";
        String converted = SimpleDOMParser.convertFromReservedCharacters(test);
        System.out.println(converted);
        System.out.println(SimpleDOMParser.convertToReservedCharacters(converted)); */
    }
}
