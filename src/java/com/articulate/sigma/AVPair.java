
/* This code is copyrighted by Articulate Software (c) 2003.
It is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.

Authors:
Adam Pease
Infosys LTD.
*/

package com.articulate.sigma;

import java.io.Serializable;

/*****************************************************************
 */
public class AVPair implements Comparable, Serializable {

    public String attribute = "";  // this is the sort field for comparison
    public String value = "";

    /*****************************************************************
     */
    public AVPair() {

    }

    /*****************************************************************
     */
    public AVPair(String attrib, String val) {

        attribute = attrib;
        value = val;
    }

    /*****************************************************************
     */
    public int compareTo(Object avp) throws ClassCastException {

        if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.AVPair"))
            throw new ClassCastException("Error in AVPair.compareTo(): "
                                         + "Class cast exception for argument of class: "
                                         + avp.getClass().getName());
        //System.out.println("Info in AVPair.compareTo(): comparing: " + attribute +
        //        "to: " + ((AVPair) avp).attribute);
        return attribute.compareTo(((AVPair) avp).attribute);
    }

    /*****************************************************************
     */
    public String toString() {

        return "[" + attribute + "," + value + "]";
    }
}
