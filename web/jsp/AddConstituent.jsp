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

 if (!KBmanager.getMgr().getPref("userName").equalsIgnoreCase("admin"))         
       response.sendRedirect("KBs.jsp");     

  String fileName = "";    
  String srcDir = KBmanager.getMgr().getPref("kbDir");      
  System.out.println("INFO in AddConstituent.jsp: KB dir: " + srcDir);
  String callingPage = "KBs";
  MultipartRequest multiPartRequest = null;
  String kbName = null;

  try {  
      multiPartRequest = new MultipartRequest(request,srcDir);
      callingPage = multiPartRequest.getParameter("callingPage");
      kbName = multiPartRequest.getParameter("kbName");
      System.out.println("INFO in AddConstituent.jsp: kbName: " + kbName);
      System.out.println("INFO in AddConstituent.jsp: filename: " + multiPartRequest.getParameter("constituent"));
      Enumeration params = multiPartRequest.getParameterNames();
      while (params.hasMoreElements()) {
          String param = params.nextElement().toString();
          System.out.println("INFO in AddConstituent.jsp: parameter: " + param);
      }
      if (kbName == null) {
          System.out.println("Error: No knowledge base name specified.");
          response.sendRedirect(callingPage + ".jsp"); 
      }

      Enumeration fileTags = multiPartRequest.getFileNames(); // There should be just one filename though.
      while (fileTags.hasMoreElements()) {                    
          String fileTag = fileTags.nextElement().toString();
          fileName = multiPartRequest.getOriginalFileName(fileTag);
          System.out.println("INFO in AddConstituent.jsp: filetag: " + fileTag);
          System.out.println("INFO in AddConstituent.jsp: filename: " + fileName);
          if (KBmanager.getMgr().getKB(kbName) == null) 
              KBmanager.getMgr().addKB(kbName);
          KB kb = KBmanager.getMgr().getKB(kbName);
          kb = KBmanager.getMgr().getKB(kbName);
          kb.addConstituent(srcDir + File.separator + fileName);
          // kb.cache();
      }
      KBmanager.getMgr().writeConfiguration();
  }
  catch (Exception e) {
      System.out.println("Error in AddConstituent.jsp: Enumerating MultipartRequest which uploads files.");
      System.out.println(e.getMessage());
      System.out.println("INFO in AddConstituent.jsp: kbName: " + kbName);
      System.out.println("INFO in AddConstituent.jsp: filename: " + fileName);
      System.out.println("INFO in AddConstituent.jsp: callingPage: " + multiPartRequest.getParameter("callingPage"));
  }

  response.sendRedirect(callingPage + ".jsp?kb=" + kbName);
%>

