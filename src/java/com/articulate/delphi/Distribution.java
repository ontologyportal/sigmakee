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
public class Distribution {

    public int numerator = 0;
    public int denominator = 0;
      /** The values are Integer(s). */
    public ArrayList values = new ArrayList();

    /** *****************************************************************
     */
    public void fromArray(Integer[] ints) {

        values = new ArrayList();
        for (int i = 0; i < ints.length; i++) {
            values.add(ints[i]);
        }
    }
}
