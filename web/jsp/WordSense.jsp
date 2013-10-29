<%@ include file="Prelude.jsp" %>

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
August 9, Acapulco, Mexico. See also http://sigmakee.sourceforge.net
*/

String sentence = request.getParameter("sentence");
String sentCounter = request.getParameter("sentCounter");

StringBuffer show = new StringBuffer();
String params = "flang=" + flang + "&lang=" + language + "&kb=" + kbName;

	
if (sentence != null && sentCounter == null) {
	if (WordNet.wn.isFile(sentence)==false) //This will fail if the input is not supposed to be a file path but contains a / or \
		show.append(WordNet.wn.sumoSentenceDisplay(sentence, sentence, params));
	else 
		show.append(WordNet.wn.sumoFileDisplay(sentence, "0", params));
}

if (sentence != null && sentCounter != null) 
	show.append(WordNet.wn.sumoFileDisplay(sentence, sentCounter, params));
	
%>


<html>
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

<h3>SUMO Word Sense/Sentiment Analysis Tool</h3>
<P>This tool provides context sensitive sense and sentiment analysis of whole sentences. Enter either a sentence or a full pathname for a .txt file.
<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>

<form name="sentenceLoader" id="sentenceLoader" action="WordSense.jsp" method="GET">
  <font face="Arial,helvetica"><b>Sentence:&nbsp;</b></font>
  <input type="textarea" rows="4" cols="50" name="sentence" VALUE=<%= "\"" + (request.getParameter("sentence")==null?"":request.getParameter("sentence")) + "\"" %>>
  <input type="submit" value="Submit">
</form>

<form name="fileUploader" id="fileUploader" action="WordSenseFile.jsp" method="POST" enctype="multipart/form-data">
  <font face="Arial,helvetica"><b>File:&nbsp;</b></font>
  <input type="file" name="textFile">
  <br>
  <input type="submit" value="Submit">
</form> 


<br>
 <%=show.toString() %><BR>
 
<%@ include file="Postlude.jsp" %>
</body>
</html>
          