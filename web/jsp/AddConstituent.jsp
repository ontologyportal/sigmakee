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
*/

boolean makeGlobal = false;
String kbDir = null;

// Default: user KB dir
String home = System.getProperty("user.home");
String userKbDir = home + "/.sigmakee/userKBs/" + username;

// Parse multipart form
MultipartParser mpp = null;
int postSize = Integer.MAX_VALUE;
Part requestPart = null;
String fileName = "";
String baseName = "";
String extension = "";
File existingFile = null;
File outfile = null;
long writeCount = -1L;

String overwrite = mgr.getPref("overwrite");
boolean overwriteP = (StringUtil.isNonEmptyString(overwrite)
                      && overwrite.equalsIgnoreCase("yes"));

try {
    boolean isError = false;
    mpp = new MultipartParser(request, postSize, true, true);
    String paramName;
    ParamPart pp;
    FilePart fp;
    int lidx;

    while ((requestPart = mpp.readNextPart()) != null) {
        paramName = requestPart.getName();
        if (paramName == null) paramName = "";
        if (requestPart.isParam()) {
            pp = (ParamPart) requestPart;
            if (paramName.equalsIgnoreCase("kb")) {
                kbName = pp.getStringValue();
            }
            else if (paramName.equalsIgnoreCase("makeGlobal")) {
                makeGlobal = "true".equalsIgnoreCase(pp.getStringValue());
            }
        }
        else if (requestPart.isFile()) {
            fp = (FilePart) requestPart;
            fileName = fp.getFileName();
            lidx = fileName.lastIndexOf(".");
            baseName = ((lidx != -1)
                        ? fileName.substring(0, lidx)
                        : fileName);
            extension = ((lidx != -1)
                        ? fileName.substring(lidx)
                        : ".kif");

            // Decide target directory now
            if (makeGlobal && role.equalsIgnoreCase("admin")) {
                kbDir = mgr.getPref("kbDir"); // global
            } else {
                kbDir = userKbDir;            // user-specific
            }

            File kbDirFile = new File(kbDir);
            kbDirFile.mkdirs();

            existingFile = new File(kbDirFile, (baseName + extension));
            System.out.println("INFO in AddConstituent.jsp: filename: " + fileName);

            outfile = StringUtil.renameFileIfExists(existingFile);
            try (OutputStream fos = new FileOutputStream(outfile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                writeCount = -1L;
                writeCount = fp.writeTo(bos);
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    String errStr = "";
    if (overwriteP && existingFile != null &&
        !existingFile.getCanonicalPath().equalsIgnoreCase(outfile.getCanonicalPath())) {
        boolean overwriteSucceeded = false;
        try {
            if (existingFile.delete() && outfile.renameTo(existingFile)) {
                outfile = existingFile;
                overwriteSucceeded = outfile.canRead();
            }
        }
        catch (Exception owex) {
            owex.printStackTrace();
        }
        if (!overwriteSucceeded)
            errStr = "Error: Could not overwrite existing consituent file";
    }

    if (StringUtil.emptyString(errStr)) {
        if (StringUtil.emptyString(kbName))
            errStr = "Error in AddConstituent.jsp: No knowledge base name specified";
        else if ((outfile == null) || !outfile.canRead())
            errStr = "Error in AddConstituent.jsp: The constituent file could not be saved or cannot be read";
    }

    if (StringUtil.isNonEmptyString(errStr)) {
        mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
        System.err.println(errStr);
        isError = true;
        response.sendRedirect("KBs.jsp");
    }
    else {
        boolean newKB = false;
        kb = null;

        if (makeGlobal && role.equalsIgnoreCase("admin")) {
            if (!mgr.existsKB(kbName)) newKB = true;
            else kb = mgr.getKB(kbName);
        } else {
            UserKBmanager userMgr = (UserKBmanager) session.getAttribute("kbManager");
            if (userMgr != null) {
                if (!userMgr.existsKB(kbName)) newKB = true;
                else kb = userMgr.getKB(kbName);
            }
        }

        List<String> list = new ArrayList<String>();
        list.add(outfile.getCanonicalPath());

        if (newKB) {
            if (makeGlobal && role.equalsIgnoreCase("admin")) {
                mgr.loadKB(kbName, list);
                KBmanager.getMgr().writeConfiguration();
            } else {
                UserKBmanager userMgr = (UserKBmanager) session.getAttribute("kbManager");
                if (userMgr != null) {
                    userMgr.loadKB(kbName, list);
                    userMgr.writeUserConfiguration();
                    userMgr.saveSerialized(); 
                }
            }
        } else {
            if (makeGlobal && role.equalsIgnoreCase("admin")) {
                kb.addConstituent(outfile.getCanonicalPath());
                if (mgr.getPref("cache").equalsIgnoreCase("yes")) {
                    kb.kbCache.buildCaches();
                }
                kb.checkArity();
                kb.loadEProver();
                KBmanager.getMgr().writeConfiguration();
            } else {
                UserKBmanager userMgr = (UserKBmanager) session.getAttribute("kbManager");
                if (userMgr != null && kb != null) {
                    kb.addConstituent(outfile.getCanonicalPath());
                    if (userMgr.getPref("cache").equalsIgnoreCase("yes")) {
                        kb.kbCache.buildCaches();
                    }
                    kb.checkArity();
                    kb.loadEProver();
                    userMgr.writeUserConfiguration();
                    userMgr.saveSerialized();
                }
            }
        }
    }

    if (!isError)
        response.sendRedirect("Manifest.jsp?kb=" + kbName);
}
catch (Exception e) {
    String errStr = "ERROR in AddConstituent.jsp: " + e.getMessage();
    mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
    System.err.println(errStr);
    System.err.println("  kbName == " + kbName);
    System.err.println("  fileName == " + fileName);
    e.printStackTrace();
    response.sendRedirect("KBs.jsp");
}
%>
