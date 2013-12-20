
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Class for invoking CELT.
 */
public class CELT {

    private Process _CELT;
    private BufferedReader _reader; 
    private BufferedWriter _writer; 
    private BufferedReader _error; 

    /** *************************************************************
     * Create a running instance of CELT.
     *
     * @throws IOException should not normally be thrown unless either
     *         Prolog executable or CELT startup file name are incorrect
     */
    public CELT() throws IOException {

        String CELT_PATH; 
        String PL_EXECUTABLE;
        String line = null;
        String erline = null;

        if (KBmanager.getMgr().getPref("prolog") == null || KBmanager.getMgr().getPref("prolog") == "")
            KBmanager.getMgr().setPref("prolog","C:\\Program Files\\pl\\bin\\plcon.exe");  
        PL_EXECUTABLE = KBmanager.getMgr().getPref("prolog"); 
        if (KBmanager.getMgr().getPref("celtdir") == null)
            KBmanager.getMgr().setPref("celtdir","C:\\PEASE\\CELT-ACE\\latestDemo\\May29");  
        CELT_PATH = KBmanager.getMgr().getPref("celtdir"); 
        System.out.println("INFO in CELT(): Setting prolog to: " + PL_EXECUTABLE);
        
        if (!(new File(PL_EXECUTABLE)).exists())
            throw new IOException("Error in CELT(): File " + PL_EXECUTABLE + " does not exist.");
        if (!(new File(CELT_PATH)).exists())
            throw new IOException("Error in CELT(): File " + CELT_PATH + " does not exist.");

        Process _CELT = Runtime.getRuntime().exec(PL_EXECUTABLE + " " + CELT_PATH + File.separator + "Startup.pl");
        InputStream stderr = _CELT.getErrorStream();
        InputStreamReader isrerror = new InputStreamReader(stderr);
        _error = new BufferedReader(isrerror);
        
        InputStream stdout = _CELT.getInputStream();
        InputStreamReader isrout = new InputStreamReader(stdout);
        _reader = new BufferedReader(isrout);

        OutputStream stdin = _CELT.getOutputStream();
        OutputStreamWriter oswin = new OutputStreamWriter(stdin);
        _writer = new BufferedWriter(oswin);

        do {
            if (_error.ready()) {
                erline = _error.readLine();
                System.out.println("error: " + erline);
            }
            else if (_reader.ready()) {
                line = _reader.readLine();
                System.out.println("line: " + line);
            }
            else {
                line = null;
                erline = null;
            }
            try {
                synchronized (this) {
                    this.wait(100);
                }
            }
            catch (InterruptedException ie) {
                System.out.println("Error in CELT.(): " + ie.getMessage());
            }
        } while (line == null || !line.equalsIgnoreCase("Done initializing."));
    }

    /** *************************************************************
     * Submit a sentence, terminated by a period or question mark and
     * return a KIF formula that is equivalent.
     *
     * @param sentence 
     * @return answer from CELT
     * @throws IOException should not normally be thrown
     */

    public String submit(String sentence) throws IOException {
    
        String line = null;
        String erline = null;
        boolean inKIF = false;
        boolean fail = false;
        StringBuffer kif = new StringBuffer();

        System.out.println("xml_eng2log(\"" + sentence + "\",X),format('~w',X).");
        _writer.write("xml_eng2log(\"" + sentence + "\",X),format('~w',X).\n\n\n",0,sentence.length()+35);
        _writer.flush();

        do {
            if (_error.ready()) {
                erline = _error.readLine();
                System.out.println("error: |" + erline + "|");
            }
            else if (_reader.ready()) {
                line = _reader.readLine();
                System.out.println("line: |" + line + "|");
            }
            else {
                line = null;
                erline = null;
            }
            if (line != null) {
                if (line.equalsIgnoreCase("</KIF>")) {
                    inKIF = false;
                }
                else if (inKIF)
                    kif = kif.append(line+"\n");
                if (line.indexOf("Could not parse") == 0) 
                    fail = true;
                if (line.equalsIgnoreCase("<KIF>")) 
                    inKIF = true;
            }
            try {
                synchronized (this) {
                    this.wait(100);
                    System.out.println("Waiting.");
                }
            }
            catch (InterruptedException ie) {
                System.out.println("Error in CELT.submit(): " + ie.getMessage());
            }
        } while (line == null || ((line.indexOf("</translation>") != 0)));
            // && (erline != null && (!erline.equalsIgnoreCase("No")))));
        if (fail) {
            System.out.println("Failed to parse.");
            return null;
        }
        else {
            System.out.println("Parse successful.");
            return kif.toString();
        }        
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        try {
            System.out.println("in CELT main.");
            CELT celt = new CELT();
            System.out.println("completed initialization.");
            System.out.println(celt.submit("John kicks the cart."));
            System.out.println(celt.submit("John pokes the antelope with a fork."));
            System.out.println(celt.submit("Who moves the cart?"));
        }   
        catch (IOException ioe) {
            System.out.println("Error in CELT.main(): " + ioe.getMessage());
        }
    }
}
