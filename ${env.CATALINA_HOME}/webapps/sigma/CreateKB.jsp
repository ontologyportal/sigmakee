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
August 9, Acapulco, Mexico.
*/
      
  String srcDir = KBmanager.getMgr().getPref("kbDir");      

  try {
      MultipartParser mp = new MultipartParser(request, 10*1024*1024); // 10MB
      Part part;
      while ((part = mp.readNextPart()) != null) {
          String name = part.getName();
          if (part.isParam()) {
              // it's a parameter part
              ParamPart paramPart = (ParamPart) part;
              String value = paramPart.getStringValue();
              out.println("param: name=" + name + "; value=" + value);
          }
          else if (part.isFile()) {
              // it's a file part
              FilePart filePart = (FilePart) part;
              String fileName = filePart.getFileName();
              String testname = filePart.getName();
              if (fileName != null) {
                  // the part actually contained a file
                  // long size = filePart.writeTo(dir);
                  out.println("file: name=" + name + "; fileName=" + fileName +
                    ", filePath=" + filePart.getFilePath() + 
                    ", contentType=" + filePart.getContentType() +
                    ", testname=" +  testname);
              }
              else { 
                  // the field did not contain a file
                  out.println("file: name=" + name + "; EMPTY");
              }
              out.flush();
          }
      }
  }
  catch (IOException lEx) {
      out.println("Error in CreateKB.jsp: reading or saving file");
  }

  /**
  try {  
      multiPartRequest = new MultipartRequest(request,srcDir);
      String kbName = multiPartRequest.getParameter("kbName");
      System.out.println("kbName: " + kbName);
      System.out.println("filename: " + multiPartRequest.getParameter("fileButtonUp"));
      Enumeration params = multiPartRequest.getParameterNames();
      while (params.hasMoreElements()) {
          String param = params.nextElement().toString();
          System.out.println("parameter: " + param);
      }
      System.out.println(params);
      if (kbName == null) {
          System.out.println("Error: No knowledge base name specified.");
          response.sendRedirect("KBs.jsp"); 
      }

      Enumeration fileTags = multiPartRequest.getFileNames(); // There should be just one filename though.
      while (fileTags.hasMoreElements()) {                    
          String fileTag = fileTags.nextElement().toString();
          System.out.println("INFO in CreateKB.jsp: filename: " + fileTag);
          System.out.println("Original filename: " + multiPartRequest.getOriginalFileName(fileTag));
          KBmanager.getMgr().addKB(kbName);
          KB kb = KBmanager.getMgr().getKB(kbName);
          kb.addConstituent(fileTag);
      }
  }
  catch (Exception e) {
      System.out.println("Error in upload.jsp: Enumerating MultipartRequest which uploads files.");
      System.out.println(e.getMessage());
  }
  */

  response.sendRedirect("KBs.jsp");
%>

