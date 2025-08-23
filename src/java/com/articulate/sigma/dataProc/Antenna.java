package com.articulate.sigma.dataProc;

import com.articulate.sigma.HTMLformatter;
import com.articulate.sigma.KB;
import com.articulate.sigma.KBmanager;
import com.articulate.sigma.KButilities;
import com.articulate.sigma.tp.EProver;
import com.articulate.sigma.tp.LEO;
import com.articulate.sigma.tp.Vampire;
import com.articulate.sigma.trans.SUMOKBtoTPTPKB;
import com.articulate.sigma.trans.SUMOformulaToTPTPformula;
import com.articulate.sigma.trans.TPTP3ProofProcessor;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Antenna {

    public static final Map<String,String> mapping = Map.of(
            "minimum_gain", "minimumGain",
            "azimuth_beamwidth", "azimuthBeamwidth",
            "elevation_beamwidth", "elevationBeamwidth",
            "WSF_ANTENNA_PATTERN_GENAP", "GENAPPattern",
            "dB", "Decibel",
            "deg", "AngularDegree"
            );

    public ArrayList<String> sumoForms = new ArrayList<>();

    /** ***************************************************************
     */
    public void genSUMO(String id, String rel, String val, String unit) {

        String srel = mapping.get(rel);
        if (StringUtil.emptyString(srel))
            System.out.println("Error in genSUMO(): no mapping for relation: " + rel);
        String sunit = mapping.get(unit);
        if (StringUtil.emptyString(sunit))
            System.out.println("Error in genSUMO(): no mapping for unit: " + sunit);
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
                sumoForms.add("(instance " + id + " " + stype + ")");
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

        System.out.println("INFO in KB.main()");
        if (args != null && args.length > 0 && args[0].equals("-h"))
            showHelp();
        else {
            if (args != null)
                System.out.println("KB.main(): args[0]: " + args[0]);
            Antenna ant = new Antenna();
            if (args != null && args.length > 1 && args[0].contains("r")) {
                String filename = args[1];
                ant.readTextFile(filename);
                ant.printSUMO();
            }
        }
    }
}
