
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

import java.io.*;
import java.util.*;
import java.text.ParseException;



/** *****************************************************************
 * A class designed to adhere strictly to the SUO-KIF definition at
 * http://suo.ieee.org/suo-kif.html
 * @author Adam Pease
 */
public class KIFplus {

    String filename;
    String[] spcl = {"!" , "$" , "%" , "&" , "*" , "+" , "-" , "." , "/" , "<" , "=" , ">" , "?" , "@" , "_" , "~"};
    String[] wht = {" ", "\t", "\n"};
    ArrayList special = new ArrayList(Arrays.asList(spcl));
/*
upper ::= A | B | C | D | E | F | G | H | I | J | K | L | M | 
          N | O | P | Q | R | S | T | U | V | W | X | Y | Z

lower ::= a | b | c | d | e | f | g | h | i | j | k | l | m | 
          n | o | p | q | r | s | t | u | v | w | x | y | z

digit ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9

white ::= space | tab | return | linefeed | page

initialchar ::= upper | lower

wordchar ::= upper | lower | digit | - | _ | special

character ::= upper | lower | digit | special | white

word ::= initialchar wordchar*

string ::= "character*"

variable ::= ?word | @word

number ::= [-] digit+ [. digit+] [exponent]

exponent ::= e [-] digit+

term ::= variable | word | string | funterm | number | sentence

relword ::= initialchar wordchar*

funword ::= initialchar wordchar*

funterm ::= (funword term+)

sentence ::= word | equation | inequality | 
             relsent | logsent | quantsent

equation ::= (= term term)

relsent ::= (relword term+)

logsent ::= (not sentence) |
            (and sentence+) |
            (or sentence+) |
            (=> sentence sentence) |
            (<=> sentence sentence)

quantsent ::= (forall (variable+) sentence) |
              (exists (variable+) sentence)
*/
    
    /** *****************************************************************
     */
    private void readLogsent(FileReader fr) throws ParseException, IOException {
    }

    /** *****************************************************************
     */
    private void start(FileReader fr) throws ParseException, IOException {

        try {
            int ch = 0;
            StringBuffer predicate = new StringBuffer();
            while (fr.ready() && ch != ' ') {
                ch = fr.read();
                predicate = predicate.append(ch);
            }
            if (predicate.toString().equalsIgnoreCase("forall")) {
                readLogsent(fr);
            }
        }
        catch (ParseException pe) {
            throw new ParseException("Error in KIF.readFile(): " + pe.getMessage(),pe.getErrorOffset());
        }
        catch (java.io.IOException e) {
            throw new IOException("Error in KIF.readFile(): IO exception parsing file " + filename);
        }

    }
    /** *****************************************************************
     */
    public void main(String[] args) {

        KIFplus k = new KIFplus();
        String fname = args[0];
        try {
            FileReader fr = new FileReader(fname);
            int linenumber = 0;
            k.filename = fname;
            while (fr.ready()) {
                int ch = fr.read();
                if (ch == '(') {
                    k.start(fr);
                }
            }
        }
        catch (ParseException pe) {
            System.out.print("Error in KIF.readFile(): " + pe.getMessage());
            System.out.println(pe.getErrorOffset());
        }
        catch (java.io.IOException e) {
            System.out.println("Error in KIF.readFile(): IO exception parsing file " + filename);
        }
    }

}
