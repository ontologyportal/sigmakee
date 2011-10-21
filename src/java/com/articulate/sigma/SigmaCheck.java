/** This code is copyright Rearden Commerce 2011.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Rearden Commerce in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
package com.articulate.sigma;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/** ***************************************************************
 *  This class, when scheduled as a "cron" job, can serve as a "heartbeat"
 *  function, checking whether the sigma server is functioning properly.
 *  Invoke with
 *  java -classpath /home/user/SourceForge/sigma/build/classes:/home/user/SourceForge/sigma/lib/mail.jar 
 *    com.articulate.sigma.SigmaCheck
 */
public class SigmaCheck {

	// Will create a file object called emailSent.txt located in the Sigma home
	// directory
	// private static File emailSentFile = new File(System.getenv("SIGMA_HOME")
	// + File.separator + "emailSent.txt");

    /** ***************************************************************
     *  checkURL checks whether an inputed URL contains the string "SUMO" somewhere in it's HTML body
     *  @param targetURL is a URL to be checked
     *  @returns boolean true if the URL contains "SUMO" and false otherwise
     */
    private static boolean containsSUMO(String targetURL) {

        try {
            URL target = new URL(targetURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(target.openStream()));
        
            StringBuffer HTMLbuffer = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                HTMLbuffer.append(inputLine);
            in.close();
            String HTMLtext = HTMLbuffer.toString();        
            if (HTMLtext.contains("<TD>SUMO</TD>"))
                return true;        
            else 
                return false;        
            }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** ***************************************************************
     * @param args
     */
	public static void checkSite(String url, String email) {
		File emailSentFile = new File(System.getenv("SIGMA_HOME")
				+ File.separator + "emailSent-" + url.hashCode());

        if (!emailSentFile.exists() && !containsSUMO(url)) {
            try {
                emailSentFile.createNewFile(); //create empty File
                //send email
                Properties props = new Properties();
                //props.put("mail.smtp.host", "owa.mygazoo.com");
                //props.put("mail.from", "first.last@reardencommerce.com");
				props.put("mail.smtp.host", "owa.mygazoo.com");
				props.put("mail.from", "adam.pease@reardencommerce.com");
                Session session = Session.getInstance(props, null);

                MimeMessage msg = new MimeMessage(session);
                msg.setFrom();
                //msg.setRecipients(Message.RecipientType.TO,"first.last@reardencommerce.com");
				msg.setRecipients(Message.RecipientType.TO, email);
                msg.setSubject("SIGMA IS DOWN");
                msg.setSentDate(new Date());
				msg.setText("The Sigma main page at " + url + " is down.");
                Transport.send(msg);
                System.out.println("Email Sent");
            } 
            catch (MessagingException mex) {
                System.out.println("send failed, exception: " + mex);
            } 
            catch (IOException ioex) {
                System.out.println("File creation failed, exception: " + ioex);
            }
        }    
 else if (emailSentFile.exists() && containsSUMO(url)) {
            try {
                //delete file
                boolean deleted = emailSentFile.delete();
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	public static void main(String[] args) {
		String url = "http://sigma.ontologyportal.org:4010/sigma/KBs.jsp";
		String email = "adam.pease@reardencommerce.com";

		for (int i = 0; i < args.length; i++) {
			if (args[i].contains("http://"))
				url = args[i];
			else if (args[i].contains("@"))
				email = args[i];
		}

		try {
			checkSite(url, email);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
