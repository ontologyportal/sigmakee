<%@ page language="java" contentType="text/html; charset=US-ASCII"
import="com.articulate.sigma.*,com.articulate.sigma.wordNet.*,java.net.URLConnection, javax.servlet.http.HttpServletRequest, java.io.*"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
   <body>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzm?ller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
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
                        <b></b></td>
                </tr>
            </table>
        </td>
    </tr>
</table>

<B>Sigma is initializing</b>

<%
    out.println("<P><P>\n");
    for (String kbname : KBmanager.getMgr().kbs.keySet()) {
        KB kb = KBmanager.getMgr().getKB(kbname);
        out.println(kb.name + " : " + kb.constituents.size() + " constituents loaded<br>");
        if (kb.kbCache.initialized)
            out.println("cache initialized<br>");
        else
            out.println("cache not initialized<br>");
        out.println("<P>");
    }
    out.println("<P>");
    if (!WordNet.initNeeded)
        out.println("WordNet initialized<P>");
    else
        out.println("WordNet not initialized<P>");
%>

<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>


