/** This code is copyrighted by Rearden Commerce (c) 2011.  It is
released under the GNU Public License &lt;http://www.gnu.org/copyleft/gpl.html&gt;."\""

Users of this code also consent, by use of this code, to credit
Articulate Software in any writings, briefings, publications,
presentations, or other representations of any software which
incorporates, builds on, or uses this code.  Please cite the following
article in any publication with references:

Pease, A., (2003). The Sigma Ontology Development Environment, in Working
Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
August 9, Acapulco, Mexico.  See also http://sigmakee.sourceforge.net.
 */
package com.articulate.sigma.dataProc;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.articulate.sigma.DB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/** ***************************************************************
 */
public class Hotel {

    public String oID = "";
    public String nID = "";
    public String taID = "";
    public String name = "";
    public String address = "";
    public String address2 = "";
    public String city = "";
    public String stateProv = "";
    public String country = "United States";
    public String postCode = "";
    public String tel = "";
    public String fax = "";
    public String email = "";
    public String url = "";
    public String description = "";
    public String stars = "";
    public String lat = "";
    public String lng = "";
    public String chainCode = "";
    public String merchant = "";
    public String oMerchant = "";
    public String startPrice = "";
    public String endPrice = "";
    public String lowRate = "";
    public String currency = "";
    public String rooms = "";
    public String floors = "";
    public String checkin = "";
    public String checkout = "";
    public String areaServed = "";
    public String lastModified = "";

    // type, [title,description]
    public HashMap<String,AVPair> facilities = new HashMap<>();

      // amenity code, value
    public HashMap<String,String> amenities = new HashMap<>();

      // code, url
    public HashMap<String,String> media = new HashMap<>();
    public ArrayList<String> reviews = new ArrayList<>();

      // a map of the sense (or term) and the number of appearances
    public HashMap<String,Integer> senses = new HashMap<>();
    public HashMap<String,Integer> SUMO = new HashMap<>();

      // a numerical assessment against arbitrary labels
    public TreeMap<String,Float> buckets = new TreeMap<>();

    public List<String> feedData = new ArrayList<>();

      // overall sentiment for the hotel's reviews
    public int sentiment = 0;

      // Concept key and sentiment value reflecting the sentiment of each sentence
      // and the concepts in that sentence - an approximate association
    public HashMap<String,Integer> conceptSentiment = new HashMap<>();

    public HashMap<String,String> values = new HashMap<>();

    /** ***************************************************************
     */
    public String asCSV() {

        StringBuilder result = new StringBuilder();
        result.append("\"").append(oID).append("\",");
        result.append("\"").append(name).append("\",");
        result.append("\"").append(address).append("\",");
        result.append("\"").append(city).append("\",");
        result.append("\"").append(stateProv).append("\",");
        result.append("\"").append(country).append("\",");
        result.append("\"").append(postCode).append("\",");
        result.append("\"").append(tel).append("\",");
        //result.append("\"" + sentiment + "\",");
        for (String S : buckets.keySet()) {
            result.append("\"").append(buckets.get(S)).append("\",");
        }
        return result.toString();
    }

    /** ***************************************************************
     */
    public static String asCSVHeader() {

        StringBuilder result = new StringBuilder();
        result.append("id,");
        result.append("name,");
        result.append("address,");
        result.append("city,");
        result.append("stateProv,");
        result.append("country,");
        result.append("postCode,");
        result.append("tel,");
        result.append("Business, Child Friendly, Fitness, Romantic, Xtend Stay");
        return result.toString();
    }

    /** ***************************************************************
     */
    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();
        result.append("name: ").append(name).append("\n");
        result.append("address: ").append(address).append("\n");
        if (!StringUtil.emptyString(address2))
            result.append("address2: ").append(address2).append("\n");
        result.append("city: ").append(city).append("\n");
        result.append("stateProv: ").append(stateProv).append("\n");
        result.append("country: ").append(country).append("\n");

        for (String S : reviews) {
            result.append("\"").append(S).append("\"\n");
        }
        result.append("\n\n");
        return result.toString();
    }

    /** *************************************************************
     */
    public void addConceptSentiment(Map<String,Integer> conceptSent) {

        int val, oldVal;
        for (String term : conceptSent.keySet()) {
            val = conceptSent.get(term);
            oldVal = 0;
            if (conceptSentiment.keySet().contains(term)) {
                oldVal = conceptSentiment.get(term);
                val = val + oldVal;
            }
            conceptSentiment.put(term, val);
        }
    }

    /** *************************************************************
     */
    public static String printAllHotels(List<Hotel> hotels) {

        System.out.println("INFO in Hotel.printAllHotels(): number: " + hotels.size());
        StringBuilder sb = new StringBuilder();
        sb.append(asCSVHeader()).append("\n");
        for (int i = 0; i < hotels.size(); i++)
            sb.append(hotels.get(i).asCSV()).append("\n");
        return sb.toString();
    }

    /** *************************************************************
     */
    public static void printAllHotelAmenitySentiment(List<Hotel> hotels) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hotels.size(); i++) {
            Hotel h = hotels.get(i);
            StringBuilder result = new StringBuilder();
            result.append("\"").append(h.name).append("\",");
            result.append("\"").append(h.address).append("\",");
            result.append("\"").append(h.city).append("\",");
            result.append("\"").append(h.stateProv).append("\",");
            result.append("\"").append(h.country).append("\",");

            System.out.println(sb.toString());
        }

    }

    /** *******************************************************************
     * Read a list of lists of Strings which is the original input plus
     * some extra columns for the weights of several "buckets", indicating
     * fitness with respect to a particular criterion.  Result is a side
     * effect of setting the bucket weights for the hotels.
     */
    public static void setHotelWeights(List<Hotel> hotels) {

        List<List<String>> rawWeights = DB.readSpreadsheet("OAmenities-weights.csv",null,false);

          // amenity key, value map of bucket name key, weight value
        Map<String,Map<String,String>> weights = new HashMap<>();
        List<String> header = rawWeights.get(0);
        //System.out.println("INFO in DB.setHotelWeights(): buckets: " + buckets);
        List<String> al;
        String amenity;
        Map<String,String> amenityValues;
        String bucket, value;
        for (int i = 1; i < rawWeights.size(); i++) {
            al = rawWeights.get(i);
            amenity =  al.get(0).trim();
            amenityValues = new HashMap<>();
            for (int j = 2; j < header.size(); j++) {
                bucket = header.get(j);
                value =  al.get(j);
                amenityValues.put(bucket,value);
                //System.out.println("INFO in DB.setHotelWeights(): bucket, value: " + bucket + "," + value);
            }
            //System.out.println("INFO in DB.setHotelWeights(): adding weight for amenity: " + amenity);
            weights.put(amenity,amenityValues);
        }

        Hotel h;
        String amenityValue, bucketValue;
        Map<String,String> weightBuckets;
        Float currentValue, addValue, newTotal;
        for (int i = 0; i < hotels.size(); i++) {
            h = hotels.get(i);
            for (int k = 2; k < header.size(); k++)
                h.buckets.put(header.get(k),Float.valueOf(0));
            for (String amen : h.amenities.keySet()) {
                // go through all the amenities
                if (!StringUtil.emptyString(amen)) {
                    amenityValue = h.amenities.get(amen);
                    if (amenityValue.equals("Y")) {  // if the value for the amenity is non-empty
                        //System.out.println("INFO in DB.setHotelWeights(): amenity, amenityValue is non-empty: " + amenity + "," + amenityValue);
                        if (weights.keySet().contains(amen)) {  // if the amenity has a weight
                            //System.out.println("INFO in DB.setHotelWeights(): amenity has a weight: " + amenity);
                            weightBuckets = weights.get(amen);
                            for (String buc : weightBuckets.keySet()) {
                                //System.out.println("INFO in DB.setHotelWeights(): weight: " + weightBuckets.get(bucket));
                                //System.out.println("INFO in DB.setHotelWeights(): bucket: " + bucket);
                                currentValue = h.buckets.get(buc);
                                bucketValue = weightBuckets.get(buc);
                                //System.out.println("INFO in DB.setHotelWeights(): bucketValue: " + bucketValue);
                                if (!StringUtil.emptyString(bucketValue)) {
                                    addValue = Float.valueOf(bucketValue);
                                    newTotal = currentValue + addValue;
                                    h.buckets.put(buc, newTotal);
                                }
                            }
                        }
                        //else
                        //System.out.println("INFO in DB.setHotelWeights(): weights: " + weights.keySet() + " does not contain amenity: " + amenity);
                    }
                }
            }
        }
    }

    /** *******************************************************************
     * Used by processOneXMLHotel and HotelXMLtoCSV to compile a list of the
     * columns that should appear in the resulting CSV file.
     */
    private static TreeSet<String> hotelColumns = new TreeSet<String>();

    /** *******************************************************************
     * @param w states whether to write SUMO statements
     */
    public static List<Hotel> HotelDBImport(boolean w) {

        Map<String,String> abbrevs = DB.readStateAbbrevs();
        List<Hotel> result = new ArrayList<>();
        List<List<String>> f = DB.readSpreadsheet("Hotels_SF.csv",null,false);
        Hotel h;
        List<String> al;
        for (int i = 1; i < f.size(); i++) {
            h = new Hotel();
            al = f.get(i);
            h.feedData = al;
            String NTMHotelID                = (String) al.get(0);
            h.nID = NTMHotelID;
            String HotelName                 = (String) al.get(1);
            h.name = HotelName;
            String hotelAddress              = (String) al.get(2);
            h.address = hotelAddress.trim();
            String HotelID = StringUtil.stringToKIF(HotelName + NTMHotelID,true);
            String HotelBuildingID = "HotelBuilding-" + HotelID;
            if (w) System.out.println("(instance " + HotelID + " Hotel)");
            if (w) System.out.println("(instance " + HotelBuildingID + " HotelBuilding)");
            if (w) System.out.println("(possesses " + HotelID + " " + HotelBuildingID + ")");

            String City                      = (String) al.get(3);
            h.city = new String(City);
            City = StringUtil.stringToKIF(City,true);
            String State                     = (String) al.get(5);
            State = State.trim();
            if (State.length() > 2 && abbrevs.keySet().contains(State.toUpperCase()))
                State = abbrevs.get(State.toUpperCase());
            h.stateProv = State;
            String StreetAddressPostalCode   = (String) al.get(6);
            h.postCode = StreetAddressPostalCode;
            result.add(h);
            if (w) System.out.println("(postCity " + City + " " + HotelBuildingID + ")");
            if (w) System.out.println("(postPostcode " + StreetAddressPostalCode + " " + HotelBuildingID + ")");
            if (w) System.out.println("(postState " + State + " " + HotelBuildingID + ")");
            String latitude                  = (String) al.get(36);
            String longitude                 = (String) al.get(37);

            if (!StringUtil.emptyString(latitude) && !StringUtil.emptyString(latitude)) {
                if (w) System.out.println("(latitude "  + HotelBuildingID + " " + latitude + ")");
                if (w) System.out.println("(longitude "  + HotelBuildingID + " " + longitude + ")");
            }
            String[] Landmarks =
            {"Airport","Beach","City","ConventionCenter","Highway","Lake",
                    "Mountain","RuralArea","Suburb","TouristArea"};
            for (int j = 0; j < Landmarks.length; j++) {
                String orient = null;
                // A="At",I="In",N="Near"
                String field = (String) al.get(38+j);
                if (field.equals("A"))
                    orient = "Adjacent";
                if (field.equals("I"))
                    orient = "Inside";
                if (field.equals("N"))
                    orient = "Near";
                if (orient != null)
                    if (w) System.out.println("(exists (?A) (and (instance ?A " + Landmarks[j] + ")" +
                            " (orientation " + HotelBuildingID + " ?A " + orient + ")))");
            }
            if ((String) al.get(63) != null)
                if (w) System.out.println("(exists (?A) (and (instance ?B BusinessCenter)" +
                        " (located ?B " + HotelBuildingID + ")))");
            if ((String) al.get(64) != null)
                if (w) System.out.println("(exists (?C ?G) (and (instance ?C Computer)" +
                        " (guest ?G " + HotelID + ") (hasPurpose ?C (exists (?A) (and" +
                " (instrument ?A ?C) (agent ?A ?G))))))");
            if ((String) al.get(65) != null)
                if (w) System.out.println("(exists (?C ?G) (and (instance ?C FaxMachine)" +
                        " (guest ?G " + HotelID + ") (hasPurpose ?C (exists (?A) (and" +
                " (instrument ?A ?C) (agent ?A ?G))))))");
            if ((String) al.get(76) != null)
                if (w) System.out.println("(exists (?A) (and (instance ?A ATMMachine)" +
                        " (located ?A " + HotelBuildingID + ")))");
            if ((String) al.get(78) != null)
                if (w) System.out.println("(exists (?P) (and (instance ?P PassengerCarRental)" +
                        " (located ?P " + HotelBuildingID + ")))");
            if ((String) al.get(81) != null)
                if (w) System.out.println("(exists (?C) (and (instance ?C Concierge)" +
                        " (employs ?C " + HotelID + ")))");
            if ((String) al.get(83) != null)
                if (w) System.out.println("(or (exists (?C) (and (instance ?C Crib) " +
                        " (possesses ?C " + HotelID + ")))" +
                        " (exists (?B) (and (instance ?B MurphyBed) (possesses ?B " + HotelID + "))))");
            if ((String) al.get(85) != null)
                if (w) System.out.println("(exists (?G) (and (instance ?G GiftNovelyAndSouvenirStores)" +
                        " (located ?G " + HotelBuildingID + ")))");
            if ((String) al.get(89) != null)
                if (w) System.out.println("(exists (?G) (and (instance ?G Group)" +
                        " (memberType ?G Bar) (located ?G " + HotelBuildingID + ")" +
                        " (memberCount ?G " + (String) al.get(89) + ")))");
            if ((String) al.get(90) != null)
                if (w) System.out.println("(exists (?G) (and (instance ?G Group)" +
                        " (memberType ?G Restaurant) (located ?G " + HotelBuildingID + ")" +
                        " (memberCount ?G " + (String) al.get(89) + ")))");
            if ((String) al.get(91) != null)
                if (w) System.out.println("(exists (?R) (and (instance ?G RoomService)" +
                        " (located ?G " + HotelBuildingID + ")))");
            if (((String) al.get(104)).equals("A"))
                if (w) System.out.println("(allRoomAmenity AirConditioner " + HotelBuildingID + ")");
            if (((String) al.get(104)).equals("S"))
                if (w) System.out.println("(someRoomAmenity AirConditioner " + HotelBuildingID + ")");
            if (((String) al.get(104)).equals("A"))
                if (w) System.out.println("(allRoomAmenity Balcony " + HotelBuildingID + ")");
            if (((String) al.get(104)).equals("S"))
                if (w) System.out.println("(someRoomAmenity Balcony " + HotelBuildingID + ")");
            if ((String) al.get(105) != null)
                if (w) System.out.println("(allRoomAmenity InternetConnection " + HotelBuildingID + ")");
            if ((String) al.get(106) != null)
                if (w) System.out.println("(allRoomAmenity InternetConnection " + HotelBuildingID + ")"); // should include Fee
            if (((String) al.get(108)).equals("A"))
                if (w) System.out.println("(allRoomAmenity CoffeeMaker " + HotelBuildingID + ")");
            if (((String) al.get(108)).equals("S"))
                if (w) System.out.println("(someRoomAmenity CoffeeMaker " + HotelBuildingID + ")");
            // daily maid service is a service that operates in the room
            if (((String) al.get(110)).equals("A"))
                if (w) System.out.println("(allRoomAmenity VCRSystem " + HotelBuildingID + ")");
            if (((String) al.get(110)).equals("S"))
                if (w) System.out.println("(someRoomAmenity VCRSystem " + HotelBuildingID + ")");
            if (((String) al.get(111)).equals("A"))
                if (w) System.out.println("(allRoomAmenity Fireplace " + HotelBuildingID + ")");
            if (((String) al.get(111)).equals("S"))
                if (w) System.out.println("(someRoomAmenity Fireplace " + HotelBuildingID + ")");
            if (((String) al.get(113)).equals("A"))
                if (w) System.out.println("(allRoomAmenity FabricIron " + HotelBuildingID + ")");
            if (((String) al.get(113)).equals("S"))
                if (w) System.out.println("(someRoomAmenity FabricIron " + HotelBuildingID + ")");
            if (((String) al.get(114)).equals("A"))
                if (w) System.out.println("(allRoomAmenity KitchenArea " + HotelBuildingID + ")");
            if (((String) al.get(114)).equals("S"))
                if (w) System.out.println("(someRoomAmenity KitchenArea " + HotelBuildingID + ")");
            if (((String) al.get(115)).equals("A"))
                if (w) System.out.println("(allRoomAmenity Microwave " + HotelBuildingID + ")");
            if (((String) al.get(115)).equals("S"))
                if (w) System.out.println("(someRoomAmenity Microwave " + HotelBuildingID + ")");
            if (((String) al.get(116)).equals("A"))
                if (w) System.out.println("(allRoomAmenity HotelMiniBar " + HotelBuildingID + ")");
            if (((String) al.get(116)).equals("S"))
                if (w) System.out.println("(someRoomAmenity HotelMiniBar " + HotelBuildingID + ")");
            // newspaper - delivered each day
            // no-smoking - all/exists room with attribute NoSmoking ?
            if (((String) al.get(119)).equals("A"))
                if (w) System.out.println("(allRoomAmenity Refrigerator " + HotelBuildingID + ")");
            if (((String) al.get(119)).equals("S"))
                if (w) System.out.println("(someRoomAmenity Refrigerator " + HotelBuildingID + ")");
            if (((String) al.get(120)).equals("A"))
                if (w) System.out.println("(allRoomAmenity SafeContainer " + HotelBuildingID + ")");
            if (((String) al.get(120)).equals("S"))
                if (w) System.out.println("(someRoomAmenity SafeContainer " + HotelBuildingID + ")");
            if (((String) al.get(121)).equals("A"))
                if (w) System.out.println("(allRoomAmenity Telephone " + HotelBuildingID + ")");
            if (((String) al.get(121)).equals("S"))
                if (w) System.out.println("(someRoomAmenity Telephone " + HotelBuildingID + ")");
            if (((String) al.get(123)).equals("A"))
                if (w) System.out.println("(allRoomAmenity TelevisionReceiver " + HotelBuildingID + ")");
            if (((String) al.get(123)).equals("S"))
                if (w) System.out.println("(someRoomAmenity TelevisionReceiver " + HotelBuildingID + ")");
            if (((String) al.get(124)).equals("A"))
                if (w) System.out.println("(allRoomAmenity TelevisionReceiver " + HotelBuildingID + ")");
            if (((String) al.get(124)).equals("S"))
                if (w) System.out.println("(someRoomAmenity TelevisionReceiver " + HotelBuildingID + ")");
            if (((String) al.get(127)).equals("A"))
                if (w) System.out.println("(allRoomAmenity WhirlpoolTub " + HotelBuildingID + ")");
            if (((String) al.get(127)).equals("S"))
                if (w) System.out.println("(someRoomAmenity WhirlpoolTub " + HotelBuildingID + ")");
            if (((String) al.get(128)).equals("A"))
                if (w) System.out.println("(allRoomAmenity ElectronicLock " + HotelBuildingID + ")");
        }
        return result;
    }

    /** *******************************************************************
     * Collect all possible column names and assign them a number, then sort
     * on that frequency.
     * @result a list of SUMO term names, sorted by frequency
     */
    public static List<String> generateSUMOHeader(List<Hotel> hotels) {

        Map<String, Integer> columnNumbers = new TreeMap<>();
        List<String> result = DB.fill("",hotelColumns.size());
        Integer value, oldValue, newValue;
        for (Hotel h : hotels) {
            for (String columnName : h.SUMO.keySet()) {
                value = h.SUMO.get(columnName);
                oldValue = columnNumbers.get(columnName);
                newValue = value + oldValue;
                columnNumbers.put(columnName, newValue);
            }
        }
        Set<String> conceptsUnused = new TreeSet<String>();
        conceptsUnused.addAll(columnNumbers.keySet());
        int maxValue;
        String maxConcept;
        Integer count;
        while (!conceptsUnused.isEmpty()) {
            maxValue = -1;
            maxConcept = "";
            for (String columnName : columnNumbers.keySet()) {
                count = columnNumbers.get(columnName);
                if (count > maxValue) {
                    maxValue = count;
                    maxConcept = columnName;
                }
                result.add(columnName);
                conceptsUnused.remove(columnName);
            }
        }
        return result;
    }

    /** *******************************************************************
     */
    public static List<String> generateSUMOColumns(Hotel h, List<String> SUMOheader) {

        List<String> result = DB.fill("",SUMOheader.size());
        Integer cellValue;
        for (String columnName : h.SUMO.keySet()) {
            // iterate through the columns
            cellValue = h.SUMO.get(columnName);
            result.add(cellValue.toString());
        }
        return result;
    }

    /** *******************************************************************
     * Convert a particular XML markup into an array of hotels
     */
    public static List<Hotel> readXMLHotels(String fname) {

          // CSV data structure: a list of lines containing list of column cells
        List<Hotel> result = new ArrayList<>();

        String line;
        try (Reader fr = new FileReader(fname);
            LineNumberReader lr = new LineNumberReader(fr)) {
            StringBuilder sb = new StringBuilder();
            DocumentBuilderFactory dbf;
            DocumentBuilder db;
            byte[] bytes;
            Document dom;
            Element docEle;
            Hotel h;
            while ((line = lr.readLine()) != null) {
                if (line.contains("<hotel>")) {
                    sb.append(line).append("\n");
                    while ((line = lr.readLine()) != null && !line.contains("</hotel>"))
                        sb.append(line).append("\n");
                    if (line.contains("</hotel>")) {
                        sb.append(line).append("\n");
                        //System.out.println("INFO in Hotel.readXMLHotels(): one hotel record:");
                        //System.out.println("------------------------------------------------");
                        //System.out.println(sb.toString());
                        //System.out.println("------------------------------------------------");
                        dbf = DocumentBuilderFactory.newInstance();
                        dbf.setIgnoringElementContentWhitespace(true);
                        db = dbf.newDocumentBuilder();
                        bytes = sb.toString().getBytes("US-ASCII");
                        try (InputStream bais = new ByteArrayInputStream(bytes)) {
                            dom = db.parse(bais);
                            docEle = dom.getDocumentElement();
                            h = processOneXMLHotel(docEle);
                            System.out.println("INFO in Hotel.readXMLHotels(): " + h.name);
                            result.add(h);
                            sb.setLength(0); // reset
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            System.err.println("File error reading " + fname + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return result;
    }
    /** *******************************************************************
     * @param e is a DOM element for one hotel
     * @return a Hotel
     */
    public static Hotel processOneXMLHotel(Element e) {

        Hotel h = new Hotel();
        int maxString = 30;
        NodeList features = e.getChildNodes();
        for (int i = 0; i < features.getLength(); i++) {
            if (features.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element feature = (Element) features.item(i);
                //System.out.println(feature.toString());
                if (feature.getTagName().equals("addr")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element f = (Element) fs.item(j);
                            if (f.getTagName().equals("line1"))
                                h.address = f.getTextContent();
                            else if (f.getTagName().equals("city"))
                                h.city = f.getTextContent();
                            else if (f.getTagName().equals("postal"))
                                h.postCode = f.getTextContent();
                            else  if (f.getTagName().equals("state")) {
                                h.stateProv = f.getAttribute("name");
                            }
                            else  if (f.getTagName().equals("country")) {
                                h.country = f.getAttribute("name");
                            }
                        }
                    }
                }
                else if (feature.getTagName().equals("amenities")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element amenity = (Element) fs.item(j);
                            Element code = (Element) amenity.getElementsByTagName("code").item(0);
                            if (code != null && code.getTextContent() != null) {
                                h.amenities.put(code.getTextContent(),"Y");
                            }
                        }
                    }
                }
                else if (feature.getTagName().equals("id"))
                    h.oID = feature.getTextContent();
                else if (feature.getTagName().equals("name"))
                    h.name = feature.getTextContent();
                else if (feature.getTagName().equals("phone"))
                    h.tel = feature.getTextContent();
                else if (feature.getTagName().equals("leadprice")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element f = (Element) fs.item(j);
                            h.values.put("leadprice-" + f.getTagName(),f.getTextContent());
                            hotelColumns.add("leadprice-" + f.getTagName());
                        }
                    }
                }
                else if (feature.getTagName().equals("facilities")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element media = (Element) fs.item(j);
                            Element type = (Element) media.getElementsByTagName("type").item(0);
                            Element title = (Element) media.getElementsByTagName("title").item(0);
                            Element desc = (Element) media.getElementsByTagName("desc").item(0);
                            //values.put(type.getText() + "-" + title.getText(),desc.getText());
                            //hotelColumns.add(type.getText() + "-" + title.getText());
                            String text = desc.getTextContent();
                            if (!StringUtil.emptyString(text) && text.length() > maxString)
                                text = text.substring(0,maxString);
                            if (type != null && type.getNodeValue() != null) {
                                h.values.put(type.getTextContent(),text);
                                hotelColumns.add(type.getTextContent());
                            }
                        }
                    }
                }
                else {
                    String text = feature.getTextContent();
                    if (!StringUtil.emptyString(text) && text.length() > maxString)
                        text = text.substring(0,maxString);
                    h.values.put(feature.getTagName(),text);
                    hotelColumns.add(feature.getTagName());
                }
            }
        }
        return h;
    }

    /** *******************************************************************
     */
    public static int geocodeCount = 0;
    public static final int geocodeLimit = 100;  // to avoid Google shutting us off

    /** *******************************************************************
     */
    public static List<Hotel> readCSVHotels(String fname) {

        Map<String,String> abbrevs = DB.readStateAbbrevs();
        List<Hotel> result = new ArrayList<Hotel>();
        List<List<String>> f = DB.readSpreadsheet(fname,null,false);
        Hotel h;
        List<String> al;
        String NTMHotelID, HotelName, hotelAddress, City, Country, State, StreetAddressPostalCode, addr, orient, field;
        String[] Landmarks;
        for (int i = 1; i < f.size(); i++) {
            h = new Hotel();
            al = f.get(i);
            System.out.println(al);
            h.feedData = al;
            NTMHotelID                = al.get(0);
            h.nID = NTMHotelID;
            HotelName                 = al.get(1);
            h.name = StringUtil.removeEnclosingQuotes(HotelName);
            hotelAddress              = al.get(2);
            h.address = StringUtil.removeEnclosingQuotes(hotelAddress).trim();
            h.nID = StringUtil.stringToKIF(HotelName + NTMHotelID,true);
            City                      = al.get(3);
            Country                   = al.get(4);
            h.country = Country;
            h.city = City;
            //h.city = StringUtil.stringToKIF(h.city,true);
            State                     = al.get(5).trim();
            if (State.length() > 2 && abbrevs.keySet().contains(State.toUpperCase()))
                State = abbrevs.get(State.toUpperCase());
            h.stateProv = State;
            StreetAddressPostalCode   = al.get(6);
            h.postCode = StreetAddressPostalCode;
            result.add(h);
            h.lat                     = al.get(36);
            h.lng                     = al.get(37);
            addr = h.address + ", " + h.city + ", " + h.stateProv + ", " +
                          h.country + ", " + h.postCode;
            /*
            geocodeCount++;
            if (geocodeCount < geocodeLimit) {
                ArrayList<String> latlon = DB.geocode(addr);
                if (latlon != null) {
                    h.lat = latlon.get(0);
                    h.lng = latlon.get(1);
                }
            }
            */
            Landmarks = new String[] {"Airport","Beach","City","ConventionCenter","Highway","Lake",
                    "Mountain","RuralArea","Suburb","TouristArea"};
            for (int j = 0; j < Landmarks.length; j++) {
                orient = null;
                // A="At",I="In",N="Near"
                field = (String) al.get(38+j);
                if (field.equals("A"))
                    orient = "Adjacent";
                if (field.equals("I"))
                    orient = "Inside";
                if (field.equals("N"))
                    orient = "Near";
            }
            h.feedData = al;
        }
        return result;
    }

    /** *************************************************************
     * Set address fields in the hotel as a side effect.
     * <span class="format_address"><span class="street-address" property="v:street-address">
     * 1734 South Harbor Blvd.</span>, <span class="locality"><span property="v:locality">Anaheim</span>,
     * <span property="v:region">CA</span> <span property="v:postal-code">92802</span></span>
     */
    public static void parseHTMLAddress(Hotel h, String addr) {

        if (StringUtil.emptyString(addr))
            return;
        String a = new String(addr);
        int lastComma = a.lastIndexOf(",");
        if (lastComma > 0) {
            h.postCode = a.substring(lastComma + 1).trim();
            a = a.substring(0,lastComma);
            lastComma = a.lastIndexOf(",");
            if (lastComma > 0) {
                h.stateProv = a.substring(lastComma + 1).trim();
                a = a.substring(0,lastComma);
                lastComma = a.lastIndexOf(",");
                if (lastComma > 0) {
                    h.city = a.substring(lastComma + 1).trim();
                    int firstComma = a.indexOf(",");
                    h.address = a = a.substring(0,firstComma);
                    if (h.address.endsWith("."))
                        h.address = h.address.substring(0,h.address.length()-1);
                }
            }
        }
    }

    /** *************************************************************
     * @param fname has no file extension or directory
     */
    public static Hotel parseOneHotelReviewFile(String fname) {

        Hotel h = new Hotel();
        String name = "";
        String review = "";
        Pattern pReview = Pattern.compile("data-vocabulary.org/Review");
        Pattern pMemberReview = Pattern.compile("li class=.member-review.");
        Pattern pEndMemberReview = Pattern.compile("/li");
        Pattern pName = Pattern.compile("<h2>([^<]+)</span>");
        Pattern pAddress = Pattern.compile("<span class=.format.address.[^>]+>([^|]+)");
        // <span class="format_address">
        Pattern pTel = Pattern.compile("<span itemprop=.tel.>(.+)");
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line = null;
                boolean done = false;
                while ((line = lnr.readLine()) != null) {
                    line = line.trim();
                    //System.out.println(line);
                    Matcher mAddress = pAddress.matcher(line);
                    Matcher mTel = pTel.matcher(line);
                    Matcher mReview = pReview.matcher(line);
                    Matcher mMemberReview = pMemberReview.matcher(line);
                    if (mReview.find()) {
                        lnr.readLine();
                        line = lnr.readLine();
                        Matcher mName = pName.matcher(line);
                        if (mName.find()) {
                            //System.out.println(line);
                            h.name = mName.group(1);
                            review = lnr.readLine();
                            //System.out.println(name);
                            if (!StringUtil.emptyString(review))
                                h.reviews.add(review.trim());
                            //System.out.println(review);
                        }
                    }
                    else if (mMemberReview.find()) {
                        //System.out.println("found review: " + line);
                        Matcher mEndMemberReview = pEndMemberReview.matcher(line);
                        while ((line = lnr.readLine()) != null && !mEndMemberReview.find()) {
                            mEndMemberReview = pEndMemberReview.matcher(line);
                            if (line.startsWith("<p>"))
                                h.reviews.add(line.trim());
                        }
                    }
                    else if (mAddress.find())
                        Hotel.parseHTMLAddress(h,mAddress.group(1).trim());
                    else if (mTel.find())
                        h.tel = mTel.group(1);
                }
            }
        }
        catch (IOException ioe) {
            System.out.println("File error reading file " + fname + " : " + ioe.getMessage());
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in parseOneHotelFile()" + e.getMessage());
            }
        }
        return h;
    }

    /** *************************************************************
     * @param fname has no file extension or directory
     */
    public static Hotel parseOneHotelPreviewFile(String fname) {

        Hotel h = new Hotel();
        String name = "";
        String review = "";
        Pattern pReview = Pattern.compile("data-vocabulary.org/Review");
        Pattern pMemberReview = Pattern.compile("li class=.member-review.");
        Pattern pEndMemberReview = Pattern.compile("/li");
        Pattern pName = Pattern.compile("<h2>([^<]+)</span>");
        Pattern pAddress = Pattern.compile("<span itemprop=.address.[^>]+>([^|]+)");
        Pattern pTel = Pattern.compile("<span itemprop=.tel.>(.+)");
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line = null;
                boolean done = false;
                while ((line = lnr.readLine()) != null) {
                    line = line.trim();
                    //System.out.println(line);
                    Matcher mAddress = pAddress.matcher(line);
                    Matcher mTel = pTel.matcher(line);
                    Matcher mReview = pReview.matcher(line);
                    Matcher mMemberReview = pMemberReview.matcher(line);
                    if (mReview.find()) {
                        lnr.readLine();
                        line = lnr.readLine();
                        Matcher mName = pName.matcher(line);
                        if (mName.find()) {
                            //System.out.println(line);
                            h.name = mName.group(1);
                            review = lnr.readLine();
                            //System.out.println(name);
                            if (!StringUtil.emptyString(review))
                                h.reviews.add(review.trim());
                            //System.out.println(review);
                        }
                    }
                    else if (mMemberReview.find()) {
                        //System.out.println("found review: " + line);
                        Matcher mEndMemberReview = pEndMemberReview.matcher(line);
                        while ((line = lnr.readLine()) != null && !mEndMemberReview.find()) {
                            mEndMemberReview = pEndMemberReview.matcher(line);
                            if (line.startsWith("<p>"))
                                h.reviews.add(line.trim());
                        }
                    }
                    else if (mAddress.find())
                        Hotel.parseHTMLAddress(h,mAddress.group(1).trim());
                    else if (mTel.find())
                        h.tel = mTel.group(1);
                }
            }
        }
        catch (IOException ioe) {
            System.out.println("File error reading file " + fname + " : " + ioe.getMessage());
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in parseOneHotelFile()" + e.getMessage());
            }
        }
        return h;
    }

    /** *************************************************************
     * Read hotel review files
     * @return an ArrayList of Hotel
     */
    public static ArrayList<Hotel> parseAllHotelReviewFiles(String fname) {

        System.out.println("INFO in parseAllHotelReviewFiles()");
        ArrayList<Hotel> result = new ArrayList<Hotel>();
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line = null;
                while ((line = lnr.readLine()) != null) {
                    if (!StringUtil.emptyString(line))
                        result.add(parseOneHotelReviewFile(line));
                    if (result.size() % 100 == 0)
                        System.out.print('.');
                }
                System.out.println();
            }
        }
        catch (IOException ioe) {
            System.out.println("File error reading " + fname + ": " + ioe.getMessage());
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in parseAllHotelFiles()" + e.getMessage());
            }
        }
        return result;
    }
    /** *************************************************************
     */
    public static Hotel parseOneTHotelReviewFile(String fname) {

        Hotel h = new Hotel();
        String name = "";
        String review = "";
        Pattern pMemberReview = Pattern.compile("<span class=\"ic i-quote\">Quote:</span>");
        Pattern pEndMemberReview = Pattern.compile("</div>");
        Pattern pName = Pattern.compile("<h1 class='h2 fn' id=\"bCardName\">([^<]+)");
        Pattern pAddress = Pattern.compile("<span class='adr'>([^<]+)");
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line = null;
                boolean done = false;
                while ((line = lnr.readLine()) != null) {
                    line = line.trim();
                    //System.out.println(line);
                    Matcher mAddress = pAddress.matcher(line);
                    Matcher mMemberReview = pMemberReview.matcher(line);
                    Matcher mName = pName.matcher(line);
                    if (mName.find())
                        h.name = mName.group(1);
                    else if (mMemberReview.find()) {
                        //System.out.println("found review: " + line);
                        Matcher mEndMemberReview = pEndMemberReview.matcher(line);
                        while ((line = lnr.readLine()) != null && !mEndMemberReview.find()) {
                            mEndMemberReview = pEndMemberReview.matcher(line);
                            if (!StringUtil.emptyString(line.trim()))
                            h.reviews.add(line.trim());
                        }
                    }
                    else if (mAddress.find())
                        Hotel.parseHTMLAddress(h,mAddress.group(1).trim());
                }
            }
        }
        catch (IOException ioe) {
            System.out.println("Error in parseOneTHotelReviewFile(): File error reading file " + fname + " : " + ioe.getMessage());
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in parseOneHotelFile()" + e.getMessage());
            }
        }
        return h;
    }

    /** *************************************************************
     * Read hotel review files
     * @param fname is the directory path where the reviews are
     * @return an ArrayList of Hotel
     */
    public static ArrayList<Hotel> parseAllTHotelReviewFiles(String fname) {

        System.out.println("INFO in parseAllTHotelReviewFiles()");
        ArrayList<Hotel> result = new ArrayList<Hotel>();
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            String[] children = fin.list();
            if (children == null || children.length == 0)
                System.out.println("Error in parseAllTHotelReviewFiles(): dir: " + fname + " does not exist or is empty.");
            else {
                System.out.println("INFO in parseAllTHotelReviewFiles(): " + children.length + " files.");
                for (int i=0; i<children.length; i++) {
                    // Get filename of file or directory
                    String filename = children[i];
                    if (!StringUtil.emptyString(filename) && filename.startsWith("tvly"))
                        result.add(parseOneTHotelReviewFile(fname + File.separator + filename));
                    if (result.size() % 100 == 0)
                        System.out.print('.');
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in parseAllTHotelReviewFiles(): File error reading " + fname + ": " + e.getMessage());
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in parseAllHotelFiles()" + e.getMessage());
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static void matchHotels(Hotel feedHotel, Hotel reviewsHotel) {

        //if (feedHotel.name.equals(reviewsHotel.name)) {
        //    System.out.println(feedHotel.asCSV());
        //    System.out.println(reviewsHotel.asCSV());
        //}
        String feedAddr = feedHotel.name + "," + feedHotel.address + "," +
                            feedHotel.city + "," + feedHotel.stateProv + "," +
                            feedHotel.postCode;
        String reviewAddr = reviewsHotel.name + "," + reviewsHotel.address + "," +
                          reviewsHotel.city + "," + reviewsHotel.stateProv + "," +
                          reviewsHotel.postCode;
        if (DB.geocode(feedAddr).equals(DB.geocode(reviewAddr)))
            reviewsHotel.feedData = feedHotel.feedData;
        /*
        if (feedHotel.postCode.equals(reviewsHotel.postCode) &&
            feedHotel.stateProv.equals(reviewsHotel.stateProv) &&
            feedHotel.city.equals(reviewsHotel.city) &&
            (feedHotel.address.equals(reviewsHotel.address) ||
                feedHotel.name.equals(reviewsHotel.name) ||
                feedHotel.address.indexOf(reviewsHotel.address) > -1)) {
            //System.out.println("Match:");
            reviewsHotel.feedData = feedHotel.feedData;
            //System.out.println("Review: " + reviewsHotel.asCSV());
            //System.out.println();

        }
        */
    }

    /** *************************************************************
     */
    public static void mergeHotels(ArrayList<Hotel> feed, ArrayList<Hotel> reviews) {

        System.out.println("INFO in mergeHotels()");
        for (int i = 0; i < feed.size(); i++) {
            Hotel feedHotel = feed.get(i);
            for (int j = 0; j < reviews.size(); j++) {
                Hotel reviewHotel = reviews.get(j);
                matchHotels(feedHotel,reviewHotel);
            }
        }
    }

    /** *************************************************************
     */
    public void addAllSenses(Map<String,Integer> wnsenses) {

        for (String sense : wnsenses.keySet()) {
            if (senses.keySet().contains(sense))
                senses.put(sense, wnsenses.get(sense) + senses.get(sense));
            else
                senses.put(sense,wnsenses.get(sense));
        }
    }

    /** *************************************************************
     * @param feed is an ArrayList of Hotel containing the raw data
     * about hotels
     *
     * @return a list of hotels expressed as a list of string values
     * for several fields and then a count of SUMO terms appearing in
     * the review for the given hotel
     */
    public static ArrayList<ArrayList<String>> hotelReviewSUMOasSparseMatrix(ArrayList<Hotel> feed) {

        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        DB.disambigReviews(feed);
        DB.SUMOReviews(feed);
        hotelSentiment(feed);
        ArrayList<String> hotelAsArray = new ArrayList<String>();

          // a list of attribute value pairs where the count is in
          // the attribute and the SUMO term is the value
        List<AVPair> SUMO = DB.topSUMOInReviews(feed);

        hotelAsArray.add("name");
        hotelAsArray.add("address");
        hotelAsArray.add("city");
        hotelAsArray.add("state/prov");
        hotelAsArray.add("country");
        hotelAsArray.add("sentiment");
        int SUMOColumnLimit = 1000;
        if (SUMO.size() < SUMOColumnLimit)
            SUMOColumnLimit = SUMO.size();
        for (int i = 0; i < SUMOColumnLimit; i++) {
            AVPair avp = SUMO.get(i);
            hotelAsArray.add(avp.value);
        }
        result.add(hotelAsArray);

        for (int i = 0; i < feed.size(); i++) {
            Hotel h = feed.get(i);
            hotelAsArray = new ArrayList<String>();
            hotelAsArray.add(h.name);
            hotelAsArray.add(h.address);
            hotelAsArray.add(h.city);
            hotelAsArray.add(h.stateProv);
            hotelAsArray.add(h.country);
            hotelAsArray.add(String.valueOf(h.sentiment));
            int count = 0;
            while (count < SUMOColumnLimit) {
                AVPair avp = SUMO.get(count);
                if (h.SUMO.keySet().contains(avp.value))
                    hotelAsArray.add(h.SUMO.get(avp.value).toString());
                else
                    hotelAsArray.add("");
                count++;
            }
            result.add(hotelAsArray);
        }
        return result;
    }

    /** *************************************************************
     * @param feed is an ArrayList of Hotel containing the raw data
     * about hotels
     *
     * @return a list of hotels expressed as a list of string values
     * for several fields and then a count of SUMO terms appearing in
     * the review for the given hotel
     */
    public static ArrayList<ArrayList<String>> hotelReviewSUMOSentimentAsSparseMatrix(ArrayList<Hotel> feed, boolean write) {

        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        System.out.println("INFO in Hotel.hotelReviewSUMOSentimentAsSparseMatrix()");
        DB.disambigReviews(feed);
        System.out.println("INFO in Hotel.hotelReviewSUMOSentimentAsSparseMatrix(): Completed disambigutation");
        DB.SUMOReviews(feed);
        hotelSentiment(feed);
        System.out.println("INFO in Hotel.hotelReviewSUMOSentimentAsSparseMatrix(): Completed sentiment calculation");
        ArrayList<String> hotelAsArray = new ArrayList<>();

          // a list of attribute value pairs where the count is in
          // the attribute and the SUMO term is the value
        List<AVPair> SUMO = DB.topSUMOInReviews(feed);
        hotelAmenitySentiment(feed);

        hotelAsArray.add("name");
        hotelAsArray.add("address");
        hotelAsArray.add("city");
        hotelAsArray.add("state/prov");
        hotelAsArray.add("country");
        int SUMOColumnLimit = 1000;
        if (SUMO.size() < SUMOColumnLimit)
            SUMOColumnLimit = SUMO.size();
        for (int i = 0; i < SUMOColumnLimit; i++) {
            AVPair avp = SUMO.get(i);
            hotelAsArray.add(avp.value);
        }
        result.add(hotelAsArray);

        for (int i = 0; i < feed.size(); i++) {
            Hotel h = feed.get(i);
            hotelAsArray = new ArrayList<String>();
            hotelAsArray.add(h.name);
            hotelAsArray.add(h.address);
            hotelAsArray.add(h.city);
            hotelAsArray.add(h.stateProv);
            hotelAsArray.add(h.country);
            int count = 0;
            while (count < SUMOColumnLimit) {
                AVPair avp = SUMO.get(count);
                if (h.conceptSentiment.keySet().contains(avp.value))
                    hotelAsArray.add(h.conceptSentiment.get(avp.value).toString());
                else
                    hotelAsArray.add("");
                count++;
            }
            if (write)
                System.out.println(DB.writeSpreadsheetLine(hotelAsArray, true));
            else
                result.add(hotelAsArray);
        }
        return result;
    }

    /** *******************************************************************
     * @param h is a DOM element for one hotel
     * @return a map of column name and column value
     */
    public static Hotel processOneOXMLHotel(Element h) {

        Hotel result = new Hotel();
        int maxString = 30;
        NodeList features = h.getChildNodes();
        for (int i = 0; i < features.getLength(); i++) {
            if (features.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element feature = (Element) features.item(i);
                //System.out.println(feature.toString());
                if (feature.getTagName().equals("COUNTRY")) {
                    String text = feature.getTextContent();
                    if (!StringUtil.emptyString(text) && text.length() > maxString)
                        text = text.substring(0,maxString);
                    result.country = text;
                }
                else if (feature.getTagName().equals("CITY")) {
                    String text = feature.getTextContent();
                    if (!StringUtil.emptyString(text) && text.length() > maxString)
                        text = text.substring(0,maxString);
                    result.city = text;
                }
                else if (feature.getTagName().equals("STATE")) {
                    String text = feature.getTextContent();
                    if (!StringUtil.emptyString(text) && text.length() > maxString)
                        text = text.substring(0,maxString);
                    result.stateProv = text;
                }
                else if (feature.getTagName().equals("ADDRESS")) {
                    String text = feature.getTextContent();
                    if (!StringUtil.emptyString(text) && text.length() > maxString)
                        text = text.substring(0,maxString);
                    result.address = text;
                }
                else if (feature.getTagName().equals("Review")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element reviewFields = (Element) fs.item(j);
                            if (reviewFields.getTagName().equals("TEXT"))
                                result.reviews.add(reviewFields.getTextContent());
                        }
                    }
                }
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static ArrayList<Hotel> readOXMLhotels(String fname) {

        ArrayList<Hotel> hotels = new ArrayList<Hotel>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringElementContentWhitespace(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(fname);
            Element docEle = dom.getDocumentElement();
            NodeList nl = docEle.getElementsByTagName("Place");
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);  // a <hotel>
                    String name = el.getAttribute("name");
                    Hotel h = processOneOXMLHotel(el);
                    h.name = name;
                    hotels.add(h);
                }
            }
        }
        catch (Exception e) {
            System.out.println("File error reading " + fname + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        return hotels;
    }

    /** *************************************************************
     */
    public static int level = 0;

    /** *************************************************************
     */
    public class JSONElement {

        String key = ""; // empty key signifies root element
        String value = "";
        ArrayList<JSONElement> subelements = new ArrayList<JSONElement>();

        /** *************************************************************
         */
        public String toString() {

            StringBuilder sb = new StringBuilder();
            if (!StringUtil.emptyString(key))
                sb.append(key + ":");
            if (!StringUtil.emptyString(value))
                sb.append(value);
            else {
                if (!StringUtil.emptyString(key))
                    sb.append("[");
                else
                    sb.append("{");
                for (int i = 0; i < subelements.size(); i++) {
                    sb.append(subelements.get(i).toString());
                    if (i < subelements.size()-1)
                        sb.append(",");
                }
                if (!StringUtil.emptyString(key))
                    sb.append("]");
                else
                    sb.append("}");
            }
            return sb.toString();
        }

        /** *************************************************************
         */
        public JSONElement getElement(String key) {

            for (int i = 0; i < subelements.size(); i++) {
                if (subelements.get(i).key.equals(key)) {
                    //System.out.println("INFO in Hotel.JSONElement.getElement(): " + subelements.get(i).key);
                    return subelements.get(i);
                }
            }
            return null;
        }

        /** *************************************************************
         */
        public String getElementValue(String key) {

            JSONElement js = getElement(key);
            if (js == null)
                return "";
            if (!StringUtil.emptyString(js.value))
                return js.value;
            return "";
        }
    }

    /** *************************************************************
     * This routine adds keys and values to the parameter.  There are
     * three possibilities:
     * - string key : string value
     * - string key : integer value
     * - string key : [list]
     *
     * @return the string index
     */
    public static int parseJSONPair(String s, int ind, JSONElement js) {

        //System.out.println("INFO in parseJSONPair(): index: " + ind + " " + s.substring(ind));
        int index = ind;
        Hotel h = new Hotel();
        if (s.charAt(index) != '"') {
            System.out.println("Error in parseJSONPair(): test for quote: Bad character " + s.charAt(index) + " at character " + index);
            return index;
        }
        index++;
        int end = s.indexOf('"',index);
        String key = s.substring(index,end);
        index = end;
        index++;
        if (s.charAt(index) != ':') {
            System.out.println("Error in parseJSONPair(): test for colon: Bad character " + s.charAt(index) + " at character " + index);
            System.out.println("INFO in parseJSONPair(): key " + key);
            return index;
        }
        index++;

        if (s.charAt(index) == '"') {
            index++;
            int start = index;
            while (s.charAt(s.indexOf('"',index) - 1) == '\\')  // skip over escaped quotes
                index = s.indexOf('"',index) + 1;
            end = s.indexOf('"',index);
            String value = s.substring(start,end);
            index = end;
            index++;
            JSONElement jsNew = h.new JSONElement();
            jsNew.key = key;
            jsNew.value = value;
            js.subelements.add(jsNew);
            //System.out.println("INFO in parseJSONPair(): key,value " + key + "," + value);
            return index;
        }
        else if (Character.isDigit(s.charAt(index))) {
            int start = index;
            while (Character.isDigit(s.charAt(index)) || s.charAt(index) == '.')
                index++;
            String value = s.substring(start,index);
            JSONElement jsNew = h.new JSONElement();
            jsNew.key = key;
            jsNew.value = value;
            js.subelements.add(jsNew);
            //System.out.println("INFO in parseJSONPair(): key,value " + key + "," + value);
            return index;
        }
        else if (s.charAt(index) == '[') {
            Hotel.level++;
            JSONElement jsNew = h.new JSONElement();
            jsNew.key = key;
            index++;
            index = parseJSONElement(s,index,jsNew);
            //System.out.println("INFO in parseJSONPair(): returning " + jsNew);
            js.subelements.add(jsNew);
            return index;
        }
        else if (s.substring(index,index+4).equals("null")) {
            index = index + 4;
            return index;
        }
        else {
            System.out.println("Error in parseJSONPair(): Bad character " + s.charAt(index) + " at character " + index);
            System.out.println(s.substring(index,index+4));
            return index;
        }
    }

    /** *************************************************************
     * This routine adds elements to the parameter
     * @return the string index
     */
    public static int parseJSONElement(String s, int ind, JSONElement js) {

        //System.out.println("INFO in Hotel.parseJSONElement(): index: " + ind + " " + s.substring(ind));
        //System.out.println("INFO in Hotel.parseJSONElement(): character " + s.charAt(ind));
        //System.out.println("INFO in Hotel.parseJSONElement(): level " + Hotel.level);
        int index = ind;
        Hotel h = new Hotel();
        while (index < s.length()) {
            //System.out.println("INFO in Hotel.parseJSONElement(): testing, equals quote? " + ((s.charAt(index)) == '"'));
            if (s.charAt(index) == '}' || s.charAt(index) == ']') {
                Hotel.level--;
                //System.out.println("INFO in Hotel.parseJSONElement(): it's a close brace or bracket");
                //System.out.println("INFO in Hotel.parseJSONElement(): returning, level: " + Hotel.level);
                //System.out.println(js);
                index++;
                return index;
            }
            else if (s.charAt(index) == '{') {
                Hotel.level++;
                //System.out.println("INFO in Hotel.parseJSONElement(): it's an open brace");
                index++;
                JSONElement jsNew = h.new JSONElement();
                index = parseJSONElement(s,index,jsNew);
                //System.out.println("INFO in Hotel.parseJSONElement(): returning " + jsNew);
                //System.out.println("INFO in Hotel.parseJSONElement(): character " + s.charAt(index));
                js.subelements.add(jsNew);
            }
            else if (s.charAt(index) == '"') {
                //System.out.println("INFO in Hotel.parseJSONElement(): it's a quote");
                index = parseJSONPair(s,index,js);
            }
            else if (s.charAt(index) == ',') {
                //System.out.println("INFO in Hotel.parseJSONElement(): it's a comma");
                index++;
            }
            else {
                System.out.println("Error in parseJSONElement(): Bad character " + s.charAt(index) + " at character " + index);
                return index;
            }
            //index++;
        }
        return index;
    }

    /** *************************************************************
     */
    public static Hotel convertJSON2Hotel(JSONElement js) {

        Hotel result = new Hotel();
        JSONElement jsNew = js.subelements.get(0);
        result.name = jsNew.getElementValue("name");
        result.address = jsNew.getElementValue("address");
        result.taID = jsNew.getElementValue("id");
        result.stateProv = jsNew.getElementValue("state");
        result.city = jsNew.getElementValue("city");
        JSONElement reviews = jsNew.getElement("reviews");
        if (reviews != null) {
            for (int i = 0; i < reviews.subelements.size(); i++) {
                String review = reviews.subelements.get(i).getElementValue("review");
                result.reviews.add(review);
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public static Hotel parseOneJSONReviewFile(String fname) {

        Hotel h = new Hotel();
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line = null;
                boolean done = false;
                while ((line = lnr.readLine()) != null) {
                    line = line.trim();
                    JSONElement js = h.new JSONElement();
                    parseJSONElement(line,0,js);
                    h = convertJSON2Hotel(js);
                    //System.out.println("---------------------------");
                    //System.out.println(h);
                    //System.out.println(js);

                }
            }
        }
        catch (Exception e) {
            System.out.println("Error in parseOneJSONReviewFile(): File error reading " + fname + ": " + e.getMessage());
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
            }
            catch (Exception e) {
                System.out.println("Exception in parseOneJSONReviewFile()" + e.getMessage());
            }
        }
        return h;
    }

    /** *************************************************************
     */
    public static String normalizeSentiment(String value) {

        try {
            float val = Integer.parseInt(value);
            if (val < 0)
                return "0";
            val = 1 - ((50 + val)/(50 + val * val));
            return Float.toString(val);
        }
        catch (NumberFormatException n) {
            System.out.println("Error in Hotel.normalizeSentiment(): bad input: " + value);
            return "0";
        }
    }

    /** *************************************************************
     */
    public static void writeHotelAsXML(Hotel h, PrintWriter pw) {

        try {
            pw.println("\t<hotel>");
            pw.println("\t\t<taID value=\"" + StringUtil.encode(h.taID) + "\"/>");
            pw.println("\t\t<name value=\"" + StringUtil.encode(h.name) + "\"/>");
            pw.println("\t\t<address value=\"" + StringUtil.encode(h.address) + "\"/>");
            pw.println("\t\t<address2 value=\"" + StringUtil.encode(h.address2) + "\"/>");
            pw.println("\t\t<city value=\"" + StringUtil.encode(h.city) + "\"/>");
            pw.println("\t\t<stateProv value=\"" + StringUtil.encode(h.stateProv) + "\"/>");
            pw.println("\t\t<country value=\"" + StringUtil.encode(h.country) + "\"/>");
            pw.println("\t\t<postCode value=\"" + StringUtil.encode(h.postCode) + "\"/>");
            pw.println("\t\t<tel value=\"" + StringUtil.encode(h.tel) + "\"/>");
            pw.println("\t\t<sentiment>");
            Iterator<String> it = h.conceptSentiment.keySet().iterator();
            while (it.hasNext()) {
                String concept = it.next();
                String value = h.conceptSentiment.get(concept).toString();
                concept = WordNetUtilities.getBareSUMOTerm(concept);
                System.out.println(concept);
                //value = normalizeSentiment(value);
                pw.println("\t\t\t<sent concept=\"" + concept + "\" value=\"" + value + "\"/>");
            }
            pw.println("\t\t</sentiment>");
            pw.println("\t</hotel>");
        }
        catch (Exception e) {
            System.out.println("Error in Hotel.writeHotelAsXML(): Error writing " + pw + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** *************************************************************
     * @param writeIncremental means that each hotel review will be
     * processed and each spreadsheet line will be written after reading
     * each hotel.
     */
    public static ArrayList<Hotel> readJSONHotels(String dir, boolean writeIncremental) {

        System.out.println("INFO in readJSONHotels()");
        KBmanager.getMgr().initializeOnce();
        System.out.println("INFO in readJSONHotels(): completed KB initialization");
        WordNet.wn.initOnce();
        System.out.println("INFO in readJSONHotels(): complete reading WordNet files");

        long t1 = System.currentTimeMillis();
        ArrayList<Hotel> result = new ArrayList<Hotel>();
        LineNumberReader lnr = null;
        PrintWriter pw = null;
        try {
            File fin  = new File(dir);
            File outfile = new File(dir + File.separator + "hotelSentiment.xml");
            pw = new PrintWriter(outfile);
            if (writeIncremental)
                pw.println("<hotels>");
            String[] children = fin.list();
            if (children == null || children.length == 0)
                System.out.println("Error in Hotel.readJSONHotels(): dir: " + dir + " does not exist or is empty.");
            else {
                System.out.println("INFO in readJSONHotels(): " + children.length + " files.");
                for (int i=0; i<children.length; i++) {
                    // Get filename of file or directory
                    String filename = children[i];
                    //System.out.println("INFO in readJSONHotels(): filename: " + filename);
                    String qualifiedFilename = dir + File.separator + filename;
                    if (!StringUtil.emptyString(filename) && filename.endsWith("json")) {
                        Hotel h = parseOneJSONReviewFile(qualifiedFilename);
                        if (writeIncremental) {
                            oneHotelAmenitySentiment(h);
                            writeHotelAsXML(h,pw);
                        }
                        else
                            result.add(h);
                    }
                    if (i % 10 == 0)
                        System.out.print('.');
                }
                System.out.println("INFO in readJSONHotels(): Completed reading reviews.");
            }
            if (writeIncremental)
                pw.println("</hotels>");
        }
        catch (Exception e) {
            System.out.println("Error in Hotel.readJSONHotels(): File error reading/writing " + dir + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        finally {
            try {
                if (lnr != null) lnr.close();
                if (pw != null) pw.close();
            }
            catch (Exception e) {
                System.out.println("Exception in readJSONHotels()" + e.getMessage());
            }
        }
        System.out.println("INFO in Hotel.readJSONHotels(): done reading reviews in " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
        return result;
    }

    /** *************************************************************
     */
    public static void hotelSentiment(ArrayList<Hotel> hotels) {

        System.out.println("INFO in Hotel.hotelSentiment()");
        DB.readSentimentArray();
        DB.readStopConceptArray();
        for (int i = 0; i < hotels.size(); i++) {
            Hotel h = hotels.get(i);
            //System.out.println("======== " + h.name + " ========");
            int total = 0;
            for (int j = 0; j < h.reviews.size(); j++) {
                String review = h.reviews.get(j);
                //System.out.println(review);
                int subtotal = DB.computeSentiment(review);
                total = total + subtotal;
                //System.out.println("=== " + subtotal + " ===");
            }
            h.sentiment = total;
            //System.out.println("======== " + total + " ========");
            //System.out.println();
            //System.out.println(DB.computeSentiment("This hotel is the most abject failure of a rotten establishment."));
            //System.out.println(DB.computeSentiment("This hotel is the most outstanding elyssian paradise."));
        }
    }

    /** *************************************************************
     * Compute concept sentiment and store as a side effect.
     */
    public static void oneHotelAmenitySentiment(Hotel h) {

        //System.out.println("======== " + h.name + " ========");
        String review;
        Map<String,Integer> conceptSent;
        for (int j = 0; j < h.reviews.size(); j++) {
            review = h.reviews.get(j);
            //System.out.println(review);
            conceptSent = DB.computeConceptSentiment(review);
            //System.out.println("=== " + conceptSent + " ===");
            h.addConceptSentiment(conceptSent);
        }
    }

    /** *************************************************************
     * Compute concept sentiment and store as a side effect.
     */
    public static void hotelAmenitySentiment(List<Hotel> hotels) {

        WordNet.initOnce();
        DB.readSentimentArray();
        DB.readStopConceptArray();
        Hotel h;
        for (int i = 0; i < hotels.size(); i++) {
            h = hotels.get(i);
            oneHotelAmenitySentiment(h);
        }
    }

    /** ***************************************************************
     */
    public static void execJSON(String path) {

        //ArrayList<Hotel> hotels = readJSONHotels(path,false);
        ArrayList<Hotel> hotels = readJSONHotels(path,true);
        long t1 = System.currentTimeMillis();
        //System.out.println(DB.writeSpreadsheet(Hotel.hotelReviewSUMOSentimentAsSparseMatrix(hotels,true),true));
        System.out.println("INFO in Hotel.execJSON(): done computing sentiment in " + ((System.currentTimeMillis() - t1) / 1000.0) + " seconds");
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        if (args[0].equals("-help") || StringUtil.emptyString(args[0]) ) {
            System.out.println("usage:");
            System.out.println(">java -classpath . com.articulate.sigma.dataProc.Hotel -js /home/me/data");
        }
        if (args[0].equals("-js")) {
            String path = ".";
            if (!StringUtil.emptyString(args[1]))
                path = args[1];
            execJSON(path);
        }
        //parseOneJSONReviewFile(System.getProperty("user.home") + "/Rearden/Schema/TA/2992-Arlington.json");

        //HotelDBImport();
        //System.out.println(topSUMOInReviews());
        //System.out.println(topWordSensesInReviews());
        //System.out.println(parseOneHotelFile(""));

        /*
        ArrayList<Hotel> feed = HotelDBImport(false);
        ArrayList<Hotel> reviews = DB.disambigReviews();
        DB.SUMOReviews(reviews);
        mergeHotels(feed,reviews);
        for (int i = 0; i < reviews.size(); i++) {
            Hotel h = reviews.get(i);
            if (h != null && h.feedData != null && h.feedData.size() > 0)
                System.out.println(h.asCSV());
        }
        */

        //System.out.println(printAllHotels(readCSVHotels("NHotel-sample.csv")));

        //ArrayList<Hotel> hotels = null;
        //hotels = Hotel.readXMLHotels("OHotel.xml");
        //setHotelWeights(hotels);
        //System.out.println(DB.writeSpreadsheet(Hotel.hotelReviewSUMOSentimentAsSparseMatrix(hotels),true));
        //System.out.println(DB.writeSpreadsheet(Hotel.hotelReviewSUMOSentimentAsSparseMatrix(Hotel.readOXMLhotels()),true));
        //System.out.println(Hotel.readOXMLhotels());
        //System.out.println(writeSpreadsheet(HotelXMLtoCSV(),true));

        //SUMOReviews(al);
        //System.out.println(printAllHotels(al));
    }
}