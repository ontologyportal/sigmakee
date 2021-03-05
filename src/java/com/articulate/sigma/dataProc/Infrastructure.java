package com.articulate.sigma.dataProc;

import com.articulate.sigma.HTMLformatter;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.KButilities;
import com.articulate.sigma.utils.MapUtils;
import com.articulate.sigma.utils.StringUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.sql.*;

public class Infrastructure {

    public static boolean initialized = false;
    public static Infrastructure inf =  null;

    public static Connection conn = null;

    public static int funcIdCounter = 0;

    public HashMap<String, HashSet<String>> relsForType = new HashMap<>();

    // exterior key of product type, interior key of relation, interior set of allowed values
    public HashMap<String, HashMap<String, HashSet<String>>> allowableValues = new HashMap<>();

    public HashMap<String,String> productTypes = new HashMap<>();  // id, name
    public HashMap<String,HashSet<String>> productsByTypeNames = new HashMap<>();

    public HashMap<String,Product> products = new HashMap<>(); //id, product
    public HashMap<String,Category> categories = new HashMap<>(); //id, category
    public HashMap<String, HashSet<String>> parents = new HashMap<>(); //parent name, list of categories

    /** *************************************************************
     */
    public class Product {
        public String SUMO = null;
        public String ID = null;
        public String name = null;
        public String subCat = null; // a JSON oid
        public HashMap<String,String> attributes = new HashMap<>();

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (SUMO != null)
                sb.append("SUMO:" + SUMO + " ");
            sb.append(ID + " : " + name + " : " + subCat);
            sb.append("    " + attributes);
            return sb.toString();
        }
    }

    /** *************************************************************
     */
    public class Category {
        public String ID = null;
        public String SUMO = null;
        public String name = null;
        public String parent = null; // a JSON oid
        public String productType = null;  // a JSON oid
        public HashSet<String> sectors = new HashSet<>();

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (SUMO != null)
                sb.append("SUMO:" + SUMO + " ");
            sb.append(ID + " : " + name + " : " + parent);
            sb.append("    " + sectors);
            return sb.toString();
        }
    }

    /** *************************************************************
     * maps are old string ID keys and new (SUMO) id values
     */
    public class Mappings {
        public HashMap<String,String> terms = new HashMap<>();
        public HashMap<String,String> relations = new HashMap<>();
        public HashMap<String,String> units = new HashMap<>();
    }

    /** *************************************************************
     */
    private void processProductTypes(JSONArray arraypt) throws SQLException {

        for (Object o : arraypt) {
            JSONObject jo = (JSONObject) o;
            JSONObject jso2 = (JSONObject) jo.get("_id");
            String oid = (String) jso2.get("$oid");
            String name = (String) jo.get("name");
            productTypes.put(oid,name);
            PreparedStatement st = conn.prepareStatement("INSERT INTO edges (rel, source, target) " +
                    "VALUES ('instance','" + StringUtil.stringToKIFid(name) + "','ProductAttribute') " +
                    "ON CONFLICT DO NOTHING;");
            st.executeUpdate();
            st.close();
        }
    }

    /** *************************************************************
     */
    private Mappings processMappings(JSONObject objm) {

        Mappings mappings = new Mappings();
        JSONArray terms = (JSONArray) objm.get("terms");
        for (Object o : terms) {
            String old = (String) ((JSONObject) o).get("old");
            String newTerm = (String) ((JSONObject) o).get("new");
            mappings.terms.put(old,newTerm);
        }
        JSONArray relations = (JSONArray) objm.get("relations");
        for (Object o : relations) {
            String old = (String) ((JSONObject) o).get("old");
            String newTerm = (String) ((JSONObject) o).get("new");
            mappings.relations.put(old,newTerm);
        }
        JSONArray units = (JSONArray) objm.get("units");
        for (Object o : relations) {
            String old = (String) ((JSONObject) o).get("old");
            String newTerm = (String) ((JSONObject) o).get("new");
            mappings.units.put(old,newTerm);
        }
        return mappings;
    }

    /** *************************************************************
     */
    private void processCategories(JSONArray arrayc,
                                   Mappings mappings) throws SQLException {

        for (Object o : arrayc) {
            JSONObject jso = (JSONObject) o;
            JSONObject jso2 = (JSONObject) jso.get("_id");
            String oid = (String) jso2.get("$oid");
            String name = (String) jso.get("name");
            JSONArray sectors = (JSONArray) jso.get("sectors");
            Category c = this.new Category();
            c.ID = oid;
            c.name = name;
            c.sectors.addAll(sectors);

            JSONObject jsoParent = (JSONObject) jso.get("parentId");
            if (jsoParent != null) {
                c.parent = (String) jsoParent.get("$oid");
            }
            c.SUMO = StringUtil.stringToKIFid(c.name);
            //if (mappings.terms.containsKey(c.name))
            //    c.SUMO = mappings.terms.get(c.name);
            categories.put(c.ID,c);
            JSONObject jsoProdType = (JSONObject) jso.get("productTypeId");
            if (jsoProdType != null) {
                c.productType = (String) jsoProdType.get("$oid");
                String prodTy = productTypes.get(c.productType);
                System.out.println("(industryProductType " + c.SUMO + " " +
                        StringUtil.stringToKIFid(productTypes.get(c.productType)) + ")");
                PreparedStatement st = conn.prepareStatement("INSERT INTO edges (rel, source, target) " +
                                               "VALUES ('industryProductType','" + c.SUMO + "','" +
                                               StringUtil.stringToKIFid(productTypes.get(c.productType)) + "')" +
                                               "ON CONFLICT DO NOTHING;");
                st.executeUpdate();
                st.close();
            }
            else
                System.out.println("processCategories(): null product type for " + o);
            //System.out.println("processCategories(): " + c);
        }
        for (Category c : categories.values()) {
            if (categories.containsKey(c.parent))
            MapUtils.addToMap(parents,categories.get(c.parent).name,c.name);
        }
    }

    /** *************************************************************
     */
    private void processProduct(JSONObject jso,
                                Mappings mappings) throws SQLException {

        JSONObject jso2 = (JSONObject) jso.get("_id");
        String oid = (String) jso2.get("$oid");
        String name = (String) jso.get("name");
        JSONObject attrib = (JSONObject) jso.get("attributes");
        Collection<String> relations = attrib.keySet();
        Product p = this.new Product();
        p.ID = oid;
        p.name = name;
        //System.out.println("processProduct(): " + p.name);
        p.attributes = (HashMap<String,String>) attrib;
        JSONObject jsocat = (JSONObject) jso.get("subCategoryId");
        String subcat = (String) jsocat.get("$oid");
        if (categories.keySet().contains(subcat)) { // points to a duplicate concepts in categories
            //System.out.println("processProducts(): found id: " + cat);
            Category subCat = categories.get(subcat);
            p.subCat = subcat;
            String subCatName = subCat.name;
            MapUtils.addToMap(productsByTypeNames,subCatName,p.ID);
            System.out.println("(subclass " +
                    StringUtil.stringToKIFid(p.name) + " " +
                    StringUtil.stringToKIFid(subCatName) + ")");
            PreparedStatement st = conn.prepareStatement("INSERT INTO edges (rel, source, target) " +
                    "VALUES ('subclass','" + StringUtil.stringToKIFid(p.name)  + "','" +
                    StringUtil.stringToKIFid(subCatName)  + "') ON CONFLICT DO NOTHING;");
            st.executeUpdate();
            st.close();
            String SUMO = subCat.SUMO;
            if (SUMO == null)
                SUMO = StringUtil.stringToKIFid(subCat.name);
            //System.out.println("processProducts(): found parent: " + p.parent);
            for (String r : relations) {
                if (r.equals("Brands")) {
                    JSONArray ar = (JSONArray) attrib.get(r);
                    for (Object obj : ar) {
                        String brand = (String) obj;
                        System.out.println("(manufacturer " +
                                StringUtil.stringToKIFid(p.name) + " " +
                                StringUtil.stringToKIFid(brand) + ")");
                        PreparedStatement st2 = conn.prepareStatement("INSERT INTO edges (rel, source, target) " +
                                "VALUES ('manufacturer','" + StringUtil.stringToKIFid(p.name)  + "','" +
                                StringUtil.stringToKIFid(brand)  + "') ON CONFLICT DO NOTHING;");
                        st2.executeUpdate();
                        st2.close();
                    }
                }
                else {
                    String newR = StringUtil.stringToKIFid(r);
                    //if (mappings.relations.containsKey(r))
                   //     newR = mappings.relations.get(r);
                    MapUtils.addToMap(relsForType, subCatName, r);
                    MapUtils.addToMapMap(allowableValues, subCatName, r, attrib.get(r).toString());
                    String value = attrib.get(r).toString();
                    System.out.println("(applicableRelation " + StringUtil.stringToKIFid(subCatName) + " " + StringUtil.stringToKIFid(r) + ")");
                    PreparedStatement st2 = conn.prepareStatement("INSERT INTO edges (rel, source, target) " +
                            "VALUES ('applicableRelation','" + StringUtil.stringToKIFid(subCatName) + "','" +
                            r + "') ON CONFLICT DO NOTHING;");
                    st2.executeUpdate();
                    String funcID = "Function" + funcIdCounter++;
                    String val = processValue(value,funcID);
                    if (Character.isDigit(value.charAt(0))) {
                        System.out.println("(memberMeasure " + StringUtil.stringToKIFid(name) + " " +
                                StringUtil.stringToKIFid(r) + " " + funcID + ")");
                        st2 = conn.prepareStatement("INSERT INTO ternary (rel, arg1, arg2, arg3) " +
                                "VALUES ('memberMeasure','" + StringUtil.stringToKIFid(name) + "','" +
                                StringUtil.stringToKIFid(r) + "','" + funcID + "') ON CONFLICT DO NOTHING;");
                        st2.executeUpdate();
                        st2.close();
                    }
                }
            }
        }
        products.put(p.ID,p);
    }

    /** *************************************************************
     */
    public void toSUMObyParent() throws SQLException {

        System.out.println("toSUMObyParent(): ");
        for (String id : categories.keySet()) {
            Category c = categories.get(id);
            String child = c.SUMO;
            if (c.SUMO == null)
                child = StringUtil.stringToKIFid(c.name);
            if (c.parent != null) {
                Category p = categories.get(c.parent);
                if (p != null) {
                    String parent = p.SUMO;
                    if (p.SUMO == null)
                        parent = StringUtil.stringToKIFid(p.name);
                    System.out.println("(subclass " + child + " " + parent + ")");
                    PreparedStatement st = conn.prepareStatement("INSERT INTO edges (rel, source, target) " +
                            "VALUES ('subclass','" + child + "','" +
                            parent + "') ON CONFLICT DO NOTHING;");
                    st.executeUpdate();
                    st.close();
                }
            }
        }
    }

    /** *************************************************************
     * A list of products can be very long so parse each JSON object
     * individually so as to provide a status.
     */
    public void readProducts(String filename,
                             Mappings mappings) {

        System.out.println("readProducts(): starting");
        int counter = 0;
        FileReader frp = null;
        try {
            frp = new FileReader(filename);
            LineNumberReader lr = null;
            lr = new LineNumberReader(frp);
            String line = null;
            while ((line = lr.readLine()) != null) {
                //System.out.println("line: " + line);
                if (counter++ == 10000) {
                    System.out.print(".");
                    counter = 0;
                }
                if (line != null && !StringUtil.emptyString(line) && !line.startsWith("[") && !line.startsWith("]")) {
                    //System.out.println("readProducts(): non-null");
                    line = line.trim();
                    if (line.endsWith(","))
                        line = line.substring(0, line.length() - 1);
                    JSONParser jp = new JSONParser();
                    Object objp = jp.parse(line);
                    JSONObject obj = (JSONObject) objp;
                    //arrayp.add(obj);
                    processProduct(obj,mappings);
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error reading " + filename + "\n" + e.getMessage());
            e.printStackTrace();
        }
        System.out.println();
    }

    /** *************************************************************
     * process string measures into SUMO format
     */
    public String processValue(String val, String funcID) throws SQLException {

        val = val.toLowerCase();
        Pattern p = Pattern.compile("(\\d+)\\s+(mm|in|ft|m).*");
        Matcher m = p.matcher(val);
        String SUMO = "Milimeter";
        if (m.matches()) {
            String number = m.group(1);
            String unit = m.group(2);
            if (unit.equals("mm"))
                SUMO = "Milimeter";
            else if (unit.equals("in"))
                SUMO = "Inch";
            else if (unit.equals("ft"))
                SUMO = "FootLength";
            else if (unit.equals("m"))
                SUMO = "Meter";
            PreparedStatement st = conn.prepareStatement("INSERT INTO ternaryFn (rel, arg1, arg2, arg3) " +
                    "VALUES ('MeasureFn','" + funcID + "','" +
                    number + "','" + SUMO + "') ON CONFLICT DO NOTHING;");
            st.executeUpdate();
            st.close();
            return "(MeasureFn " + number + " " + SUMO + ")";
        }
        return val;
    }

    /** *************************************************************
     * Parse a sample of products and their categories formatted in a JSON.
     * Uses JSON API at http://alex-public-doc.s3.amazonaws.com/json_simple-1.1/index.html
     * @param filenamep must be full path
     * @param filenamec must be full path
     */
    public void productCatJSONtoSUMO(String filenamep, String filenamec, String filenamem, String filenamept) throws SQLException {

        System.out.println("productCatJSONtoSUMO(): reading files:");
        System.out.println("productCatJSONtoSUMO(): reading product files: " + filenamep);
        System.out.println("productCatJSONtoSUMO(): reading category files: " + filenamec);
        System.out.println("productCatJSONtoSUMO(): reading mapping files: " + filenamem);
        System.out.println("productCatJSONtoSUMO(): reading product type files: " + filenamept);
        FileReader frpt = null;
        FileReader frc = null;
        FileReader frm = null;
        JSONArray arraypt = null;
        JSONArray arrayc = null;
        JSONObject objm = null;
        HashMap<String,Category> categories = new HashMap<>();
        HashMap<String,String> productType = new HashMap<>();
        Mappings mappings = new Mappings();
        try {
            // read product types
            frpt = new FileReader(filenamept);
            JSONParser jpt = new JSONParser();
            Object objpt = jpt.parse(frpt);
            arraypt = (JSONArray) objpt;

            // read categories
            frc = new FileReader(filenamec);
            JSONParser jpc = new JSONParser();
            Object objc = jpc.parse(frc);
            arrayc = (JSONArray) objc;

            // read mapping
            frm = new FileReader(filenamem);
            JSONParser jpm = new JSONParser();
            objm = (JSONObject) jpm.parse(frm);

            processProductTypes(arraypt);
            mappings = processMappings((JSONObject) objm);

            // process categories
            processCategories(arrayc,mappings);
            toSUMObyParent();

            readProducts(filenamep,mappings);
            //System.out.println("(applicableRelation " + SUMO + " " + newR + ")");
            //System.out.println("(allowableValue " + SUMO + " " + newR + " \"" + attrib.get(r) + "\")");
            for (String ptype : relsForType.keySet()) {
                HashSet<String> rels = relsForType.get(ptype);
                for (String rel : rels) {
                    System.out.println("(applicableRelation " + StringUtil.stringToKIFid(ptype) + " " + StringUtil.stringToKIFid(rel) + ")");
                    PreparedStatement st = conn.prepareStatement("INSERT  INTO edges (rel, source, target) " +
                            "VALUES ('applicableRelation','" + StringUtil.stringToKIFid(ptype) + "','" +
                            rel + "') ON CONFLICT DO NOTHING;");
                    st.executeUpdate();
                    st.close();
                }
            }
            for (String ptype : allowableValues.keySet()) {
                HashMap<String,HashSet<String>> rels = allowableValues.get(ptype);
                for (String rel : rels.keySet()) {
                    HashSet<String> vals = rels.get(rel);
                    for (String val : vals) {
                        String funcID = "Function" + funcIdCounter++;
                        val = processValue(val,funcID);
                        if (val.startsWith("(")) {
                            System.out.println("(allowableValue " + StringUtil.stringToKIFid(ptype) + " " + StringUtil.stringToKIFid(rel)  + " " + val + ")");
                            PreparedStatement st = conn.prepareStatement("INSERT INTO ternary (rel, arg1, arg2, arg3) " +
                                    "VALUES ('allowableValue','" + StringUtil.stringToKIFid(ptype) + "','" +
                                    rel + "','" + val + "') ON CONFLICT DO NOTHING;");
                            st.executeUpdate();
                            st.close();
                        }
                        else {
                            System.out.println("(allowableValue " + StringUtil.stringToKIFid(ptype) + " " + StringUtil.stringToKIFid(rel)  + " \"" + val + "\")");
                            PreparedStatement st = conn.prepareStatement("INSERT INTO ternary (rel, arg1, arg2, arg3) " +
                                    "VALUES ('allowableValue','" + StringUtil.stringToKIFid(ptype) + "','" +
                                    rel + "','" + val + "') ON CONFLICT DO NOTHING;");
                            st.executeUpdate();
                            st.close();
                        }
                    }
                }
            }
            //System.out.println(allowableValues);
        }
        catch (Exception e) {
            System.out.println("Error reading " + filenamep + " " + filenamec + "\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *************************************************************
     */
    public ArrayList<String> getProductTypes() {

        ArrayList<String> al = new ArrayList<>();
        al.addAll(productTypes.values());
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getProductTypesDB() {

        System.out.println("getProductTypesDB()");
        ArrayList<String> al = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT source FROM edges WHERE target='ProductAttribute';");
            while (rs.next()) {
                String result = rs.getString("source");
                if (!al.contains(result))
                    al.add(result);
            }
            rs.close();
            st.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getCategories(String productType) {

        HashSet<String> result = new HashSet<>();
        for (String s : categories.keySet()) {
            Category c = categories.get(s);
            if (StringUtil.emptyString(c.parent)) {
                String pTypeID = c.productType;
                String pTypeName = productTypes.get(pTypeID);
                if (productType != null & productType.equals(pTypeName))
                    result.add(c.name);
            }
        }
        ArrayList<String> al = new ArrayList<>();
        al.addAll(result);
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getCategoriesDB(String productType) {

        System.out.println("getCategoriesDB(): subCategory: " + productType);
        if (StringUtil.emptyString(productType))
            return null;
        ArrayList<String> al = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT source FROM edges WHERE rel='industryProductType' AND target='" + productType + "';");
            while (rs.next()) {
                String result = rs.getString("source");
                if (!al.contains(result))
                    al.add(result);
            }
            rs.close();
            st.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getSubCategories(String category) {

        if (StringUtil.emptyString(category) || !parents.containsKey(category))
            return null;
        ArrayList<String> al = new ArrayList<>();
        al.addAll(parents.get(category));
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getSubCategoriesDB(String category) throws SQLException {

        System.out.println("getSubCategoriesDB(): category: " + category);
        if (StringUtil.emptyString(category))
            return null;
        ArrayList<String> al = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT source FROM edges WHERE rel='subclass' and target='" + category + "';");
            while (rs.next()) {
                String result = rs.getString("source");
                if (!al.contains(result))
                    al.add(result);
            }
            rs.close();
            st.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    /** *************************************************************
     */
    public HashSet<String> getAllowedRelations(String subCategory) {

        if (StringUtil.emptyString(subCategory) )
            return null;
        subCategory = StringUtil.decode(subCategory);
        System.out.println("getAllowedRelations(): subCategory: " + subCategory);
        System.out.println("getAllowedRelations(): " + relsForType);
        return relsForType.get(subCategory);
    }

    /** *************************************************************
     */
    public HashSet<String> getAllowedRelationsDB(String subCategory) throws SQLException {

        if (StringUtil.emptyString(subCategory) )
            return null;
        HashSet<String> result = new HashSet<>();
        subCategory = StringUtil.decode(subCategory);
        System.out.println("getAllowedRelationsDB(): subCategory: " + subCategory);
        System.out.println("getAllowedRelationsDB(): " + relsForType);
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT target FROM edges WHERE rel='applicableRelation' and source='" + subCategory + "';");
            while (rs.next())
                result.add(rs.getString("target"));
            rs.close();
            st.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** *************************************************************
     */
    public ArrayList<String> getAllowableValues(String subCategory, String rel) {

        if (StringUtil.emptyString(subCategory) )
            return null;
        ArrayList<String> al = new ArrayList<>();
        System.out.println("getAllowableValues(): subCategory: " + subCategory);
        System.out.println("getAllowableValues(): rel: " + rel);
        System.out.println("getAllowableValues(): " + allowableValues);
        if ((allowableValues.get(subCategory) != null)) {
            System.out.println("getAllowableValues(): value for subCategory: " + allowableValues.get(subCategory));
            if (allowableValues.get(subCategory).get(rel) != null) {
                System.out.println("getAllowableValues(): values for rel: " + allowableValues.get(subCategory).get(rel));
                al.addAll(allowableValues.get(subCategory).get(rel));
            }
        }
        System.out.println("getAllowableValues(): al: " + al);
        System.out.println();
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getAllowableValuesDB(String subCategory, String rel) {

        if (StringUtil.emptyString(subCategory) )
            return null;
        ArrayList<String> al = new ArrayList<>();
        System.out.println("getAllowableValuesDB(): subCategory: " + subCategory);
        System.out.println("getAllowableValuesDB(): rel: " + rel);
        System.out.println("getAllowableValuesDB(): " + allowableValues);
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT arg3 FROM ternary WHERE rel='allowableValue' and arg1='" + subCategory + "'" +
                    " and arg2='" + rel + "';");
            while (rs.next())
                al.add(rs.getString("arg3"));
            rs.close();
            st.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("getAllowableValuesDB(): al: " + al);
        System.out.println();
        return al;
    }

    /** *************************************************************
     */
    public ArrayList<String> getProducts(Map<String, String> params) {

        System.out.println("getProducts(): params: " + params);
        ArrayList<String> result = new ArrayList<>();
        String subCat = params.get("subCategory");
        System.out.println("getProducts(): subCat: " + subCat);
        if (StringUtil.emptyString(subCat))
            return null;
        Collection<String> prods = productsByTypeNames.get(StringUtil.decode(subCat));
        System.out.println("getProducts(): products for subcat: " + prods);
        if (prods != null) {
            for (String id : prods) {
                Product p = products.get(id);
                //System.out.println("getProducts(): products: " + products);
                System.out.println("getProducts(): product: " + p);
                boolean allMatch = true;
                for (String attribName : params.keySet()) {
                    if (attribName.equals("subCategory") || attribName.equals("category") ||
                        attribName.equals("submit") || attribName.equals("productType"))
                        continue;
                    System.out.println("getProducts(): attribName: " + attribName);
                    String search = StringUtil.decode(params.get(attribName));
                    System.out.println("getProducts(): search: " + search);
                    if (attribName.startsWith("filter-"))
                        attribName = attribName.substring(7);
                    System.out.println("getProducts(): attribName without prefix: " + attribName);
                    String value = p.attributes.get(HTMLformatter.decodeFromURL(attribName));
                    System.out.println("getProducts(): value: " + value);
                    if (!search.equals(value))
                        allMatch = false;
                }
                if (allMatch) {
                    System.out.println("getProducts(): success! adding: " + p.name);
                    result.add(p.name);
                }
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public ArrayList<String> getProductsByTypeDB(String subCat) {

        ArrayList<String> al = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT source FROM edges WHERE rel='subclass' and target='" + subCat + "';");
            while (rs.next()) {
                String result = rs.getString("source");
                if (!al.contains(result))
                    al.add(result);
            }
            rs.close();
            st.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    /** *************************************************************
     * @return a count of products
     */
    public int productCount() {

        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT source) FROM edges WHERE rel='manufacturer';");
            while (rs.next()) {
                int result = rs.getInt(1);
                return result;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /** *************************************************************
     * @param params is a map of attribute value pairs of the features being searched for
     * @return a list of products
     */
    public ArrayList<String> getProductsDB(Map<String, String> params) {

        System.out.println("getProductsDB(): params: " + params);
        ArrayList<String> result = new ArrayList<>();
        String subCat = params.get("subCategory");
        String cat = params.get("category");
        System.out.println("getProductsDB(): subCat: " + subCat);
        System.out.println("getProductsDB(): cat: " + cat);
        if (StringUtil.emptyString(subCat))
            subCat = cat;
        Collection<String> prods = getProductsByTypeDB(StringUtil.decode(subCat));
        System.out.println("getProductsDB(): products for subcat: " + prods);
        if (prods == null | prods.size() == 0)
            prods = getProductsByTypeDB(StringUtil.decode(cat));
        if (prods != null) {
            for (String id : prods) {
                System.out.println("getProductsDB(): product: " + id);
                boolean allMatch = true;
                for (String attribName : params.keySet()) {
                    if (attribName.equals("subCategory") || attribName.equals("category") ||
                            attribName.equals("submit") || attribName.equals("productType"))
                        continue;
                    System.out.println("getProductsDB(): attribName: " + attribName);
                    String search = StringUtil.decode(params.get(attribName));
                    System.out.println("getProductsDB(): search: " + search);
                    if (attribName.startsWith("filter-"))
                        attribName = attribName.substring(7);
                    System.out.println("getProductsDB(): attribName without prefix: " + attribName);
                    boolean found = false;
                    try {
                        Statement st = conn.createStatement();
                        ResultSet rs = st.executeQuery("SELECT arg3 FROM ternary WHERE rel='allowableValue' AND " +
                            "arg1='" + id + "' AND arg2='" + HTMLformatter.decodeFromURL(attribName) + "';");
                        while (rs.next()) {
                            String result2 = rs.getString("arg3");
                            if (result2.startsWith("Function")) {

                            }
                            else if (search.equals(result2))
                                found = true;
                        }
                        rs.close();
                        st.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!found)
                        allMatch = false;
                }
                if (allMatch) {
                    System.out.println("getProductsDB(): success! adding: " + id);
                    result.add(id);
                }
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static void initOnce() {

        if (initialized)
            return;
        String productsF = System.getenv("PRODUCTS");
        String mappingsF = System.getenv("MAPPINGS");
        String categoriesF = System.getenv("CATEGORIES");
        String typesF = System.getenv("TYPES");
        inf = new Infrastructure();
        try {
            inf.productCatJSONtoSUMO(productsF, categoriesF, mappingsF, typesF);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        initialized = true;
    }

    /** *************************************************************
     */
    public static void initOnceDB() {

        if (initialized)
            return;
        try {
            String url = "jdbc:postgresql://localhost/sumo";
            Properties props = new Properties();
            props.setProperty("user", "postgres");
            props.setProperty("password", "postgres");
            props.setProperty("ssl", "false");
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(url, props);
            String productsF = System.getenv("PRODUCTS");
            String mappingsF = System.getenv("MAPPINGS");
            String categoriesF = System.getenv("CATEGORIES");
            String typesF = System.getenv("TYPES");
            inf = new Infrastructure();
            inf.productCatJSONtoSUMO(productsF, categoriesF, mappingsF, typesF);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //inf = new Infrastructure();
        //inf.productCatJSONtoSUMO(productsF, categoriesF, mappingsF, typesF);
        initialized = true;
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KButilities class");
        System.out.println("  options:");
        System.out.println("  -h - show this help screen");
        System.out.println("  -i initialize");
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            if (args != null && args.length > 1 && args[0].equals("-i")) {
                initOnceDB();
            }
            else
                showHelp();
        }
    }
}
