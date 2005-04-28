/** This code is copyright Articulate Software (c) 2005.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Ted Gordon in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  */
package com.articulate.delphi;
import java.util.*;
import java.io.*;
import java.text.*;
import com.articulate.sigma.*;

/** *****************************************************************
 * A class that contains information about a cell in a Delphi matrix.
 */
public class Cell {

      /** Whether the cell value is an outlier compared to the group. */
    public boolean outlier;
      /** The String representation of an integer value, signifying the 
       * degree to which a given criterion supports a given decision. */
    public String value = null;
      /** The explanation user's explanation for choosing a given value. */
    public String justification = null;
  
    /** ***************************************************************** 
     * The justification String is the body of the <cell> tag.
     */
    public String toXML() {

        StringBuffer result = new StringBuffer();
        result.append("<cell value=\"");
        result.append(value);
        result.append("\" outlier=\"");
        result.append(String.valueOf(outlier) + "\">\n");
        result.append(justification);
        result.append("</cell>\n");       
        return result.toString();
    }

    /** ***************************************************************** 
     *  Read in an XML input
     */
    public void fromXML(BasicXMLelement xml) {

        value = (String) xml.attributes.get("value");
        String outlierStr = (String) xml.attributes.get("outlier");
        justification = xml.contents;
    }
}
