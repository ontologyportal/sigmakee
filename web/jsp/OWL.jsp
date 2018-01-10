<%@ page
   language="java"
   import="com.articulate.sigma.*,java.text.ParseException,java.net.URLConnection,java.net.URL,ClientHttpRequest.*,com.oreilly.servlet.*,com.oreilly.servlet.multipart.*,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"
   pageEncoding="UTF-8"
   contentType="text/xml;charset=UTF-8"
%>

<%
/** This code is copyright Articulate Software (c) 2003.  
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://github.com/ontologyportal
*/
 System.out.println("in OWL.jsp");

 if (!KBmanager.getMgr().initialized) {
     System.out.println("OWL.jsp: kb manager not initialized, redirecting");
     response.sendRedirect("init.jsp");
     return;
 }

 String kbName = request.getParameter("kb");
 if (kbName == null || StringUtil.emptyString(kbName)) 
     kbName = "SUMO";

 KB kb = null;
 if (!StringUtil.emptyString(kbName)) {
     kb = KBmanager.getMgr().getKB(kbName);
     if (kb != null)
         TaxoModel.kbName = kbName;
 }
 else {
     System.out.println("OWL.jsp: empty kb name, redirecting");
     response.sendRedirect("login.html");
 }
 String term = request.getParameter("term");
 if (term == null) term = "Process";
 OWLtranslator.initOnce(kbName);
 OWLtranslator.ot.writeTerm(new PrintWriter(out),term);
%>


