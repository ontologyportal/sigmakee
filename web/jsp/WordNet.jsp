<%@ include	file="Prelude.jsp" %>

<head>
  <title>SUMO Search Tool</title>
</head>
<BODY BGCOLOR=#FFFFFF>
<table width=95% cellspacing=0 cellpadding=0>
    <tr><td valign="top">
        <table cellspacing=0 cellpadding=0>
            <tr><td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
                <td>&nbsp;</td><td align="left" valign="top"><img src="pixmaps/logoText-gray.gif">
<BR>&nbsp;&nbsp;&nbsp;<font COLOR=teal></font></td></tr></table>
        </td><td valign="bottom"></td><td>
<font face="Arial,helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A> ]</b></FONT></td></tr></table>

<h3>SUMO Search Tool</h3>
<P>This tool relates English terms to concepts from the
   <a href="http://ontology.teknowledge.com">SUMO</a>
   ontology by means of mappings to
   <a href="http://www.cogsci.princeton.edu/~wn/">WordNet</a> synsets.
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<form action="WordNet.jsp" method="GET">
  <font face="Arial,helvetica"><b>English Term:&nbsp;</b></font>
  <input type="text" name="word" VALUE=<%= "\"" + (request.getParameter("word")==null?"":request.getParameter("word")) + "\"" %>>
    <select name="POS">
      <option value="1">Noun
      <option value="2">Verb
      <option value="3">Adjective 
      <option value="4">Adverb
    </select>
  <input type="submit" value="Submit">
</form>

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

  WordNet.initOnce();

  String sumokbname = KBmanager.getMgr().getPref("sumokbname");
  if (sumokbname == null)
      sumokbname = "SUMO";
  String word = request.getParameter("word");
  String POS = request.getParameter("POS");
  if (word !=null && POS !=null)
      out.println(WordNet.wn.page(word,Integer.decode(POS).intValue(),sumokbname));  
%>
<BR>

</body>