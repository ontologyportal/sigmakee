<%@ page import="com.articulate.sigma.KBmanager" %>
<%@ page import="java.io.File" %>
<%@ include file="fragments/universal/Prelude.jspf" %>
<html>
<head>
    <title>SUMO Downloads</title>
</head>
<body bgcolor="#FFFFFF">
<%
    String pageName = "Downloads";
    String pageString = "SUMO Downloads";
    String[] downloadFiles = {
            "SUMO.tptp",
            "SUMO.tff",
            "SUMO_modals.thf",
            "SUMO_plain.thf"
    };
    File kbDirectory = new File(KBmanager.configuration.getKbDir());
%>
<%@ include file="fragments/universal/CommonHeader.jspf" %>

<p>Generated SUMO files for use with TPTP-compatible theorem provers.</p>

<table border="1" cellpadding="6" cellspacing="0">
    <tr>
        <th>File</th>
        <th>Format</th>
        <th>Size</th>
    </tr>
<%
    for (String downloadFile : downloadFiles) {
        File file = new File(kbDirectory, downloadFile);
        boolean available = file.isFile();
        String format;
        if (downloadFile.endsWith(".tptp")) format = "TPTP FOF";
        else if (downloadFile.endsWith(".tff")) format = "TPTP TFF";
        else if (downloadFile.contains("modals")) format = "TPTP THF (modal)";
        else format = "TPTP THF (plain)";
%>
    <tr>
        <td>
        <% if (available) { %>
            <a href="DownloadServlet?file=<%=downloadFile%>"><%=downloadFile%></a>
        <% } else { %>
            <%=downloadFile%> (not available)
        <% } %>
        </td>
        <td><%=format%></td>
        <td><%=available ? String.format("%,d bytes", file.length()) : "-"%></td>
    </tr>
<%
    }
%>
</table>

</body>
</html>
