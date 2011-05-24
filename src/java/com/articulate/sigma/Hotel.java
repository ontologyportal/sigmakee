/**
 *
 */
package com.articulate.sigma;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** ***************************************************************
 */
public class Hotel {

    public String oID = "";
    public String nID = "";
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
    public HashMap<String,AVPair> facilities = new HashMap<String,AVPair>();

      // amenity code, value
    public HashMap<String,String> amenities = new HashMap<String,String>();

      // code, url
    public HashMap<String,String> media = new HashMap<String,String>();
    public ArrayList<String> reviews = new ArrayList<String>();
    public ArrayList<String> senses = new ArrayList<String>();
    public ArrayList<String> SUMO = new ArrayList<String>();
    public ArrayList<String> feedData = new ArrayList<String>();

    /** ***************************************************************
     */
    public String asCSV() {

        StringBuffer result = new StringBuffer();
        result.append("\"" + name + "\",");
        result.append("\"" + address + "\",");
        if (!StringUtil.emptyString(address2))
            result.append("\"" + address2 + "\",");
        result.append("\"" + city + "\",");
        result.append("\"" + stateProv + "\",");
        result.append("\"" + country + "\",");
        result.append("\"" + postCode + "\",");
        result.append("\"" + tel + "\",");
        //for (int i = 0; i < senses.size(); i++) {
        //    String sense = senses.get(i);
        //    result.append("\"" + sense + "\",");
        //}
        for (int i = 0; i < feedData.size(); i++) {
            String S = feedData.get(i);
            result.append(S + ",");
        }
        for (int i = 0; i < SUMO.size(); i++) {
            String S = SUMO.get(i);
            result.append("\"" + S + "\",");
            }
            return result.toString();
        }


    /** *************************************************************
     */
    public static String printAllHotels(ArrayList<Hotel> reviews) {

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < reviews.size(); i++)
            sb.append(reviews.get(i).asCSV());
        return sb.toString();
    }

    /** *******************************************************************
     * @return a list of lists of Strings which is the original input plus
     * some extra columns for the weights of several "buckets", indicating
     * fitness with respect to a particular critereon.
     */
    public static ArrayList<ArrayList<String>> setHotelWeights() {

        ArrayList<ArrayList<String>> rawWeights = DB.readSpreadsheet("OAmenities-weights.csv",null,false);

          // amenity key, value map of bucket name key, weight value
        HashMap<String,HashMap<String,String>> weights = new HashMap<String,HashMap<String,String>>();
        ArrayList<String> buckets = rawWeights.get(0);
        //System.out.println("INFO in DB.setHotelWeights(): buckets: " + buckets);
        for (int i = 1; i < rawWeights.size(); i++) {
            ArrayList<String> al = rawWeights.get(i);
            String amenity =  al.get(0).trim();
            HashMap<String,String> amenityValues = new HashMap<String,String>();
            for (int j = 2; j < buckets.size(); j++) {
                String bucket = buckets.get(j);
                String value =  al.get(j);
                amenityValues.put(bucket,value);
                //System.out.println("INFO in DB.setHotelWeights(): bucket, value: " + bucket + "," + value);
            }
            //System.out.println("INFO in DB.setHotelWeights(): adding weight for amenity: " + amenity);
            weights.put(amenity,amenityValues);
        }

        ArrayList<ArrayList<String>> hotels = DB.readSpreadsheet("O-sample.csv",null,false);
        ArrayList<String> header = hotels.get(0);

          //hotel ID key, hash map value of bucket name key, string numeric value
        HashMap<String,HashMap<String,String>> hotelWeights = new HashMap<String,HashMap<String,String>>();

        ArrayList<String> hotelHeader = hotels.get(0);
        for (int i = 2; i < buckets.size(); i++)
            hotelHeader.add(buckets.get(i));

        for (int i = 1; i < hotels.size(); i++) {
            TreeMap<String,Float> values = new TreeMap<String,Float>();  // bucket key and total weight value
            for (int k = 2; k < buckets.size(); k++)
                values.put(buckets.get(k),new Float(0));
            ArrayList<String> al = hotels.get(i);
            for (int j = 0; j < al.size(); j++) {  // go through all the amenities
                String amenityValue = al.get(j);
                String amenity = header.get(j);
                if (!StringUtil.emptyString(amenity) && amenityValue.equals("Y")) {  // if the value for the amenity is non-empty
                    //System.out.println("INFO in DB.setHotelWeights(): amenity, amenityValue is non-empty: " + amenity + "," + amenityValue);
                    if (weights.keySet().contains(amenity)) {  // if the amenity has a weight
                        //System.out.println("INFO in DB.setHotelWeights(): amenity has a weight: " + amenity);
                        HashMap<String,String> weightBuckets = weights.get(amenity);
                        Iterator<String> it = weightBuckets.keySet().iterator();
                        while (it.hasNext()) {
                            String bucket = it.next();
                            //System.out.println("INFO in DB.setHotelWeights(): weight: " + weightBuckets.get(bucket));
                            //System.out.println("INFO in DB.setHotelWeights(): bucket: " + bucket);
                            Float currentValue = values.get(bucket);
                            String bucketValue = weightBuckets.get(bucket);
                            //System.out.println("INFO in DB.setHotelWeights(): bucketValue: " + bucketValue);
                            if (!StringUtil.emptyString(bucketValue)) {
                                Float addValue = Float.parseFloat(bucketValue);
                                Float newTotal = currentValue.floatValue() + addValue;
                                values.put(bucket,new Float(newTotal));
                            }
                        }
                    }
                    //else
                        //System.out.println("INFO in DB.setHotelWeights(): weights: " + weights.keySet() + " does not contain amenity: " + amenity);
                }
            }
            Iterator<String> it = values.keySet().iterator();
            while (it.hasNext()) {
                String b = it.next();
                Float val = values.get(b);
                al.add("\"" + val.toString() + "\"");
            }
        }

        return hotels;
    }

    /** *******************************************************************
     * Used by processOneXMLHotel and HotelXMLtoCSV to compile a list of the
     * columns that should appear in the resulting CSV file.
     */
    private static TreeSet<String> hotelColumns = new TreeSet<String>();


    /** *******************************************************************
     * @param w states whether to write SUMO statements
     */
    public static ArrayList<Hotel> HotelDBImport(boolean w) {

        // 0 NTMHotelID 1 HotelName 2 StreetAddressLine1    3 City  4 Country   5 State 6 StreetAddressPostalCode
        // 7 ChainName  8 ChainWeb  9 ChainTollFree 10 HotelEMailAddress    11 HotelInternetWebsite
        // 12 ManagementCompany 13 DialCodeCountry  14 CurrentFaxAreaCode   15 CurrentFaxTelephoneNumber
        // 16 CurrentLocalAreaCode  17 CurrentLocalTelephoneNumber  18 CurrentTollFreeAreaCode
        // 19 CurrentTollFreeTelephoneNumber    20 AddIdAmadeus 21 AddIdAmadeus2    22 AddIdAmadeus3
        // 23 AddIdGalileo  24 AddIdGalileo2    25 AddIdGalileo3    26 AddIdSabre   27 AddIdSabre2  28 AddIdSabre3
        // 29 AddIdWorldspan    30 AddIdWorldspan2  31 AddIdWorldspan3  32 QtyFloors    33 QtyTotalRms  34 YearBuilt
        // 35 YearLastRenovated 36 Latitude 37 Longitude    38 AirportLocInd    39 BeachLocInd  40 CityLocInd
        // 41 ConventionCenterLocInd    42 HighwayLocInd    43 LakeLocInd   44 MountainLocInd   44 RuralAreaLocInd
        // 46 SuburbLocInd  47 TouristAreaLocInd    48 LocDesc  49 HotelDescription 50 CheckInTime  51 CheckOutTime
        // 52 AllSuiteTypeInd   53 BedBreakfastTypeInd 54 CondominiumTypeInd    55 ConferenceCenterTypeInd
        // 56 ExtendedStayTypeInd   57 HotelTypeInd 58 InnTypeInd   59 LodgeTypeInd 60 MotelTypeInd
        // 61 ResortTypeInd 62 AVEquipmentRentalInd 63 BusinessCenterInd    64 AmenHtlComputerUseInd
        // 65 AmenHtlFaxGuestInd    66 AmenHtlMtgFacInd 67 AmenHtlPhotoSvcInd   68 AmenHtlSecretarySvcInd
        // 69 AmenTechTrainCtrInd   70 AmenHtlBabySitInd    71 NearestStreet    72 NearestCity  73 NearestCityDist
        // 74 NearestCityDistUnit   75 AmenHtlWheelchairPublInd 76 AmenHtlATMInd    77 BarberHairStylistInd
        // 78 AmenHtlCarRentalInd   79 ComplementaryCoffeeInd   80 ComplementaryTransportationInd
        // 81 ConciergeServicesInd  82 ConciergeClubFloorInd    83 AmenHtlCribRollawayInd   84 CurrencyExchangeInd
        // 85 GiftShopInd   86 LaundryRoomInd   87 LaundryDryCleaningServiceInd 88 AmenHtlMultiLingualStaffInd
        // 89 OnSiteBarLoungeQty    90 OnSiteRestrQty   91 RoomServiceLimitedInd    92 RestrDesc1   93 RestrDesc2
        // 94 RestrDesc3    95 RestaurantName1  96 RestaurantName2  97 RestrName3   98 RestaurantOnsiteInd1
        // 99 RestaurantOnsiteInd2  100 RestrOnSiteInd3 101 RestaurantType1 102 RestaurantType2 103 RestrType3
        // 104 AirConditioningInRoomInd 105 BalconyTerraceInd   106 AmenRmBroadbandInternetInd
        // 107 AmenRmBroadbandInternetFeeInd    108 CoffeeMakerInd  109 DailyMaidServiceInd 110 VCRInRoomInd
        // 111 FireplaceInd 112 AmenRmFitnessEqtInd 113 IronInd 114 KitchenKitchenetteInd   115 MicrowaveOvenInd
        // 116 MiniBarInd   117 NewspaperFreeInd    118 NonSmokingRoomsInd  119 RefrigeratorInd 120 SafeInRoomInd
        // 121 AmenRmPhoneInd   122 TelephoneVoicemailInd   123 AmenRmTVInd 124 TelevisionCableSatelliteInd
        // 125 AmenRmWirelessInternetInd    126 AmenRmWirelessInternetFeeInd    127 WhirlpoolInd    128 AmenHtlElecKeyInd
        // 129 AmenHtlPrkgLitInd    130 AmenHtlSmokeAlarmInd    131 AmenHtlSprinkersInd 132 AmenSurveillanceCameraInd
        // 133 AmenHtlUniformSecurityInd    134 AmenHtlWomenOnlyFlrInd  135 AmenRecBeachInd 136 AmenRecBikesInd
        // 137 AmenRecGameRmInd 138 AmenRecGolfInd  139 HealthClubLimitedind    140 AmenRecHealthSpaInd
        // 141 AmenRecHottubInd 142 AmenRecJoggingInd   143 AmenRecPoolIndoorInd    144 AmenRecPoolOutdoorInd
        // AmenRecSnowskiInd    AmenRecTennisOutdoorInd ArptCode1   ArptCode2   ArptDir1    ArptDir2    ArptDist1   ArptDist2   ArptDistUnit1   ArptDistUnit2   ArptTime1   ArptTime2   ArptFreeTransInd1   ArptFreeTransInd2   ArptName1   ArptName2   AmenPrkgOnFreeInd   AmenPrkgOnPaidInd   AmenPrkgOnValetInd  ArptNearInd AmenHtlCommonInetBroadInd   AmenHtlCommonInetBroadFeeInd    AmenHtlCommonInetWirelessInd    AmenHtlCommonInetWirelessFeeInd MtgRmBroadbandInternetInd   MtgRmBroadbandInternetFeeInd    MtgRmWirelessInternetInd    MtgRmWirelessInternetFeeInd MtgTotalCapacity    MtgTotalRmSpace MtgTotalRms MtgChtCapBanq1  MtgChtCapBanq2  MtgChtCapBanq3  MtgChtCapBanq4  MtgChtCapClassRm1   MtgChtCapClassRm2   MtgChtCapClassRm3   MtgChtCapClassRm4   MtgChtDimension1    MtgChtDimension2    MtgChtDimension3    MtgChtDimension4    MtgChtName1 MtgChtName2 MtgChtName3 MtgChtName4 MtgChtSize1 MtgChtSize2 MtgChtSize3 MtgChtSize4 CreditCardsAcceptInd    PolicyCancel    PolicyDeposit   PolicyResvHeld  PolicyResvGtyInd    PolicyRestrictions  MealIncAllIncInd    MealIncAPInd    MealIncCPInd    MealIncEPInd    MealIncFBInd    MealIncMAPInd   DiscountCorpInd DiscountGroupInd    RateCalcDblMax  RateCalcDblMin  RateCalcSteMax  RateCalcSteMin  RatesCurrencyCode   RateDailyWklyInd    POIDesc1    POIDesc2    POIDesc3    POIDir1 POIDir2 POIDir3 POIDist1    POIDist2    POIDist3    POIDistUnit1    POIDistUnit2    POIDistUnit3    POIName1    POIName2    POIName3    ImageFileName   DateOfExtract   NTMCrownRating

        HashMap<String,String> abbrevs = DB.readStateAbbrevs();
        ArrayList<Hotel> result = new ArrayList<Hotel>();
        ArrayList<ArrayList<String>> f = DB.readSpreadsheet("Hotels_SF.csv",null,false);
        for (int i = 1; i < f.size(); i++) {
            Hotel h = new Hotel();
            ArrayList al = f.get(i);
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
     * Convert a particular XML markup into an array suitable for export to csv file
     */
    public static ArrayList<ArrayList<String>> readHotelXML() {

          // CSV data structure: a list of lines containing list of column cells
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();

          // CSV precursor - a list of rows, which is a map of column name keys and cell contents values
        ArrayList<TreeMap<String,String>> lines = new ArrayList<TreeMap<String,String>>();

        String fname = "HotelSample-O.xml";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringElementContentWhitespace(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(fname);
            Element docEle = dom.getDocumentElement();
            NodeList nl = docEle.getElementsByTagName("hotel");
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);  // a <hotel>
                    TreeMap<String,String> entry = processOneXMLHotel(el);
                    lines.add(entry);
                }

                  // collect all possible column names and assign them a number
                TreeMap<String,Integer> columnNumbers = new TreeMap<String,Integer>();
                int count = 0;
                ArrayList<String> header = DB.fill("",hotelColumns.size());
                Iterator<String> it = hotelColumns.iterator();
                while (it.hasNext()) {
                    String columnName = it.next();
                    header.set(count,columnName);
                    columnNumbers.put(columnName,new Integer(count++));
                }
                result.add(header);

                  // iterate through all rows
                for (int i = 0; i < lines.size(); i++) {
                    TreeMap<String,String> oneHotel = lines.get(i);
                    Iterator<String> it2 = oneHotel.keySet().iterator();
                    ArrayList<String> line = DB.fill("",hotelColumns.size());
                    while (it2.hasNext()) {  // iterate through the columns
                        String columnName = it2.next();
                        String cellValue = oneHotel.get(columnName);
                        Integer colNum = columnNumbers.get(columnName);
                        line.set(colNum.intValue(),cellValue);
                    }
                    result.add(line);
                }
            }
        }
        catch (Exception e) {
            System.out.println("File error reading " + fname + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return result;
    }

    /** *******************************************************************
     */
    public static TreeMap<String,String> processOneXMLHotel(Element h) {

        int maxString = 30;
        TreeMap<String,String> values = new TreeMap<String,String>();
        NodeList features = h.getChildNodes();
        for (int i = 0; i < features.getLength(); i++) {
            if (features.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element feature = (Element) features.item(i);
                //System.out.println(feature.toString());
                if (feature.getTagName().equals("addr")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element f = (Element) fs.item(j);
                            String tag = "addr-" + f.getTagName();
                            String text = f.getTextContent();
                            if (!StringUtil.emptyString(text) && text.length() > maxString)
                                text = text.substring(0,maxString);
                            values.put(tag,text);
                            // String.format("%03d", count) + "-" +
                            hotelColumns.add(tag);
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
                                values.put(code.getTextContent(),"Y");
                                hotelColumns.add(code.getTextContent());
                            }
                        }
                    }
                }
                else if (feature.getTagName().equals("medias")) {
                    /*
                    ArrayList<SimpleElement> fs = feature.getChildElements();
                    for (int j = 0; j < fs.size(); j++) {
                        SimpleElement media = fs.get(j);
                        SimpleElement type = media.getChildByFirstTag("type");
                        SimpleElement url = media.getChildByFirstTag("url");
                        values.put("media-" + type.getText(),url.getText());
                        hotelColumns.add("media-" + type.getText());
                    }
                    */
                }
                else if (feature.getTagName().equals("leadprice")) {
                    NodeList fs = feature.getChildNodes();
                    for (int j = 0; j < fs.getLength(); j++) {
                        if (fs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            Element f = (Element) fs.item(j);
                            values.put("leadprice-" + f.getTagName(),f.getTextContent());
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
                                values.put(type.getTextContent(),text);
                                hotelColumns.add(type.getTextContent());
                            }
                        }
                    }
                }
                else {
                    String text = feature.getTextContent();
                    if (!StringUtil.emptyString(text) && text.length() > maxString)
                        text = text.substring(0,maxString);
                    values.put(feature.getTagName(),text);
                    hotelColumns.add(feature.getTagName());
                }
            }
        }
        return values;
    }

    /** *******************************************************************
     */
    public static ArrayList<Hotel> readCSVHotels(String fname) {

        // 0 NTMHotelID 1 HotelName 2 StreetAddressLine1    3 City  4 Country   5 State 6 StreetAddressPostalCode
        // 7 ChainName  8 ChainWeb  9 ChainTollFree 10 HotelEMailAddress    11 HotelInternetWebsite
        // 12 ManagementCompany 13 DialCodeCountry  14 CurrentFaxAreaCode   15 CurrentFaxTelephoneNumber
        // 16 CurrentLocalAreaCode  17 CurrentLocalTelephoneNumber  18 CurrentTollFreeAreaCode
        // 19 CurrentTollFreeTelephoneNumber    20 AddIdAmadeus 21 AddIdAmadeus2    22 AddIdAmadeus3
        // 23 AddIdGalileo  24 AddIdGalileo2    25 AddIdGalileo3    26 AddIdSabre   27 AddIdSabre2  28 AddIdSabre3
        // 29 AddIdWorldspan    30 AddIdWorldspan2  31 AddIdWorldspan3  32 QtyFloors    33 QtyTotalRms  34 YearBuilt
        // 35 YearLastRenovated 36 Latitude 37 Longitude    38 AirportLocInd    39 BeachLocInd  40 CityLocInd
        // 41 ConventionCenterLocInd    42 HighwayLocInd    43 LakeLocInd   44 MountainLocInd   44 RuralAreaLocInd
        // 46 SuburbLocInd  47 TouristAreaLocInd    48 LocDesc  49 HotelDescription 50 CheckInTime  51 CheckOutTime
        // 52 AllSuiteTypeInd   53 BedBreakfastTypeInd 54 CondominiumTypeInd    55 ConferenceCenterTypeInd
        // 56 ExtendedStayTypeInd   57 HotelTypeInd 58 InnTypeInd   59 LodgeTypeInd 60 MotelTypeInd
        // 61 ResortTypeInd 62 AVEquipmentRentalInd 63 BusinessCenterInd    64 AmenHtlComputerUseInd
        // 65 AmenHtlFaxGuestInd    66 AmenHtlMtgFacInd 67 AmenHtlPhotoSvcInd   68 AmenHtlSecretarySvcInd
        // 69 AmenTechTrainCtrInd   70 AmenHtlBabySitInd    71 NearestStreet    72 NearestCity  73 NearestCityDist
        // 74 NearestCityDistUnit   75 AmenHtlWheelchairPublInd 76 AmenHtlATMInd    77 BarberHairStylistInd
        // 78 AmenHtlCarRentalInd   79 ComplementaryCoffeeInd   80 ComplementaryTransportationInd
        // 81 ConciergeServicesInd  82 ConciergeClubFloorInd    83 AmenHtlCribRollawayInd   84 CurrencyExchangeInd
        // 85 GiftShopInd   86 LaundryRoomInd   87 LaundryDryCleaningServiceInd 88 AmenHtlMultiLingualStaffInd
        // 89 OnSiteBarLoungeQty    90 OnSiteRestrQty   91 RoomServiceLimitedInd    92 RestrDesc1   93 RestrDesc2
        // 94 RestrDesc3    95 RestaurantName1  96 RestaurantName2  97 RestrName3   98 RestaurantOnsiteInd1
        // 99 RestaurantOnsiteInd2  100 RestrOnSiteInd3 101 RestaurantType1 102 RestaurantType2 103 RestrType3
        // 104 AirConditioningInRoomInd 105 BalconyTerraceInd   106 AmenRmBroadbandInternetInd
        // 107 AmenRmBroadbandInternetFeeInd    108 CoffeeMakerInd  109 DailyMaidServiceInd 110 VCRInRoomInd
        // 111 FireplaceInd 112 AmenRmFitnessEqtInd 113 IronInd 114 KitchenKitchenetteInd   115 MicrowaveOvenInd
        // 116 MiniBarInd   117 NewspaperFreeInd    118 NonSmokingRoomsInd  119 RefrigeratorInd 120 SafeInRoomInd
        // 121 AmenRmPhoneInd   122 TelephoneVoicemailInd   123 AmenRmTVInd 124 TelevisionCableSatelliteInd
        // 125 AmenRmWirelessInternetInd    126 AmenRmWirelessInternetFeeInd    127 WhirlpoolInd    128 AmenHtlElecKeyInd
        // 129 AmenHtlPrkgLitInd    130 AmenHtlSmokeAlarmInd    131 AmenHtlSprinkersInd 132 AmenSurveillanceCameraInd
        // 133 AmenHtlUniformSecurityInd    134 AmenHtlWomenOnlyFlrInd  135 AmenRecBeachInd 136 AmenRecBikesInd
        // 137 AmenRecGameRmInd 138 AmenRecGolfInd  139 HealthClubLimitedind    140 AmenRecHealthSpaInd
        // 141 AmenRecHottubInd 142 AmenRecJoggingInd   143 AmenRecPoolIndoorInd    144 AmenRecPoolOutdoorInd
        // AmenRecSnowskiInd    AmenRecTennisOutdoorInd ArptCode1   ArptCode2   ArptDir1    ArptDir2    ArptDist1   ArptDist2   ArptDistUnit1   ArptDistUnit2   ArptTime1   ArptTime2   ArptFreeTransInd1   ArptFreeTransInd2   ArptName1   ArptName2   AmenPrkgOnFreeInd   AmenPrkgOnPaidInd   AmenPrkgOnValetInd  ArptNearInd AmenHtlCommonInetBroadInd   AmenHtlCommonInetBroadFeeInd    AmenHtlCommonInetWirelessInd    AmenHtlCommonInetWirelessFeeInd MtgRmBroadbandInternetInd   MtgRmBroadbandInternetFeeInd    MtgRmWirelessInternetInd    MtgRmWirelessInternetFeeInd MtgTotalCapacity    MtgTotalRmSpace MtgTotalRms MtgChtCapBanq1  MtgChtCapBanq2  MtgChtCapBanq3  MtgChtCapBanq4  MtgChtCapClassRm1   MtgChtCapClassRm2   MtgChtCapClassRm3   MtgChtCapClassRm4   MtgChtDimension1    MtgChtDimension2    MtgChtDimension3    MtgChtDimension4    MtgChtName1 MtgChtName2 MtgChtName3 MtgChtName4 MtgChtSize1 MtgChtSize2 MtgChtSize3 MtgChtSize4 CreditCardsAcceptInd    PolicyCancel    PolicyDeposit   PolicyResvHeld  PolicyResvGtyInd    PolicyRestrictions  MealIncAllIncInd    MealIncAPInd    MealIncCPInd    MealIncEPInd    MealIncFBInd    MealIncMAPInd   DiscountCorpInd DiscountGroupInd    RateCalcDblMax  RateCalcDblMin  RateCalcSteMax  RateCalcSteMin  RatesCurrencyCode   RateDailyWklyInd    POIDesc1    POIDesc2    POIDesc3    POIDir1 POIDir2 POIDir3 POIDist1    POIDist2    POIDist3    POIDistUnit1    POIDistUnit2    POIDistUnit3    POIName1    POIName2    POIName3    ImageFileName   DateOfExtract   NTMCrownRating

        HashMap<String,String> abbrevs = DB.readStateAbbrevs();
        ArrayList<Hotel> result = new ArrayList<Hotel>();
        ArrayList<ArrayList<String>> f = DB.readSpreadsheet(fname,null,false);
        for (int i = 1; i < f.size(); i++) {
            Hotel h = new Hotel();
            ArrayList al = f.get(i);
            h.feedData = al;
            String NTMHotelID                = (String) al.get(0);
            h.nID = NTMHotelID;
            String HotelName                 = (String) al.get(1);
            h.name = StringUtil.removeEnclosingQuotes(HotelName);
            String hotelAddress              = (String) al.get(2);
            h.address = StringUtil.removeEnclosingQuotes(hotelAddress).trim();
            String HotelID = StringUtil.stringToKIF(HotelName + NTMHotelID,true);
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
            String latitude                  = (String) al.get(36);
            String longitude                 = (String) al.get(37);
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
            }
        }
        return result;
    }

    /** *************************************************************
     * @param fname has no file extension or directory
     */
    public static Hotel parseOneHotelFile(String fname) {

        Hotel h = new Hotel();
        ArrayList<AVPair> result = new ArrayList<AVPair>();
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
                        DB.parseAddress(h,mAddress.group(1).trim());
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
     */
    public static ArrayList<Hotel> parseAllHotelFiles() {

        System.out.println("INFO in parseAllHotelFiles()");
        ArrayList<Hotel> result = new ArrayList<Hotel>();
        String fname = "hotelonly-SF.txt";
        LineNumberReader lnr = null;
        try {
            File fin  = new File(fname);
            FileReader fr = new FileReader(fin);
            if (fr != null) {
                lnr = new LineNumberReader(fr);
                String line = null;
                while ((line = lnr.readLine()) != null) {
                    if (!StringUtil.emptyString(line))
                        result.add(parseOneHotelFile(line));
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
    public static void matchHotels(Hotel feedHotel, Hotel reviewsHotel) {

        //if (feedHotel.name.equals(reviewsHotel.name)) {
        //    System.out.println(feedHotel.asCSV());
        //    System.out.println(reviewsHotel.asCSV());
        //}
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

    /** ***************************************************************
     */
    public static void main(String[] args) {

        //HotelDBImport();
        //System.out.println(topSUMOInReviews());
        //System.out.println(topWordSensesInReviews());
        //System.out.println(parseOneHotelFile(""));

        /*
        ArrayList<Hotel> feed = HotelDBImport(false);
        ArrayList<Hotel> reviews = disambigReviews();
        SUMOReviews(reviews);
        mergeHotels(feed,reviews);
        for (int i = 0; i < reviews.size(); i++) {
            Hotel h = reviews.get(i);
            if (h != null && h.feedData != null && h.feedData.size() > 0)
                System.out.println(h.asCSV());
        }
        */
        System.out.println(DB.writeSpreadsheet(Hotel.setHotelWeights(),true));

        //System.out.println(writeSpreadsheet(HotelXMLtoCSV(),true));

        //SUMOReviews(al);
        //System.out.println(printAllHotels(al));

    }

}
