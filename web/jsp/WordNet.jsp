<%@ include	file="Prelude.jsp" %>

<%

  String kbname = KBmanager.getMgr().getPref("sumokbname");
Set kbNames = KBmanager.getMgr().getKBnames();

// This is a kluge.  We need some way of specifying the "current",
// loaded KB.  There might not be a value for the key "sumokbname", or
// it might be stale.  For now, if we can't find kbname in the Set of
// loaded KBs, we just take the first value retrieved from the Set and
// bind kbname to it.  If all else fails, we use the name "SUMO", but
// this won't be of much use if a KB named "SUMO" is not loaded.
if ( ! kbNames.isEmpty() ) {
    if ( (! Formula.isNonEmptyString(kbname)) || (! kbNames.contains(kbname)) ) {
	Iterator it = kbNames.iterator();
	while ( it.hasNext() ) {
	    kbname = (String) it.next();
	    break;
	}
    }
}
if ( ! Formula.isNonEmptyString(kbname) ) {
      kbname = "SUMO";
}
  String word = request.getParameter("word");
  String writeProlog = request.getParameter("writeProlog");
  String synset = request.getParameter("synset");
  String POS = request.getParameter("POS");
      
  if (POS == null)
      POS = "1";  
%>
<head>
  <title>Sigma WordNet mapping browser</title>
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
   <a href="http://www.ontologyportal.org">SUMO</a>
   ontology by means of mappings to
   <a href="http://www.cogsci.princeton.edu/~wn/">WordNet</a> synsets.
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<form action="WordNet.jsp" method="GET">
  <font face="Arial,helvetica"><b>English Word:&nbsp;</b></font>
  <input type="text" name="word" VALUE=<%= "\"" + (request.getParameter("word")==null?"":request.getParameter("word")) + "\"" %>>
    <select name="POS">
      <option <%= POS.equals("1")?"selected":"" %> value="1">Noun
      <option <%= POS.equals("2")?"selected":"" %> value="2">Verb
      <option <%= POS.equals("3")?"selected":"" %> value="3">Adjective 
      <option <%= POS.equals("4")?"selected":"" %> value="4">Adverb
    </select>
  <input type="submit" value="Submit">
</form>

<%
          if (KBmanager.getMgr().getPref("userName") != null && 
              KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
%>

<FORM name=writeProlog ID=writeProlog action="WordNet.jsp" method="GET">
    <INPUT type="submit" NAME="writeProlog" VALUE="writeProlog">
</FORM>

<%
          }

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
  KB kb = KBmanager.getMgr().getKB(kbname);
  if (writeProlog != null) 
      WordNet.wn.writeProlog(kb);
  if (word !=null && word != "")
      out.println(WordNet.wn.page(word,Integer.decode(POS).intValue(),kbname,synset));
  else
      if (synset !=null && synset != "")
          out.println(WordNet.wn.displaySynset(kbname,synset));  
%>
<BR>

<%@ include file="Postlude.jsp" %>
</body>
