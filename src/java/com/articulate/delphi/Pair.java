
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
 */
public class Pair implements Comparable {

    public int value = -1;
    public String str = null;

    /** *****************************************************************
     */
    public int compareTo(Object o) {

        Pair p = (Pair) o;
        if (value < p.value) 
            return -1;
        if (value == p.value) 
            return 0;
        return 1;        
    }
}
