<%@ include file="Prelude.jsp" %>

<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017:-present,
    Infosys (c) 2017-2019.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/

String sentence = request.getParameter("sentence");
String sentCounter = request.getParameter("sentCounter");

StringBuffer show = new StringBuffer();
String params = "flang=" + flang + "&lang=" + language + "&kb=" + kbName;

if (sentence != null && sentCounter == null) {
	if (WordNet.wn.isFile(sentence) == false) // This will fail if the input is not supposed to be a file path but contains a / or \
		show.append(WordNet.wn.sumoSentenceDisplay(sentence, sentence, params));
	else 
		show.append(WordNet.wn.sumoFileDisplay(sentence, "0", params));
}

if (sentence != null && sentCounter != null) 
	show.append(WordNet.wn.sumoFileDisplay(sentence, sentCounter, params));	
%>

<html>
<head>
  <title>SUMO Word Sense/Sentiment Analysis Tool</title>
</head>
<BODY BGCOLOR=#FFFFFF>

    <%
        String pageName = "WordSense";
        String pageString = "SUMO Word Sense/Sentiment Analysis Tool";
    %>
    <%@include file="CommonHeader.jsp" %>

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
          