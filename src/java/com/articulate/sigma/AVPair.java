
/* This code is copyrighted by Articulate Software (c) 2003.
It is released underthe GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users ofthis code also consent, by use of this code, to credit Articulate Software in any
writings, briefings,publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/

package com.articulate.sigma;


public class AVPair implements Comparable {

    public String attribute ="";  // this is the sort field for comparison
    public String value = "";

    public int compareTo(Object avp) throws ClassCastException {

        if (!avp.getClass().getName().equalsIgnoreCase("com.articulate.sigma.AVPair"))
            throw new ClassCastException("Error in AVPair.compareTo(): "
                                         + "Class cast exception for argument of class: "
                                         + avp.getClass().getName());
        return attribute.compareTo(((AVPair) avp).attribute);
    }

    public String toString() {

    	return "[" + attribute + "," + value + "]";
    }
}
