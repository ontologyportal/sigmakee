package com.articulate.sigma.dataProc;

import com.articulate.sigma.*;
import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.LEO;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.FileUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Antenna {

    public static final HashMap<String,String> mapping = new HashMap<>();
    public int idcount = 1;
    public ArrayList<String> sumoForms = new ArrayList<>();

    /** ***************************************************************
     */
    public Antenna() {
        mapping.put("minimum_gain", "minimumGain");
        mapping.put("azimuth_beamwidth", "azimuthBeamwidth");
        mapping.put("az_beam_width", "azimuthBeamwidth");
        mapping.put("elevation_beamwidth", "elevationBeamwidth");
        mapping.put("el_beam_width", "elevationBeamwidth");
        mapping.put("WSF_ANTENNA_PATTERN_GENAP", "GENAPPattern");
        mapping.put("SIN_XOVERX", "RectangularSinexOverx");
        mapping.put("dB", "Decibel");
        mapping.put("deg", "AngularDegree");
        mapping.put("Mathlib::Decibel", "Decibel");
        mapping.put("Mathlib::Degree", "AngularDegree");
    }

    /** ***************************************************************
     */
    public void genSUMO(String id, String rel, String val, String unit) {

        String srel = mapping.get(rel);
        if (StringUtil.emptyString(srel))
            System.out.println("Error in genSUMO(): no mapping for relation: " + rel);
        String sunit = mapping.get(unit);
        if (StringUtil.emptyString(sunit))
            System.out.println("Error in genSUMO(): no mapping for unit: " + unit);
        sumoForms.add("(" + srel + " " + id + " (MeasureFn " + val + " " + sunit + "))");
    }

    /** ***************************************************************
     */
    public void printSUMO() {

        for (String s : sumoForms)
            System.out.println(s);
    }

    /** ***************************************************************
     */
    public void readXMLFile(String fname) {

        //List<String> lines = FileUtil.readLines(fname);
        SimpleElement ant = null;
        File xmlFile = new File(fname);
        try (Reader br = new BufferedReader(new FileReader(xmlFile))) {
            SimpleDOMParser sdp = new SimpleDOMParser();
            ant = sdp.parse(br);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.print("Parse completed.  Number of elements: ");
        //System.out.println(ant.getChildElements().size());
        //System.out.print("Parse completed.  First element: ");
        //System.out.println(ant.getChildElements().get(0));
        //StringBuffer sb = new StringBuffer();
        //for (String s : lines)
        //    sb.append(s + "\\n");
        //BasicXMLparser bp = new BasicXMLparser(sb.toString());
        String type = "";
        String sunit = "";
        String id = "";
        for (SimpleElement be : ant.getChildElements()) {
            //System.out.println("Info in genSUMO(): be: " + be);
            if (be.getTagName().equals("antpatterntype")) {
                type = be.getText();
                String stype = mapping.get(type);
                id = type + Integer.toString(idcount++);
                if (StringUtil.emptyString(stype))
                    System.out.println("Error in genSUMO(): no mapping for type: " + type);
                sumoForms.add("(instance " + id + " Antenna)");
                sumoForms.add("(antennaPattern " + id + " " + stype + ")");
            }
            if (mapping.keySet().contains(be.getTagName())) {
                if (be.getAttribute("unit") != null) {
                    String unit = be.getAttribute("unit");
                    sunit = mapping.get(unit);
                    if (StringUtil.emptyString(sunit))
                        System.out.println("Error in genSUMO(): no mapping for unit: " + unit);
                }
                String srel = mapping.get(be.getTagName());
                String val = be.getText();
                sumoForms.add("(" + srel + " " + id + " (MeasureFn " + val + " " + sunit + "))");
            }
        }
    }

    /** ***************************************************************
     */
    public void readTextFile(String fname) {

        List<String> lines = FileUtil.readLines(fname);
        boolean inProps = false;
        String id = "";
        for (String line : lines) {
            if (line.startsWith("antenna_pattern")) {
                inProps = true;
                String[] args = line.trim().split(" ");
                id = args[1];
                String type = args[2];
                String stype = mapping.get(type);
                sumoForms.add("(instance " + id + " Antenna)");
                sumoForms.add("(antennaPattern " + id + " " + stype + ")");
                continue;
            }
            if (line.trim().startsWith("//"))
                continue;
            if (line.startsWith("end_antenna_pattern")) {
                inProps = false;
                continue;
            }
            if (inProps) {
                String[] args = line.trim().split(" ");
                String rel = args[0];
                String val = args[1];
                String unit = args[2];
                genSUMO(id, rel,val,unit);
            }
        }
    }

    /** ***************************************************************
     */
    public static void showHelp() {

        System.out.println("KB class");
        System.out.println("  options (with a leading '-'):");
        System.out.println("  h - show this help screen");
        System.out.println("  r - read files and translate to SUMO");
    }

    /** ***************************************************************
     * Command line entry point for this class
     *
     * @param args command line arguments (examples from showHelp)
     */
    public static void main(String[] args) throws IOException {

        //System.out.println("INFO in Antenna.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            //if (args != null)
            //    System.out.println("Antenna.main(): args[0]: " + args[0]);
            Antenna ant = new Antenna();
            if (args != null && args.length > 1 && args[0].contains("r")) {
                String filename = args[1];
                if (filename.endsWith("txt"))
                    ant.readTextFile(filename);
                if (filename.endsWith("xml"))
                    ant.readXMLFile(filename);
                ant.printSUMO();
            }
        }
    }
}
