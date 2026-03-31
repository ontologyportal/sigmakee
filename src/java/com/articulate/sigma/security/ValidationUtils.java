package com.articulate.sigma.security;
import com.articulate.sigma.utils.StringUtil;
/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or
other representations of any software which incorporates, builds on, or uses this
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment,
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
*/


/** 
 * This is a class includes input validation functions useful in many different contexts throughout the application.
*/

public final class ValidationUtils {

    private ValidationUtils() {}


    /****************************************************************
     * Returns the integer value of a string after validating that it is
     * an integer and removing any HTML.
     *
     * @param s A String
     * @return Validated integer value
     */
    public static int returnValidatedInteger(String s) {
        if (StringUtil.emptyString(s) || !StringUtil.isInteger(s)) {
            return 1;
        } else {
            s = StringUtil.removeHTML(s);
            return Integer.parseInt(s);
        }
    }
}