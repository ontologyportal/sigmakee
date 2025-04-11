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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**  *****************************************************************
 * <code>SimpleElement</code> is the only node type for
 * simplified DOM model.  Note that all CDATA values are stored with
 * reserved characters &gt; and &lt; converted to entity types
 * respectively.
 */
public class SimpleElement {

    private String tagName;
    private String text;
    private final Map<String,String> attributes;
    private final List<SimpleElement> childElements;

    public SimpleElement(String tagName) {
        this.tagName = tagName;
        attributes = new HashMap<>();
        childElements = new ArrayList<>();
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getText() {

        if (text != null && !"".equals(text))
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
        if (attribute != null && !"".equals(attribute))
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

    public List<SimpleElement> getChildElements() {
        return childElements;
    }

    /** *****************************************************************
     * Retrieves the first child element by name
     *
     * @param tag an element name to search for
     * @return the first child with the given tag name, null if none
     */
     public SimpleElement getChildByFirstTag(String tag) {

         if (childElements == null || childElements.size() < 1)
             return null;
         SimpleElement se;
         for (int i = 0; i < childElements.size(); i++) {
             se = childElements.get(i);
             if (se.tagName.equals(tag))
                 return se;
         }
         return null;
     }

    /** *****************************************************************
    */
    public String toString(int indent, boolean forFile) {

        StringBuilder strindent = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            strindent.append("  ");
        }
        StringBuilder result = new StringBuilder();
        result.append(strindent.toString()).append("<").append(getTagName()).append(" ");
        Set<String> names = new HashSet<>();
        names.addAll(getAttributeNames());
        String value;
        for (String attName : names) {
            value = getAttribute(attName);
            if (forFile)
                value = SimpleDOMParser.convertFromReservedCharacters(value);
            result.append(attName).append("=\"").append(value).append("\" ");
        }
        List<SimpleElement> children = getChildElements();
        SimpleElement element;
        if (children.isEmpty() && (getText() == null || getText().equals("null")))
            result.append("/>\n");
        else {
            result.append(">\n");
            if (getText() != null && !"".equals(getText()) && !getText().equals("null")) {
                if (forFile)
                    result.append(SimpleDOMParser.convertFromReservedCharacters(getText()));
                else
                    result.append(getText() );
                result.append("\n");
            }
            for (int i = 0; i < children.size(); i++) {
                element = children.get(i);
                result.append(element.toString(indent+1,forFile));
            }
            result.append(strindent.toString()).append("</").append(getTagName()).append(">\n");
        }

        return result.toString();
    }

    @Override
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
