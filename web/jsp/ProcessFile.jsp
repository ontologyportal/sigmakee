<%@ include	file="Prelude.jsp" %>

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
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net
*/

  System.out.println("ENTER ProcessFile.jsp");
  // KBmanager mgr = KBmanager.getMgr();
  if (!role.equalsIgnoreCase("admin")) {
       System.out.println("EXIT ProcessFile.jsp 1");
       response.sendRedirect("KBs.jsp");     
  }
  else {
      StringBuilder result = new StringBuilder();
      String kbDir = mgr.getPref("kbDir");
      File kbDirFile = new File(kbDir);
      //System.out.println("INFO in AddConstituent.jsp: KB dir: " + kbDir);
      MultipartParser mpp = null;
      String kbName = "";
      String ontology = "";
      int postSize = Integer.MAX_VALUE;
      Part requestPart = null;
      String fileName = "";
      String suffix = "";
      String baseName = "";
      boolean overwriteP = false;
      String load = "";
      boolean loadP = false;
      File existingFile = null;
      File outfile = null;
      long writeCount = -1L;
      try {  
          boolean isError = false;

          System.out.println("request == " + request);

          mpp = new MultipartParser(request, postSize, true, true);

          System.out.println("mpp == " + mpp);

          while ((requestPart = mpp.readNextPart()) != null) {
              String paramName = requestPart.getName();
              if (paramName == null) 
                  paramName = "";
              if (requestPart.isParam()) {
                  ParamPart pp = (ParamPart) requestPart;
                  String ppval = pp.getStringValue();
                  if (ppval == null) ppval = "";
                  System.out.println("paramName == " + paramName);
                  System.out.println(" paramVal == " + ppval);
                  if (paramName.equalsIgnoreCase("kb"))
                      // && StringUtil.emptyString(kbName))
                      kbName = ppval;
                  else if (paramName.equalsIgnoreCase("ontology"))
                      // && StringUtil.emptyString(ontology))
                      ontology = ppval;
                  else if (paramName.equalsIgnoreCase("load"))
                      // && StringUtil.emptyString(load))
                      load = ppval;
              }
              else if (requestPart.isFile()) {
                  FilePart fp = (FilePart) requestPart;

                  // First, we copy the data file into the KBs
                  // directory.  This is inefficient if the data file
                  // is already in the KBs directory, but safer than
                  // reading the file directly from the input stream
                  // while inside this loop.
                  fileName = fp.getFileName();
                  int lidx = fileName.lastIndexOf(".");
                  baseName = fileName;
                  suffix = "";
                  if (lidx != -1) {
                      baseName = fileName.substring(0, lidx);
                      suffix = fileName.substring(lidx);
                  }
                  existingFile = new File(kbDirFile, (baseName + suffix));
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
          String overwrite = mgr.getPref("overwrite");
          overwriteP = (StringUtil.isNonEmptyString(overwrite)
                        && overwrite.equalsIgnoreCase("yes"));
          loadP = (StringUtil.isNonEmptyString(load)
                   && load.equalsIgnoreCase("yes"));
              

          String errStr = "";
          if (overwriteP
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
                  errStr = "Error: Could not overwrite existing data file";
          }
          if (StringUtil.emptyString(errStr)) {
              if (StringUtil.emptyString(kbName)) {
                  errStr = "Error: No knowledge base name specified";
              }
              else if ((outfile == null) || !outfile.canRead()) 
                  errStr = "Error: The data file could not be saved or cannot be read";
          }
          if (StringUtil.isNonEmptyString(errStr)) {
              mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
              System.out.println(errStr);
              isError = true;
              System.out.println("EXIT ProcessFile.jsp 2");
              response.sendRedirect("KBs.jsp"); 
          }
          else {
              if (mgr.getKB(kbName) == null) 
                  mgr.addKB(kbName);
              KB kb = mgr.getKB(kbName);

              // The newly written data file is now the input to the
              // next step, which is the translation of the data file
              // to a KIF constituent file.
              File kifFile = DocGen.dataFileToKifFile(kb, 
                                                      ontology, 
                                                      baseName, 
                                                      outfile,
                                                      overwriteP,
                                                      loadP);
              if ((kifFile == null) || !kifFile.canRead()) {
                  errStr = "Error: The KIF file could not be saved or cannot be read";
                  mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
                  System.out.println(errStr);
                  isError = true;
                  System.out.println("EXIT ProcessFile.jsp 3");
                  response.sendRedirect("KBs.jsp"); 
              }
          }
          if (!isError) {
              System.out.println("EXIT ProcessFile.jsp 4");
              response.sendRedirect("Manifest.jsp?kb=" + kbName);
          // response.sendRedirect("MiscUtilities.jsp?kb=" + kbName);     
          }
      }
      catch (Exception e) {
          String errStr = "ERROR in ProcessFile.jsp: " + e.getMessage();
          mgr.setError(mgr.getError() + "\n<br/>" + errStr + "\n<br/>");
          System.out.println(errStr);
          System.out.println("  kbName == " + kbName);
          System.out.println("fileName == " + fileName);
          e.printStackTrace();
          System.out.println("EXIT ProcessFile.jsp ERROR");
          response.sendRedirect("KBs.jsp"); 
      }
  }

%>



