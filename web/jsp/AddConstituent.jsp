<%@ include file="Prelude.jsp" %>

<%
/** This code is copyright Articulate Software (c) 2003.  Some portions
copyright Teknowledge (c) 2003 and reused under the terms of the GNU license.
This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.
Users of this code also consent, by use of this code, to credit Articulate Software
and Teknowledge in any writings, briefings, publications, presentations, or 
other representations of any software which incorporates, builds on, or uses this 
code.  Please cite the following article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, 
in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.
*/

  // KBmanager mgr = KBmanager.getMgr();
  if (!mgr.getPref("userRole").equalsIgnoreCase("administrator")) 
       response.sendRedirect("KBs.jsp");     
  else {
      System.out.println("ENTER AddConstituent.jsp");
      StringBuilder result = new StringBuilder();
      String kbDir = mgr.getPref("kbDir");
      File kbDirFile = new File(kbDir);
      System.out.println("INFO in AddConstituent.jsp: KB dir: " + kbDir);
      MultipartParser mpp = null;
      String kbName = "";
      int postSize = Integer.MAX_VALUE;
      Part requestPart = null;
      String fileName = "";
      String baseName = "";
      String extension = "";
      String overwrite = "";
      File existingFile = null;
      File outfile = null;
      long writeCount = -1L;

      try {  
          boolean isError = false;
          System.out.println("INFO in AddConstituent.jsp: request == " + request);
          mpp = new MultipartParser(request, postSize, true, true);
          System.out.println("INFO in AddConstituent.jsp: mpp == " + mpp);

          while ((requestPart = mpp.readNextPart()) != null) {
              String paramName = requestPart.getName();
              if (paramName == null) 
                  paramName = "";
              if (requestPart.isParam()) {
                  ParamPart pp = (ParamPart) requestPart;
                  if (paramName.equalsIgnoreCase("kb"))
                      kbName = pp.getStringValue();
                  else if (paramName.equalsIgnoreCase("overwrite"))
                      overwrite = pp.getStringValue();
              }
              else if (requestPart.isFile()) {
                  FilePart fp = (FilePart) requestPart;
                  fileName = fp.getFileName();
                  int lidx = fileName.lastIndexOf(".");
                  baseName = ((lidx != -1)
                              ? fileName.substring(0, lidx)
                              : fileName);
                  extension = ((lidx != -1)
                              ? fileName.substring(lidx,fileName.length())
                              : ".kif");
                  existingFile = new File(kbDirFile, (baseName + extension));

                  System.out.println("INFO in AddConstituent.jsp: filename: " + fileName);
                  outfile = StringUtil.renameFileIfExists(existingFile);
                  FileOutputStream fos = new FileOutputStream(outfile);
                  BufferedOutputStream bos = new BufferedOutputStream(fos);
                  writeCount = -1L;
                  try {
                      writeCount = fp.writeTo(bos);
                      bos.flush();
                      bos.close();
                      fos.close();
                  }
                  catch (Exception ioe) {
                      ioe.printStackTrace();
                  }
              }
          }

          String errStr = "";
          if (StringUtil.isNonEmptyString(overwrite)
              && overwrite.equalsIgnoreCase("yes")
              && !existingFile.getCanonicalPath().equalsIgnoreCase(outfile.getCanonicalPath())) {
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
              if (StringUtil.emptyString(kbName)) {
                  errStr = "Error: No knowledge base name specified";
              }
              else if ((outfile == null) || !outfile.canRead()) 
                  errStr = "Error: The constituent file could not be saved or cannot be read";
          }
          if (StringUtil.isNonEmptyString(errStr)) {
              mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
              System.out.println(errStr);
              isError = true;
              response.sendRedirect("KBs.jsp"); 
          }
          else {
              if (mgr.getKB(kbName) == null) 
                  mgr.addKB(kbName);
              KB kb = mgr.getKB(kbName);
              // Remove the constituent, if it is already present.
              for (ListIterator lit = kb.constituents.listIterator(); lit.hasNext();) {
                  String constituent = (String) lit.next();
                  if (StringUtil.isNonEmptyString(baseName) && constituent.contains(baseName))
                      lit.remove();
              }
              result.append(kb.addConstituent(outfile.getCanonicalPath(),true,false));
              if (mgr.getPref("cache").equalsIgnoreCase("yes")) 
                  result.append(kb.cache());                          
              kb.loadVampire();
              KBmanager.getMgr().writeConfiguration();
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

