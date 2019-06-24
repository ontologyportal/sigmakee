package edu.dlsu.SUMOs.main;

import java.awt.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.tp.Vampire;

import edu.dlsu.SUMOs.obj.Input;
import edu.dlsu.SUMOs.obj.Instance;
import edu.dlsu.SUMOs.obj.Rule;
import edu.dlsu.SUMOs.util.DateUtils;
import edu.dlsu.SUMOs.util.ReadWriteTextFile;

public class PictureEditor {

    private HashMap<String, String> hashmap = new HashMap<String, String>();
    private static ArrayList<String> multipleAttributes;
    private static int MAXDAYS = 1;
    private static int days = 0;
    
    //for statistics only
    private static String timeBegin = "";
    private static String timeEnd = "";
    private static boolean generalCount = false;
    private static boolean conflictCount = false;
    private static int general_count = 0;
    private static int conflict_count = 0;
    private static int query_count = 0;
    private static int assert_count = 0;
    
    // vampire query parameters
    private final static int TIME_lIMIT = 30;
    private final static int RESULTS_LIMIT = 10;
    
    // print in console
    //        isPrint = print queries and results
    //        isPrint2 = print assertions
    public static boolean isPrint = true;
    public static boolean isPrint2 = true;
    
    // themes
    private final static int THEME_BEHONEST = 1;
    //private final static int THEME_EATGOODFOOD = 2;
    private final static int THEME_SLEEPEARLY = 3;
    private final static int THEME_BEBRAVE = 4;
    private final static int THEME_BECAREFUL = 5;
    
    // characters
    private final static int FEMALE_CHILD_RABBIT = 1;
    private final static int MALE_CHILD_RABBIT = 2;
    private final static int FEMALE_ADULT_RABBIT = 3;
    private final static int MALE_ADULT_RABBIT = 4;
    private final static int FEMALE_ADULT_ELEPHANT = 5;
    private final static int FEMALE_CHILD_ELEPHANT = 6;
    
    //objects
    private final static int OBJECT_LAMP = 1;
    private final static int OBJECT_CANDY = 2;
    private final static int OBJECT_TOYS = 3;
    private final static int OBJECT_DRINKINGCUP = 4;
    
    //backgrounds
    private final static int BG_LIVINGROOM = 1; 
    private final static int BG_CLASSROOM = 2;
    
    private boolean isChangePhase = false;
    private boolean reset = false;
    private String log = "";
    private boolean writeLog = true;
    private boolean saveResults = true;
    private boolean isConflict = false;

    private boolean getCaseRoles = false;
    private boolean allowDuplicates = true;
    private ArrayList<String> phaseKnowledge;
    private String phase;
    private ArrayList<String> omissibleKnowledge;
    private final String DATABASE = System.getenv("SIGMA_HOME")+"\\inference\\SUMO-v.kif";
    private String result;
    private ArrayList<String> resettableAttributes;
    private ArrayList<String> resettableKnowledge;
    private ArrayList<String> results;
    private ArrayList<String> oldKnowledge;
    private ArrayList<String> oneTimeEventsAndAttributes;
    private int instanceCnt;
    private Vampire vampire;
    private ArrayList<String> instances;
    private Collection<String> operationalKnowledge;
    private Collection<Instance> childCharacters;
    private Rule rule;
    private Instance childCharacter;
    private Instance adultCharacter;
    private Instance object;
    private Instance background;
    private Instance story;
    private static boolean doneSomething = false;
    
    public PictureEditor(final Input input) {
        try {    
            System.out.println("**********Picture Editor v0.4**********");
            initializeSettings();
            initializeStory(input);
            initializeTime(input);
            assertCharacters(input);
            
            assertObjects(input);
            assertBackground(input.getBackground());
            obtainLocations();
                 
            // introduction phase: introduce the names of the child characters
            for(int i=0;i<childCharacters.size();i++) {
                submitQuery("name", ((Instance) childCharacters.toArray()[i]).getName(), "?X");
                addStory("(name "+((Instance) childCharacters.toArray()[i]).getName()+" "+results.get(0).toString()+")");
            }     
            
//             parent statement
             checkPredicates();                
            
            // problem phase: introduce the problem? problems could be accidents, etc.
                // depending on the theme being chosen 
                // step 1: Check what is the first capable action that can be done by the child character
                // step 2: Check any problem / rules defied by the character
            allowDuplicates = false;
            
            changeStoryPhase("Problem");            
            
            while(true) {
                determineCapabilities(((Instance) childCharacters.toArray()[0]));
                checkRuleDone();
                submitQuery("instance", "?X", "ProblemEvent");
                
                if(!results.get(0).equals("no")) {
                    //check if we can still do something before moving on
                    while(true) {
                        determineCapabilities(((Instance) childCharacters.toArray()[0]));
                        if(!doneSomething) {
                            break;
                        }
                    }
                    break;
                } 
            } 
            
            // rising action phase: these are the consequences to the problem
                // depending on the theme being chosen
                // step 1: check for any attribute changes for all things first; e.g. vase is broken and character is feeling anxiety - now it is in the changestoryphase method
                    // note on step 1: i believe the checking of attributes would happen every time any query is made
                // step 2: check for any possible action the child may do due to the problem
                // step 3: if parent character was not near the child before, he/she will go near to discover problem
                // step 4: check again for any possible action the child may do when near adult
                // step 5: check attribute changes; by now character is guilty for lying
            changeStoryPhase("RisingAction");
            
            determineCapabilities((Instance) childCharacters.toArray()[0]);
            
            // the adult character should be in the same room with child
            submitFormula("located",adultCharacter.getName(),background.getName());
            int ctr = 0;    
            do {
                if(ctr==10) {
                    System.out.println("insufficient story knowledge.. skipping to next phase");
                    break;
                }
                ctr++;
                determineCapabilities(adultCharacter);
                determineCapabilities((Instance) childCharacters.toArray()[0]);
                submitQuery("instance", "?X", "RisingActionEvent");
            } while(results.get(0).equals("no"));
                    
            // solution phase: the solution to the problem
                // step 1: continue asking what the child can do until a solution event happens which could be:
                     // child has the good trait by now
                     // confesses his bad deed / complains about consequence
                // step 2: adult character tells child again of rule / gives a new rule
            changeStoryPhase("Solution");
            submitQuery("(attribute RabbitCharacter5 ?X)");
            
            do
            {
                for(int i=0;i<childCharacters.size();i++) {
                    determineCapabilities((Instance) childCharacters.toArray()[i]);
                }
                submitQuery("instance", "?X", "SolutionEvent");
            } while (results.get(0).equals("no"));
            
            // tell rule again
            if(rule!=null) {
                tell(adultCharacter.getName(), ((Instance) childCharacters.toArray()[0]).getName(), rule.getOrig());
            }
            
            determineCapabilities(adultCharacter);
            determineCapabilities((Instance) childCharacters.toArray()[0]);
            
            // climax phase: the moral lesson learned by the child - it is assumed that he has changed his bad traits already when we got this far
                // step 1: character will follow rule
                // step 2: character will not experience consequence
                // step 3: character will usually feel happy
            changeStoryPhase("Climax");
            
            if(rule!=null) {
                followRule();
            }
            
            checkConflicts(this.phase);

            vampire.terminate();
            createLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doGeneralQueries() {
        if(!phase.equals("Introduction")) {
            checkPredicates();
            checkNewEvents();
        }
    }
    
    private void obtainLocations() {
        System.out.println("*****obtainLocations()");
        
        //child characters
        for(int i=0;i<childCharacters.size();i++) {
            submitQuery("located", ((Instance) childCharacters.toArray()[i]).getName(), "?X");
            if(!results.get(0).equals("no")) {
                addStory("(located "+((Instance) childCharacters.toArray()[i]).getName()+" "+results.get(0)+")");
            }
        }
        
        //adult character
        submitQuery("located", adultCharacter.getName(), "?X");
        if(!results.get(0).equals("no")) {
            addStory("(located "+adultCharacter.getName()+" "+results.get(0)+")");
        }
        
        //object
        if(object!=null) {
            submitQuery("located", object.getName(), "?X");
            if(!results.get(0).equals("no")) {
                addStory("(located "+object.getName()+" "+results.get(0)+")");
            }
            
            //orientation of child characters to object
            for(int i=0;i<childCharacters.size();i++) {
                submitQuery("orientation", ((Instance) childCharacters.toArray()[i]).getName(), object.getName(), "?X");
                if(!results.get(0).equals("no")) {
                    addStory("(orientation "+((Instance) childCharacters.toArray()[i]).getName()+" "+object.getName()+" "+results.get(0)+")");
                }
            }
        }
    }

    /**
     * creates a query of the desire of parent then check to see if it has been done
     */
    private void checkRuleDone() {
        System.out.println("*****checkRuleDone()");
        if(rule!=null) {
            System.out.println("There is a rule");
            String query = "";
            if(rule.getAttributeCnt()==0) {
                query = "(instance ?X "+rule.getInstance()+")";
            } else {
                query = "(and (instance ?X "+rule.getInstance()+")";
                if(rule.getAgent()!=null) {
                    query +=" (agent ?X "+rule.getAgent()+")";
                } 
                if(rule.getExperiencer()!=null) {
                    query +=" (agent ?X "+rule.getExperiencer()+")";
                } 
                if(rule.getManner()!=null) {
                    query +=" (agent ?X "+rule.getManner()+")";
                }
                query+=")";
            }
            submitQuery(query);
            if(results.get(0).equals("no")) {
                submitFormula("(not "+rule.getRule()+")");
                submitFormula("instance","ProblemEvent","ProblemEvent");
            }
        } else {
            System.out.println("no rule");
        }
    }

    private void parseRule(String rule) {
        this.rule = new Rule();
        this.rule.setOrig(rule);
        rule = rule.replaceAll("`", "");
        this.rule.setRule(rule);
        StringTokenizer tokenizer = new StringTokenizer(rule,"(|)| ");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if(token.equals("instance")) {
                tokenizer.nextToken();
                this.rule.setInstance(tokenizer.nextToken());
            } else if(token.equals("agent")) {
                tokenizer.nextToken();
                this.rule.setAgent(tokenizer.nextToken());
                this.rule.addAttributeCnt();
            } else if(token.equals("experiencer")) {
                tokenizer.nextToken();
                this.rule.setExperiencer(tokenizer.nextToken());
                this.rule.addAttributeCnt();
            } else if(token.equals("manner")) {
                tokenizer.nextToken();
                this.rule.setManner(tokenizer.nextToken());
                this.rule.addAttributeCnt();
            }
        }
    }
    
    /**
     * checks the rules/statement/desire of adult
     * if the desire is done in the introduction phase it becomes the "rule"
     */
    private void checkPredicates() {
        System.out.println("*****checkPredicates()");
        
        //adults
        submitQuery("(desires "+adultCharacter.getName()+" ?X)");
        if(!(results.get(0).equals("no")||(results.get(0).equals("yes")))) {
            String desire = "(desires "+adultCharacter.getName()+" "+results.get(0)+")";
            if(!phaseKnowledge.contains(desire)) {
                addStory(desire);
                phaseKnowledge.add(desire);
                tell(adultCharacter.getName(), ((Instance) childCharacters.toArray()[0]).getName(), results.get(0).toString());
                //System.out.println(results.get(0).toString());
                if(phase.equals("Introduction")) {
                    if(results.get(0).toString().contains("instance"))
                        parseRule(results.get(0).toString());
                }
            } else {
                System.out.println("DUPLICATE: desire");
            }
        }
        submitQuery("states",adultCharacter.getName(),"?X");
        if(!results.get(0).equals("no")) {
            String statement = "(states "+adultCharacter.getName()+" "+results.get(0)+")";
            if(!phaseKnowledge.contains(statement)) {
                addStory(statement);
                phaseKnowledge.add(statement);
                tell(adultCharacter.getName(), ((Instance) childCharacters.toArray()[0]).getName(), results.get(0).toString());
            }
        }
        
        //children
        for(int a=0;a<childCharacters.size();a++) {
            try {
                submitQuery("desires",((Instance) childCharacters.toArray()[a]).getName(),"?X");
                if(!results.get(0).equals("no")) {
                    for(int i=0;i<results.size();i++) {
                        String desire = "(desires "+((Instance) childCharacters.toArray()[a]).getName()+" "+results.get(i)+")";
                        if(!phaseKnowledge.contains(desire)) {
                            addStory(desire);
                            phaseKnowledge.add(desire);
                            // if main child wants to be friends with someone, he will say it
                            if(results.get(i).toString().contains("(friend") && a == 0) {
                                tell(((Instance) childCharacters.toArray()[a]).getName(), ((Instance) childCharacters.toArray()[1]).getName(), results.get(i).toString());
                            }
                        } else {
                            System.out.println("ERROR2: "+desire);
                        }
                    }
                }
                // check disapprovals of the characters
                submitQuery("disapproves",((Instance) childCharacters.toArray()[a]).getName(),"?X");
                if(!results.get(0).equals("no")) {
                    for(int i=0;i<results.size();i++) {
                        String disapproval = "(disapproves "+((Instance) childCharacters.toArray()[a]).getName()+" "+results.get(i)+")";
                        if(!phaseKnowledge.contains(disapproval)) {
                            addStory(disapproval);
                            phaseKnowledge.add(disapproval);
                        }
                    }
                }
                
                // check beliefs of the characters
                submitQuery("believes",((Instance) childCharacters.toArray()[a]).getName(),"?X");
                if(!results.get(0).equals("no")) {
                    for(int i=0;i<results.size();i++) {
                        String belief = "(believes "+((Instance) childCharacters.toArray()[a]).getName()+" "+results.get(i)+")";
                        if(!phaseKnowledge.contains(belief)) {
                            addStory(belief);
                            phaseKnowledge.add(belief);
                        }
                    }
                }
            } catch (Exception e) { }
        }
        for(int a=0;a<childCharacters.size();a++) {
            try {
                // check friends of the characters
                submitQuery("friend",((Instance) childCharacters.toArray()[a]).getName(),"?X");
                if(!results.get(0).equals("no")) {
                    String friend = "(friend "+((Instance) childCharacters.toArray()[a]).getName()+" "+results.get(0)+")";
                    if(!phaseKnowledge.contains(friend)) {
                        addStory(friend);
                    }
                }
            } catch (Exception e) { }
        }     
    }

    private void initializeSettings() {
        resettableKnowledge = new ArrayList();
        resettableKnowledge.add("desires");
        resettableKnowledge.add("disapproves");
        resettableKnowledge.add("states");
        resettableAttributes = new ArrayList();
        resettableAttributes.add("HeadAche");
        omissibleKnowledge = new ArrayList();
        omissibleKnowledge.add("Event");
        phaseKnowledge = new ArrayList();
        oneTimeEventsAndAttributes = new ArrayList<String>();
        oneTimeEventsAndAttributes.add("ChangeDayEvent");
        multipleAttributes = new ArrayList<String>();
        multipleAttributes.add("TraitAttribute");
        multipleAttributes.add("PositiveAttribute");
        multipleAttributes.add("NegativeAttribute");
    }

    private void followRule() {
        System.out.println("*****follow rule*****");
        String instance = rule.getInstance();
        
        // if rule is instance
        if(instance!=null) {
            submitFormula("instance", instance, instance);
            if(rule.getAgent()!=null) {
                submitFormula("agent",instances.get(instances.size()-1).toString(),rule.getAgent());
            }
            if(rule.getExperiencer()!=null) {
                submitFormula("experiencer",instances.get(instances.size()-1).toString(),rule.getExperiencer());
            }
            if(rule.getManner()!=null) {
                submitFormula("manner",instances.get(instances.size()-1).toString(),rule.getManner());
            }
            if(rule.getPatient()!=null) {
                submitFormula("patient",instances.get(instances.size()-1).toString(),rule.getPatient());
            }
        } else {
            // if rule is attribute
            submitFormula(rule.getRule());
        }
    }

    private void tell(String sender, String receiver, String rule) {
        submitFormula("instance", "Stating", "Stating");
        String orderingInstance = (instances.get(instances.size()-1)).toString();
        submitFormula("experiencer", orderingInstance, sender);
        submitFormula("patient", orderingInstance, receiver);
        submitFormula("patient", orderingInstance, rule);
    }

    private void initializeTime(Input input) {
        switch(input.getTheme()) {
            case THEME_SLEEPEARLY:
                submitFormula("instance", "NightTime", "NightTime");
                break;
            default:
                submitFormula("instance", "DayTime", "DayTime");
        }
        submitFormula("instance","Friday","Friday");
    }

    private void checkNewEvents() {
        System.out.println("*****checkNewEvents()");
        // check any new instances
        submitQuery("instance", "?X", "StoryEvent");
                
        if(!(results.get(0).equals("no"))) {
            ArrayList eventList = results;
            for(int j=0;j<eventList.size();j++) {
                String query = "(instance "+eventList.get(j)+" ?X)";
                submitQuery(query);
                if(!(results.get(0).equals("yes"))) {
                    String line = "(instance "+eventList.get(j)+" "+results.get(0)+")";
                    String event = results.get(0);
                    
                    boolean isNew = true;
                    if (oneTimeEventsAndAttributes.contains(event) && phaseKnowledge.contains(event)) {
                        isNew = false;
                    }
                    if(isNew)
                    if(!operationalKnowledge.contains(line)) {
                        addStory(line);
                        String instance = results.get(0);
                        
                        submitQuery("agent",eventList.get(j).toString(),"?X");
                        if(!results.get(0).equals("no"))
                            for(int a=0;a<results.size();a++)
                                addStory("(agent "+eventList.get(j).toString()+" "+results.get(a)+")");
                        
                        submitQuery("patient",eventList.get(j).toString(),"?X");
                        if(!results.get(0).equals("no"))
                            for(int a=0;a<results.size();a++)
                                addStory("(patient "+eventList.get(j).toString()+" "+results.get(a)+")");
                        if(days < MAXDAYS)
                        if(line.contains("ChangeDayEvent")) { // change the day manually
                            days++;
                            nextDay();
                            reset = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * delete previous instances in the operational knowledge that have "not" when a new day arrives since it is now possible to do it
     * other instances are deleted also
     * ex) (not (exists (?X0) (and (instance ?X0 Sleeping) (manner ?X0 Early) (agent ?X0 RabbitCharacter4))))
     */
    private void deleteInstances() {
        System.out.println("*****deleteInstances()*****");
        for(int i=0;i<operationalKnowledge.size();i++) {
            if(operationalKnowledge.toArray()[i].toString().replace(" ", "").startsWith("(not(exists")) {
                //System.out.println("removed: "+operationalKnowledge.toArray()[i].toString());
                operationalKnowledge.remove(operationalKnowledge.toArray()[i]);
                isConflict = true;
            }
            if(operationalKnowledge.toArray()[i].toString().endsWith("ChangeDayEvent)")) {
                //System.out.println("removed: "+operationalKnowledge.toArray()[i].toString());
                operationalKnowledge.remove(operationalKnowledge.toArray()[i]);
                isConflict = true;
            }
        }
    }

    /**
     * reset certain attributes of child characters if a day is transitioned
     * currently they are: pain, ...
     */
    private void resetCharacters() {
        System.out.println("*****resetCharacters()*****");
        for(int i=0;i<childCharacters.size();i++) {
            String character = ((Instance) childCharacters.toArray()[i]).getName();
            for(int j=0;j<resettableAttributes.size();j++) {
                if(operationalKnowledge.contains("(attribute "+character+" "+resettableAttributes.get(j)+")")) {
                    //System.out.println("removed: "+"(attribute "+character+" "+resettableAttributes.get(j)+")");
                    ((Instance) childCharacters.toArray()[i]).getAttributes().remove(resettableAttributes.get(j));
                    operationalKnowledge.remove("(attribute "+character+" "+resettableAttributes.get(j)+")");
                    isConflict = true;
                }
            }
        }
        System.out.println("*****/resetCharacters()*****");
    }

    private void nextDay() {
        System.out.println("****nextDay()");
        submitQuery("instance", "?X", "Friday");
        if(!results.get(0).equals("no")) {
            operationalKnowledge.remove("(instance "+results.get(0)+" Friday)");
            submitFormula("instance", "Saturday", "Saturday");
            
            submitQuery("instance", "?X", "NightTime");
            if(!results.get(0).equals("no")) {
                operationalKnowledge.remove("(instance "+results.get(0)+" NightTime)");
                submitFormula("instance", "NightTime", "NightTime");
            }
        }
    }

    private void changeStoryPhase(String phase) {
            isChangePhase = true;
            generalCount = true;
            checkConflicts(this.phase);
            this.phase = phase;
            phaseKnowledge.clear();
            submitFormula("attribute", "Story1", phase+"Phase");
            checkConflicts("");
            generalCount = false;
    }

    /**
     * removes specific operational knowledge every story phase
     */
    private void removeKnowledge() {
        System.out.println("*****removeKnowledge()");
        for(int i=0;i<operationalKnowledge.size();i++) {
            for(int j=0;j<resettableKnowledge.size();j++) {
                if(operationalKnowledge.toArray()[i].toString().contains(resettableKnowledge.toArray()[j].toString())) {
                    //System.out.println("removed: "+operationalKnowledge.toArray()[i].toString());
                    operationalKnowledge.remove(operationalKnowledge.toArray()[i]);
                    i--;
                }
            }
        }
        System.out.println("*****/removeKnowledge()");
    }

    private void initializeStory(Input input) {
        
        File file = new File(DATABASE);
        file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        KBmanager.getMgr().initializeOnce();
        vampire = Vampire.getNewInstance(DATABASE);
        
        instances = new ArrayList();
        childCharacters = new ArrayList<Instance>();
        operationalKnowledge = new LinkedHashSet();
        oldKnowledge = new ArrayList();
                
        submitFormula("instance", "Story", "Story");
        story = new Instance("Story" + instanceCnt);
        setTheme(input);
        this.phase = "Introduction";
        changeStoryPhase("Introduction");
    }

    private void setTheme(Input input) {
        switch(input.getTheme()) {
            case THEME_SLEEPEARLY:
                submitFormula("attribute",story.getName(),"SleepEarly");
                break;
            case THEME_BEBRAVE:
                submitFormula("attribute",story.getName(),"BeBrave");
                break;
            case THEME_BEHONEST:
                submitFormula("attribute",story.getName(),"BeHonest");
                break;
            case THEME_BECAREFUL:
                submitFormula("attribute",story.getName(),"BeCareful");
                break;
        }
    }

    private void determineCapabilities(Instance instance) {
        System.out.println("*****determineCapabilities("+instance.getName()+")");
        doneSomething = false;
        // check capability character
        submitQuery("(or (capability ?X experiencer "+instance.getName()+") (capability ?X agent "+instance.getName()+"))");
        if(!results.get(0).equals("no")) {
            
            // check if he can only do it once in a phase
            String action = results.get(results.size() - 1).toString();
            boolean isNew = true;
            if (oneTimeEventsAndAttributes.contains(action) && phaseKnowledge.contains(action)) {
                isNew = false;
            }
            
            if(isNew) {
                // choose one of the possible actions
                submitFormula("instance", results.get(results.size() - 1).toString(), results.get(results.size() - 1).toString());
                
                // obtain the case roles from the capability chosen
                if(getCaseRoles) {
                    //System.out.println("determining case roles for "+results.get(results.size() - 1).toString());
                    determineCaseRolesFromCapability(results.get(results.size() - 1).toString(), instances.get(instances.size()-1).toString());
                    getCaseRoles = false;
                }
                else {
                    //System.out.println("skipping case roles for "+results.get(results.size() - 1).toString());
                }
            }
        }
        checkConflicts("");
    }

    /**
     * Generates the story plot
     */
    private void createLog() {
        try {
            File story = new File("story.txt");
            story.createNewFile();
            ReadWriteTextFile logWriter = new ReadWriteTextFile();
            logWriter.setContents(story, log);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    /**
     * Check the attributes of all instances and creates a snapshot if any conflicts are found
     * Removes obsolete knowledge also if certain conditions are met (ex. a day passed)
     */
    private void checkConflicts(String phase) {
        do {
            doGeneralQueries();
            isConflict = false;
            System.out.println("*****checkConflicts("+phase+")");
            conflictCount = true;
            writeLog = false;
            System.out.println("**********<checking attributes>**********");
            if(object!=null) {
                checkAttribute(object);
            }
            System.out.println("**********<child characters>**********");
            for(int i=0;i<childCharacters.size();i++) {
                checkAttribute((Instance)childCharacters.toArray()[i]);
            }
            System.out.println("**********</child characters>**********");
            System.out.println("**********</checking attributes>**********");
            if(reset) {
                deleteInstances();
                resetCharacters();
                reset = false;
            }
            removeKnowledge();
            if(isChangePhase) {
                while(operationalKnowledge.contains("(attribute Story1 "+phase+"Phase)")) {
                    operationalKnowledge.remove("(attribute Story1 "+phase+"Phase)");
                }
            }
            if(isConflict || isChangePhase) {
                System.out.println("Creating new snapshot.............................................................");
                try {
                    vampire.terminate();
                    vampire = Vampire.getNewInstance(DATABASE);
                    resubmitFormulas();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            writeLog = true;
            conflictCount = false;
            isChangePhase = false;
        } while (isConflict);
    }
    
    /**
     * Obtains the different case roles of the capability chosen and submits it to the operational knowledge
     * @param item
     */
    private void determineCaseRolesFromCapability(String action, String instance) {
        String caseRole = "";
        for(int i=0; i<6;i++) {
            switch(i) {
                case 0:
                    caseRole = "experiencer";
                    submitQuery("capability", action, "experiencer", "?X");
                    break;
                case 1:
                    caseRole = "patient";
                    submitQuery("capability", action, "patient", "?X");
                    break;
                case 2:
                    caseRole = "agent";
                    submitQuery("capability", action, "agent", "?X");
                    break;
                case 3:
                    caseRole = "origin";
                    submitQuery("capability", action, "origin", "?X");
                    break;
                case 4:
                    caseRole = "instrument";
                    submitQuery("capability", action, "instrument", "?X");
                    break;
                case 5:
                    caseRole = "manner";
                    submitQuery("capability", action, "manner", "?X");
                    break;
            }
            if(!(results.get(0).equals("no")||results.get(0).equals("yes"))) {
                for(int j=0;j<results.size();j++) {
                    hashmap.put(caseRole, results.get(j).toString());
                    submitFormula(caseRole, instance, results.get(j).toString());
                }
            }
        }
        
        //higher order assertions handling
        //orientation to object
        if(object!=null) {
            submitQuery("orientation",childCharacter.getName(),object.getName(),"?X");
            if(!(results.get(0).equals("no")||results.get(0).equals("yes"))) {
                submitFormula("(holdsDuring (WhenFn "+instance+") `(orientation "+childCharacter.getName()+" "+object.getName()+" "+results.get(0)+"))");
            }
        }
        
        //attribute change during event
        if(hashmap.get("agent")!=null) {
            submitQuery("(holdsDuring (WhenFn "+instance+") `(attribute "+hashmap.get("agent")+" ?X))");
            if(!results.get(0).equals("no")) {
                submitFormula("(attribute "+hashmap.get("agent")+" "+results.get(0)+")");
            }
        }
        hashmap.clear();
    }
    
    /**
     * Checks any changes to the list of attributes each instance has
     * @param object2
     */
    private void checkAttribute(Instance instance) {
        System.out.println("*****checkAttribute("+instance.getName()+")");
        submitQuery("attribute", instance.getName(), "?X");
        int origSize = instance.getAttributes().size();
        int i = results.size() - origSize;
        //showResults();
        //showAttributes(instance);
        int size = results.size();
        ArrayList<String> attributes = results;
        if(i>0 && !results.get(0).equals("no")) {
            for(int j=0;j<size;j++) {
                String attribute = attributes.get(j);
                System.out.print("checking if "+attribute+" is a new attribute");
                
                boolean skip = false;
                boolean newAttribute = true;
                for(int a=0;a<instance.getAttributes().size();a++) {
                    if(instance.getAttributes().get(a).equals(attribute)) {
                        newAttribute = false;
                        break;
                    }
                }
                
                if(phaseKnowledge.contains("(attribute "+instance.getName()+" "+attribute+")") && oneTimeEventsAndAttributes.contains(attribute)) {
                    System.out.println(": skip");
                } else if(!attribute.equals("yes")) {
                    if(newAttribute) {
                        System.out.println(": yes");
                        addStory("(attribute "+instance.getName()+" "+attribute+")");
                        phaseKnowledge.add("(attribute "+instance.getName()+" "+attribute+")");
                        instance.getAttributes().add(attribute);
                        
                        // check for conflicts
                        
                        // multiple attribute type (e.g. TraitAttribute)
                        submitQuery("instance",attribute,"?X");
                        if(multipleAttributes.contains(results.get(0))) {
                            System.out.println("We can have multiple attributes of this type");
                            submitQuery("(or (contraryAttribute "+attribute+" ?X) (contraryAttribute ?X "+attribute+"))");
                            String contraryAttribute = results.get(0);
                            submitQuery("attribute",instance.getName(),contraryAttribute);
                            if(!results.get(0).equals("no")) {
                                isConflict = true;
                                System.out.print("CONFLICT DETECTED: ");
                                for(int k=0;k<operationalKnowledge.size();k++) {
                                    if(operationalKnowledge.toArray()[k].equals("(attribute "+instance.getName()+" "+contraryAttribute+")")) {
                                        String remove = contraryAttribute;
                                        oldKnowledge.add(remove);
                                        System.out.println("removing from operational knowledge: "+operationalKnowledge.toArray()[k]);
                                        operationalKnowledge.remove(operationalKnowledge.toArray()[k]);
                                    }
                                }
                            }
                        } 
                        
                        // single attribute type
                        else { 
                            submitQuery("subAttribute",attribute,"?X");
    
                            if(!results.get(0).equals("no")) {
                                // state of mind change
                                submitQuery("(and (attribute "+instance.getName()+" ?X) (or (subAttribute ?X Anxiety) (subAttribute ?X Unhappiness) (subAttribute ?X Happiness)))");
                            } else {
                                // other changes
                                submitQuery("instance",attribute,"?X");
                                if(results.size()==1) {
                                    submitQuery("(and(attribute "+instance.getName()+" "+"?X) (instance ?X "+results.get(0)+"))");
                                } else {
                                    submitQuery("(and(attribute "+instance.getName()+" "+"?X) (or (instance ?X "+results.get(0)+") (instance ?X "+results.get(1)+")))");
                                }
                            }
                            
                            if(results.size()>1) {
                                isConflict = true;
                                boolean found = false;
                                String remove = "";
                                System.out.print("CONFLICT DETECTED: ");
                                for(int k=0;k<operationalKnowledge.size();k++) {
                                    for(int a=0;a<results.size();a++) {
                                        if(operationalKnowledge.toArray()[k].equals("(attribute "+instance.getName()+" "+results.get(a)+")")) {
                                            //System.out.println("removing from operational knowledge: "+operationalKnowledge.toArray()[k]);
                                            //phaseKnowledge.add(operationalKnowledge.toArray()[k].toString());
                                            remove = (String) results.get(a);
                                            oldKnowledge.add(remove);
                                            operationalKnowledge.remove(operationalKnowledge.toArray()[k]);
                                            System.out.println("removing from operational knowledge: "+operationalKnowledge.toArray()[k]);
                                            found = true;
                                            break;
                                        }
                                    }
                                    if(found) {
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        System.out.println(": no");
                    }
                }
            }
            
        }        
        for(int k=0;k<instance.getAttributes().size();k++) {
            for(int a=0;a<oldKnowledge.size();a++) {
                if(instance.getAttributes().get(k).equals(oldKnowledge.get(a))) {
                    //System.out.println("removing from attribute list: "+instance.getAttributes().get(k));
                    instance.getAttributes().remove(k);
                }
            }
        }
        oldKnowledge.clear();
    }

    /**
     * Converts chosen background to SUMO and add to operational knowledge
     * @param background
     */
    private void assertBackground(final int nBackground) {
        switch(nBackground) {
            case BG_LIVINGROOM:
                submitFormula("instance", "LivingRoom", "LivingRoom");
                background = new Instance("LivingRoom" + instanceCnt);
                break;
            case BG_CLASSROOM:
                submitFormula("instance", "ClassRoom", "ClassRoom");
                background = new Instance("ClassRoom" + instanceCnt);
                break;
        }
    }
    
    /**
     * Converts chosen objects to SUMO and add to operational knowledge
     * @param input
     */
    private void assertObjects(Input input) {
        List objects = input.getObjects();
        String objectName = "";
        for(int i=0;i<objects.getItemCount();i++) {
            switch(input.getObject(i)) {
                case OBJECT_LAMP:
                    objectName = "Lamp";
                    break;
                case OBJECT_CANDY:
                    objectName = "Candy";
                    break;
                case OBJECT_TOYS:
                    objectName = "Toys";
                    break;
                case OBJECT_DRINKINGCUP:
                    objectName = "DrinkingCup";
                    break;
            }
            submitFormula("instance", objectName, objectName);
            object = new Instance(objectName + instanceCnt);
            obtainAttributes(object);
        } 
    }

    private void addStory(String text) {
        if(!operationalKnowledge.contains(text)) {
            if(isPrint2)
            System.err.println("operation knowledge added to log: "+text);
            for(int i=0;i<omissibleKnowledge.size();i++) {
                if(!text.contains((String) omissibleKnowledge.get(i))) {
                    log += text+"\n";
                } else {
                    //System.out.println(text+" has been ommitted from the story");
                }
            }
            operationalKnowledge.add(text);
        }
    }
    
    /**
     * Obtains the the attributes of the instance and stores it in the attribute list of the class
     * @param instance
     */
    private void obtainAttributes(Instance instance) {
        submitQuery("attribute",instance.getName(),"?X");
        if(!results.get(0).equals("no")) {
            for(int i=0;i<results.size();i++) {
                instance.getAttributes().add(results.get(i));
                addStory("(attribute "+instance.getName()+" "+results.get(i)+")");
            }
        }
    }
    
    /**
     * Converts chosen characters to SUMO and add to operational knowledge
     * @param input
     */
    private void assertCharacters(Input input) {
        List characters = input.getCharacters();
        submitFormula("(instance Children Group)");
        for(int i=0;i<characters.getItemCount();i++) {
            switch(input.getCharacter(i)) {
                case FEMALE_CHILD_RABBIT:
                    submitFormula("instance", "RabbitCharacter", "RabbitCharacter");
                    childCharacter = new Instance("RabbitCharacter" + instanceCnt);
                    submitFormula("attribute", childCharacter.getName(), "Child");
                    submitFormula("attribute", childCharacter.getName(), "Female");
                    submitFormula("member", childCharacter.getName(), "Children");
                    break;
                case MALE_CHILD_RABBIT:
                    submitFormula("instance", "RabbitCharacter", "RabbitCharacter");
                    childCharacter = new Instance("RabbitCharacter" + instanceCnt);
                    submitFormula("attribute", childCharacter.getName(), "Child");
                    submitFormula("attribute", childCharacter.getName(), "Male");
                    submitFormula("member", childCharacter.getName(), "Children");
                    break;
                case FEMALE_ADULT_RABBIT:
                    submitFormula("instance", "RabbitCharacter", "RabbitCharacter");
                    adultCharacter = new Instance("RabbitCharacter" + instanceCnt);
                    submitFormula("attribute", adultCharacter.getName(), "Adult");
                    submitFormula("attribute", adultCharacter.getName(), "Female");
                    obtainAttributes(adultCharacter);
                    break;
                case FEMALE_CHILD_ELEPHANT:
                    submitFormula("instance", "ElephantCharacter", "ElephantCharacter");
                    childCharacter = new Instance("ElephantCharacter" + instanceCnt);
                    submitFormula("attribute", childCharacter.getName(), "Child");
                    submitFormula("attribute", childCharacter.getName(), "Female");
                    submitFormula("member", childCharacter.getName(), "Children");
                    break;
            }
            if(childCharacter!=null) {
                childCharacters.add(childCharacter);
                if(childCharacters.size()==1) {
                    submitFormula("attribute", childCharacter.getName(), "MainRole");
                } else {
                    submitFormula("attribute", childCharacter.getName(), "SupportRole");
                }
                obtainAttributes(childCharacter);
                childCharacter = null;
            }
        }
        submitFormula("memberCount", "Children", childCharacters.size()+"");
        submitQuery("(memberCount Children ?X)");
        childCharacter = (Instance) childCharacters.toArray()[0];
    }

    private void submitFormula(String string) {
        if(!phaseKnowledge.contains(string) || allowDuplicates) {
            phaseKnowledge.add(string);
            if (string.startsWith("(instance")) {
                instanceCnt++;
                getCaseRoles = true;
            }
            try {
                vampire.assertFormula(string);
                assert_count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
            addStory(string);
        }
    }
    
    /**
     * Resubmit all operation knowledge to SIGMA
     * 
     * @param par1
     * @param par2
     * @param par3
     * @param par4
     */
    private void resubmitFormulas() {
        isPrint = false;
        try {
            for(int i=0;i<operationalKnowledge.size();i++) {
                    vampire.assertFormula(operationalKnowledge.toArray()[i].toString());
            }
        isPrint = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Delegate method to assertFormula of SIGMA
     * 
     * @param par1
     * @param par2
     * @param par3
     * @param par4
     */
    private void submitFormula(String par1, String par2, String par3, String par4) {
        try {
            if(!phaseKnowledge.contains(par1+par2+par3+par4) || allowDuplicates) {
                phaseKnowledge.add(par1+par2+par3+par4);
                if (par1.equals("instance")) {
                    instanceCnt++;
                    par2 += instanceCnt;
                    getCaseRoles = true;
                }
                if(isPrint2)
                System.err.println("operation knowledge added to SIGMA: " + "(" + par1 + " " + par2 + " " + par3 + " " + par4 + ")");
                assert_count++;
                vampire.assertFormula("(" + par1 + " " + par2 + " " + par3 + " " + par4 + ")");
                addStory("(" + par1 + " " + par2 + " " + par3 + " " + par4 + ")");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delegate method to assertFormula of SIGMA
     * 
     * @param par1
     * @param par2
     * @param par3
     */
    private void submitFormula(String par1, String par2, String par3) {
        try {
            if(!phaseKnowledge.contains(par1+par2+par3) || allowDuplicates) {
                phaseKnowledge.add(par1+par2+par3);
                if (par1.equals("instance")) {
                    instanceCnt++;
                    par2 += instanceCnt;
                    instances.add(par2);
                    getCaseRoles = true;
                    doneSomething = true;                    
                }
                if(isPrint2)
                System.err.println("operation knowledge added to SIGMA: " + "(" + par1 + " "
                        + par2 + " " + par3 + ")");
                vampire.assertFormula("(" + par1 + " " + par2 + " " + par3 + ")");
                assert_count++;
                addStory("(" + par1 + " " + par2 + " " + par3 + ")");
            } else {
                getCaseRoles = false;
                //System.out.println("submitFormula: "+par2+" has already been added this phase");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delegate method to submitQuery of SIGMA
     * 
     * @param par1
     * @param par2
     * @param par3
     * @param par4
     */
    private void submitQuery(String par1, String par2, String par3, String par4) {
        try {
            if(isPrint)
            System.out.println("query: (" + par1 + " " + par2 + " " + par3 + " "
                    + par4 + ")");
            results = XmlParser.parseXml(vampire.submitQuery("(" + par1 + " " + par2 + " "
                    + par3 + " " + par4 + ")", 9999, 10));
            query_count++;
            if(conflictCount) {
                conflict_count++;
            }
            if(generalCount) {
                general_count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delegate method to submitQuery of SIGMA
     * 
     * @param par1
     * @param par2
     * @param par3
     */
    private void submitQuery(String query) {
        try {
            if(isPrint)
            System.out.println("query: "+query);
            results = XmlParser.parseXml(vampire.submitQuery(query, TIME_lIMIT, RESULTS_LIMIT));
            query_count++;
            if(conflictCount) {
                conflict_count++;
            }
            if(generalCount) {
                general_count++;
            }
        } catch (IOException e) {
            //System.out.println("ERROR: "+query);
            e.printStackTrace();
        }
    }
    
    /**
     * Delegate method to submitQuery of SIGMA
     * 
     * @param par1
     * @param par2
     * @param par3
     */
    private ArrayList submitQuery(String par1, String par2, String par3) {
        try {
            if(isPrint)
                System.out.println("query: (" + par1 + " " + par2 + " " + par3 + ")");
            if(saveResults) {
                results = XmlParser.parseXml(vampire.submitQuery("(" + par1 + " " + par2 + " "
                        + par3 + ")", TIME_lIMIT, RESULTS_LIMIT));
                query_count++;
                if(conflictCount) {
                    conflict_count++;
                }
                if(generalCount) {
                    general_count++;
                }
            } else {
                query_count++;
                if(conflictCount) {
                    conflict_count++;
                }
                if(generalCount) {
                    general_count++;
                }
                return XmlParser.parseXml(vampire.submitQuery("(" + par1 + " " + par2 + " "
                        + par3 + ")", TIME_lIMIT, RESULTS_LIMIT));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        Input input = new Input();
        input.addChildCharacter(FEMALE_CHILD_RABBIT);
//        input.addChildCharacter(FEMALE_CHILD_ELEPHANT);
        input.addCharacter(FEMALE_ADULT_RABBIT);
        input.addObject(OBJECT_LAMP);
        input.setBackground(BG_LIVINGROOM);
        input.setTheme(THEME_BEHONEST);
        timeBegin = DateUtils.now("H:mm:ss:SSS");
        new PictureEditor(input);
        timeEnd = DateUtils.now("H:mm:ss:SSS");
        printData();
    }
    
    private static void printData() {
        System.out.println("General queries: "+general_count);
        System.out.println("Conflict queries: "+conflict_count);
        System.out.println("Total queries: "+query_count);
        System.out.println("Assertions: "+assert_count);
        System.out.println("Time begin: "+timeBegin);
        System.out.println("Time end: "+timeEnd);
    }
}
