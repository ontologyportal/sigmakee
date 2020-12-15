/** *****************************************************************
 * @(#)SimpleDOMParser.java
 * From DevX
 * http://www.devx.com/xml/Article/10114
 * Further modified for Articulate Software by Adam Pease 12/2005
 */

package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**  *****************************************************************
 * <code>SimpleElement</code> is the only node type for
 * simplified DOM model.  Note that all CDATA values are stored with
 * reserved any characters '>' '<' converted to &gt; and &lt;
 * respectively.
 */
public class SimpleElement {

    private String tagName;
    private String text;
    private HashMap<String,String> attributes;
    private ArrayList<SimpleElement> childElements;

    public SimpleElement(String tagName) {
        this.tagName = tagName;
        attributes = new HashMap<String,String>();
        childElements = new ArrayList<SimpleElement>();
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getText() {

        if (text != null && text != "")
            return SimpleDOMParser.convertToReservedCharacters(text);
        else
            return text;
    }

    public void setText(String text) {

        if (!StringUtil.emptyString(text))
            this.text = SimpleDOMParser.convertFromReservedCharacters(text);
        else
            this.text = text;
    }

    public String getAttribute(String name) {

        String attribute = (String) attributes.get(name);
        if (attribute != null && attribute != "")
            return SimpleDOMParser.convertToReservedCharacters(attribute);
        else
            return attribute;
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public void setAttribute(String name, String value) {

        if (!StringUtil.emptyString(value))
            value = SimpleDOMParser.convertFromReservedCharacters(value);
        attributes.put(name, value);
    }

    public void addChildElement(SimpleElement element) {
        childElements.add(element);
    }

    public ArrayList<SimpleElement> getChildElements() {
        return childElements;
    }

    /** *****************************************************************
     * @return the first child with the given tag name, null if none
     */
     public SimpleElement getChildByFirstTag(String tag) {

         if (childElements == null || childElements.size() < 1)
             return null;
         for (int i = 0; i < childElements.size(); i++) {
             SimpleElement se = childElements.get(i);
             if (se.tagName.equals(tag))
                 return se;
         }
         return null;
     }

    /** *****************************************************************
    */
    public String toString(int indent, boolean forFile) {

        StringBuffer strindent = new StringBuffer();
        for (int i = 0; i < indent; i++) {
            strindent.append("  ");
        }
        StringBuffer result = new StringBuffer();
        result.append(strindent.toString() + "<" + getTagName() + " ");
        HashSet names = new HashSet();
        names.addAll(getAttributeNames());
        Iterator it = names.iterator();
        while (it.hasNext()) {
            String attName = (String) it.next();
            String value = getAttribute(attName);
            if (forFile)
                value = SimpleDOMParser.convertFromReservedCharacters(value);
            result.append(attName + "=\"" + value + "\" ");
        }
        ArrayList children = getChildElements();
        if (children.size() == 0 && (getText() == null || getText().equals("null")))
            result.append("/>\n");
        else {
            result.append(">\n");
            if (getText() != null && getText() != "" && !getText().equals("null")) {
                if (forFile)
                    result.append(SimpleDOMParser.convertFromReservedCharacters(getText()));
                else
                    result.append(getText() );
                result.append("\n");
            }
            for (int i = 0; i < children.size(); i++) {
                SimpleElement element = (SimpleElement) children.get(i);
                result.append(element.toString(indent+1,forFile));
            }
            result.append(strindent.toString() + "</"  + getTagName() + ">\n");
        }

        return result.toString();
    }

    /** *****************************************************************
    */
    public String toString() {

        return toString(0,false);
    }

    /** *****************************************************************
    */
    public String toFileString() {

        return toString(0,true);
    }
    /** *****************************************************************
    */
    public String toFileString(int indent) {

        return toString(indent,true);
    }
}
