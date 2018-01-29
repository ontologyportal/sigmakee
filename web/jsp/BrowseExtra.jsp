<%@ include file="Prelude.jsp" %>
<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzmüller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal

  @param start is the number of the element to being displaying, in the case where
         there are more statements to display than the maximum allowed for a given
         class of user
  @param type is the type of statements to display
  @param arg is the argument position to display, used only when type="arg"
  @param kb is the name of the knowledge base
  @param term is the term name to display
  @param lang is the language in which to generate paraphrases
*/

 StringBuffer show = new StringBuffer();       // Variable to contain the HTML page generated.
 String formattedFormula = null;
 int start = 0;
 String startString = request.getParameter("start");
 if (!StringUtil.emptyString(startString))
     start = Integer.decode(startString).intValue();
 int arg = 1;
 String argString = request.getParameter("arg");
 if (!StringUtil.emptyString(argString))
     arg = Integer.decode(argString).intValue();
 String type = request.getParameter("type");
 Map theMap = null;     // Map of natural language format strings.

 HTMLformatter.kbHref = "http://" + hostname + ":" + port + "/sigma/Browse.jsp" + "?lang=" + language + "?flang=" + flang + "&kb=" + kbName;

 if (kb != null && (term == null || term.equals("")))        // Show statistics only when no term is specified.
    show.append(HTMLformatter.showStatistics(kb));
 else if (kb != null && kb.containsTerm(term)) {                // Build the HTML format for all the formulas in                                                         
    show.append("<title>Sigma KEE - " + term + "</title>\n");   // which the given term appears.
    show.append("<table width='95%'><tr><td width='50%'><FONT face='Arial,helvetica' size=+3><b>");
    if (term != null) {
    	term = term.intern();
        show.append(term);
        show.append("</b></FONT>");
    	if (Character.isLowerCase(term.charAt(0)) || term.endsWith("Fn")) {
    	    Map fm = kb.getFormatMap(language);
    	    String fmValue = null;
    	    if (fm != null)
                fmValue = (String) fm.get(term); 
    	    if (fmValue == null)
                System.out.println("INFO in BrowseBody.jsp: No format map entry for \"" +
                                   term + "\" in language " + language);	   
    	}
    	else {
    	    Map tfm = kb.getTermFormatMap(language);
    	    String tfmValue = null;
    	    if (tfm != null)
                tfmValue = (String) tfm.get(term);
    	    if (tfmValue != null) 
                show.append("(" + tfmValue + ")");	    
    	    else
                System.out.println("INFO in BrowseBody.jsp: No term format map entry for \"" +
                                   term + "\" in language " + language);	   
    	}
        show.append("</td>");
        show.append("</tr></table>\n");
    }
    else
        show.append ("</b></FONT></td></tr></table>\n");

    int limit = 25;
    if (role != null && !role.equalsIgnoreCase("guest")) {
        limit = 200;
    }
    show.append(HTMLformatter.browserSectionFormatLimit(term,"", kb, language,flang,start,limit,arg,type));
 }
%>
<%=show.toString() %><BR>
<%@ include file="Postlude.jsp" %>
