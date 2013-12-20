
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/** ***************************************************************
 * A class designed to mirror the information in a basic XML tag.
 */
public class BasicXMLelement {

     /** The name of the tag */
    public String tagname = null;
     /** The attributes of the tag in key=value form */
    public HashMap<String,String> attributes = new HashMap<String,String>();
     /** Any subelements of the tag, meaning any other
      *  tags that are nested within this one. */
    public ArrayList<BasicXMLelement> subelements = new ArrayList<BasicXMLelement>();
     /** The contents between the start and end of this tag */
    public String contents = "";


    /** ***************************************************************
     * Convert the XML element to a String.
     */
    public String toString() {

        StringBuffer result = new StringBuffer();
        result = result.append("<" + tagname);
        Iterator it = attributes.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String value = attributes.get(key);
            result = result.append(" " + key + "='" + value + "'");
        }
        result = result.append(">");
        if (contents != null)
            result = result.append(contents);
        for (int i = 0; i < subelements.size(); i++) {
            BasicXMLelement el = subelements.get(i);
            result = result.append(el.toString());
        }
        result = result.append("</" + tagname + ">\n");
        return result.toString();
    }
}

