package com.articulate.sigma.VerbNet;

import com.articulate.sigma.SimpleElement;
import com.articulate.sigma.utils.StringUtil;
import com.articulate.sigma.utils.AVPair;
import com.articulate.sigma.wordNet.WordNet;
import com.articulate.sigma.wordNet.WordNetUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/** This code is copyright Infosys 2019.
 This software is released under the GNU Public License <http://www.gnu.org/copyleft/gpl.html>.

 Pease, A., (2003). The Sigma Ontology Development Environment,
 in Working Notes of the IJCAI-2003 Workshop on Ontology and Distributed Systems,
 August 9, Acapulco, Mexico. See also http://github.com/ontologyportal

 Authors:
 Adam Pease
 Infosys LTD.
 */
public class Verb {

    public static boolean debug = false;

    public String ID;

    public HashMap<String,Member> members = new HashMap<>();

    public HashSet<Role> roles = new HashSet<>();

    public class Role {
        String type;
        HashSet<AVPair> restrictions = new HashSet<>();
    }

    public class Member {
        String name;
        HashSet<String> wn = new HashSet<>();
        String grouping;
    }

    public HashSet<Verb> subclasses = new HashSet<>();

    public HashSet<Frame> frames = new HashSet<>();

    /** *************************************************************
     */
    public Member readMember(SimpleElement mem) {

        Member m = new Member();
        m.name = (String) mem.getAttribute("name");
        String wn = (String) mem.getAttribute("wn");
        String sumo = "";
        if (!StringUtil.emptyString(wn)) {
            String[] synsets = wn.split(" ");
            for (String s : synsets) {
                String pos = WordNetUtilities.getPOSNumFromColonKey(s);
                String synset = pos + WordNet.wn.senseKeys.get(s);
                m.wn.add(synset);
                VerbNet.wnMapping.put(synset,ID + "|" + m.name);
                if (!StringUtil.emptyString(WordNet.wn.getSUMOMapping(synset))) {
                    sumo = WordNet.wn.getSUMOMapping(synset);
                    if (debug) System.out.println("readMemberSUMO: " + sumo);
                }
            }
        }
        m.grouping = (String) mem.getAttribute("grouping");
        if (debug) System.out.println("readMember(): Member: " + m.name + ", " + m.wn + ", " + m.grouping + ", " + sumo);
        return m;
    }

    /** *************************************************************
     */
    public void readMembers(SimpleElement verb) {

        if (debug) System.out.println("VerbNet.readMembers()");
        if (debug) System.out.println("read members: ");
        for (int i = 0; i < verb.getChildElements().size(); i++) {
            SimpleElement element = (SimpleElement) verb.getChildElements().get(i);
            if (element.getTagName().equals("MEMBER")) {
                Member m = readMember(element);
                members.put(m.name,m);
            }
            else
                System.out.println("Error in Verb.readMembers(): Bad tag: " + verb.getTagName());
        }
    }

    /** *************************************************************
     */
    public static ArrayList<AVPair> readSelrestrs(SimpleElement selrestrs) {

        ArrayList<AVPair> result = new ArrayList<>();
        if (debug) System.out.println("VerbNet.readSelrestrs()");
        String logic = (String) selrestrs.getAttribute("logic");
        if (debug) System.out.println("Selectional Restriction: " + logic);
        for (int i = 0; i < selrestrs.getChildElements().size(); i++) {
            SimpleElement element = (SimpleElement) selrestrs.getChildElements().get(i);
            if (element.getTagName().equals("SELRESTR")) {
                AVPair avp = new AVPair();
                avp.attribute = (String) element.getAttribute("Value");
                avp.value = (String) element.getAttribute("type");
                if (debug) System.out.println("Selectional Restriction: " + avp.attribute + ", " + avp.value);
                result.add(avp);

            }
            else if (element.getTagName().equals("SELRESTRS")) {
                if (debug) System.out.println("Nested Selectional Restriction: ");
                result.addAll(readSelrestrs(element));
            }
            else {
                System.out.println("Error in Verb.readSelrestrs(): unknown tag: " + element);
            }
        }
        return result;
    }

    /** *************************************************************
     */
    public Role readThemeRole(SimpleElement themrole) {

        Role r = new Role();
        r.type = (String) themrole.getAttribute("type");
        if (debug) System.out.println("Theme Role: " + r.type);
        for (int j = 0; j < themrole.getChildElements().size(); j++) {
            SimpleElement element2 = (SimpleElement) themrole.getChildElements().get(j);
            if (element2.getTagName().equals("SELRESTRS"))
                r.restrictions.addAll(readSelrestrs(element2));
            else {
                System.out.println("Error in Verb.readThemeRoles(): unknown tag: " + element2);
            }
        }
        return r;
    }

    /** *************************************************************
     */
    public void readThemeRoles(SimpleElement themrole) {

        if (debug) System.out.println("VerbNet.readThemeRoles()");
        for (int i = 0; i < themrole.getChildElements().size(); i++) {
            SimpleElement element = (SimpleElement) themrole.getChildElements().get(i);
            if (element.getTagName().equals("THEMROLE")) {
                String type = (String) element.getAttribute("type");
                if (debug) System.out.println("Theme Role: " + type);
                roles.add(readThemeRole(element));
            }
            else {
                System.out.println("Error in Verb.readThemeRoles(): unknown tag: " + element);
            }
        }
    }

    /** *************************************************************
     */
    public void readFrames(SimpleElement verb) {

        if (debug) System.out.println("VerbNet.readFrames(): read frames ");
        for (int i = 0; i < verb.getChildElements().size(); i++) {
            SimpleElement element = (SimpleElement) verb.getChildElements().get(i);
            if (element.getTagName().equals("FRAME")) {
                if (debug) System.out.println("Frame");
                Frame f = new Frame();
                f.readFrame(element);
                frames.add(f);
            }
            else {
                System.out.println("Error in Verb.readFrames(): unknown tag: " + element);
            }
        }
    }

    /** *************************************************************
     */
    public void readSubclasses(SimpleElement verb) {

        if (debug) System.out.println("VerbNet.readSubclasses()");
        for (int i = 0; i < verb.getChildElements().size(); i++) {
            SimpleElement element = (SimpleElement) verb.getChildElements().get(i);
            if (element.getTagName().equals("VNSUBCLASS")) {
                String ID = element.getAttribute("ID");
                if (debug) System.out.println("ID: " + ID);
                subclasses.add(readVerb(element));
            }
        }
    }

    /** *************************************************************
     */
    public Verb readVerb(SimpleElement verb) {

        Verb v = new Verb();
        for (int i = 0; i < verb.getChildElements().size(); i++) {
            SimpleElement element = (SimpleElement) verb.getChildElements().get(i);
            if (element.getTagName().equals("MEMBERS"))
                readMembers(element);
            else if (element.getTagName().equals("THEMROLES"))
                readThemeRoles(element);
            else if (element.getTagName().equals("FRAMES"))
                readFrames(element);
            else if (element.getTagName().equals("SUBCLASSES"))
                readSubclasses(element);
            else {
                System.out.println("Error in Verb.main(): unknown tag: " + element);
            }
        }
        return v;
    }
}
