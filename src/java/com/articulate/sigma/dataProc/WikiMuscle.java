package com.articulate.sigma.dataProc;

import com.articulate.sigma.DB;
import com.articulate.sigma.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class WikiMuscle {

    public class Record {
        public String muscle = "";
        public String muscleText = "";
        public String origin = "";
        public String insertion = "";
        public String artery = "";
        public String nerve = "";
        public String nerveText = "";
        public String action = "";
        public String antagonist = "";
        public int numOccurr = 0;
        public int wikiNum = 0;

    }

    public List<Record> records = new ArrayList<>();

    /** ***************************************************************
     */
    public void read(String fname) {

        List<List<String>> cells = DB.readSpreadsheet(fname, null,false,',');
        int count = 0;
        for (List<String> line : cells) {
            Record rec = this.new Record();
            if (count++ > 1 && line != null && line.size() > 7 &&
                    (!StringUtil.emptyString(line.get(1)) ||
                    !StringUtil.emptyString(line.get(2)) ||
                            !StringUtil.emptyString(line.get(3)))) {
                if (!StringUtil.emptyString(line.get(0))) {
                    rec.muscle = StringUtil.toCamelCase(line.get(0));        // muscle
                    rec.muscleText = line.get(0);
                }
                if (!StringUtil.emptyString(line.get(1)))
                    rec.origin = StringUtil.toCamelCase(line.get(1));        // origin
                if (!StringUtil.emptyString(line.get(2)))
                    rec.insertion = StringUtil.toCamelCase(line.get(2));        // insertion
                if (!StringUtil.emptyString(line.get(3)))
                    rec.artery = StringUtil.toCamelCase(line.get(3));        // artery
                if (!StringUtil.emptyString(line.get(4))) {
                    rec.nerve = StringUtil.toCamelCase(line.get(4));        // nerve
                    rec.nerveText = line.get(4);        // nerve
                }
                if (!StringUtil.emptyString(line.get(5)))
                    rec.action = line.get(5);        // action
                if (!StringUtil.emptyString(line.get(6)))
                    rec.antagonist = StringUtil.toCamelCase(line.get(6));        // antagonist
                records.add(rec);
            }
        }
    }

    /** ***************************************************************
     */
    public void export() {

        for (Record rec : records) {
            if (!StringUtil.emptyString(rec.muscle)) {
                System.out.println("(subclass " + rec.muscle + "Muscle Muscle)");
                System.out.println("(termFormat EnglishLanguage " + rec.muscle + "Muscle \"" + rec.muscleText + "\")");
            }
            if (!StringUtil.emptyString(rec.muscle) && !StringUtil.emptyString(rec.origin))
                System.out.println("(muscleOrigin " + rec.muscle + "Muscle " + rec.origin + ")");
            if (!StringUtil.emptyString(rec.muscle) && !StringUtil.emptyString(rec.insertion))
                System.out.println("(muscleInsertion " + rec.muscle + "Muscle " + rec.insertion + ")");
            if (!StringUtil.emptyString(rec.muscle) && !StringUtil.emptyString(rec.artery))
                System.out.println("(suppliesBlood " + rec.artery + " " + rec.muscle + "Muscle)");
            if (!StringUtil.emptyString(rec.muscle) && !StringUtil.emptyString(rec.nerve)) {
                System.out.println("(ennervates " + rec.nerve + " " + rec.muscle + "Muscle)");
                System.out.println("(subclass " + rec.nerve + " Nerve)");
                System.out.println("(termFormat EnglishLanguage " + rec.nerve + " \"" + rec.nerveText + "\")");
            }
            if (!StringUtil.emptyString(rec.muscle) && !StringUtil.emptyString(rec.action))
                System.out.println("(documentation " + rec.muscle + "Muscle EnglishLanguage \"The &%" +
                        rec.muscle + "Muscle " + rec.action + ".\")");
            if (!StringUtil.emptyString(rec.muscle) && !StringUtil.emptyString(rec.antagonist))
                System.out.println("(antagonistMuscles " + rec.antagonist + "Muscle " + rec.muscle + "Muscle)");
            System.out.println();
        }
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        WikiMuscle wm = new WikiMuscle();
        wm.read(args[0]);
        wm.export();
    }
}
