package com.articulate.sigma;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Downloads generated SUMO prover files from the configured KB directory. */
@WebServlet("/DownloadServlet")
public class DownloadServlet extends HttpServlet {

    private static final Set<String> DOWNLOADABLE_FILES;

    static {
        Set<String> files = new HashSet<>();
        files.add("SUMO.tptp");
        files.add("SUMO.tff");
        files.add("SUMO_modals.thf");
        files.add("SUMO_plain.thf");
        DOWNLOADABLE_FILES = Collections.unmodifiableSet(files);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fileName = request.getParameter("file");
        if (!DOWNLOADABLE_FILES.contains(fileName)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Unknown downloadable file");
            return;
        }

        Path kbDirectory = Paths.get(KBmanager.configuration.getKbDir())
                .toAbsolutePath().normalize();
        Path file = kbDirectory.resolve(fileName).normalize();

        if (!file.startsWith(kbDirectory) || !Files.isRegularFile(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Generated file is not available: " + fileName);
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"");
        response.setContentLengthLong(Files.size(file));

        try (InputStream input = Files.newInputStream(file);
             OutputStream output = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1)
                output.write(buffer, 0, count);
        }
    }
}
