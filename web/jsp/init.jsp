<%@ include	file="Prelude.jsp" %>

<html>
   <body>

<%
/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/
    out.println("<html>");
    out.println("  <head>");
    out.println("    <title>Sigma Knowledge Engineering Environment - Initializing</title>");
    out.println("  </head>");
    out.println("  <body bgcolor=\"#FFFFFF\">");

    if (KBmanager.initialized) {
        System.out.println("init.jsp: initialized.  Redirecting to KBs.jsp.");
        response.sendRedirect("KBs.jsp");
        Thread.sleep(1000);
        return;
    }

    // Set refresh, autoload time as 15 seconds
    response.setIntHeader("Refresh", 15);
 %>

<table width="95%" cellspacing="0" cellpadding="0">
    <tr>
        <td valign="top">
            <table cellspacing="0" cellpadding="0">
                <tr>
                    <td align="left" valign="top"><img src="pixmaps/sigmaSymbol.gif"></td>
                    <td>&nbsp;&nbsp;</td>
                    <td align="left" valign="top"><img src="pixmaps/logoText.gif"><BR>
                        <b> <%=welcomeString%>></b></td>
                </tr>
            </table>
        </td>
    </tr>
</table>

<B>Sigma is initializing</b>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>

 
