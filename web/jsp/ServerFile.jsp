<%@ page import="java.io.*, java.nio.charset.StandardCharsets, java.nio.file.*, java.util.*" %>
<%
    response.setContentType("application/json; charset=UTF-8");

    String action = request.getParameter("action");
    String pathParam = request.getParameter("path");
    String code = request.getParameter("code");

    try {
        if (action == null) throw new IOException("Missing action");
        if (pathParam == null || pathParam.trim().isEmpty()) throw new IOException("Missing path");

        Path file = Paths.get(pathParam.trim()).toAbsolutePath().normalize();

        String name = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase();
        boolean textLike =
            name.endsWith(".kif") || name.endsWith(".tq") || name.endsWith(".tptp") ||
            name.endsWith(".tff") || name.endsWith(".thf") || name.endsWith(".fof") ||
            name.endsWith(".cnf") || name.endsWith(".p") || name.endsWith(".txt") ||
            name.endsWith(".java") || name.endsWith(".jsp") || name.endsWith(".jspf") ||
            name.endsWith(".js") || name.endsWith(".css") || name.endsWith(".html") ||
            name.endsWith(".xml") || name.endsWith(".properties") || name.endsWith(".sh") ||
            name.endsWith(".md") || name.endsWith(".json") || name.endsWith(".conf");

        if (!textLike) throw new IOException("Refusing non-text-like file: " + file);

        if ("load".equalsIgnoreCase(action)) {
            if (!Files.exists(file)) throw new FileNotFoundException("File not found: " + file);
            if (!Files.isRegularFile(file)) throw new IOException("Not a regular file: " + file);
            if (!Files.isReadable(file)) throw new IOException("File is not readable: " + file);
            if (Files.size(file) > 5_000_000) throw new IOException("File too large: " + file);

            String contents = Files.readString(file, StandardCharsets.UTF_8)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");

            String displayName = file.getFileName().toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");

            String absPath = file.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");

            out.print("{\"success\":true,\"name\":\"" + displayName + "\",\"path\":\"" + absPath + "\",\"contents\":\"" + contents + "\"}");
            return;
        }

        if ("save".equalsIgnoreCase(action)) {
            if (code == null) code = "";

            Path parent = file.getParent();
            if (parent == null) throw new IOException("File has no parent directory: " + file);
            if (!Files.exists(parent)) throw new IOException("Parent directory does not exist: " + parent);
            if (!Files.isWritable(parent)) throw new IOException("Parent directory is not writable: " + parent);
            if (Files.exists(file) && !Files.isWritable(file)) throw new IOException("File is not writable: " + file);

            Path tmp = Files.createTempFile(parent, file.getFileName().toString() + ".", ".tmp");
            Files.writeString(tmp, code, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }

            String absPath = file.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");

            out.print("{\"success\":true,\"path\":\"" + absPath + "\",\"message\":\"Saved\"}");
            return;
        }

        throw new IOException("Unknown action: " + action);
    }
    catch (Exception e) {
        response.setStatus(400);
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        msg = msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
        out.print("{\"success\":false,\"message\":\"" + msg + "\"}");
    }
%>