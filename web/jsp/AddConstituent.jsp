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

if (!role.equalsIgnoreCase("admin"))
    response.sendRedirect("KBs.jsp");
else {
    String kbDir = mgr.getPref("kbDir");
    File kbDirFile = new File(kbDir);
    MultipartParser mpp = null;
    int postSize = Integer.MAX_VALUE;
    Part requestPart = null;
    String fileName = "";
    String baseName = "";
    String overwrite = mgr.getPref("overwrite");
    boolean overwriteP = (StringUtil.isNonEmptyString(overwrite)
                          && overwrite.equalsIgnoreCase("yes"));
    String extension = "";
    File existingFile = null;
    File outfile = null;
    long writeCount = -1L;

    try {
        boolean isError = false;
        mpp = new MultipartParser(request, postSize, true, true);
        String paramName;
        ParamPart pp;
        FilePart fp;
        int lidx;
        while ((requestPart = mpp.readNextPart()) != null) {
            paramName = requestPart.getName();
            if (paramName == null)
                paramName = "";
            if (requestPart.isParam()) {
                pp = (ParamPart) requestPart;
                if (paramName.equalsIgnoreCase("kb"))
                    kbName = pp.getStringValue();
            }
            else if (requestPart.isFile()) {
                fp = (FilePart) requestPart;
                fileName = fp.getFileName();
                lidx = fileName.lastIndexOf(".");
                baseName = ((lidx != -1)
                            ? fileName.substring(0, lidx)
                            : fileName);
                extension = ((lidx != -1)
                            ? fileName.substring(lidx,fileName.length())
                            : ".kif");
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
        if (overwriteP && !existingFile.getCanonicalPath().equalsIgnoreCase(outfile.getCanonicalPath())) {
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
            if (!mgr.existsKB(kbName))
                newKB = true;
            else
                kb = mgr.getKB(kbName);

            if (newKB) {
                List<String> list = new ArrayList<String>();
                list.add(outfile.getCanonicalPath());
                mgr.loadKB(kbName, list);
            }
            else { // Remove the constituent, if it is already present.
                //ListIterator<String> lit = kb.constituents.listIterator();
                //while (lit.hasNext()) {
                //    String constituent = lit.next();
                //    if (StringUtil.isNonEmptyString(baseName) && constituent.contains(baseName))
                //        lit.remove();
                //}
                //kb.addNewConstituent(outfile.getCanonicalPath());
                kb.addConstituent(outfile.getCanonicalPath());
                kb.checkArity();
                if (mgr.getPref("cache").equalsIgnoreCase("yes")) {
                    kb.kbCache.buildCaches();
//                    kb.kbCache.writeCacheFile();
                }
                kb.loadEProver();
                KBmanager.getMgr().writeConfiguration();
            }
        }
        if (!isError)
            response.sendRedirect("Manifest.jsp?kb=" + kbName);
    }
    catch (Exception e) {
        String errStr = "ERROR in AddConstituent.jsp: " + e.getMessage();
        mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
        System.out.println(errStr);
        System.out.println("  kbName == " + kbName);
        System.out.println("  fileName == " + fileName);
        e.printStackTrace();
        response.sendRedirect("KBs.jsp");
    }
}
%>

