package com.articulate.sigma;

import java.util.*;
import java.io.*;

class ATPSystem {
  public String name;
  public String version;
  public String command;
  public String url;
  public String status;
  
  public Vector<String> solved[];
  public Vector<String> startSoln[];
  public Vector<String> endSoln[];
  
  private String getValue (BasicXMLelement element, String match) {
    Iterator it = element.attributes.keySet().iterator();
    while (it.hasNext()) {
      String key = (String) it.next();
      String value = (String) element.attributes.get(key);
      if (key.equals(match)) {
        return value;
      }
    }
    return "ERROR: couldn't find a value for: " + element.tagname;
  }

  public ATPSystem (BasicXMLelement system) {
    solved = new Vector[2];
    solved[0] = new Vector();
    solved[1] = new Vector();
    startSoln = new Vector[2];
    startSoln[0] = new Vector();
    startSoln[1] = new Vector();
    endSoln = new Vector[2];
    endSoln[0] = new Vector();
    endSoln[1] = new Vector();

    name = getValue(system, "value");
    System.out.println("atp name: " + name);
    ArrayList<BasicXMLelement> elements = system.subelements;
    for (int i = 0; i < elements.size(); i++) {
      BasicXMLelement subelement = elements.get(i);
      //      System.out.println("---------");
      //      System.out.println("element: " + elements.get(i));
      //      System.out.println("------------");
      if (subelement.tagname.equals("version")) {
        version = getValue(subelement, "value");
        System.out.println("version: " + version);
      }
      if (subelement.tagname.equals("url")) {
        url = getValue(subelement, "value");
        System.out.println("url: " + url);
      }
      if (subelement.tagname.equals("command")) {
        command = getValue(subelement, "value");
        System.out.println("command: " + command);
      }
      if (subelement.tagname.equals("statuses")) {
        System.out.println("finding statuses");
        ArrayList<BasicXMLelement> subelements = subelement.subelements;
        for (int j = 0; j < subelements.size(); j++) {
          System.out.println(" new status");
          BasicXMLelement subsubelement = subelements.get(j);
          if (subsubelement.tagname.equals("status")) {
            solved[0].add(getValue(subsubelement, "name"));
            solved[1].add(getValue(subsubelement, "value"));            
          }
        }
      }
      if (subelement.tagname.equals("solutions")) {
        System.out.println("finding solutions");
        ArrayList<BasicXMLelement> subelements = subelement.subelements;
        for (int j = 0; j < subelements.size(); j++) {
          System.out.println(" new solution");
          BasicXMLelement subsubelement = subelements.get(j);
          if (subsubelement.tagname.equals("solution")) {            
            String solutionName = getValue(subsubelement, "name");
            startSoln[0].add(solutionName);
            startSoln[1].add(getValue(subsubelement, "start"));
            endSoln[0].add(solutionName);
            endSoln[1].add(getValue(subsubelement, "end"));
          }
        }
      }
      if (subelement.tagname.equals("status")) {
        status = getValue(subelement, "value");
      }
    }
  }
}

public class SystemOnTPTP {

  private static final String SystemsDirectory = KBmanager.getMgr().getPref("systemsDir");
  //private static final String SystemsDirectory = "../../../../../systems";
  //private static final String SystemsDirectory = "/home/graph/strac/geoff/Sigma/sigma/systems";

  private static final String SystemsInfo = KBmanager.getMgr().getPref("baseDir") + "/KBs/systemsInfo.xml";

  private static Vector<ATPSystem> atpSystemList = null;
  private static Process process;

  private static final String TIMEOUT = "Timeout";
  private static final String GIVEUP  = "Give Up";

  private static class ATPThread extends Thread { 

    private long startTime;
    private long stopTime;

    private Process process;    
    private final String filename;
    private final String quietFlag;
    private final String format;

    private String response = ""; // process response
    private String solution = ""; // solution results within response
    private String status = SystemOnTPTP.TIMEOUT; // status of prover

    private String solutionType = "";

    private ATPSystem atpSystem;

    private int solvedIndex = -1;
    private int solutionIndex = -1;

    //private BufferedReader writer; // write to process
    private BufferedReader reader; // reader for process output
    private BufferedReader error;  // error reader for process error messages

    public ATPThread (Process process, ATPSystem atpSystem, String quietFlag, String format, String filename) {
      this.process = process;
      this.atpSystem = atpSystem;
      this.quietFlag = quietFlag;
      this.format = format;
      this.filename = filename;
      setDaemon(true);
    }
    
    private void checkSolved (String responseLine) {
      if (solvedIndex != -1) {
        // already have a solved status
        return;
      } else {
        // check if responseLine has a solved status in it
        int size = atpSystem.solved[0].size();
        for (int i = 0; i < size; i++) {
          if (responseLine.contains(atpSystem.solved[1].elementAt(i))) {
            solvedIndex = i;
            status = atpSystem.solved[0].elementAt(i);
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
            System.out.println("--------------------------------------------------");
            System.out.println("not solution: " + responseLine);
            System.out.println("compared to: " + atpSystem.startSoln[1].elementAt(i));
          }
        }
      }
    }

    public void run () {
      System.out.println("---Start thread");
      startTime = System.currentTimeMillis();      
      String responseLine = "";
      try {
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        error  = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((responseLine = reader.readLine()) != null) { 
          checkSolved(responseLine);
          checkSolution(responseLine);
          response += responseLine + "\n";
        }
        reader.close();
        while ((responseLine = error.readLine()) != null) {
          // check every responseLine to see if it matches solutions and solved statuses
          response += responseLine + "\n";
        }
        error.close();
        if (status.equals(SystemOnTPTP.TIMEOUT)) {
          status = SystemOnTPTP.GIVEUP;
        }
      } catch (Exception e) {
        System.out.println("SystemOnTPTP.java Exception: " + e);
      }
      stopTime = System.currentTimeMillis();
      System.out.println("---End thread");
    }
    
    public double getTime () {
      return (double)(stopTime - startTime) / 1000.0;
    }
    
    public String getResponse () {
      System.out.println("\n-----");
      System.out.println(response);
      System.out.println("\n-----");
      return response;
    }

    public String getSolution () {
      String solutionResult = "";
      try {
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
        return "Error in pretty printing tptp format: " + e + "\n.  Solution: " + solution;
      }
    }

    public String getStatus () {
      return status;
    }
    
    public String getSolutionType () {
      return solutionType;
    }

    // return everything
    public String q0 () {
      String res = "";
      res += q01() + "\n";
      res += q2();
      return res;
    }
    // return system information
    public String q01 () {
      return getResponse();
    }
    // return my start info (currently, lets just pretty print tptp solution)
    public String q2 () {
      String res = "";
      res += "Pretty Print of Solution:\n";
      res += getSolution() + "\n";
      res += q3();
      return res;
    }
    // return my result
    public String q3 () {
      String res = "";
      res += "% END OF SYSTEM OUTPUT\n";
      res += "RESULT: " + filename + " - " + atpSystem.name + "---" + atpSystem.version + " - says " + getStatus() + " - Total time: " + getTime() + "\n";
      res += "OUTPUT: " + filename + " - " + atpSystem.name + "---" + atpSystem.version + " - says " + getSolutionType() + " - Total time: " + getTime();
      return res;
    }

    public String getResults () {
      // if format == -S, bypass quiet flags and only print tptp format
      if (format.equals("-S")) {
        return getSolution();
      }
      if (quietFlag.equals("-q0")) {
        return q0();
      } else if (quietFlag.equals("-q01")) {
        return q01();
      } else if (quietFlag.equals("-q2")) {
        return q2();
      } else if (quietFlag.equals("-q3")) {
        return q3();
      } else {
        // -q4: return nothing
        return "";
      }
    }
  }

  public static void loadSystems () {
    try {
      FileReader file = new FileReader(new File(SystemsInfo));
      BufferedReader bin = new BufferedReader(file);
      String info = "";
      String res;
      while ((res = bin.readLine()) != null) {
        info += res + "\n";
      }

      atpSystemList = new Vector();
      BasicXMLparser bp = new BasicXMLparser(info);
      ArrayList atp = ((BasicXMLelement)bp.elements.get(0)).subelements;
      for (int i = 0; i < atp.size(); i++) {
        atpSystemList.add(new ATPSystem((BasicXMLelement)atp.get(i)));
      }
    } catch (Exception err) {
      System.out.println(err);
    }
  }

  public static ArrayList<String> listSystems () {
    if (atpSystemList == null) {
      loadSystems();
    }
    ArrayList<String> systems = new ArrayList<String>();
    try {
      for (int i = 0; i < atpSystemList.size(); i++) {
        ATPSystem atpSystem = atpSystemList.get(i);
        systems.add(atpSystem.name + "---" + atpSystem.version);
      }
    } catch (Exception err) {
      System.out.println(err);      
    }
    return systems;
  }

  public static String SystemOnTPTP (String systemVersion, int limit, String quietFlag, String format, String filename) {
    // read in SystemsInfo.xml and find prover (system---version)
    // retrieve necessary info (Command, Solved, StartSoln/EndSoln, etc)
    // IMPLEMENT

    if (atpSystemList == null) {
      loadSystems();
    }
    ATPSystem atpSystem = null;
    String commandLine;
    ATPThread atp;

    for (int i = 0; i < atpSystemList.size(); i++) {
      ATPSystem currentSystem = atpSystemList.get(i);
      String currentName = currentSystem.name + "---" + currentSystem.version;      
      System.out.println("current system: " + currentName);
      System.out.println(" comparing to : " + systemVersion);
      if (currentName.equals(systemVersion)) {
        System.out.println("found system");
        atpSystem = currentSystem;
        break;
      }
    }

    if (atpSystem == null) {
      return "SystemOnTPTP.java ERROR: could not find system";
    }

    // make sure prover exists
    String systemDir = SystemsDirectory + "/" + systemVersion;
    if (!(new File(systemDir)).exists()) {
      return "SystemOnTPTP.java ERROR: Prover (" + systemVersion + ") does not exist inside System Directory: " + SystemsDirectory;
    } 

    // build command line    
    commandLine = String.format(SystemsDirectory + "/" + systemVersion + "/" + atpSystem.command, limit, filename);

    try {
      // create process for atp call
      System.out.println("command line: " + commandLine);
      SystemOnTPTP.process = Runtime.getRuntime().exec(commandLine);
      
      // create thread for atp call    
      atp = new ATPThread(process, atpSystem, quietFlag, format, filename);
      // start atp system on the problem
      atp.start();
      // time limit is in seconds
      atp.join(Integer.valueOf(limit).intValue() * 1000);
      // times up (stop process if still going)
      process.destroy();
      //      return atp.getResponse() + "\n\nStatus: " + atp.getStatus() + "\n\nSolution (in tptp format): \n" + atp.getSolution() + "\nSolution type: " + atp.getSolutionType() + "\nTotal running time: " + atp.getTime();
      return atp.getResults();
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return "SystemOnTPTP.java ERROR: Something wrong with commandLine(" + commandLine + ") from prover (" + systemVersion + "): " + e;
    }
  }

  public static String getSystemsDir () { return SystemsDirectory; }
  public static String getSystemsInfo () { return SystemsInfo; }
  
  public static void main(String[] args) throws Exception {
    System.out.println("SystemsDir: " + SystemsDirectory);
    System.out.println("SystemsInfo: " + SystemsInfo);
    String result = SystemOnTPTP.SystemOnTPTP("EP---0.999", 300, "-q1", "-S", SystemsDirectory + "/../problem.p");
    //String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",300,"/home/graph/tptp/TPTP/Problems/PUZ/PUZ001+1.p");
    //String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",Integer.valueOf(args[0]).intValue(),args[1]);

    System.out.println("Result: \n" + result);
  }

}

