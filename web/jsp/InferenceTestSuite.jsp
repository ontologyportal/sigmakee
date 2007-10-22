
<%@ include file="Prelude.jsp" %>
<html>                                             
<HEAD><TITLE>Test Suite</TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">

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
August 9, Acapulco, Mexico.
*/

  StringBuffer sb = new StringBuffer();
  String kbName = request.getParameter("kb");
  KB kb = KBmanager.getMgr().getKB(kbName);
  String inferenceTestDir = KBmanager.getMgr().getPref("inferenceTestDir");
  if (inferenceTestDir == null)
      sb = sb.append("Error: No inference test directory specified.  Set in <A href=\"Preferences.jsp\">Preferences</A>");
  else {
      String test = request.getParameter("test");
      if (test != null && test.equalsIgnoreCase("inference")) 
          sb = sb.append(InferenceTestSuite.test(kb));          
      if (test != null && test.equalsIgnoreCase("english")) 
          sb = sb.append(CELTTestSuite.test(kb));
  }

%>

<%
  out.println(sb.toString());
%>
<p>

<%@ include file="Postlude.jsp" %>

