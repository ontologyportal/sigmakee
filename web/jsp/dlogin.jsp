<HTML>
<HEAD>
<BODY>
<CENTER><H2>Delphi Decision-maker</H2></CENTER>
<CENTER>
    <IMG SRC="head.jpg" WIDTH=200>
</CENTER><P>

<FORM METHOD="POST"  ACTION="Projects.jsp">
<TABLE ALIGN=CENTER BORDER="0" >
<TR>
  <TD VALIGN=TOP ALIGN=RIGHT><B>User name:</B></TD>
  <TD VALIGN=TOP><B><INPUT NAME="userName" TYPE="TEXT" MAXLENGTH="10" SIZE="10"></B></TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT><B>Password:</B></TD>
  <TD VALIGN=TOP><B><INPUT NAME="password" TYPE="Password" MAXLENGTH="6" SIZE="6"></B></TD>
</TR>

<TR>
  <TD VALIGN=CENTER><B><INPUT name="login" VALUE="Log In" TYPE="SUBMIT"></B></TD>
  <TD VALIGN=CENTER><B><INPUT name="newUser" VALUE="New User" TYPE="SUBMIT"></B></TD>
</TR>
</TABLE>
</FORM><P>

<%
    StringBuffer show = new StringBuffer();       // Variable to contain the HTML code generated.
    String message = request.getParameter("message");
    if (message != null && message != "") 
        show.append(message + "\n");
%>

<center>Log in with a user name and password to begin using the Delphi Decision-maker.</center><P>

The Delphi Decision-maker allows a group of users to develop a matrix of decisions and the criteria
that go into making decisions.  The system uses numerical weights, supplied by users, that indicate
the importance of each critereon, and the degree to which each critereon is satisfied by a given
decision.  The system averages the matrices to calculate a group decision.<P>

Users are expected to consider changing their matrices when looking at the group contribution, if
there are criteria they have not considered, or weights that on reflection should be altered.  
Consensus is not a requirement, but users should think carefully when their weights or criteria are
very different than that of the group as a whole.<P>

<P>
 <%=show.toString() %><BR>
</BODY>
</HTML>
