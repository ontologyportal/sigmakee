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

String kbName = "";
String namespace = "";
String language = "";
KB kb = null;

  if (!KBmanager.getMgr().getPref("userRole").equalsIgnoreCase("administrator")) {
       response.sendRedirect("KBs.jsp");     
  }
  else {      
      String fileName = "";    
      try {  
          int postSize = 2000000;
          boolean isError = false;
          MultipartRequest multiPartRequest = null;
          StringBuffer result = new StringBuffer();
          String srcDir = KBmanager.getMgr().getPref("kbDir");
          File dir = new File( srcDir );
          System.out.println("request == " + request);
          multiPartRequest = new MultipartRequest(request,srcDir,postSize);
          System.out.println("multiPartRequest == " + multiPartRequest);
          kbName = multiPartRequest.getParameter("kb");
          namespace = multiPartRequest.getParameter("namespace");;
          if (kbName == null || KBmanager.getMgr().getKB(kbName) == null) 
              System.out.println(" no such knowledge base " + kbName);
          else
              kb = KBmanager.getMgr().getKB(kbName);
          language = multiPartRequest.getParameter("lang");
          String action = multiPartRequest.getParameter("action");
          System.out.println("INFO in ProcessFile.jsp: action = " + action);
          language = HTMLformatter.processLanguage(language,kb);

          // kbName = multiPartRequest.getParameter("kb");
          Enumeration params = multiPartRequest.getParameterNames();
          while (params.hasMoreElements()) {
              String param = params.nextElement().toString();
              System.out.println("INFO in ProcessFile.jsp: param == " + param);
          }
          if (!Formula.isNonEmptyString(kbName)) {
              System.out.println();
          }
          if (action != null && action != "" && action.equals("kifFromCSV")) {
              Enumeration fileTags = multiPartRequest.getFileNames(); // There should be just one filename though.
              System.out.println("fileTags == " + fileTags);
              if (fileTags == null) {
                  String errStr = "Error: The input file does not exist or cannot be read";
                  KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                              + "\n<br/>" + errStr + "\n<br/>");
                  System.out.println(errStr);
                  isError = true;
                  response.sendRedirect("KBs.jsp"); 
              }
              else {
                  while (fileTags.hasMoreElements()) {                    
                      String fileTag = fileTags.nextElement().toString();
                      System.out.println("fileTag == " + fileTag);
                      fileName = multiPartRequest.getOriginalFileName(fileTag);
                      System.out.println("fileName == " + fileName);

                      File file = null;
                      if (fileName != null) {
                          file = multiPartRequest.getFile(fileTag);
                      }

                      System.out.println("file == " + file);
                      if (action.equals("kifFromCSV")) {
                          if ((file == null) || !file.exists()) {
                              String errStr = "Error: The input file does not exist or cannot be read";
                              KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                                          + "\n<br/>" + errStr + "\n<br/>");
                              System.out.println(errStr);
                              isError = true;
                              response.sendRedirect("KBs.jsp"); 
                              break;
                          }
                          else if (file.length() == 0) {
                              try {
                                  file.delete();
                              }
                              catch (Exception ex) {
                              }
                              String errStr = "Error: The input file does not exist or cannot be read";
                              KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                                          + "\n<br/>" + errStr + "\n<br/>");
                              System.out.println(errStr);
                              isError = true;
                              response.sendRedirect("KBs.jsp"); 
                              break;
                          }
                          else {
                              String outputFilename = file.getCanonicalPath().substring(0,file.getCanonicalPath().lastIndexOf(".")) + ".kif";
                              System.out.println("INFO in ProcessFile.jsp: Writing file: " + outputFilename);
                              ArrayList al = DB.readSpreadsheet(file.getCanonicalPath(),true);
                              try {                              
                                  DB.processSpreadsheet(al,namespace,outputFilename);
                              } catch (java.io.IOException e) {
                                  System.out.println("Error writing file " + file.getCanonicalPath() + "\n" + e.getMessage());
                              }
                              Set kbs = KBmanager.getMgr().getKBnames();
                              Iterator it = kbs.iterator();
                              while (it.hasNext()) {
                                  String kbn = (String) it.next();
                                  KB kb2 = KBmanager.getMgr().getKB(kbn);
                                  ArrayList constituents = kb2.constituents;
                                  boolean reloaded = false;
                                  for (int i = 0; i < constituents.size(); i++) {
                                      String conName = (String) constituents.get(i);
                                      if (conName.endsWith(file.getCanonicalPath()) && !reloaded) {
                                          System.out.println("INFO in Processfile.jsp: Reloading KB");
                                          kb2.reload();
                                          reloaded = true;
                                      }
                                  }
                              }
                          }
                      }
                  }
              }
          }
          if (!isError) {
              response.sendRedirect("MiscUtilities.jsp?kb=" + kbName);
          }
      }
      catch (Exception e) {
          String errStr = "ERROR in ProcessFile.jsp: " + e.getMessage();
          KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                      + "\n<br/>" + errStr + "\n<br/>");
          System.out.println(errStr);
          System.out.println("  kbName == " + kbName);
          System.out.println("  filename == " + fileName);
          e.printStackTrace();
          response.sendRedirect("KBs.jsp"); 
      }
  }

%>



