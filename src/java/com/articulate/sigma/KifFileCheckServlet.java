import com.articulate.sigma.KifFileChecker;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/CheckKifFile")
@MultipartConfig
public class KifFileCheckServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        Part filePart = request.getPart("kifFile");
        String fileName = filePart.getSubmittedFileName();

        if (fileName == null || !fileName.toLowerCase().endsWith(".kif")) {
            request.setAttribute("errorMessage", "Only .kif files are allowed.");
            request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);
            return;
        }

        // Store uploaded file temporarily
        File tmp = File.createTempFile("upload_", "_" + fileName);
        try (InputStream in = filePart.getInputStream();
             OutputStream outStream = new FileOutputStream(tmp)) {
            in.transferTo(outStream);
        }

        // Validate syntax
        List<String> errors = KifFileChecker.check(tmp);

        // Read raw file contents to display
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(tmp))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        // Put results into request scope
        request.setAttribute("fileName", fileName);
        request.setAttribute("fileContent", lines);
        request.setAttribute("errors", errors);

        // Forward back to JSP for display
        request.getRequestDispatcher("/CheckKifFile.jsp").forward(request, response);

        // Cleanup
        tmp.delete();
    }
}