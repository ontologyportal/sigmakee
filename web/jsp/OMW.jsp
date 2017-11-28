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
August 9, Acapulco, Mexico.  See also http://github.com/ontologyportal
*/
  String synset = request.getParameter("synset");
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
                  <BR> <%=welcomeString%></td></tr></table>
        </td><td valign="bottom"></td><td>
<font face="Arial,helvetica" SIZE=-1><b>[ <A href="KBs.jsp">Home</A> ]</b></FONT></td></tr></table>

<h3>Language mappings</h3>

<br><table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<%
  String params = "flang=" + flang + "&lang=" + language + "&kb=" + kbName;
  if (synset != null && synset != "")
      out.println(OMWordnet.displaySynset(kbName,synset,params));           
%>
<BR>
<%
String SUMOterm = "";
switch(synset.charAt(synset.length()-1)) {
case 'n': SUMOterm = WordNet.wn.nounSUMOHash.get(OMWordnet.fromOMWsynset(synset)); break;
case 'v': SUMOterm = WordNet.wn.verbSUMOHash.get(OMWordnet.fromOMWsynset(synset)); break;
case 'a': SUMOterm = WordNet.wn.adjectiveSUMOHash.get(OMWordnet.fromOMWsynset(synset)); break;
case 'r': SUMOterm = WordNet.wn.adverbSUMOHash.get(OMWordnet.fromOMWsynset(synset)); break;
}
String baseSUMOterm = WordNetUtilities.getBareSUMOTerm(SUMOterm);
%>

SUMO mapping: <a href="Browse.jsp?<%= params%>&term=<%= baseSUMOterm %>"><%= baseSUMOterm %></a><P>

<a href="http://www.casta-net.jp/~kuribayashi/cgi-bin/wn-multi.cgi?term=<%=synset %>">Browse</a> Open Multilingual Wordnet site<P>

<small>
Source language data (linked through OMW) from:
<A HREF="http://fjalnet.com/">Albanet</A>,
<A HREF="http://www.globalwordnet.org/AWN/">Arabic WordNet (AWN)</A>,
<A HREF="http://lope.linguistics.ntu.edu.tw/cwn/">Chinese Wordnet (Taiwan)</A>,
<A HREF="http://wordnet.dk/lang">DanNet</A>,
<A HREF="http://wordnet.princeton.edu/">Princeton WordNet</A>,
<A HREF="http://www.pwn.ir/">Persian Wordnet</A>,
<A HREF="http://www.ling.helsinki.fi/en/lt/research/finnwordnet/">FinnWordNet</A>,
<A HREF="http://alpage.inria.fr/%7Esagot/wolf-en.html">WOLF (Wordnet Libre du Fran&#231;ais)</A>,
<A HREF="http://cl.haifa.ac.il/projects/mwn/index.shtml">Hebrew Wordnet</A>,
<A HREF="http://multiwordnet.fbk.eu/english/home.php">MultiWordNet</A>,
<A HREF="http://nlpwww.nict.go.jp/wn-ja/">Japanese Wordnet</A>,
<A HREF="http://adimen.si.ehu.es/web/MCR/">Multilingual Central Repository</A>,
<A HREF="http://wn-msa.sourceforge.net/">Wordnet Bahasa</A>,
<A HREF="http://www.nb.no/spraakbanken/tilgjengelege-ressursar/leksikalske-ressursar">Norwegian Wordnet</A>,
<A HREF="http://plwordnet.pwr.wroc.pl/wordnet/">plWordNet</A>,
<A HREF="https://github.com/arademaker/wordnet-br">OpenWN-PT</A>,
<A HREF="http://th.asianwordnet.org/"></small><P>

<%@ include file="Postlude.jsp" %>
</body>