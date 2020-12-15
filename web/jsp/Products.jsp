<%@ page
   language="java"
   import=" com.articulate.sigma.dataProc.Infrastructure,com.articulate.sigma.*,com.articulate.sigma.tp.*,com.articulate.sigma.trans.*,com.articulate.sigma.wordNet.*,com.articulate.sigma.VerbNet.*,com.articulate.sigma.CCheckManager.*,java.text.ParseException,java.net.URLConnection, javax.servlet.http.HttpServletRequest, java.net.URL,com.oreilly.servlet.multipart.MultipartParser,com.oreilly.servlet.multipart.Part,com.oreilly.servlet.multipart.ParamPart,com.oreilly.servlet.multipart.FilePart,java.util.*,java.io.*, tptp_parser.*, TPTPWorld.*"
   pageEncoding="UTF-8"
   contentType="text/html;charset=UTF-8"
%>
<h2>Product Search</h2>

<form action="Products.jsp">

<%
  String productType = request.getParameter("productType");
  if (!StringUtil.emptyString(productType))
      productType = HTMLformatter.decodeFromURL(productType);
  String category = request.getParameter("category");
  if (!StringUtil.emptyString(category))
      category = HTMLformatter.decodeFromURL(category);
  String subCategory = request.getParameter("subCategory");
  if (!StringUtil.emptyString(subCategory))
      subCategory = HTMLformatter.decodeFromURL(subCategory);
  String productName = request.getParameter("productName");
  if (!StringUtil.emptyString(productName))
      productName = HTMLformatter.decodeFromURL(productName);
  String manufacturer = request.getParameter("manufacturer");
  if (!StringUtil.emptyString(manufacturer))
      manufacturer = HTMLformatter.decodeFromURL(manufacturer);
  Enumeration enumeration = request.getParameterNames();
  Map<String, String> params = new HashMap<>();
  while (enumeration.hasMoreElements()) {
      String parameterName = (String) enumeration.nextElement();
      if (parameterName.equals("submit"))
          continue;
      String value = null;
      if (!StringUtil.emptyString(request.getParameter(parameterName))) {
          value = StringUtil.decode(request.getParameter(parameterName));
          params.put(parameterName, value);
          System.out.println("Products.jsp: " + parameterName + ":" + value);
      }
  }


  Infrastructure.initOnceDB();
  out.println(Infrastructure.inf.productCount() + " Products<P>");
%>

<table ALIGN="LEFT" WIDTH=80%><tr><TD BGCOLOR='#AAAAAA'><IMG SRC='pixmaps/1pixel.gif' width=1 height=1 border=0></TD></tr></table><BR>
<table ALIGN="LEFT" WIDTH=80%><tr><td>
      <font face="Arial,helvetica"><b>Product Type: </b>
      <%=HTMLformatter.createMenu("productType",productType,Infrastructure.inf.getProductTypesDB())%>
      <%=Infrastructure.inf.getProductTypesDB().size()%> products types
      <P>
      <% if (!StringUtil.emptyString(productType)) { %>
          <font face="Arial,helvetica"><b>Category: </b>
          <%=HTMLformatter.createMenu("category",category,Infrastructure.inf.getCategoriesDB(productType))%>
          <P>
      <% } %>
      <% if (!StringUtil.emptyString(category)) { %>
            <font face="Arial,helvetica"><b>Sub-Category: </b>
            <%
            ArrayList<String> al = Infrastructure.inf.getSubCategoriesDB(category);
            if (al != null && al.size() > 0)
                out.println(HTMLformatter.createMenu("subCategory",subCategory,al));
            %>
            <P>
      <% } %>
      <%
      String productClass = subCategory;
      if (StringUtil.emptyString(subCategory))
          productClass = category;
      if (!StringUtil.emptyString(productClass)) { %>
           <font face="Arial,helvetica"><b>Filters: </b><br>
           <%
           HashSet<String> allowableRelations = Infrastructure.inf.getAllowedRelationsDB(productClass);
           if (allowableRelations == null || allowableRelations.size() == 0) {
               productClass = category;
               allowableRelations = Infrastructure.inf.getAllowedRelationsDB(productClass);
           }
           System.out.println("Products.jsp: allowable relations: " + allowableRelations);
           if (allowableRelations != null) {
               for (String r : allowableRelations) {
                   out.println(r + " ");
                   String filtername = "filter-" + r;
                   String filterEncoded = HTMLformatter.encodeForURL(filtername);
                   String value = request.getParameter(filterEncoded);
                   System.out.println("Products.jsp: filtername: " + filterEncoded);
                   System.out.println("Products.jsp: params: " + params);
                   System.out.println("Products.jsp: value: " + value);
                   System.out.println("Products.jsp: allowed values: " + Infrastructure.inf.getAllowableValuesDB(productClass,r));
                   String decodedValue = null;
                   if (value != null)
                       decodedValue = HTMLformatter.decodeFromURL(value);
                   out.println(HTMLformatter.createMenu(filtername,decodedValue,Infrastructure.inf.getAllowableValuesDB(productClass,r)));
                   out.println("<br>");
              }
          }
          %>
        <P># Products: <%
            ArrayList<String> prods = Infrastructure.inf.getProductsByTypeDB(productClass);
            if (prods != null)
                out.println(prods.size()); %><P>
        <font face="Arial,helvetica"><b>Product names: </b>
        <%=HTMLformatter.createMenu("productName",productName,Infrastructure.inf.getProductsDB(params))%>
      <% } %>
  </td></tr>
  </table>
  <p>
  <input type="submit" name="submit" value="submit">
</form>
<p>
