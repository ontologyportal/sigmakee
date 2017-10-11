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
     August 9, Acapulco, Mexico.  See also http://github.com/ontologyportal
     */
    //----Check if Builtin Systems exist
    String systemsDir = KBmanager.getMgr().getPref("systemsDir");
    String systemsInfo = systemsDir + "/SystemInfo";
    boolean builtInExists = (new File(systemsDir)).exists()
            && (new File(systemsInfo)).exists();
    String defaultSystemBuiltIn = "";
    ArrayList<String> systemListBuiltIn = new ArrayList<String>();

    //----If built in Systems Directory exist, call built-in SystemOnTPTP
    if (builtInExists) {
        systemListBuiltIn = SystemOnTPTP.listSystems(systemsDir,"SoTPTP");
        defaultSystemBuiltIn = "EP---0.999";
    }

    //----Check if SystemOnTPTP exists in a local copy of TPTPWorld
    String TPTPWorld = KBmanager.getMgr().getPref("tptpHomeDir");
    InterfaceTPTP.init();
    ArrayList<String> systemListLocal = InterfaceTPTP.systemListLocal;
    ArrayList<String> systemListRemote = InterfaceTPTP.systemListRemote;
    String defaultSystemLocal = InterfaceTPTP.defaultSystemLocal;
    String defaultSystemRemote = InterfaceTPTP.defaultSystemRemote;
    boolean tptpWorldExists = InterfaceTPTP.tptpWorldExists;

    //---keep until debugged
    String SoTPTP = TPTPWorld + "/SystemExecution/SystemOnTPTP";
    String responseLine;
    String SystemOnTPTPFormReplyURL = "http://www.cs.miami.edu/~tptp/cgi-bin/SystemOnTPTPFormReply";
    BufferedReader reader;

    //----Code for building the query part
    String stmt = request.getParameter("stmt");
    int maxAnswers = 1;
    int timeout = 30;

    //---SystemOnTPTP request parameters
    String systemChosenLocal = request.getParameter("systemChosenLocal");
    String systemChosenRemote = request.getParameter("systemChosenRemote");
    String systemChosenBuiltIn = request.getParameter("systemChosenBuiltIn");

    String quietFlag = request.getParameter("quietFlag");
    String location = request.getParameter("systemOnTPTP");
    String tstpFormat = request.getParameter("tstpFormat");
    String sanitize = request.getParameter("sanitize");
    String systemChosen;

    if (request.getParameter("maxAnswers") != null)
        maxAnswers = Integer.parseInt(request.getParameter("maxAnswers"));
    if (request.getParameter("timeout") != null)
        timeout = Integer.parseInt(request.getParameter("timeout"));
    if (quietFlag == null)
        quietFlag = "hyperlinkedKIF";
    if (systemChosenLocal == null)
        systemChosenLocal = defaultSystemLocal;
    if (systemChosenRemote == null)
        systemChosenRemote = defaultSystemRemote;
    if (systemChosenBuiltIn == null)
        systemChosenBuiltIn = defaultSystemBuiltIn;
    if (location == null) {
        if (tptpWorldExists) 
            location = "local";
        else if (builtInExists) 
            location = "local";
        else 
            location = "remote";        
    }
    if (location.equals("local")) {
        if (tptpWorldExists) 
            systemChosen = systemChosenLocal;
        else 
            systemChosen = systemChosenBuiltIn;        
    } 
    else 
        systemChosen = systemChosenRemote;    

    if (tstpFormat == null) 
        tstpFormat = "";    
    if (sanitize == null) 
        sanitize = "no";    
    if (stmt == null || stmt.equalsIgnoreCase("null")) 
        stmt = "(exists (?X) (instance ?X Relation))";
    else 
        System.out.println(stmt.trim());    
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
<%if (tptpWorldExists && location.equals("local")) {%>
    var current_location = "Local";
<%} else if (builtInExists && location.equals("local")) {%>
    var current_location = "BuiltIn";
<%} else {%>
    var current_location = "Remote";
<%}%>
//----Toggle to either the local/builtin/remote list by showing new and hiding current
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
         <A HREF="Graph.jsp?kb=<%=kbName%>&lang=<%=language%>"><B>Graph</B></A>&nbsp;|&nbsp;
         <A HREF="Properties.jsp"><B>Prefs</B></A>&nbsp;]&nbsp;
         <B>KB</B>:&nbsp;
<%
    out.println(HTMLformatter.createKBMenu(kbName));
%>
         <B>Language:</B>&nbsp;<%=HTMLformatter.createMenu("lang", language,
                    kb.availableLanguages())%>
         <BR></TD>
  </TR>
  </TABLE>

  <IMG SRC='pixmaps/1pixel.gif' WIDTH=1 HEIGHT=1 BORDER=0><BR>
  <TEXTAREA ROWS=5 COLS=70" NAME="stmt"><%=stmt%></TEXTAREA><BR>

  <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="local"
<%if (!tptpWorldExists && !builtInExists) {
                out.print(" DISABLED");
            }%>
<%if (location.equals("local")) {
                out.print(" CHECKED");
            }%>
<%if (tptpWorldExists) {
                out.println("onClick=\"javascript:toggleList('Local');\"");
            } else {
                out.println("onClick=\"javascript:toggleList('BuiltIn');\"");
            }%>
  >Local SystemOnTPTP
  <INPUT TYPE=RADIO NAME="systemOnTPTP" VALUE="remote"
<%if (location.equals("remote")) {
                out.print(" CHECKED");
            }%>
  onClick="javascript:toggleList('Remote');">Remote SystemOnTPTP
&nbsp;
  System:
<%
      String params;
      //----Create atp drop down list for local
      if (tptpWorldExists) {
          if (location.equals("local"))
              params = "ID=systemListLocal style='display:inline'";
          else
              params = "ID=systemListLocal style='display:none'";
          out.println(HTMLformatter.createMenu("systemChosenLocal",
                  systemChosenLocal, systemListLocal, params));
      }
      //----Create atp drop down list for builtin
      if (builtInExists && !tptpWorldExists) {
          if (location.equals("local"))
              params = "ID=systemListBuiltIn style='display:inline'";
          else
              params = "ID=systemListBuiltIn style='display:none'";
          out.println(HTMLformatter.createMenu("systemChosenBuiltIn",
                  systemChosenBuiltIn, systemListBuiltIn, params));
      }
      //----Create atp drop down list for remote
      if ((!tptpWorldExists && !builtInExists)
              || location.equals("remote"))
          params = "ID=systemListRemote style='display:inline'";
      else
          params = "ID=systemListRemote style='display:none'";
      out.println(HTMLformatter.createMenu("systemChosenRemote",
              systemChosenRemote, systemListRemote, params));
  %>
  <BR>

  Maximum answers: <INPUT TYPE=TEXT SIZE=3 NAME="maxAnswers" VALUE="<%=maxAnswers%>">
  Query time limit:<INPUT TYPE=TEXT SIZE=3 NAME="timeout" VALUE="<%=timeout%>">
  <BR>
  <INPUT TYPE="hidden" NAME="sanitize" VALUE="yes">
  <INPUT TYPE="hidden" NAME="tstpFormat" VALUE="-S">
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="-q4"
<%if (quietFlag.equals("-q4")) {
                out.print(" CHECKED");
            }%>
  >TPTP Proof
  <INPUT TYPE=RADIO NAME="quietFlag" VALUE="IDV"
<%if (quietFlag.equals("IDV")) {
                out.print(" CHECKED");
            }%>
  >IDV-Proof tree
  <INPUT TYPE=RADIO NAME="quietFlag" ID="hyperlinkedKIF" VALUE="hyperlinkedKIF"
<%if (quietFlag.equals("hyperlinkedKIF")) {
                out.print(" CHECKED");
            }%>
  >Hyperlinked KIF
  <BR>
  <INPUT TYPE=SUBMIT NAME="request" value="Test">
  <INPUT TYPE=SUBMIT NAME="request" value="Ask">
<%
    if (KBmanager.getMgr().getPref("userRole") != null
            && KBmanager.getMgr().getPref("userRole")
                    .equalsIgnoreCase("administrator")) {
%>
    <INPUT type="submit" name="request" value="Tell"><BR>
<%
    }
%>
  </FORM>
  <hr>
<%
    String req = request.getParameter("request");
    boolean syntaxError = false;
    String originalKBFileName = null;
    // As in AskTell:
    //----If there has been a request, do it and report result
    if (req != null && !syntaxError) {
        try {
            if (req.equalsIgnoreCase("tell")) {
                Formula statement = new Formula();
                statement.theFormula = stmt;
                String port = KBmanager.getMgr().getPref("port");
                if ((port == null) || port.equals(""))
                    port = "8080";
                String kbHref = HTMLformatter.createHrefStart()
                        + "/sigma/Browse.jsp?kb=" + kbName;
                out.println("Status: ");
                out.println(kb.tell(stmt) + "<P>\n"    + statement.htmlFormat(kbHref));
            } 
            else if (req.equalsIgnoreCase("test")) {
                StringBuffer sb = new StringBuffer();
                sb = sb.append(InferenceTestSuite.test(kb,
                        systemChosen, out));
                out.println(sb.toString());
            } 
            else if (req.equalsIgnoreCase("Ask")) {
                String lineHtml = "<table ALIGN='LEFT' WIDTH='40%'><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>\n";
                //----Code for doing the query
                String TPTP_QUESTION_SYSTEM = "SNARK---";
                String TPTP_ANSWER_SYSTEM = "Metis---";
                StringBuffer sbStatus = new StringBuffer();
                String kbFileName = null;
                Formula conjectureFormula;
                //----Result of query (passed to tptp4X then passed to HTMLformatter.formatProofResult)
                String result = "";
                String newResult = "";
                String idvResult = "";
                String originalResult = "";
                String command;
                Process proc;
                boolean isQuestion = systemChosen.startsWith(TPTP_QUESTION_SYSTEM);
                String conjectureTPTPFormula = "";

                // Build query: Add KB contents here
                conjectureFormula = new Formula();
                conjectureFormula.theFormula = stmt;
                conjectureFormula.theFormula = conjectureFormula.makeQuantifiersExplicit(true);
                conjectureFormula.tptpParse(true, kb);
                Iterator it = conjectureFormula.getTheTptpFormulas().iterator();
                String theTPTPFormula = (String) it.next();
                String originalConjecture = theTPTPFormula;
                if (isQuestion)
                    conjectureTPTPFormula = "fof(1" + ",question,("    + theTPTPFormula + ")).";
                else
                    conjectureTPTPFormula = "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";

                SUMOKBtoTPTPKB skb = new SUMOKBtoTPTPKB();
                skb.kb = kb;
                originalKBFileName = skb.writeTPTPFile(null, null,
                        sanitize.equalsIgnoreCase("yes"), systemChosen,
                        isQuestion);
                //----Add while loop to check for more answers
                //----If # of answers == maximum answers, exit loop
                //----If last check for an answer failed (no answer found or empty answer list), exit loop
                //----Each loop around, add ld axioms
                ArrayList<Binding> lastAnswer = null;
                ArrayList<Binding> originalAnswer = null;
                int numAnswers = 0;
                TreeSet<TPTPParser.Symbol> symbolsSoFar = new TreeSet(
                        new TPTPParser.SymbolComparator());
                ArrayList<String> ldAxiomsSoFar = new ArrayList();
                ldAxiomsSoFar.addAll(LooksDifferent.getUniqueAxioms());
                //----Create symbol list from entire kbFile
                TreeSet<TPTPParser.Symbol> symbolList = TPTPParser
                        .getSymbolList(originalKBFileName);
                //----While loop start:
                do {
                    originalResult = "";
                    result = "";
                    //----If we found a new set of answers, update query and axiom list
                    if (lastAnswer != null) {
                        out.println("<hr>");   //----Get symbols from lastAnswer                        
                        TreeSet<TPTPParser.Symbol> newSymbols = TPTPParser
                                .getSymbolList(lastAnswer);
                        //----Find uniqueSymbols from lastAnswer not in symbolsSoFar
                        TreeSet<TPTPParser.Symbol> uniqueSymbols = LooksDifferent
                                .getUniqueSymbols(symbolsSoFar,newSymbols);
                        //----symbolsSOFar = uniqueSymbols U symbolsSoFar
                        symbolsSoFar.addAll(uniqueSymbols);
                        //----Get new set of ld axioms from the unique symbols
                        ArrayList<String> ldAxiomsNew = LooksDifferent
                                .addAxioms(uniqueSymbols, symbolList);
                        //----Add ld axioms for those uniqueSymbols to ldAxiomsSoFar
                        ldAxiomsSoFar.addAll(ldAxiomsNew);
                        //----Add last answer to conjecture
                        theTPTPFormula = LooksDifferent
                                .addToConjecture(theTPTPFormula,lastAnswer);
                        //----Create new conjectureTPTPFormula
                        if (isQuestion)
                            conjectureTPTPFormula = "fof(1"    + ",question,(" + theTPTPFormula + ")).";
                        else
                            conjectureTPTPFormula = "fof(1" + ",conjecture,(" + theTPTPFormula + ")).";
                        //----keep originalKBFile intact so that we do not have to keep recreating it, just copy and append to copy then delete copy, only delete original at the end of run
                        //----delete last kbFileName
                        //----kbFileName = originalKBFileName + all ld axioms + conjectureTPTPFormula;
                        //----Copy original kb file
                        kbFileName = kb.copyFile(originalKBFileName);
                        //----Append ld axioms and conjecture to the end
                        kb.addToFile(kbFileName, ldAxiomsSoFar, conjectureTPTPFormula);                        
                        lastAnswer = null;  //----Reset last answer
                    } 
                    else { 
                        kbFileName = kb.copyFile(originalKBFileName);  //----Copy original kb file                        
                        kb.addToFile(kbFileName, null,conjectureTPTPFormula);  //----Append conjecture to the end
                    }                    
                    if (location.equals("remote")) {   //----Call RemoteSoT
                        if (systemChosen.equals("Choose%20system")) 
                            out.println("No system chosen");
                        else {  //----Need to check the name exists                            
                            Hashtable URLParameters = new Hashtable();
                            URLParameters.put("NoHTML", "1");
                            if (quietFlag.equals("IDV")) {
                                URLParameters.put("IDV", "-T");
                                URLParameters.put("QuietFlag", "-q4");
                                URLParameters.put("X2TPTP", tstpFormat);
                            } 
                            else if (quietFlag.equals("hyperlinkedKIF")) {
                                URLParameters.put("QuietFlag", "-q3");
                                URLParameters.put("X2TPTP", "-S");
                            } 
                            else {
                                URLParameters.put("QuietFlag",quietFlag);
                                URLParameters.put("X2TPTP", tstpFormat);
                            }
                            //----Need to offer automode
                            URLParameters.put("System___System",systemChosen);
                            URLParameters.put("TimeLimit___TimeLimit",new Integer(timeout));
                            URLParameters.put("ProblemSource", "UPLOAD");
                            URLParameters.put("UPLOADProblem",new File(kbFileName));
                            URLParameters.put("SubmitButton","RunSelectedSystems");

                            reader = new BufferedReader(new InputStreamReader(
                                            ClientHttpRequest.post(new URL(SystemOnTPTPFormReplyURL),URLParameters)));
                            if (numAnswers == 0)
                                out.println("(Remote SystemOnTPTP call)");
                            out.println("<PRE>");
                            boolean tptpEnd = false;
                            while ((responseLine = reader.readLine()) != null) {
                                if (responseLine.startsWith("Loading IDV"))
                                    tptpEnd = true;
                                if (!responseLine.equals("")
                                        && !responseLine.substring(0, 1).equals("%")
                                        && !tptpEnd)
                                    result += responseLine + "\n";
                                if (tptpEnd && quietFlag.equals("IDV"))
                                    idvResult += responseLine + "\n";
                                originalResult += responseLine + "\n";
                                if (!quietFlag.equals("hyperlinkedKIF")
                                        && !quietFlag.equals("IDV"))
                                    out.println(responseLine);
                            }
                            out.println("</PRE>");
                            reader.close();
                        }
                    } 
                    else if (location.equals("local") && tptpWorldExists) {
                        //----Call local copy of TPTPWorld instead of using RemoteSoT
                        if (systemChosen.equals("Choose%20system"))
                            out.println("No system chosen");
                        else {
                            if (quietFlag.equals("hyperlinkedKIF")) 
                                command = SoTPTP + " " + "-q3" + " "
                                        + // quietFlag
                                        systemChosen + " " + timeout
                                        + " " + "-S" + " " + //tstpFormat
                                        kbFileName;                            
                            else if (quietFlag.equals("IDV")) 
                                command = SoTPTP + " " + "-q4" + " "
                                        + // quietFlag
                                        systemChosen + " " + timeout
                                        + " " + "-S" + " " + //tstpFormat
                                        kbFileName;                        
                            else 
                                command = SoTPTP + " " + quietFlag
                                        + " " + systemChosen + " "
                                        + timeout + " " + tstpFormat
                                        + " " + kbFileName;                            
                            if (numAnswers == 0)
                                out.println("(Local SystemOnTPTP call)");
                            proc = Runtime.getRuntime().exec(command);
                            reader = new BufferedReader(new InputStreamReader(
                                            proc.getInputStream()));
                            out.println("<PRE>");
                            while ((responseLine = reader.readLine()) != null) {
                                if (!responseLine.equals("")
                                        && !responseLine.substring(0, 1).equals("%"))
                                    result += responseLine + "\n";
                                originalResult += responseLine + "\n";
                                if (!quietFlag.equals("hyperlinkedKIF")    && !quietFlag.equals("IDV"))
                                    out.println(responseLine);
                            }
                            out.println("</PRE>");
                            reader.close();
                        }
                    } 
                    else if (location.equals("local")
                            && builtInExists && !tptpWorldExists) {
                        //----Call built in SystemOnTPTP instead of using RemoteSoT or local
                        if (systemChosen.equals("Choose%20system"))
                            out.println("No system chosen");
                        else {   //----Set quiet flag                            
                            String qq;
                            String format;
                            if (quietFlag.equals("IDV")) {
                                qq = "-q4";
                                format = "-S";
                            } 
                            else if (quietFlag.equals("hyperlinkedKIF")) {
                                qq = "-q4";
                                format = "-S";
                            } 
                            else {
                                qq = quietFlag;
                                format = tstpFormat;
                            }
                            result = SystemOnTPTP.SystemOnTPTP(
                                    systemChosen, systemsDir, timeout,
                                    qq, format, kbFileName);
                            originalResult += result;
                            if (numAnswers == 0)
                                out.println("(Built-In SystemOnTPTP call)");
                            out.println("<PRE>");
                            if (!quietFlag.equals("hyperlinkedKIF")
                                    && !quietFlag.equals("IDV"))
                                out.println(result);
                            if (quietFlag.equals("IDV")) {
                                StringTokenizer st = new StringTokenizer(result, "\n");
                                String temp = "";
                                while (st.hasMoreTokens()) {
                                    String next = st.nextToken();
                                    if (!next.equals("") && !next.substring(0, 1).equals("%"))
                                        temp += next + "\n";
                                }
                                result = temp;
                            }
                            out.println("</PRE>");
                        }
                    } 
                    else
                        out.println("INTERNAL ERROR: chosen option not valid: "
                                + location
                                + ".  Valid options are: 'Local SystemOnTPTP, Built-In SystemOnTPTP, or Remote SystemOnTPTP'.");
                    //----If selected prover is not an ANSWER system, send proof to default ANSWER system (Metis)
                    if (!systemChosen.startsWith(TPTP_ANSWER_SYSTEM)) {
                        String answerResult = AnswerFinder.findProofWithAnswers(result,systemsDir);
                        //----If answer is blank, ERROR, or WARNING, do not place in result
                        if (!answerResult.equals("")
                                && !answerResult.startsWith("% ERROR:")
                                && !answerResult.startsWith("% WARNING:"))
                            result = answerResult;
                        //----If ERROR is answer result, report to user
                        if (answerResult.startsWith("% ERROR:"))
                            out.println("==" + answerResult);
                    }
                    if (systemChosen.startsWith(TPTP_QUESTION_SYSTEM)) {
                        //----Procedure if SNARK was chosen
                        String conj = "fof(1" + ",conjecture,("    + theTPTPFormula + ")).";
                        ArrayList<Binding> answer = SystemOnTPTP.getSZSBindings(conj, originalResult);
                        lastAnswer = answer;
                        newResult = TPTP2SUMO.convert(result, answer,false);
                    } 
                    else {
                        //----Procedure if not SNARK (call one answer system: Metis)
                        TPTPParser parser = TPTPParser.parse(new BufferedReader(
                                        new StringReader(result)));
                        lastAnswer = AnswerExtractor.extractAnswers(parser.ftable);
                        //----Get original variable names
                        lastAnswer = SystemOnTPTP.getSZSBindings(conjectureTPTPFormula, lastAnswer);
                        newResult = TPTP2SUMO.convert(result, false);
                    }
                    if (quietFlag.equals("IDV")    && location.equals("remote")) {
                        if (SystemOnTPTP.isTheorem(originalResult)) {
                            int size = SystemOnTPTP.getTPTPFormulaSize(result);
                            if (size == 0)
                                out.println("No solution output by system.  IDV tree unavaiable.");
                            else
                                out.println(idvResult);
                        } 
                        else
                            out.println("Not a theorem.  IDV tree unavailable.");
                    } 
                    else if (quietFlag.equals("IDV") && !location.equals("remote")) {
                        if (SystemOnTPTP.isTheorem(originalResult)) {
                            int size = SystemOnTPTP.getTPTPFormulaSize(result);
                            if (size > 0) {
                                String port = KBmanager.getMgr().getPref("port");
                                if ((port == null) || port.equals(""))
                                    port = "8080";
                                String libHref = HTMLformatter.createHrefStart() + "/sigma/lib";
                                out.println("<APPLET CODE=\"IDVApplet\" archive=\""
                                        + libHref + "/IDV.jar," + libHref + "/TptpParser.jar,"
                                        + libHref + "/antlr-2.7.5.jar,"    + libHref
                                        + "/ClientHttpRequest.jar\"");
                                out.println("WIDTH=800 HEIGHT=100 MAYSCRIPT=true>");
                                out.println("  <PARAM NAME=\"TPTP\" VALUE=\""
                                        + result + "\">");
                                out.println("  Hey, you cant see my applet!!!");
                                out.println("</APPLET>");
                            } 
                            else
                                out.println("No solution output by system.  IDV tree unavaiable.");
                        } 
                        else
                            out.println("Not a theorem.  IDV tree unavailable.");
                    } 
                    else if (quietFlag.equals("hyperlinkedKIF")) {
                        if (originalAnswer == null) 
                            originalAnswer = lastAnswer;
                        else {
                            //----This is not the first answer, that means result has dummy ld predicates, bind conjecture with new answer, remove outside existential
                            if (!lastAnswer.equals("")) {
                                String bindConjecture = "fof(bindConj"
                                        + ", conjecture,(" + LooksDifferent.bindConjecture(originalConjecture,
                                                        originalAnswer,lastAnswer) + ")).";
                                //----With new bindConjecture, take last result, filter out anything with LDs in it, put in prover
                                String axioms = LooksDifferent.filterLooksDifferent(originalResult);
                                //----Redo proof using OneAnswerSystem again
                                String bindProblem = axioms + " " + bindConjecture;
                                String bindResult = AnswerFinder.findProof(bindProblem,systemsDir);
                                newResult = TPTP2SUMO.convert(bindResult, lastAnswer, true);
                            }
                        }
                        boolean isTheorem = SystemOnTPTP.isTheorem(originalResult);
                        boolean isCounterSatisfiable = SystemOnTPTP.isCounterSatisfiable(originalResult);
                        boolean proofExists = SystemOnTPTP.proofExists(originalResult);
                        int timeUsed = SystemOnTPTP.timeUsed(originalResult);
                        if (isTheorem) {
                            if (proofExists)                                     
                                out.println(HTMLformatter.formatProofResult(
                                            newResult, stmt, stmt, lineHtml,kbName, language));
                            else //----Proof does not exist, but was a theorem                                
                                out.println("Answer 1. Yes [Theorem]<br>");
                        } 
                        else if (isCounterSatisfiable)
                            out.println("Answer 1. No [CounterSatisfiable]<br>");

                        else 
                            if (numAnswers == 0)
                                out.println("Answer 1. No<br>");                        
                        out.flush();
                    }
                    //----If lastAnswer != null (we found an answer) && there is an answer (lastAnswer.size() > 0)
                    if (lastAnswer != null && lastAnswer.size() > 0)
                        numAnswers++;
                    //----Add query time limit to while loop break
                } while (numAnswers < maxAnswers && lastAnswer != null
                        && lastAnswer.size() > 0);
            }
        } 
        catch (IOException ioe) {
            out.println(ioe.getMessage());
        }
    }
%>
<p>

<%@ include file="Postlude.jsp" %>
   </BODY>
   </HTML>
