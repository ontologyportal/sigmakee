<%@ page import="java.io.*, java.nio.file.*, com.articulate.sigma.KBmanager" %>
<%
    response.setContentType("text/plain; charset=UTF-8");

    String name = request.getParameter("name");
    if (name == null || !name.endsWith(".tq") || name.contains("..") || name.contains("/") || name.contains("\\")) {
        response.setStatus(400);
        out.print("Bad request");
        return;
    }

    String base = KBmanager.getMgr().getPref("inferenceTestDir");
    if (base == null) {
        response.setStatus(500);
        out.print("inferenceTestDir not configured");
        return;
    }

    Path baseDir = Paths.get(base).toAbsolutePath().normalize();
    Path file = baseDir.resolve(name).normalize();
    if (!file.startsWith(baseDir) || !Files.exists(file)) {
        response.setStatus(404);
        out.print("Test not found");
        return;
    }

    response.setHeader("Content-Disposition","inline; filename=\"" + name + "\"");
    try (BufferedReader br = Files.newBufferedReader(file)) {
        String line;
        while ((line = br.readLine()) != null)
            out.println(line);
    }
%>