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

  if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin")) {
       response.sendRedirect("KBs.jsp");     
  }
  else {

      System.out.println("ENTER AddConstituent.jsp");

      StringBuffer result = new StringBuffer();
      String fileName = "";    
      String srcDir = KBmanager.getMgr().getPref("kbDir");
      File dir = new File( srcDir );
      //System.out.println("INFO in AddConstituent.jsp: KB dir: " + srcDir);
      MultipartRequest multiPartRequest = null;
      String kbName = null;
      int postSize = 4000000;

      try {  
          boolean isError = false;
          System.out.println("request == " + request);
          multiPartRequest = new MultipartRequest(request,srcDir,postSize);
          System.out.println("multiPartRequest == " + multiPartRequest);

          kbName = multiPartRequest.getParameter("kb");
          // System.out.println("INFO in AddConstituent.jsp: kb: " + kbName);
          // System.out.println("INFO in AddConstituent.jsp: filename: " + multiPartRequest.getParameter("constituent"));
          Enumeration params = multiPartRequest.getParameterNames();
          while (params.hasMoreElements()) {
              String param = params.nextElement().toString();
              System.out.println("INFO in AddConstituent.jsp: param == " + param);
          }
          if (!Formula.isNonEmptyString(kbName)) {
              String errStr = "Error: No knowledge base name specified";
              KBmanager.getMgr().setError(KBmanager.getMgr().getError()
                                          + "\n<br/>" + errStr + "\n<br/>");
              System.out.println(errStr);
              isError = true;
              response.sendRedirect("KBs.jsp"); 
          }
          else {
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
                          file = new File(dir, fileName);
                      }

                      System.out.println("file == " + file);
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
                          //System.out.println("INFO in AddConstituent.jsp: filetag: " + fileTag);
                          //System.out.println("INFO in AddConstituent.jsp: filename: " + fileName);
                          if (KBmanager.getMgr().getKB(kbName) == null) 
                              KBmanager.getMgr().addKB(kbName);
                          KB kb = KBmanager.getMgr().getKB(kbName);
                          result.append( kb.addConstituent(file.getCanonicalPath(), true, false) );
                          if (KBmanager.getMgr().getPref("cache").equalsIgnoreCase("yes")) {
                              result.append(kb.cache());
                          }
                          kb.loadVampire();
                      }
                  }
              }
              KBmanager.getMgr().writeConfiguration();
          }
          if (!isError) {
              response.sendRedirect("Manifest.jsp?kb=" + kbName);
          }
      }
      catch (Exception e) {
          String errStr = "ERROR in AddConstituent.jsp: " + e.getMessage();
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

