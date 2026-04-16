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
import java.util.*;

public class SimFile {

    public static final HashMap<String,String> mapping = new HashMap<>();
    public int idcount = 1;
    public ArrayList<String> sumoForms = new ArrayList<>();

    /** ***************************************************************
     */
    public SimFile() {
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
                sumoForms.add("(instance " + id + " SimFile)");
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
     * Read instance file format into a data structure that consists
     * of an instance name key and a frame value.  Frames have a slot
     * for their type information, keyed by instance name and other
     * slots consist of a slot name key and a list of values.
     */
    public Map<String,HashMap<String,ArrayList<String>>> readInstanceFile(String fname) {

        HashMap<String,HashMap<String,ArrayList<String>>> frames = new HashMap<>();
        List<String> lines = new ArrayList<String>();
        List<String> ls = FileUtil.readLines(fname);
        boolean inProps = false;
        String id = "";
        for (String line : ls) {
            if (StringUtil.emptyString(line) || line.startsWith("#") || line.trim() == "")
                continue;
            lines.add(line);
        }
        HashMap<String,ArrayList<String>> frame = new HashMap<>();
        boolean first = true;
        String instName = "";
        String type = "";
        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            String line = it.next();
            System.out.println("SimFile.readInstanceFile(): line: " + line);
            if (line.startsWith("include_once")) {
                String fnameInc = System.getProperty("user.dir") + File.separator +
                        line.trim().substring(line.indexOf(" ")+1);
                System.out.println("SimFile.readInstanceFile(): read include file: " + fnameInc);
                //    frames.putAll(readInstanceFile(fnameInc));
            }
            else if (!line.startsWith(" ") && !line.startsWith("include") && !line.startsWith("end_")) { // start of a frame
                ArrayList<String> header = new ArrayList<>(Arrays.asList(line.split(" ")));
                type = header.get(0);
                instName = header.get(1);
                frame = new HashMap<>();
                frame.put(instName,header);
            }
            else if (line.startsWith("end_")) {
                frames.put(instName,frame);
            }
            else {
                line = line.trim();
                ArrayList<String> slot = new ArrayList<>();
                slot.addAll(Arrays.asList(line.split(" ")));
                String key = slot.get(0);
                ArrayList<String> newslot = new ArrayList<>();
                newslot.addAll(slot.subList(1,slot.size()));
                frame.put(key, newslot);
            }
        }
        return frames;
    }

    /** ***************************************************************
     */
    public void  instFileToSUMO(Map<String,HashMap<String,ArrayList<String>>> frames) {

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
                sumoForms.add("(instance " + id + " SimFile)");
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
        System.out.println("  file <fname> - read frame file");
    }

    /** ***************************************************************
     * Command line entry point for this class
     *
     * @param args command line arguments (examples from showHelp)
     */
    public static void main(String[] args) throws IOException {

        System.out.println("INFO in SimFile.main()");
        Map<String, List<String>> argMap = CLIMapParser.parse(args);
        if (argMap.isEmpty() || argMap.containsKey("h"))
            showHelp();
        else {
            //if (args != null)
            //    System.out.println("SimFile.main(): args[0]: " + args[0]);
            SimFile ant = new SimFile();
            if (argMap.containsKey("r")) {
                String filename = args[1];
                if (filename.endsWith("txt"))
                    ant.readTextFile(filename);
                if (filename.endsWith("xml"))
                    ant.readXMLFile(filename);
                ant.printSUMO();
            }
            else if (argMap.containsKey("file")) {
                String fname = argMap.get("file").get(0);
                System.out.println(ant.readInstanceFile(fname));
            }
        }
    }
}
