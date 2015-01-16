package com.articulate.sigma;

import java.io.*;
import java.util.*;
import java.text.ParseException;

public class DependencyConverter {

    public static final List<String> MONTHS = Arrays.asList("January",
            "February","March","April","May","June","July","August",
            "September","October","November","December");

    public static KB kb;
    public static StringBuffer output = new StringBuffer();
    public static HashSet<String> maleNames = new HashSet<String>();
    public static HashSet<String> femaleNames = new HashSet<String>();
    
    /** *************************************************************
     */
    public static boolean isModal(String arg) {
        
        if (arg.equals("may") || arg.equals("should") || arg.equals("might") ||
                arg.equals("must") || arg.equals("shall"))
            return true;
        else
            return false;
    }
    
    /** *************************************************************
     */
    public static String getArg(int argnum, String line) {
        
        if (argnum < 0 || argnum > 2 || line == null) {
            System.out.println("Error in DependencyConverter.getArg(): argnum,string: " + 
                    argnum + ", " + line);
            return "";
        }
        String result = "";
        if (argnum == 0)
            return line.substring(0,line.indexOf('('));
        if (argnum == 1)
            return line.substring(line.indexOf('(') + 1,line.indexOf(','));
        if (argnum == 2)
            return line.substring(line.indexOf(',') + 2,line.indexOf(')'));
        return result;
    }
    
    /** *************************************************************
     * remove punctuation
     */
    private static String processInput(String st) {
        
        if (st.indexOf("'") > -1)
            return st.replace("'", "");
        else
            return st;
    }
    
    /** *************************************************************
     * Get the output of the Stanford Dependency Parser for the given
     * input file.
     */
    public static ArrayList<String> getDependencies(String input) throws IOException {
        
        ArrayList<String> result = new ArrayList<String>();
        Process _nlp;
        BufferedReader _reader; 
        BufferedWriter _writer; 
        BufferedReader _error;
        String tmpfname = "tmp.txt";
        String execString = "/home/apease/Programs/java/jdk1.8.0_25/bin/java -mx150m -classpath /home/apease/Programs/stanford-parser-full-2014-08-27" + 
                "/stanford-parser.jar edu.stanford.nlp.parser.lexparser.LexicalizedParser " + 
                "-outputFormat typedDependencies /home/apease/Programs/stanford-parser-full-2014-08-27/englishPCFG.ser.gz " + tmpfname;
        
        FileWriter fr = null;
        PrintWriter pr = null;

        try {
            fr = new FileWriter(tmpfname);
            pr = new PrintWriter(fr);
            pr.println(input);
        }
        catch (java.io.IOException e) {
            System.out.println("Error in DependencyConverter.getDependencies(): Error writing file " + tmpfname);
            e.printStackTrace();
        }
        finally {
            if (pr != null) { pr.close(); }
            if (fr != null) { fr.close(); }
        }
        System.out.println("INFO in DependencyConverter.getDependencies(): executing: " + execString);
        
        ProcessBuilder builder = new ProcessBuilder(WordNet.splitToArrayList(execString));
        builder.redirectErrorStream(true);
        _nlp = builder.start();
        
        //_nlp = Runtime.getRuntime().exec(execString);
        _reader = new BufferedReader(new InputStreamReader(_nlp.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_nlp.getErrorStream()));
        //System.out.println("INFO in DependencyConverter.getDependencies(): initializing process");
        String line = null; 
        boolean recording = false;
        while ((line = _reader.readLine ()) != null) {
            // line = _reader.readLine(); 
            
            if (line != null && line.startsWith("Parsed file"))
                break;

            if (!StringUtil.emptyString(line) && recording) {
                //System.out.println("INFO in DependencyConverter.getDependencies(): line: " + line);
                result.add(processInput(line));
            }
            if (line != null && line.startsWith("Parsing ["))
                recording = true;
        }
        _writer = new BufferedWriter(new OutputStreamWriter(_nlp.getOutputStream()));
        
        return result;
    }
    
    /** *************************************************************
     * Run the Stanford NLP tools on the given text file to split a 
     * text into sentences.
     */
    public static ArrayList<String> splitSentences(String infile) throws IOException {
        
        ArrayList<String> result = new ArrayList<String>();
        Process _nlp;
        BufferedReader _reader; 
        BufferedWriter _writer; 
        BufferedReader _error;
        String execString = "java -classpath /home/apease/Programs/stanford-parser-full-2014-08-27" + 
                "/stanford-parser.jar edu.stanford.nlp.process.DocumentPreprocessor " + 
                infile;
        //System.out.println("INFO in DependencyConverter.splitSentences(): executing: " + execString);
        _nlp = Runtime.getRuntime().exec(execString);
        _reader = new BufferedReader(new InputStreamReader(_nlp.getInputStream()));
        _error = new BufferedReader(new InputStreamReader(_nlp.getErrorStream()));
        //System.out.println("INFO in DependencyConverter.splitSentences(): initializing process");
        String line = null; 
        while (true) {
            line = _reader.readLine(); 
            if (line == null)
                break;
            result.add(line);           
        }
        _writer = new BufferedWriter(new OutputStreamWriter(_nlp.getOutputStream()));        
        return result;
    }

    /** ***************************************************************
     */
    public static void readFirstNames() {

        System.out.println("INFO in DependencyConverter.readFirstNames(): Reading first names");
        LineNumberReader lr = null;
        File swFile = null;
        String canonicalPath = "";
        try {
            String baseDir = KBmanager.getMgr().getPref("kbDir");
            swFile = new File(baseDir + File.separator + "FirstNames.csv");
            if (swFile == null) {
                System.out.println("Error in DependencyConverter.readFirstNames(): " + 
                                    "The first names file does not exist in " + baseDir);
                return;
            }
            canonicalPath = swFile.getCanonicalPath();
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(swFile);
            lr = new LineNumberReader(r);
            String line;
            lr.readLine(); // throw away the header
            while ((line = lr.readLine()) != null) {
                int comma = line.indexOf(',');
                if (comma < 0)
                    throw new Exception("missing comma in '" + line + '"');
                String name = StringUtil.removeEnclosingChars(line.substring(0,comma).trim(),Integer.MAX_VALUE,'"');
                String gender = StringUtil.removeEnclosingChars(line.substring(comma+1,line.length()).trim(),Integer.MAX_VALUE,'"');
                //System.out.println("INFO in DependencyConverter.readFirstNames(): gender: " + gender);
                if (gender.equals("M"))
                    maleNames.add(name);   
                else if (gender.equals("F"))
                    femaleNames.add(name); 
                else 
                    throw new Exception("bad gender tag in '" + line + "'");
            }
            System.out.println("  " + ((System.currentTimeMillis() - t1) / 1000.0)
                    + " seconds to process " + canonicalPath );
        }
        catch (Exception i) {
            System.out.println("Error in DependencyConverter.readFirstNames() reading file "
                    + canonicalPath + ": " + i.getMessage());
            i.printStackTrace();
        }
        finally {
            try {
                if (lr != null) {
                    lr.close();
                }
            }
            catch (Exception ex) {
            }
        }
        return;
    }

    /** *************************************************************
     * Process one grammatical dependency statements from the Stanford
     * parser into a logical expression using SUMO terms.  Use the
     * context parameter from the given Node as needed to interpret
     * the context of the individual relation.  Generate and return
     * a new context to be inherited by child nodes.
     */
    public static HashMap<String,String> processDependency(Node n) {
        
        HashMap<String,String> context = new HashMap<String,String>();
        String process = "";
        String Subject = "";
        String year = "";
        String month = "";
        String day = "";
        String subject = "";
        String multiword = "";
        Iterator<String> it = n.depStrings.iterator();
        while (it.hasNext()) {
            String dep = it.next();
            if (!StringUtil.emptyString(dep)) {
                System.out.println("# " + dep);
                //ArrayList<String> al = WordNet.splitToArrayList("The British occupied Bloemfontein in February 7, 1900.");
                String prep = getArg(0,dep);
                String arg1 = getArg(1,dep);
                String bareArg1 = arg1.substring(0,arg1.indexOf('-'));
                String arg2 = getArg(2,dep);
                String bareArg2 = arg2.substring(0,arg2.indexOf('-'));
                if (prep.equals("nsubj")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,2)));
                    context.put("SubjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    context.put("ProcessInstance",arg1);
                    output.append("(instance " + arg1 + " " + context.get("ProcessType") + ") ");
                    output.append("(agent " + arg1 + " " + arg2 + ")");
                }
                else if (prep.equals("agent")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,2)));
                    context.put("SubjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    context.put("ProcessInstance",arg1);
                    output.append("(instance " + arg1 + " " + context.get("ProcessType") + ") ");
                    output.append("(instance " + arg2 + " " + context.get("SubjectType") + ") ");
                    output.append("(agent " + arg1 + " " + arg2 + ")");
                }
                else if (prep.equals("amod")) {
                    String sumoNoun = WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,1));
                    String sumoAdj = WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,3));
                    if (!StringUtil.emptyString(sumoAdj))
                        output.append("(attribute " + arg1 + " " + sumoAdj + ") ");
                }
                else if (prep.equals("cop")) {
                    String sumoNoun = WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMO(bareArg1));
                    String sumoAdj = WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,3));
                    if (kb.isChildOf(sumoNoun,"Attribute")) {
                        if (!StringUtil.emptyString(sumoAdj))
                            output.append("(attribute " + arg1 + " " + sumoAdj + ") ");
                    }
                    else
                        output.append("(instance " + subject + " " + sumoNoun + ") ");
                }
                else if (prep.equals("dobj")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,2)));
                    context.put("ObjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    System.out.println("Info in DependencyConverter.processDependency(): object: " + context.get("ObjectType"));
                    context.put("ObjectInstance",arg2);
                    context.put("ProcessInstance",arg1);
                    output.append("(instance " + arg1 + " " + context.get("ProcessType") + ") ");
                    output.append("(instance " + arg2 + " " + context.get("ObjectType") + ") ");
                    output.append("(patient " + arg1 + " " + arg2 + ")");
                }
                else if (prep.equals("nsubjpass")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,2)));
                    context.put("SubjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));                        
                    context.put("ProcessInstance",arg1);
                    output.append("(instance " + arg1 + " " + context.get("ProcessType") + ") ");
                    output.append("(patient " + arg1 + " " + arg2 + ")");
                }
                else if (prep.equals("det")) {
                    context.put("SubjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,1)));                        

                    if (bareArg2.startsWith("wh"))
                        output.append("(instance ?" + arg1 + " " + context.get("Subject") + ") ");
                }
                else if (prep.equals("aux")) {
                    if (isModal(arg1)) {
                        // do something with it
                    }
                }                
                else if (prep.equals("mwe")) {
                    // rather than, as well as, such as, because of, instead of, 
                    // in addition to, all but, such as, instead of, due to
                }
                else if (prep.equals("neg")) {
                    output.append("(not ");
                    context.put("Enclosing", "true");
                    // need to negate arg1
                }
                else if (prep.equals("nn")) {
                    if (multiword == "")
                        multiword = arg1 + "_" + arg2;
                    else
                        multiword = multiword + "_" + arg2;
                }
                else if (prep.equals("pobj")) {
                    context.put("ObjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    context.put("ObjectInstance",bareArg2);

                    if (bareArg1.equals("at")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Region")) {
                            output.append("(location " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("on")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Region")) {
                            output.append("(location " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("in")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Region")) {
                            output.append("(location " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("for")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Human")) {
                            output.append("(destination " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("through")) {}
                    else if (bareArg1.equals("with")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Human")) {
                            output.append("(agent " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                        else if (kb.isChildOf(context.get("ObjectType"),"Object")) {
                            output.append("(instrument " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("across")) {}
                    else if (bareArg1.equals("within")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Object")) {
                            output.append("(properlyFills " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("into")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Object")) {
                            output.append("(properlyFills " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("from")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Object")) {
                            output.append("(origin " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("through")) {
                        if (kb.isChildOf(context.get("ObjectType"),"Object")) {
                            output.append("(origin " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                        }
                    }
                    else if (bareArg1.equals("until")) {}
                    else if (bareArg1.equals("after")) {}
                    else if (bareArg1.equals("before")) {}
                }
                else if (prep.equals("poss")) {
                    context.put("ObjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    if (kb.isChildOf(context.get("ObjectType"),"Object")) {
                        output.append("(possesses " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                    }
                }
                else if (prep.equals("prt")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1+"_"+bareArg2,2)));
                }
                else if (prep.equals("prep_in")) {
                    context.put("ObjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    if (kb.isChildOf(context.get("ObjectType"),"Region")) {
                        output.append("(location " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                    }
                    else if (StringUtil.isNumeric(bareArg2) && bareArg2.length() == 4)
                        output.append("(during " + context.get("SubjectInstance") + " (YearFn " + bareArg2 + ")) ");
                }
                else if (prep.equals("prep_into")) {
                    context.put("ObjectType",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    if (kb.isChildOf(context.get("ObjectType"),"Region")) {
                        output.append("(location " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                    }
                    else if (StringUtil.isNumeric(bareArg2) && bareArg2.length() == 4)
                        output.append("(during " + context.get("SubjectInstance") + " (YearFn " + bareArg2 + ")) ");
                }
                else if (prep.equals("prep_until")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,2)));
                    context.put("ObjectInstance",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    if (context.get("ObjectInstance") != null && kb.isChildOf(context.get("ObjectInstance"),"TimePosition")) {
                        output.append("(earlier " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                    }
                    else if (StringUtil.isNumeric(bareArg2) && bareArg2.length() == 4)
                        output.append("(earlier " + context.get("SubjectInstance") + " (YearFn " + bareArg2 + ")) ");
                }
                else if (prep.equals("prep_through")) {
                    context.put("ProcessType", WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg1,2)));
                    context.put("ObjectInstance",WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense(bareArg2,1)));
                    System.out.println("# through: " + context.get("ObjectInstance"));
                    if (kb.isChildOf(context.get("ObjectInstance"),"TimePosition")) {
                        output.append("(earlier " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") ");
                    }
                    else if (StringUtil.isNumeric(bareArg2) && bareArg2.length() == 4)
                        output.append("(earlier " + context.get("SubjectInstance") + " (YearFn " + bareArg2 + ")) ");
                    else {
                        //context.put("ObjectInstance",arg2);
                        if (kb.isChildOf(context.get("ObjectInstance"),"Region")) {                           
                            output.append("(traverses " + context.get("SubjectInstance") + " " + context.get("ObjectInstance") + ") "); 
                        }

                    }
                }
                else if (prep.equals("ref")) {
                    output.append("(equal " + arg1 + " ?" + arg2 + ") ");
                }
                else if (prep.equals("num")) {                    
                    if (MONTHS.contains(bareArg1)) {
          
                        context.put("Month", bareArg1);
                        if (bareArg2.length() == 4) {
                            context.put("Year",bareArg2);
                        }
                        else {
                            context.put("Day", bareArg2);
                        }
                    }  
                    else {
                        output.append("(memberCount " + arg1 + " " + bareArg2 + ") ");
                    }
                    if (context.get("Day") != null && context.get("Month")  != null && context.get("Year") != null)
                        output.append("(overlapsTemporally " + context.get("ProcessInstance") + " (DayFn " + 
                                context.get("Day") + " (MonthFn " + 
                                context.get("Month") + " (YearFn " + context.get("Year") + ")))) ");
                }
            }
        }
        if (context.get("Close") != null)
            output.append(")");
        return context;
    }

    /** *************************************************************
     */
    public void traverseNodes(Node root) {
        
        ArrayDeque<Node> Q = new ArrayDeque<Node>();
        HashSet<Node> V = new HashSet<Node>();
        Q.add(root);
        V.add(root);
        while (!Q.isEmpty()) {
            Node t = Q.remove();
            HashMap<String,String> context = processDependency(t);
            t.context = context;
            //System.out.println("visiting " + t);
            if (t.nodes != null & t.nodes.size() > 0) {
                for (int i = 0; i < t.nodes.size(); i++) {
                    Node newNode = t.nodes.get(i);
                    if (context.get("Enclosing") != null)
                        newNode.context.put("Close","true");
                    if (!V.contains(newNode)) {
                        V.add(newNode);
                        Q.addFirst(newNode);
                    }  
                }
            }
        }
    }
    
    /** *************************************************************
     */
    public class Node {
        public String name;
        public HashSet<String> depStrings =  new HashSet<String>(); // the dependencies that are encoded into the nodes and edges
        public ArrayList<String> edges = new ArrayList<String>();
        public ArrayList<Node> nodes = new ArrayList<Node>();
        public HashMap<String,String> context = new HashMap<String,String>();
    }
    
    /** *************************************************************
     */
    public void addNode(String dep, HashMap<String,Node> index) {
    
        String pred = getArg(0,dep);
        String arg1 = getArg(1,dep);
        String arg2 = getArg(2,dep);
        Node n1 = new Node();
        Node n2 = new Node();
        if (!index.containsKey(arg1)) {
            n1.name = arg1;
            index.put(arg1, n1);
        }
        else
            n1 = index.get(arg1);
        if (!index.containsKey(arg2)) {
            n2.name = arg2;
            index.put(arg2, n2);
        }
        else
            n2 = index.get(arg2);
        n1.depStrings.add(dep);
        n1.edges.add(pred);
        n1.nodes.add(n2);
    }

    /** *************************************************************
     */
    public Node findRoot(HashMap<String,Node> index) {
        
        HashSet<String> hasParent = new HashSet<String>();
        HashSet<String> noParent = new HashSet<String>();
        noParent.addAll(index.keySet());
        Iterator<String> it = index.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            System.out.println("Info in DependencyConverter.findRoot(): visiting: " + key);
            Node n = index.get(key);
            if (n.nodes != null && n.nodes.size() > 0)
            for (int i = 0; i < n.nodes.size(); i++) {
                Node n2 = n.nodes.get(i);
                hasParent.add(n2.name);
                noParent.remove(n2.name);
            }
        }
        if (noParent.size() < 1) {
            System.out.println("Error in DependencyConverter.findRoot(): no root");
            return null;
        }
        if (noParent.size() > 1) {
            System.out.println("Error in DependencyConverter.findRoot(): more than one root");
            return null;
        }
        System.out.println("Info in DependencyConverter.findRoot(): root: " + noParent);
        return index.get(noParent.iterator().next());
    }
    
    /** *************************************************************
     */
    public Node createGraph(ArrayList<String> deps) {
        
        System.out.println("Info in DependencyConverter.createGraph(): deps: " + deps);
        HashMap<String,Node> index = new HashMap<String,Node>();
        for (int i = 0; i < deps.size(); i++) {
            addNode(deps.get(i),index);
        }
        Node n = findRoot(index);
        if (n == null) {
            System.out.println("Error in createGraph(): no root");
            return null;
        }
        n.context.put("Enclosing", "true");
        if (n.nodes != null && n.nodes.size() > 0) 
            output.append("(and ");
        return n;
    }
    
    /** *************************************************************
     */
    public static String formatSUMO(HashSet<String> SUMO) {
        
        StringBuffer sb = new StringBuffer();
        sb.append("(and \n");
        Iterator<String> it = SUMO.iterator();
        while (it.hasNext()) {
            String stmt = it.next();
            sb.append("  " + stmt);
            if (it.hasNext())
                sb.append("\n");
            else
                sb.append(")\n");
        }
        return sb.toString();
    }
    
    /** *************************************************************
     */
    public static void main(String[] args) {
        
        try{
            KBmanager.getMgr().initializeOnce();
            kb = KBmanager.getMgr().getKB("SUMO");
            WordNet.wn.initOnce();
            //System.out.println("Africa: " + WSD.getBestDefaultSUMOsense("Africa",1));
            //System.out.println("Africa: " + WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense("Africa",1)));

            //System.out.println("Info in DependencyConverter.main(): simplification: " + WordNetUtilities.subst("rolls","s$",""));
            //System.out.println("Info in DependencyConverter.main(): is there a substitution: " + WordNetUtilities.substTest("rolls","s$","",WordNet.wn.verbSynsetHash)); 
            //System.out.println("Info in DependencyConverter.main(): synsets for roll: " + WordNet.wn.verbSynsetHash.get("roll")); 
            //System.out.println("Info in DependencyConverter.main(): root form: " + WordNet.wn.verbRootForm("rolls","rolls")); 
            DependencyConverter dc = new DependencyConverter();
            // ArrayList<String> results = getDependencies("After an unsuccessful Baltimore theatrical debut in 1856, John played minor roles in Philadelphia until 1859, when he joined a Shakespearean stock company in Richmond, Va.");
            ArrayList<String> results = getDependencies("The bank hired John.");
            //ArrayList<String> results = getDependencies("John rolls the ball through Africa.");/
            //ArrayList<String> results = getDependencies("John sticks the pin through the apple.");
            System.out.println(results);
            
            Node n = dc.createGraph(results);
            dc.traverseNodes(n);
            output.append(")");
            System.out.println(Formula.textFormat(output.toString()));
            //System.out.println("Info in DependencyConverter.main(): " + WordNetUtilities.getBareSUMOTerm(WSD.getBestDefaultSUMOsense("pin",1)));
            //System.out.println(kb.isChildOf("Africa","Region"));
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
