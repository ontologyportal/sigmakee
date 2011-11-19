<%@ include file="Prelude.jsp" %>
<%
/** This code is copyright by Peter Denno (c) 2004.  Some portions
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
 	String ccheckPage = request.getParameter("page");
 	if (ccheckPage == null)
    	ccheckPage = "0";
  	String action = request.getParameter("action");
	String override = request.getParameter("override");
  
	StringBuffer show = new StringBuffer();       // Variable to contain the HTML page generated.
  	String kbHref = null;
  	String htmlDivider = "<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><BR>\n";
  	String formattedFormula = null;
  	Map theMap = null;
  	HttpSession hsObj = request.getSession();   
  	hsObj.setMaxInactiveInterval(-1);
  	kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?lang=" + language + "&kb=" + kbName + "&flang=" + flang;
  	InterfaceTPTP.init();
  	ArrayList<String> systemListBuiltIn = InterfaceTPTP.systemListBuiltIn;
  	ArrayList<String> systemListLocal = InterfaceTPTP.systemListLocal;
  	ArrayList<String> systemListRemote = InterfaceTPTP.systemListRemote;
  	String defaultSystemBuiltIn = InterfaceTPTP.defaultSystemBuiltIn;
  	String defaultSystemLocal = InterfaceTPTP.defaultSystemLocal;
  	String defaultSystemRemote = InterfaceTPTP.defaultSystemRemote;
  
%>

<script type="text/javascript">//<![CDATA[
  var tstp_dump;
  var chosenLocation = "Local";                      
  function openSoTSTP (dump) {
    var tstp_url = 'http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTSTP';
    var tstp_browser = window.open(tstp_url, '_blank');
    tstp_dump = dump;
  }

  function getTSTPDump () {
    return tstp_dump;
  }
  
  function toggleList (location) {
  		chosenLocation = location;       

        var obj;
        
        obj = window.document.getElementById("systemListLocal");
        obj.style.display='none';
        obj = window.document.getElementById("systemListBuiltIn");
        obj.style.display='none';
        obj = window.document.getElementById("systemListRemote");
		obj.style.display='none';        
        obj = window.document.getElementById("systemList" + location);
        obj.style.display='inline';
  }
  
  function submit (location) {
  	
  }
</script>

<html>                                             
<HEAD><TITLE> Knowledge base Browser</TITLE></HEAD>
<BODY BGCOLOR="#FFFFFF">
<FORM action="CCheck.jsp">
    <table width="95%" cellspacing="0" cellpadding="0">
        <tr>
            <td align="left" valign="top"><img src="pixmaps/sigmaSymbol-gray.gif"></td>
            <td>&nbsp;</td>
            <td align="left" valign="top"><img src="pixmaps/logoText-gray.gif"><br>
                <B>Knowledge Base Diagnostics</B></td>
            <td valign="bottom"></td>
            <td><b>[ <a href="KBs.jsp">Home</b></a>&nbsp;|&nbsp;
                <A href="AskTell.jsp?kb=<%=kbName %>&lang=<%=language %>"><b>Ask/Tell</b></A>&nbsp;|&nbsp;
                <a href="Properties.jsp"><b>Prefs</b></a>&nbsp;
                <B>]</B> <br>
                <img src="pixmaps/1pixel.gif" HEIGHT="3"><br>
                <b>KB:&nbsp;
<%
                ArrayList kbnames = new ArrayList();
                kbnames.addAll(KBmanager.getMgr().getKBnames());
                out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
%>              
                </b>
                <b>Language:&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %></b>
            </td>
        </tr>
    </table>
    <br>
<%
  show = new StringBuffer();
  int pageNum = Integer.parseInt(ccheckPage);
  boolean overrideValue = false;
  if (action != null && action != "")
  	overrideValue = true;
  if (override != null && override.equalsIgnoreCase("true"))
     overrideValue = true;
     
  if (KBmanager.ccheckStatus(kb.name) == CCheckStatus.ONGOING) {
  	show.append(HTMLformatter.formatConsistencyCheck(kb.name + " is currently undergoing checks.  Partial results are available.", KBmanager.ccheckResults(kb.name), language, pageNum));
	show.append("<p>[&nbsp; <a href='CCheck.jsp?kb=" + kb.name + "&lang=" + language + "&page=" + pageNum + "&override=false'>Refresh</a>&nbsp; ] </p>");
  }	
  else if (KBmanager.ccheckStatus(kb.name) == CCheckStatus.DONE)
    show.append(HTMLformatter.formatConsistencyCheck(kb.name + "  has been checked. Results can be found below. [<a href=CCheck.jsp?kb=" + kb.name + "&lang=" + language + "&override=true&page=0>Restart Check</a>]", KBmanager.ccheckResults(kb.name), language, pageNum));
  else if (KBmanager.ccheckStatus(kb.name) == CCheckStatus.QUEUED) {
  	show.append(kb.name + " has been added to the queue for consistency checks.");
	show.append("<p>[&nbsp; <a href='CCheck.jsp?kb=" + kb.name + "&lang=" + language + "&page=" + pageNum + "&override=false'>Refresh</a>&nbsp; ] </p>");
  }
  else if (!overrideValue && KBmanager.ccheckStatus(kb.name) == CCheckStatus.NOCCHECK) {	
  	boolean tptpWorldExists = InterfaceTPTP.tptpWorldExists;
  	boolean builtInExists = InterfaceTPTP.builtInExists;
  
	show.append("Please set timeout value and choose an inference engine.<br>");
	show.append("Query time limit: <input TYPE='TEXT' NAME='timeout' VALUE='30'><BR>");
	show.append("Choose an inference engine:<BR>");
	if (kb.inferenceEngine == null)
	    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='Vampire onclick=\"document.getElementById('SoTPTPControl').style.display='none'\" disabled>Vampire<br>");	    
	else show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='Vampire' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\" checked>Vampire<br>");
	show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='SInE' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\">SInE (+Vampire) (experimental)<BR>");
    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='STP' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\">STP (experimental)<BR>");
    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='STP2' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\">STP2 (experimental)<BR>");
    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='LeoSine' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\">LEO-II with SInE (experimental)<BR>");
    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='LeoLocal' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\">LEO-II local (experimental)<BR>");
    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='LeoGlobal' onclick=\"document.getElementById('SoTPTPControl').style.display='none'\">LEO-II global (experimental)<BR>");
    show.append("<INPUT TYPE=RADIO NAME='inferenceEngine' VALUE='SoTPTP' onclick=\"document.getElementById('SoTPTPControl').style.display='inline'\">System on TPTP<BR>");

    show.append("<DIV ID='SoTPTPControl' style='display:none'><IMG SRC='pixmaps/1pixel.gif' width=30 height=1 border=0>");
    
    if (!tptpWorldExists && !builtInExists) 
		show.append("<INPUT TYPE=RADIO NAME='systemOnTPTP' VALUE='Local' disabled>Local");
    else if (tptpWorldExists)
    	show.append("<INPUT TYPE=RADIO NAME='systemOnTPTP' VALUE='Local' onClick=\"toggleList('Local')\">Local");
    else if (builtInExists)
      	show.append("<INPUT TYPE=RADIO NAME='systemOnTPTP' VALUE='BuiltIn' onClick=\"toggleList('BuiltIn')\">Local");
    
    show.append("<INPUT TYPE=RADIO NAME='systemOnTPTP' VALUE='Remote' onClick=\"toggleList('Remote')\">Remote<br>");

	String params = "";
    show.append("System: ");
    if (tptpWorldExists) {
       	params = "ID=systemListLocal";     
      	show.append(HTMLformatter.createMenu("systemChosenLocal",defaultSystemLocal,
                                         systemListLocal, params)); 
  	}

   	params = "ID=systemListBuiltIn style='display:none'";     
   	show.append(HTMLformatter.createMenu("systemChosenBuiltIn", defaultSystemBuiltIn,
                                         systemListBuiltIn, params));

    params = "ID=systemListRemote style='display:none'";
  	show.append(HTMLformatter.createMenu("systemChosenRemote",defaultSystemRemote,
                                       systemListRemote, params));
    
    show.append("</div>");
    show.append("<br><input type='submit' name='action' value='Submit Consistency Check' />");    
   }
  else if (overrideValue && KBmanager.ccheckStatus(kb.name) == CCheckStatus.NOCCHECK) {
  	String chosenEngine = request.getParameter("inferenceEngine");
  	String systemChosen = "";
  	String timeoutStr = request.getParameter("timeout");
  	boolean ccheck = false;
	String location = "";
	int timeout = 30;	
		
	if (timeoutStr != null && !timeoutStr.equals(""))
		timeout = Integer.parseInt(timeoutStr);
	  	 
	if (chosenEngine == null || chosenEngine.equals("")) 
		show.append("Cannot start consistency check as no inference engine was chosen.");  	 
  	else if (chosenEngine.equals("SoTPTP")) {
  		location = request.getParameter("systemOnTPTP");
  		if (location == null || location.equals(""))
  			show.append("Cannot start consistency check as no System for System on TPTP was chosen.");
  	    else {
  	    	systemChosen = request.getParameter("systemChosen" + location);
  	    	if (systemChosen == null || systemChosen.equals(""))
  	    		show.append("Cannot start consistency check as no System for System on TPTP was chosen.");
  	    	else {
                systemChosen = systemChosen.replace("%2E", ".");
                ccheck = true;
            }
  	    }
  	}
  	else ccheck = true;
  	
  	if (ccheck) {
  		show.append("Chosen inference engine: " + chosenEngine + "<br>"); 
  		if (chosenEngine.equals("SoTPTP"))
  			show.append("Chosen system: " + systemChosen + "<br>");
  		show.append("Entered timeout: " + timeout + " seconds.<br>");
	  	if (KBmanager.initiateCCheck(kb, chosenEngine, systemChosen, location, language, timeout) == CCheckStatus.QUEUED) {	
		  	show.append(kb.name + " has been added to the queue for consistency checks. <br>");
		  	show.append("<p>[&nbsp; <a href='CCheck.jsp?kb=" + kb.name + "&lang=" + language + "&page=" + pageNum + "&override=false'>Refresh</a>&nbsp; ] </p>");	  	
	  	}
	  	else 
	  		show.append("Error trying to start consistency check for " + kb.name + ". Please try again.");
  	}
  }
  else
  	show.append("Error trying to start consistency check for " + kb.name + ". Please try agian.");

  show.append("</form>");
%>
   
<table ALIGN='LEFT' WIDTH='50%'><tr><TD BGCOLOR='#A8BACF'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR><BR>
  <%=show.toString() %><BR>
<%@ include file="Postlude.jsp" %>
</BODY>
</HTML>
