<%@ page language="java" contentType="text/html; charset=US-ASCII"
    pageEncoding="US-ASCII"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=US-ASCII">
<title>Register</title>
</head>
<body>
<%
/** This code is copyright Teknowledge (c) 2003, Articulate Software (c) 2003-2017,
    Infosys (c) 2017-present.

    This software is released under the GNU Public License
    <http://www.gnu.org/copyleft/gpl.html>.

    Please cite the following article in any publication with references:

    Pease A., and Benzm?ller C. (2013). Sigma: An Integrated Development Environment
    for Logical Theories. AI Communications 26, pp79-97.  See also
    http://github.com/ontologyportal
*/
%>

<form method="post" action="CreateUser.jsp">
<p>
<table align="left" border="0" >
<TR>
  <TD colspan="2">
    All fields are required<P>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>first/given name:</B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="firstName" TYPE="TEXT" MAXLENGTH="20" SIZE="10"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>last/surname:</B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="lastName" TYPE="TEXT" MAXLENGTH="20" SIZE="10"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>User name:</B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="userName" TYPE="TEXT" MAXLENGTH="20" SIZE="10"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>Password:</B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="password" TYPE="Password" MAXLENGTH="20" SIZE="6"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>Organization:</B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="organization" TYPE="TEXT" MAXLENGTH="20" SIZE="10"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>Email:</B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="email" TYPE="TEXT" MAXLENGTH="40" SIZE="40"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=TOP ALIGN=RIGHT>
	<B>Say briefly why you're not a robot: </B>
  </TD>

  <TD VALIGN=TOP>
	<B><INPUT NAME="notRobot" TYPE="TEXT" MAXLENGTH="80" SIZE="80"></B>
  </TD>
</TR>

<TR>
  <TD VALIGN=CENTER>
	<B><INPUT VALUE="Register" TYPE="SUBMIT"></B>
  </TD>
</TR>
</table>
</form>

</body>
</html>