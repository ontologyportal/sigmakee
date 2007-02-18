<%@include file="Prelude.jsp" %>
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
    //System.out.println("INFO in SystemOnTPTP.jsp");

    String hostname = KBmanager.getMgr().getPref("hostname");
    if (hostname == null) {
        hostname = "localhost";
    }
//-----------------------------------------------------------------------------
//----Code for getting the list of systems
    String responseLine; 
    ArrayList<String> systemList = new ArrayList<String>();
    Hashtable URLParameters = new Hashtable();
    BufferedReader myResponse;

//----Note, using www.tptp.org does not work
    String SystemOnTPTPFormReplyURL =
        "http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTPTPFormReply";

    systemList.add("Choose system");
    URLParameters.put("NoHTML","1");
    URLParameters.put("QuietFlag","-q2");
    URLParameters.put("SubmitButton","ListSystems");
    URLParameters.put("ListStatus","SoTPTP");

    try {
        myResponse = new BufferedReader(new InputStreamReader(
            ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
        while ((responseLine = myResponse.readLine()) != null) {
            systemList.add(responseLine);
        }
        myResponse.close();
    } catch (Exception ioe) {
        System.err.println("Exception: " + ioe.getMessage());
    }
//-----------------------------------------------------------------------------
//----Code for building the query part
    String language = request.getParameter("lang");
    String kbName = request.getParameter("kb");
    KB kb;
    String stmt = request.getParameter("stmt");
    int maxAnswers = 1;
    int timeout = 30; 
    Iterator systemIterator;
    String systemName;
    String quietFlag = request.getParameter("quietFlag");
    String systemChosen = request.getParameter("systemChosen");
    String tstpFormat = request.getParameter("tstpFormat");
    String sanitize = request.getParameter("sanitize");

    if (kbName == null) {
        kb = null;
    } else {
        kb = KBmanager.getMgr().getKB(kbName);
    }
    if (request.getParameter("maxAnswers") != null) {
        maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
    }
    if (request.getParameter("timeout") != null) {
        timeout = Integer.parseInt(request.getParameter("timeout"));
    }
    if (quietFlag == null) {
        quietFlag = "-q2";
    }
    if (systemChosen == null) {
        systemChosen = "";
    }
    if (tstpFormat == null) {
        tstpFormat = "";
    }
    if (sanitize == null) {
        sanitize = "sanitize";
    }
    if (stmt == null || stmt.equalsIgnoreCase("null")) {
        stmt = "(exists (?X) (instance ?X Relation))";
    } else {
        System.out.println(stmt.trim());
    }
%>

    <HEAD>
    <TITLE>Sigma Knowledge Engineering Environment - TPTP</TITLE>
    <!-- <style>@import url(kifb.css);</style> -->
    </HEAD>
    <BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF>

    <FORM name="SystemOnTPTP" ID="SystemOnTPTP" action="SystemOnTPTP.jsp" METHOD="POST">
    <TABLE width="95%" cellspacing="0" cellpadding="0">
    <TR>
    <TD ALIGN=LEFT VALIGN=TOP><IMG SRC="pixmaps/sigmaSymbol-gray.gif"></TD>
    <TD ALIGN=LEFT VALIGN=TOP><img src="pixmaps/logoText-gray.gif"><BR>
        <B>SystemOnTPTP Interface</B></TD>
    <TD VALIGN=BOTTOM></TD>
    <TD> <FONT FACE="Arial, Helvetica" SIZE=-1>
         [ <A HREF="KBs.jsp"><B>Home</B></A>&nbsp;|&nbsp;
           <A HREF="Graph.jsp?kb=<%=kbName %>&lang=<%=language %>"><B>Graph</B></A>&nbsp;|&nbsp;                                                      
           <A HREF="Properties.jsp"><B>Prefs</B></A>&nbsp;]&nbsp;
           <B>KB</B>:&nbsp;
<%
           ArrayList kbnames = new ArrayList();
           kbnames.addAll(KBmanager.getMgr().getKBnames());
           out.println(HTMLformatter.createMenu("kb",kbName,kbnames)); 
%>              
           <B>Language:</B>&nbsp;<%= HTMLformatter.createMenu("lang",language,kb.availableLanguages()) %>
           <BR></TD>
    </TR>
    </TABLE>
    
    <IMG SRC='pixmaps/1pixel.gif' WIDTH=1 HEIGHT=1 BORDER=0><BR>
    <!--FORM name="SystemOnTPTP" ID="SystemOnTPTP" action="<%= SystemOnTPTPFormReplyURL%>" METHOD="POST"-->
    <TEXTAREA ROWS=5 COLS=70" NAME="stmt"><%=stmt%></TEXTAREA><BR>
    <!--TEXTAREA ROWS=5 COLS=70" NAME="FORMULAEProblem"><%=stmt%></TEXTAREA-->
    <!--INPUT TYPE=HIDDEN NAME="ProblemSource" VALUE="FORMULAE"-->
    Maximum answers: <INPUT TYPE=TEXT SIZE=3 NAME="maxAnswers" VALUE="<%=maxAnswers%>">
    Query time limit:<INPUT TYPE=TEXT SIZE=3 NAME="timeout" VALUE="<%=timeout%>">
    <!--INPUT TYPE=TEXT NAME="timeout" VALUE="<%= timeout %>"-->
    System:
<%
    out.println(HTMLformatter.createMenu("systemChosen",systemChosen,
                                         systemList));
%>
    <INPUT TYPE="CHECKBOX" NAME="sanitize" VALUE="sanitize"
<% if (sanitize.equals("sanitize")) { out.print(" CHECKED"); } %>
    >Sanitize
    <BR>
    <INPUT TYPE="CHECKBOX" NAME="tstpFormat" VALUE="-S"
<% if (tstpFormat.equals("-S")) { out.print(" CHECKED"); } %>
    >TPTP&nbsp;format
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q4" 
<% if (quietFlag.equals("-q4")) { out.print(" CHECKED"); } %>
    >Only TPTP format
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q3" 
<% if (quietFlag.equals("-q3")) { out.print(" CHECKED"); } %>
    >Result
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q2"
<% if (quietFlag.equals("-q2")) { out.print(" CHECKED"); } %>
    >Progress
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q1"
<% if (quietFlag.equals("-q1")) { out.print(" CHECKED"); } %>
    >System
    <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q0"
<% if (quietFlag.equals("-q0")) { out.print(" CHECKED"); } %>
    >Everything

    <INPUT TYPE=SUBMIT NAME="request" value="SystemOnTPTP">
    <!--INPUT TYPE=SUBMIT NAME="SubmitButton" value="RunSelectedSystems"-->
    </FORM>
<%
//-----------------------------------------------------------------------------
//----Code for doing the query
    String req = request.getParameter("request");
    boolean syntaxError = false;
    StringBuffer sbStatus = new StringBuffer();
    String kbFileName;
    Formula conjectureFormula;

//----If there has been a request, do it and report result
    if (req != null && !syntaxError) {
        try {
            if (req.equalsIgnoreCase("SystemOnTPTP")) {
                if (systemChosen.equals("Choose%20system")) {
                    out.println("No system chosen");
                } else {
//----Need to check the name exists
                    URLParameters.clear();
                    URLParameters.put("NoHTML","1");
                    URLParameters.put("QuietFlag",quietFlag);
                    URLParameters.put("X2TPTP",tstpFormat);
                    URLParameters.put("IDV","-T");
//----Need to offer automode
                    URLParameters.put("System___System",systemChosen);
                    URLParameters.put("TimeLimit___TimeLimit",
                                      new Integer(timeout));
//----Add KB contents here
                    conjectureFormula = new Formula();
                    conjectureFormula.theFormula = stmt;
                    conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
                    //System.out.println("INFO in SystemOnTPTP.jsp: " + conjectureFormula.theFormula);
                    conjectureFormula.tptpParse(true);
                    //System.out.println("INFO in SystemOnTPTP.jsp: " + conjectureFormula.theTPTPFormula);
                    kbFileName = kb.writeTPTPFile(null,conjectureFormula,
                                                  sanitize.equals("sanitize"));
                    URLParameters.put("ProblemSource","UPLOAD");
                    URLParameters.put("UPLOADProblem",new File(kbFileName));
                    URLParameters.put("SubmitButton","RunSelectedSystems");
            
                    myResponse = new BufferedReader(new InputStreamReader(
                        ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
//----Delete the kbFile
                    (new File(kbFileName)).delete();
%>
                    <PRE>
<%
                    while ((responseLine = myResponse.readLine()) != null) {
                        out.println(responseLine);
                    }
%>
                    </PRE>
<%
                    myResponse.close();
                }
            }
            if (req.equalsIgnoreCase("tell")) {
                Formula statement = new Formula();
                statement.theFormula = stmt;
                String port = KBmanager.getMgr().getPref("port");
                if (port == null)
                    port = "8080";
                String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName;
                sbStatus.append(kb.tell(stmt) + "<P>\n" + statement.htmlFormat(kbHref));
            }
        }
        catch (ParseException e) {
            out.println(e.getMessage());
        }
        catch (IOException ioe) {
            out.println(ioe.getMessage());
        }
    }
%>

    </BODY>
    </HTML>
