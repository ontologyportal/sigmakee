package TPTPWorld;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import tptp_parser.SimpleTptpParserOutput;
import tptp_parser.TptpLexer;
import tptp_parser.TptpParser;

import com.articulate.sigma.KBmanager;

public class SystemOnTPTP {
    
    public static final String NEW_LINE_TEXT = "%------------------------------------------------------------------------------\n";
    //private static final String SystemDirectory = "../System";
    private static String SystemDirectory = KBmanager.getMgr().getPref("systemsDir");
    //private static final String SystemDirectory = "../../../../../systems";
    //private static String SystemDirectory = "/home/graph/strac/geoff/TPTPWorld/Systems";

    //private static final String Systeminfo = KBmanager.getMgr().getPref("baseDir") + "/KBs/systemsInfo.xml";
    private static String SystemInfo = SystemDirectory + "/" + "systemInfo.xml";

    private static Vector<ATPSystem> atpSystemList = null;
    private static Process process;

    public static String SZS_ANSWERS_SHORT = "% SZS answers short";
    public static String SZS_STATUS_THEOREM = "% Result     : Theorem";
    public static String SZS_STATUS_COUNTER_SATISFIABLE = "% Result     : CounterSatisfiable";
    public static String RESULT = "% Result     : ";

    private static final String SOLVED_TYPE_TIMEOUT = "Timeout";
    private static final String SOLVED_TYPE_GIVEUP  = "Give Up";
    private static final String SOLUTION_TYPE_NONE = "None";
    private static final String SOLUTION_TYPE_ASSURANCE = "Assurance";

    private static class ATPThread extends Thread { 

        private int limit;
        private long startTime;
        private long stopSolvedTime;
        private long stopSolutionTime;
        private Logger logger;

        private Process process;    
        private final String problemFile;
        private final String quietFlag;
        private final String format;

        private String harness = "";
        private String commentedResponse = "";
        private String response = ""; // process response
        private String solution = ""; // solution results within response
        private String solvedType = SystemOnTPTP.SOLVED_TYPE_TIMEOUT; // solvedType of prover

        private String solutionType = SystemOnTPTP.SOLUTION_TYPE_NONE; // default solution type

        private ATPSystem atpSystem;
        private String commandLine;

        private int solvedIndex = -1;
        private int solutionIndex = -1;

        //private BufferedReader writer; // write to process
        private BufferedReader reader; // reader for process output
        private BufferedReader error;  // error reader for process error messages

        public ATPThread (Process process, ATPSystem atpSystem, String quietFlag, int limit, String format, String commandLine, String problemFile) {
            this.process = process;
            this.atpSystem = atpSystem;
            this.quietFlag = quietFlag;
            this.format = format;
            this.problemFile = problemFile;
            this.commandLine = commandLine;
            this.limit = limit;
            this.logger = Logger.getLogger(this.getClass().getName());
            setDaemon(true);
        }
    
        private void checkSolved (String responseLine) {
            if (solvedIndex != -1) {
                // already have a solved solvedType
                return;
            } else {
                // check if responseLine has a solved type in it
                int size = atpSystem.solved[0].size();
                for (int i = 0; i < size; i++) {
                    if (responseLine.contains(atpSystem.solved[1].elementAt(i))) {
                        solvedIndex = i;
                        solvedType = atpSystem.solved[0].elementAt(i);
                        // record time taken for prover to solve problem
                        stopSolvedTime = System.currentTimeMillis();      
                        // found solution, solution is at least of type "Assurance"
                        solutionType = SystemOnTPTP.SOLUTION_TYPE_ASSURANCE;
                        return;
                    }
                }
            }
        }

        private void checkSolution (String responseLine) {
            if (solutionIndex == -2) {
                // solution found and finished
                // return;
            } else if (solutionIndex != -1) {
                // currently recording solution
                if (responseLine.contains(atpSystem.endSoln[1].elementAt(solutionIndex))) {
                    solutionIndex = -2;
                } else {
                    solution += responseLine + "\n";
                }
            } else {
                // check if this is start of solution
                int size = atpSystem.startSoln[0].size();
                for (int i = 0; i < size; i++) {
                    if (responseLine.contains(atpSystem.startSoln[1].elementAt(i))) {
                        solutionIndex = i;
                        solutionType = atpSystem.startSoln[0].elementAt(i);
                        //            solution = atpSystem.startSoln[0].elementAt(i) + "\n";
                    } else {
                        //            System.out.println("--------------------------------------------------");
                        //            System.out.println("not solution: " + responseLine);
                        //            System.out.println("compared to: " + atpSystem.startSoln[1].elementAt(i));
                    }
                }
            }
        }

        public void run () {
            //      System.out.println("---Start thread");
            harness += "SystemOnTPTP.java - Start atp thread: " + atpSystem.name + "---" + atpSystem.version + "\n";
            startTime = System.currentTimeMillis();      
            long maxTime = (long) (limit * 1000);
            stopSolvedTime = startTime + maxTime;
            stopSolutionTime = startTime + maxTime;
            String responseLine = "";
            // Set initial response info (start of system output)
            response += "% START OF SYSTEM OUTPUT\n";
            // Set initial commented response info ("original system output")
            commentedResponse += "%----START OF ORIGINAL SYSTEM OUTPUT\n";
            try {
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                error  = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((responseLine = reader.readLine()) != null) { 
                    checkSolved(responseLine);
                    checkSolution(responseLine);
                    if (!responseLine.equals("")) {
                        response += responseLine + "\n";
                    }
                    commentedResponse += "% " + responseLine + "\n";
                }
                reader.close();
                while ((responseLine = error.readLine()) != null) {
                    // check every responseLine to see if it matches solutions and solved types
                    if (!responseLine.equals("")) {
                        response += responseLine + "\n";
                    }
                    commentedResponse += "% " + responseLine + "\n";
                }
                error.close();
                if (solvedType.equals(SystemOnTPTP.SOLVED_TYPE_TIMEOUT)) {
                    solvedType = SystemOnTPTP.SOLVED_TYPE_GIVEUP;
                }
            } catch (Exception e) {
                System.out.println("SystemOnTPTP.java Exception: " + e);
                harness += "SystemOnTPTP.java - could not finish thread successfully: " + atpSystem.name + "---" + atpSystem.version + "\n";
            }
            // Set final response info (end of system output)
            response += "% END OF SYSTEM OUTPUT\n";
            // Set final commented response info
            commentedResponse += "%----END OF ORIGINAL SYSTEM OUTPUT\n";
            // record time taken to finish retrieving solution
            stopSolutionTime = System.currentTimeMillis();
            //      System.out.println("---End thread");
            harness += "SystemOnTPTP.java - End atp thread, finished calling atp system successfully: " + atpSystem.name + "---" + atpSystem.version + "\n";
        }
    
        public double getSolvedTime () {
            return (double)(stopSolvedTime - startTime) / 1000.0;
        }
        public double getSolutionTime () {
            return (double)(stopSolutionTime - startTime) / 1000.0;
        }
    
        public String getResponse () {
        	logger.finest(response);
            return response;
        }
        // take each response line and comment
        public String getCommentedResponse () {
            return commentedResponse;
        }

        public String getSolution () {
            String solutionResult = "";
            try {
            	logger.finest(solution);
                BufferedReader bin = new BufferedReader(new StringReader(solution));
                TptpLexer lexer = new TptpLexer(bin);
                TptpParser parser = new TptpParser(lexer);
                SimpleTptpParserOutput outputManager = new SimpleTptpParserOutput();
                for (SimpleTptpParserOutput.TopLevelItem item = 
                         (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager);
                     item != null;
                     item = (SimpleTptpParserOutput.TopLevelItem)parser.topLevelItem(outputManager)) {
                    solutionResult += item.toString() + "\n";
                }
                return solutionResult;
            } catch (Exception e) {
				String[] split = solution.split("\n");
				String result = "%  Error in pretty-printing tptp format: " + e
						+ "\n";
				result = result + "%  Solution: \n";

				for (int i = 0; i < split.length; i++)
					result = result + "%  " + split[i] + "\n";

				return result;
            }
        }

        // return information about this process
        public String getHeader () {
            String res = "";
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM  d HH:mm:ss z yyyy");
            Calendar cal = Calendar.getInstance();
            res += "% command line: " + commandLine + "\n";
            res += "% File       : " + atpSystem.name + "---" + atpSystem.version + "\n";
            res += "% Problem    : " + problemFile + "\n";
            res += "% Transform  : " + "none" + "\n";
            res += "% Format     : " + "tptp:raw" + "\n";
            res += "% Command    : " + atpSystem.command + "\n";
            res += "\n";
            res += "% Computer   : " + System.getenv("HOST") + "\n";
            //      res += "% Model      : " + "" + "\n";
            //      res += "% CPU        : " + "" + "\n";
            res += "% OS Arch    : " + System.getProperty("os.arch") + "\n";
            res += "% OS         : " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n";
            res += "% JVM Version: " + System.getProperty("java.vm.version") + "\n";
            //      res += "% Free Mem.  : " + Runtime.getRuntime().freeMemory() + "\n";
            res += "% Memory     : " + Runtime.getRuntime().totalMemory()  + "\n";
            res += "% CPULimit   : " + limit + "s" + "\n";
            res += "% Date       : " + sdf.format(cal.getTime()) + "\n";
            res += "\n";
            res += "% Result     : " + solvedType + " " + getSolvedTime() + "s" + "\n";
            res += "% Output     : " + solutionType + " " + getSolutionTime() + "s" + "\n";
            res += "\n";
            res += "% Comments   : \n";
            return res;
        }

        public String getSolvedType () {
            return solvedType;
        }
    
        public String getSolutionType () {
            return solutionType;
        }

        public String getResults () {
            String res = "";
            // return HARNESS if q0, q1, q2
            if (quietFlag.equals("-q0") ||
                quietFlag.equals("-q1") ||
                quietFlag.equals("-q2")) {
                res += harness;
            }

            // return system output (between, including, start/end) if q0, q1, q01
            if (format.equals("-S")) {
                // return pretty print system output
                res += SystemOnTPTP.NEW_LINE_TEXT;
                res += getHeader();
                res += SystemOnTPTP.NEW_LINE_TEXT;
                res += getSolution();
                res += SystemOnTPTP.NEW_LINE_TEXT;
                res += getCommentedResponse();
                res += SystemOnTPTP.NEW_LINE_TEXT;
            } else if (quietFlag.equals("-q0") ||
                       quietFlag.equals("-q1") ||
                       quietFlag.equals("-q01")) {
                // return system output
                res += getResponse();
            }

            // return RESULT/OUTPUT if q0, q1, q2
            if (quietFlag.equals("-q0") ||
                quietFlag.equals("-q1") ||
                quietFlag.equals("-q2")) {
                res += "RESULT: " + problemFile + " - " + atpSystem.name + "---" + atpSystem.version + " - says " + getSolvedType() + " - Total time: " + getSolvedTime() + "\n";
                res += "OUTPUT: " + problemFile + " - " + atpSystem.name + "---" + atpSystem.version + " - says " + getSolutionType() + " - Total time: " + getSolutionTime();
            }

            // return SHORT RESULT/OUTPUT if q3
            if (quietFlag.equals("-q3")) {
                res += "% " + problemFile + " - " + getSolvedType() + " - Total time: " + getSolvedTime() + "\n";
                res += "% " + problemFile + " - " + getSolutionType() + " - Total time: " + getSolutionTime();
            }

            return res;
        }
    }

    public static void checkSystemDirectory (String systemDir) {
        String tptpHome = System.getenv("JTPTP_HOME");
        if (systemDir != null && systemDir != "") {
            SystemDirectory = systemDir;
            SystemInfo = SystemDirectory + "/" + "SystemInfo";
        } else if (tptpHome != null && tptpHome != "") {
            SystemDirectory = tptpHome + "/" + "Systems";
            SystemInfo = SystemDirectory + "/" + "SystemInfo";
        }
    }

    public static void loadSystems (String systemDir) {
        try {
            checkSystemDirectory(systemDir);
            //      String systemInfo = systemDir + "/" + "SystemInfo";
            //String systemInfo = systemDir + "/" + "systemInfo.xml";
            FileReader file = new FileReader(new File(SystemInfo));
            SystemInfoParser sp = new SystemInfoParser(file);
            atpSystemList = sp.getSystemList();
        } catch (Exception err) {
            System.out.println(err);
        }
    }

    public static ArrayList<String> listSystems (String systemDir) {
        return listSystems(systemDir, null);
    }

    public static ArrayList<String> listSystems (String systemDir, String status) {
        /*
          if (atpSystemList == null) {
          loadSystems(systemDir);
          }
        */
        loadSystems(systemDir);
        ArrayList<String> systems = new ArrayList<String>();
        try {
            for (int i = 0; i < atpSystemList.size(); i++) {
                boolean addSystem = false;
                ATPSystem atpSystem = atpSystemList.get(i);
                if (status == null) {
                    addSystem = true;
                } else {
                    for (int j = 0; j < atpSystem.status.size(); j++) {
                        if (atpSystem.status.elementAt(j).equals(status)) {
                            addSystem = true;
                        }
                    }         
                }
                if (addSystem) {
                    systems.add(atpSystem.name + "---" + atpSystem.version);
                }
            }
        } catch (Exception err) {
            System.out.println(err);      
        }
        return systems;
    }

    public static String SystemOnTPTP (String systemVersion, String systemDir, int limit, String quietFlag, String format, String filename) {
        // read in SystemInfo.xml and find prover (system---version)
        // retrieve necessary info (Command, Solved, StartSoln/EndSoln, etc)
        // IMPLEMENT
        //checkSystemDirectory(systemDir);
		Logger logger = Logger.getLogger("");
        File problemFile = new File(filename);

        if (atpSystemList == null) {
            //      loadSystems(SystemDirectory);
            loadSystems(systemDir);
        }
        ATPSystem atpSystem = null;
        String problemPath = "";
        String commandLine;
        ATPThread atp;

        for (int i = 0; i < atpSystemList.size(); i++) {
            ATPSystem currentSystem = atpSystemList.get(i);
            String currentName = currentSystem.name + "---" + currentSystem.version;      
            //      System.out.println("current system: " + currentName);
            //      System.out.println(" comparing to : " + systemVersion);
            if (currentName.equals(systemVersion)) {
                //        System.out.println("found system");
                atpSystem = currentSystem;
                break;
            }
        }

        if (atpSystem == null) {
            return "% SystemOnTPTP.java ERROR: Could not find system";
        }

        // make sure prover exists
        String dir = SystemDirectory + "/" + systemVersion;
        try {
            File executableDir = new File(dir);
            if (!executableDir.exists()) {
                return "% SystemOnTPTP.java ERROR: Prover (" + systemVersion + ") does not exist inside System Directory: " + SystemDirectory + " [systemDir: " + executableDir.getCanonicalPath() + "]";
            } 
            commandLine = "";
      
            // build command line    
            boolean sd_match = Pattern.matches(".*%s.*%d.*", atpSystem.command);
            boolean ds_match = Pattern.matches(".*%d.*%s.*", atpSystem.command);
            boolean s_match = Pattern.matches(".*%s.*", atpSystem.command);
            problemPath = problemFile.getCanonicalPath();
            // replace all single \ with \\
            problemPath = problemPath.replace("\\", "\\\\");
            if (sd_match) {
                commandLine = String.format(executableDir.getCanonicalPath() + "/" + atpSystem.command, problemPath, limit);
            } else if (ds_match) {
                commandLine = String.format(executableDir.getCanonicalPath() + "/" + atpSystem.command, limit, problemPath);
            } else if (s_match) {
                commandLine = String.format(executableDir.getCanonicalPath() + "/" + atpSystem.command, problemPath);
            }
            if (!atpSystem.preCommand.equals("")) {
                commandLine = atpSystem.preCommand + " " + commandLine;
            }
			logger.finer("commandLine = " + commandLine);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return "% SystemOnTPTP.java ERROR: Something wrong path for systems directory: " + dir + " or with problem file location: " + filename + " : " + e;
        }

        try {
            // create process for atp call
            //      System.out.println("command line: " + commandLine);
            SystemOnTPTP.process = Runtime.getRuntime().exec(commandLine);
      
            // create thread for atp call    
            atp = new ATPThread(process, atpSystem, quietFlag, limit, format, commandLine, problemPath);
            // start atp system on the problem
            atp.start();
            // time limit is in seconds
            atp.join(Integer.valueOf(limit).intValue() * 1000);
            // times up (stop process if still going)
            process.destroy();
            //      return atp.getResponse() + "\n\nStatus: " + atp.getStatus() + "\n\nSolution (in tptp format): \n" + atp.getSolution() + "\nSolution type: " + atp.getSolutionType() + "\nTotal running time: " + atp.getTime();
			String results = atp.getResults();
			logger.exiting("SystemOnTPTP", "SystemOnTPTP", results);
			return results;
        } catch (Exception e) {
            e.printStackTrace();
            return "% SystemOnTPTP.java ERROR: Something wrong with commandLine(" + commandLine + ") from prover (" + systemVersion + "): " + e;
        }
    }

    public static String getSystemDir () { return SystemDirectory; }
    public static String getSystemInfo () { return SystemInfo; }
  
    public static void printSystems (String status) {
        ArrayList<String> systems;
        String tptpHome = System.getenv("JTPTP_HOME");
        if (tptpHome != null && tptpHome != "") {
            SystemDirectory = tptpHome + "/" + "Systems";
            SystemInfo = SystemDirectory + "/" + "SystemInfo";
        }    
        systems = listSystems(SystemDirectory, status);
        for (int i = 0; i < systems.size(); i++) {
            System.out.println(systems.get(i));
        }
    }

    public static int getTPTPFormulaSize (String tptp) throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(tptp));
        TPTPParser parser = TPTPParser.parse(reader);
        return parser.Items.size();
    }

    // given a tptp result, return true if status theorem in results, else false
    public static boolean isTheorem (String tptp) throws Exception {
        boolean theorem = false;
        String line;
        BufferedReader bin =  new BufferedReader(new StringReader(tptp));
        while ((line = bin.readLine()) != null) {      
            if (line.startsWith(SZS_STATUS_THEOREM)) {
                theorem = true;
                break;
            }
        }
        return theorem;
    }

    // given a tptp result, return true if status countersatisfiable in results, else false
    public static boolean isCounterSatisfiable (String tptp) throws Exception {
        boolean counter_satisfiable = false;
        String line;
        BufferedReader bin =  new BufferedReader(new StringReader(tptp));
        while ((line = bin.readLine()) != null) {      
            if (line.startsWith(SZS_STATUS_COUNTER_SATISFIABLE)) {
                counter_satisfiable = true;
                break;
            }
        }
        return counter_satisfiable;
    }

    public static boolean proofExists (String tptp) throws Exception {
        TPTPParser parser = new TPTPParser(new BufferedReader(new StringReader(tptp)));
        boolean exists = parser.Items.size() > 0;
        return exists;
    }

    public static int timeUsed (String tptp) throws Exception {
        int time = 0;
        String line;
        String result = "";
        BufferedReader bin =  new BufferedReader(new StringReader(tptp));
        while ((line = bin.readLine()) != null) {      
            if (line.startsWith(RESULT)) {
                result = line;
                break;
            }
        }
        result = result.substring(result.indexOf(":") + 1, result.length());
        String num = "";
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == '.') {
                break;
            }
            if (Character.isDigit(result.charAt(i))) {
                num += result.charAt(i);
            }
        }
        if (num != "")
        	time = Integer.parseInt(num) + 1;
        else time = 1;
        
        return time;
    }

    //  public static ArrayList<String> getSZSAnswers (String tptp, JspWriter out) throws Exception {
    public static ArrayList<String> getSZSAnswers (String tptp) throws Exception {
        String SZS_ANSWERS_SHORT = "% SZS answers short";
        String line;
        BufferedReader bin =  new BufferedReader(new StringReader(tptp));
        ArrayList<String> answers = new ArrayList();
        while ((line = bin.readLine()) != null) {      
            if (line.startsWith(SZS_ANSWERS_SHORT)) {
                int split = SZS_ANSWERS_SHORT.length();
                String answers_short = line.substring(split+1, line.length());
                StringTokenizer st = new StringTokenizer(answers_short, "[], ", false);
                while (st.hasMoreTokens()) {
                    String next = st.nextToken();
                    if (!next.equals("")) {
                        answers.add(next);
                    }
                }
            }
        }
        return answers;
    }

    //  public static ArrayList<Binding> getSZSBindings (String conjecture, String tptp, JspWriter out) throws Exception {
    public static ArrayList<Binding> getSZSBindings (String conjecture, String tptp) throws Exception {
        ArrayList<Binding> bind = new ArrayList();
        BufferedReader reader = new BufferedReader(new StringReader(conjecture));
        TPTPParser parser = TPTPParser.parse(reader);
        // should only have conjecture in there, not dealing with anything else
        if (parser.Items.size() != 1) {
            return bind;
        }
        SimpleTptpParserOutput.TopLevelItem item = parser.Items.elementAt(0);
        //    return getSZSBindings(item, tptp, out);
        return getSZSBindings(item, tptp);
    }

    public static ArrayList<Binding> getSZSBindings (String conjecture, ArrayList<Binding> answers) throws Exception {
        ArrayList<Binding> bind = new ArrayList();
        BufferedReader reader = new BufferedReader(new StringReader(conjecture));
        TPTPParser parser = TPTPParser.parse(reader);
        // should only have conjecture in there, not dealing with anything else
        if (parser.Items.size() != 1) {
            return bind;
        }
        SimpleTptpParserOutput.TopLevelItem item = parser.Items.elementAt(0);
        return getSZSBindings(item, answers);
    }

    //  public static ArrayList<Binding> getSZSBindings (SimpleTptpParserOutput.TopLevelItem item, String tptp, JspWriter out) throws Exception {
    public static ArrayList<Binding> getSZSBindings (SimpleTptpParserOutput.TopLevelItem item, String tptp) throws Exception {
        ArrayList<Binding> bind = new ArrayList();
        if (item.getKind() != SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
            return bind;
        }
        SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
        ArrayList<String> variables = new ArrayList();
        variables = TPTPParser.identifyQuantifiedVariables(AF.getFormula(), variables);
        if (variables.isEmpty()) {
            return bind;
        }
        // uneven number of variables to answers, weirdness
        ArrayList<String> answers = getSZSAnswers(tptp);
        if (variables.size() != answers.size()) {
            return bind;
        }
        for (int i = 0; i < variables.size(); i++) {
            String variable = variables.get(i);
            String answer = answers.get(i);
            bind.add(new Binding(variable, answer));
        }
        return bind;
    }

    public static ArrayList<Binding> getSZSBindings (SimpleTptpParserOutput.TopLevelItem item, ArrayList<Binding> answers) throws Exception {
        ArrayList<Binding> bind = new ArrayList();
        if (item.getKind() != SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
            return bind;
        }
        SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
        ArrayList<String> variables = new ArrayList();
        variables = TPTPParser.identifyQuantifiedVariables(AF.getFormula(), variables);
        if (variables.isEmpty()) {
            return bind;
        }
        // uneven number of variables to answers, weirdness
        if (variables.size() != answers.size()) {
            return bind;
        }
        for (int i = 0; i < variables.size(); i++) {
            String variable = variables.get(i);
            String answer = answers.get(i).binding;
            bind.add(new Binding(variable, answer));
        }
        return bind;
    }

    public static void main (String[] args) throws Exception {
        if (args.length > 0) {
            if (args[0].equals("-w")) {
                if (args.length > 1) {
                    printSystems(args[1]);
                } else {
                    printSystems(null);
                }
                System.exit(0);
            } 
        }
        if (args.length == 0) {
            System.err.println("ERROR: Please provide filename to run SystemOnTPTP on.");
            System.exit(0);
        }
        String filename = args[0];
        System.out.println("SystemDir: " + SystemDirectory);
        System.out.println("SystemInfo: " + SystemInfo);
        System.out.println("filename: " + filename);
        String result = SystemOnTPTP.SystemOnTPTP("EP---0.999", null, 300, "-q1", "-S", filename);
        //String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",300,"/home/graph/tptp/TPTP/Problems/PUZ/PUZ001+1.p");
        //String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",Integer.valueOf(args[0]).intValue(),args[1]);

        System.out.println("Result: \n" + result);
    }

}


