<%@ include	file="Prelude.jsp" %>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

  String word = request.getParameter("word");
  String writeProlog = request.getParameter("writeProlog");
  String synset = request.getParameter("synset");
  String POS = request.getParameter("POS");
  String key = request.getParameter("key");      
  if (POS == null) {
      if (synset.length() == 9)
          POS = synset.substring(0,1);
      else
          POS = "0";
  }
%>
<head>
  <title>Sigma WordNet mapping browser</title>
</head>
<BODY BGCOLOR=#FFFFFF>

    <%
        String pageName = "WordNet";
        String pageString = "WordNet mapping browser";
    %>
    <%@include file="CommonHeader.jsp" %>

<h3>SUMO Search Tool</h3>
<P>This tool relates English terms to concepts from the
   <a href="http://www.ontologyportal.org">SUMO</a>
   ontology by means of mappings to
   <a href="http://wordnet.princeton.edu">WordNet</a> synsets.
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<form action="WordNet.jsp" method="GET">
  <font face="Arial,helvetica"><b>English Word:&nbsp;</b></font>
  <input type="text" name="word" VALUE=<%= "\"" + (request.getParameter("word")==null?"":request.getParameter("word")) + "\"" %>>
    <select name="POS">
      <option <%= POS.equals("0")?"selected":"" %> value="0">Any
      <option <%= POS.equals("1")?"selected":"" %> value="1">Noun
      <option <%= POS.equals("2")?"selected":"" %> value="2">Verb
      <option <%= POS.equals("3")?"selected":"" %> value="3">Adjective 
      <option <%= POS.equals("4")?"selected":"" %> value="4">Adverb
    </select>
  <input type="submit" value="Submit">
</form>

<%
  if (role != null && role.equalsIgnoreCase("admin")) {
%>
     <FORM name=writeProlog ID=writeProlog action="WordNet.jsp" method="GET">
         <INPUT type="submit" NAME="writeProlog" VALUE="writeProlog">
     </FORM>
<%
  }

  if (writeProlog != null) 
      WordNet.wn.writeProlog(kb);
  String params = "flang=" + flang + "&lang=" + language + "&kb=" + kbName;
  if (word != null && word != "")
      out.println(WordNet.wn.page(word,Integer.decode(POS).intValue(),kbName,synset,params));
  else
      if (synset != null && synset != "")
          out.println(WordNet.wn.displaySynset(kbName,synset,params)); 
      else if (key != null) 
          out.println(WordNet.wn.displayByKey(kbName,key,params));  
  if (synset != null) {
      String OMWsynset = synset.substring(1) + "-" + WordNetUtilities.posNumberToLetter(POS.charAt(0));
      out.println("\n<a href=\"" + HTMLformatter.createHrefStart() + "/sigma/OMW.jsp?" +
              "kb=" + kbName + "&synset=" + OMWsynset + "\">Show Open Multilingual Wordnet links</a><p>\n");
  }
  if (synset != null && synset != "") {
      System.out.println("WordNet.jsp: synset: " + synset);
      System.out.println("WordNet.jsp: POS: " + POS);
      if (synset.length() == 8 && POS.length() == 1)
          out.println(WordNetUtilities.showVerbFrames(POS + synset));
      else
          out.println(WordNetUtilities.showVerbFrames(synset));
  }
  if (synset != null && synset != "")
  	  out.println("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/OWL.jsp?" +
                  "kb=" + kbName + "&term=WN30-" + synset + "\">Show OWL translation</a></small><p>\n");              
  else if (word != null && word != "")
  	  out.println("\n<small><a href=\"" + HTMLformatter.createHrefStart() + "/sigma/OWL.jsp?" +
                  "kb=" + kbName + "&term=WN30Word-" + word + "\">Show OWL translation</a></small><p>\n");   
  
%>
<BR>

<%@ include file="Postlude.jsp" %>
</body>
