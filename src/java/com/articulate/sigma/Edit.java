
package com.articulate.sigma;

import java.util.*;
import java.io.*;

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

/** Class for invoking a client side text editor. 
 */
public class Edit {

    public static void editFile(String filename, int lineNumber) {

        System.out.println("INFO in Edit.editFile(): Editing file " + filename);
        StringBuffer execString = new StringBuffer();
        String editorCommand = KBmanager.getMgr().getPref("editorCommand");
        String lineNumberCommand = KBmanager.getMgr().getPref("lineNumberCommand");
        if (editorCommand != null && editorCommand != "") {
            execString.append(editorCommand + " " + filename);
            if (lineNumberCommand != null && lineNumberCommand != "")
                execString.append(" " + lineNumberCommand + (new Integer(lineNumber)).toString());
            execString.append("')\">");
        }

        System.out.println("INFO in Edit.editFile(): Exec string " + execString.toString());
        try {
            Process _edit = Runtime.getRuntime().exec(execString.toString());        
        }
        catch (IOException ioe) {
            System.out.println("Error in Edit.editFile(): Unable to start editor: " + editorCommand);
            System.out.println(ioe.getMessage());
        }
    }

}

