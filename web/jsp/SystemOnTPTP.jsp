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
//----Check if SystemOnTPTP exists in a local copy of TPTPWorld
  String TPTPWorld = KBmanager.getMgr().getPref("tptpHomeDir");
  String SystemOnTPTP =  TPTPWorld + "/SystemExecution/SystemOnTPTP";
  String tptp4X = TPTPWorld + "/ServiceTools/tptp4X";
  boolean tptpWorldExists = (new File(SystemOnTPTP)).exists();
  String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";

//----Code for getting the list of systems
  String responseLine;
  String defaultSystemLocal = "";
  String defaultSystemRemote = "";
  ArrayList<String> systemListLocal = new ArrayList<String>();
  ArrayList<String> systemListRemote = new ArrayList<String>();
  BufferedReader reader;
  BufferedWriter writer;

//----If local copy of TPTPWorld exists, call local SystemOnTPTP
  if (tptpWorldExists) {
    String command = SystemOnTPTP + " " + "-w";
    Process proc = Runtime.getRuntime().exec(command);
    systemListLocal.add("Choose system");
    try {
      reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
//----Read List of Local Systems
      while ((responseLine = reader.readLine()) != null) {
        systemListLocal.add(responseLine);
//----Try use EP as the default system
        if (responseLine.startsWith("EP---")) {
          defaultSystemLocal = responseLine;
        }
      }
      reader.close();
    } catch (Exception ioe) {
      System.err.println("Exception: " + ioe.getMessage());
    }
  }

//----Call RemoteSoT to retrieve remote list of systems
  Hashtable URLParameters = new Hashtable();

//----Note, using www.tptp.org does not work
  String SystemOnTPTPFormReplyURL =
    "http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTPTPFormReply";

  systemListRemote.add("Choose system");
  URLParameters.put("NoHTML","1");
  URLParameters.put("QuietFlag","-q2");
  URLParameters.put("SubmitButton","ListSystems");
  URLParameters.put("ListStatus","SoTPTP");

  try {
    reader = new BufferedReader(new InputStreamReader(
    ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
//----Read List of Remote Systems
    while ((responseLine = reader.readLine()) != null) {
      systemListRemote.add(responseLine);
//----Try use EP as the default system
      if (responseLine.startsWith("EP---")) {
        defaultSystemRemote = responseLine;
      }
    }
    reader.close();
  } catch (Exception ioe) {
    System.err.println("Exception: " + ioe.getMessage());
  }

//-----------------------------------------------------------------------------
//----Code for building the query part
  String language = request.getParameter("lang");
  if ( language == null ) {
    language = "en";
  }
  String kbName = request.getParameter("kb");
  KB kb;
  String stmt = request.getParameter("stmt");
  int maxAnswers = 1;
  int timeout = 30;
  Iterator systemIterator;
  String systemName;
  String quietFlag = request.getParameter("quietFlag");
  String systemChosenLocal = request.getParameter("systemChosenLocal");
  String systemChosenRemote = request.getParameter("systemChosenRemote");
  String location = request.getParameter("systemOnTPTP");  
  String tstpFormat = request.getParameter("tstpFormat");
  String sanitize = request.getParameter("sanitize");
  String systemChosen;

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
  if (systemChosenLocal == null) {
    systemChosenLocal = defaultSystemLocal;
  }
  if (systemChosenRemote == null) {
    systemChosenRemote = defaultSystemRemote;
  }
  if (location == null) {
    if (tptpWorldExists) {
      location = "local";
    } else {
      location = "remote";
    }
  }
  systemChosen = location.equals("local") ? systemChosenLocal : systemChosenRemote;    

  if (tstpFormat == null) {
    tstpFormat = "";
  }
  if (sanitize == null) {
    sanitize = "no";
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
  <script type="text/javascript">//<![CDATA[
    var tstp_dump;
    function openSoTSTP (dump) {
      var tstp_url = 'http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTSTP';
      var tstp_browser = window.open(tstp_url, '_blank');
      tstp_dump = dump;
    }
    function getTSTPDump () {
      return tstp_dump;
    }
<% if (tptpWorldExists) { %>
<%   if (location.equals("local")) { %>
    var current_location = "Local";
<%   } else { %>
    var current_location = "Remote";
<%   } %>
//----Toggle to either the local or remote list by showing one and hiding other
    function toggleList (location) {
      if (current_location == location) {
        return;
      }
      var obj;
      obj = window.document.getElementById("systemList" + current_location);
      if (obj) {
        obj.setAttribute("style","display:none");
      }
      current_location = location;
      obj = window.document.getElementById("systemList" + location);
      if (obj) {
        obj.setAttribute("style","display:inline");
      }
    }
<% } %>
  //]]></script>
  </HEAD>
  <BODY style="face=Arial,Helvetica" BGCOLOR=#FFFFFF">

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
  <TEXTAREA ROWS=5 COLS=70" NAME="stmt"><%=stmt%></TEXTAREA><BR>
  Maximum answers: <INPUT TYPE=TEXT SIZE=3 NAME="maxAnswers" VALUE="<%=maxAnswers%>">
  Query time limit:<INPUT TYPE=TEXT SIZE=3 NAME="timeout" VALUE="<%=timeout%>">
  <BR>
  System:
<%
  String params;
  if (!tptpWorldExists || location.equals("remote")) {
    params = "ID=systemListRemote style='display:inline'";
  } else {
    params = "ID=systemListRemote style='display:none'";
  }
  out.println(HTMLformatter.createMenu("systemChosenRemote",systemChosenRemote,
                                       systemListRemote, params));
%>
<%
  if (tptpWorldExists) {
    if (location.equals("local")) {
      params = "ID=systemListLocal style='display:inline'";
    } else {
      params = "ID=systemListLocal style='display:none'";
    }
    out.println(HTMLformatter.createMenu("systemChosenLocal",systemChosenLocal,
                                         systemListLocal, params)); 
  }
%>
  <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="local"
<% if (!tptpWorldExists) { out.print(" DISABLED"); } %>
<% if (location.equals("local")) { out.print(" CHECKED"); } %>
  onClick="javascript:toggleList('Local');">Local SystemOnTPTP
  <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="remote"
<% if (location.equals("remote")) { out.print(" CHECKED"); } %>
  onClick="javascript:toggleList('Remote');">Remote SystemOnTPTP
  <BR>
  <INPUT TYPE="CHECKBOX" NAME="sanitize" VALUE="yes"
<% if (sanitize.equalsIgnoreCase("yes")) { out.print(" CHECKED"); } %>
  >Sanitize
  <INPUT TYPE="CHECKBOX" NAME="tstpFormat" VALUE="-S"
<% if (tstpFormat.equals("-S")) { out.print(" CHECKED"); } %>
  >TPTP&nbsp;format
  <BR>
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q4"
<% if (quietFlag.equals("-q4")) { out.print(" CHECKED"); } %>
  >Only TPTP format
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q3"
<% if (quietFlag.equals("-q3")) { out.print(" CHECKED"); } %>
  >Result
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q2"
<% if (quietFlag.equals("-q2")) { out.print(" CHECKED"); } %>
  >Progress
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q01"
<% if (quietFlag.equals("-q01")) { out.print(" CHECKED"); } %>
  >System
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q0"
<% if (quietFlag.equals("-q0")) { out.print(" CHECKED"); } %>
  >Everything
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="IDV"
<% if (quietFlag.equals("IDV")) { out.print(" CHECKED"); } %>
  >IDV-Proof tree
  <INPUT TYPE=RADIO NAME="quietFlag" ID="hyperlinkedKIF" VALUE="hyperlinkedKIF"
<% if (quietFlag.equals("hyperlinkedKIF")) { out.print(" CHECKED"); } %>
  >Hyperlinked KIF
  <INPUT TYPE=SUBMIT NAME="request" value="SystemOnTPTP">
  </FORM>
  <hr>
<%
//-----------------------------------------------------------------------------
//----Code for doing the query
  String req = request.getParameter("request");
  boolean syntaxError = false;
  StringBuffer sbStatus = new StringBuffer();
  String kbFileName;
  Formula conjectureFormula;
//----Result of query (passed to tptp4X then passed to HTMLformatter.formatProofResult)
  String result = "";
  String command;
  Process proc;

//----If there has been a request, do it and report result
  if (req != null && !syntaxError) {
    try {
//----Add KB contents here
      conjectureFormula = new Formula();
      conjectureFormula.theFormula = stmt;
      conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
      //System.out.println("INFO in SystemOnTPTP.jsp: " + conjectureFormula.theFormula);
      conjectureFormula.tptpParse(true,kb);
      //System.out.println("INFO in SystemOnTPTP.jsp: " + conjectureFormula.getTheTptpFormulas());
      kbFileName = kb.writeTPTPFile(null,
                                    conjectureFormula,
                                    sanitize.equalsIgnoreCase("yes"),
                                    systemChosen);
//-----------------------------------------------------------------------------
//----Call RemoteSoT
      if (location.equals("remote")) {
        if (req.equalsIgnoreCase("SystemOnTPTP")) {
          if (systemChosen.equals("Choose%20system")) {
            out.println("No system chosen");
//          } else if (quietFlag.equals("hyperlinkedKIF")) {
//            out.println("HyperlinkedKIF output not supported for remote SystemOnTPTP");
          } else {
//----Need to check the name exists
            URLParameters.clear();
            URLParameters.put("NoHTML","1");
            if (quietFlag.equals("IDV")) {
              URLParameters.put("IDV","-T");
              URLParameters.put("QuietFlag","-q4");
              URLParameters.put("X2TPTP",tstpFormat);
            } else if (quietFlag.equals("hyperlinkedKIF")) {
              URLParameters.put("QuietFlag","-q3");
              URLParameters.put("X2TPTP","-S");
            }else {
              URLParameters.put("QuietFlag",quietFlag);
              URLParameters.put("X2TPTP",tstpFormat);
            }
//----Need to offer automode
            URLParameters.put("System___System",systemChosen);
            URLParameters.put("TimeLimit___TimeLimit",
                              new Integer(timeout));
            URLParameters.put("ProblemSource","UPLOAD");
            URLParameters.put("UPLOADProblem",new File(kbFileName));
            URLParameters.put("SubmitButton","RunSelectedSystems");
  
            reader = new BufferedReader(new InputStreamReader(
                         ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
            out.println("(Remote SystemOnTPTP call)");
            out.println("<PRE>");
            while ((responseLine = reader.readLine()) != null) {
              result += responseLine + "\n";
              if (!quietFlag.equals("hyperlinkedKIF")) { out.println(responseLine); }
            }
            out.println("</PRE>");
            reader.close();
//-----------------------------------------------------------------------------
//----Calling remote tptp4X 
            /*
            if (quietFlag.equals("hyperlinkedKIF")) {
              URLParameters.clear();
              URLParameters.put("NoHTML","1");
              URLParameters.put("X2TPTP",tstpFormat);
              URLParameters.put("QuietFlag","-q0");
              URLParameters.put("System___System","tptp4X---0.0");
              URLParameters.put("TimeLimit___TimeLimit", new Integer(30));
              URLParameters.put("ProblemSource","FORMULAE");
              URLParameters.put("FORMULAEProblem",result);
              URLParameters.put("SubmitButton","RunSelectedSystems");
              reader = new BufferedReader(new InputStreamReader(
                           ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
              while ((responseLine = reader.readLine()) != null) {
                result += responseLine + "\n";
              }
              reader.close();
              out.println(HTMLformatter.formatProofResult(result,
                                                          stmt,
                                                          stmt,
                                                          lineHtml,
                                                          kbName,
                                                          language));       
            }
            */
          }
        }
        if (req.equalsIgnoreCase("tell")) {
          Formula statement = new Formula();
          statement.theFormula = stmt;
          String port = KBmanager.getMgr().getPref("port");
          if (port == null) {
            port = "8080";
          }
          String kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp?kb=" + kbName;
          sbStatus.append(kb.tell(stmt) + "<P>\n" + statement.htmlFormat(kbHref));
        }
      } else {
//-----------------------------------------------------------------------------
//----Call local copy of TPTPWorld instead of using RemoteSoT
        if (systemChosen.equals("Choose%20system")) {
          out.println("No system chosen");
        } else {
          if (quietFlag.equals("hyperlinkedKIF")) {
            command = SystemOnTPTP + " " +
                      "-q3"        + " " +  // quietFlag
                      systemChosen + " " + 
                      timeout      + " " +
                      "-S"         + " " +  //tstpFormat
                      kbFileName;
          } else {
            command = SystemOnTPTP + " " + 
                      quietFlag    + " " + 
                      systemChosen + " " + 
                      timeout      + " " + 
                      tstpFormat   + " " +
                      kbFileName;
          }
          out.println("(Local SystemOnTPTP call)");
          proc = Runtime.getRuntime().exec(command);
          reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          out.println("<PRE>");
          while ((responseLine = reader.readLine()) != null) {
            result += responseLine + "\n";
            if (!quietFlag.equals("hyperlinkedKIF")) { out.println(responseLine); }
          }
          out.println("</PRE>");
          reader.close();
//-----------------------------------------------------------------------------
//----Calling local tptp4X (if tptpWorldExists and toggle button is on "local")
/*
          if (quietFlag.equals("hyperlinkedKIF")) {
            command = tptp4X    + " " + 
                      "-f sumo" + " " +
                      "--";
            proc = Runtime.getRuntime().exec(command);
            writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            writer.write(result);
            writer.flush();
            writer.close();
            result = "";
            while ((responseLine = reader.readLine()) != null) {
              result += responseLine + "\n";
            }
            reader.close();
            out.println(HTMLformatter.formatProofResult(result,
                                                        stmt,
                                                        stmt,
                                                        lineHtml,
                                                        kbName,
                                                        language));       
          }
*/
        }
      }
      if (quietFlag.equals("hyperlinkedKIF")) {
        if (tptpWorldExists) {
          command = tptp4X    + " " + 
                    "-f sumo" + " " +
                    "--";
          proc = Runtime.getRuntime().exec(command);
          writer = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
          reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          writer.write(result);
          writer.flush();
          writer.close();
          result = "";
          while ((responseLine = reader.readLine()) != null) {
            result += responseLine + "\n";
          }
          reader.close();
          out.println(HTMLformatter.formatProofResult(result,
                                                      stmt,
                                                      stmt,
                                                      lineHtml,
                                                      kbName,
                                                      language));       
        } else {
          out.println("Hyperlinked KIF output not supported for remote SystemOnTPTP at this time.  Need local installation of TPTPWorld.");
        }
      }
//----Delete the kbFile
      (new File(kbFileName)).delete();
    } catch (IOException ioe) {
    out.println(ioe.getMessage());
    }
  }
%>
<p>

<%@ include file="Postlude.jsp" %>
   </BODY>
   </HTML>
