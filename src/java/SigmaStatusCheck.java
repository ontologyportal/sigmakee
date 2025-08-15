import java.io.*;
import java.util.*;
import java.net.*;

import javax.mail.*;
import javax.mail.internet.*;

/* This code is copyrighted by Articulate Software (c) 2011.
It is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software in any
writings, briefings, publications, presentations, or other representations of any
software which incorporates, builds on, or uses this code.*/

public class SigmaStatusCheck {

    private static File emailSentFile = new File("/emailSent.txt"); //Will create a file object called emailSent.txt located in the root directory

    /** ***************************************************************
     *  checkURL checks whether a URL contains the string "SUMO" somewhere in its HTML body
     *  @param targetURL is a URL to be checked
     *  @returns boolean true if the URL contains "SUMO" and false otherwise
     */
    private static boolean containsSUMO(String targetURL) {

        try {
            URL target = new URL(targetURL);
            BufferedReader in = new BufferedReader(new InputStreamReader(target.openStream()));

            StringBuilder HTMLbuffer = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                HTMLbuffer.append(inputLine);
            in.close();
            String HTMLtext = HTMLbuffer.toString();
            return HTMLtext.contains("<TD>SUMO</TD>");
        }
        catch (IOException E) {
            E.printStackTrace();
            return false;
        }
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        if (!containsSUMO("http://localhost:8080/sigma/KBs.jsp") && !emailSentFile.exists()) {
            try {
                emailSentFile.createNewFile(); //create empty File

                //send email
                Properties props = new Properties();
                props.put("mail.smtp.host", "smtp.office365.com");
                props.put("mail.from", "notify@articulatesoftware.com");
                Session session = Session.getInstance(props, null);

                MimeMessage msg = new MimeMessage(session);
                msg.setFrom();
                msg.setRecipients(Message.RecipientType.TO,
                                  "apease@articulatesoftware.com");
                msg.setSubject("SIGMA IS DOWN");
                msg.setSentDate(new Date());
                msg.setText("The Sigma main page at http://sigma.ontologyportal.org:8080/sigma/KBs.jsp is down.");
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
        else if (containsSUMO("http://localhost:8080/sigma/KBs.jsp") && emailSentFile.exists()) {
            try {
                //delete file
                boolean deleted = emailSentFile.delete();
            }
            catch (Exception E) {
                E.printStackTrace();
            }
        }
    }
}
