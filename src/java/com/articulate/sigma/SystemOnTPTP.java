package com.articulate.sigma;

import java.util.*;
import java.io.*;


public class SystemOnTPTP {

  private static final String SystemsDirectory = KBmanager.getMgr().getPref("systemsDir");
  //private static final String SystemsDirectory = "../../../../../systems";
  //private static final String SystemsDirectory = "/home/graph/strac/geoff/Sigma/sigma/systems";

  private static final String SystemsInfo = SystemsDirectory + "/systemInfo.xml";

  private static Process process;

  private static final String TIMEOUT = "Timeout";
  private static final String GIVEUP  = "Give Up";

  private static class ATPThread extends Thread { 

    private long startTime;
    private long stopTime;

    private Process process;

    private String response = ""; // process response
    private String solution = ""; // solution results within response
    private String status = SystemOnTPTP.TIMEOUT; // status of prover

    private Vector<String> solved[];
    private Vector<String> startSoln[];
    private Vector<String> endSoln[];

    private int solvedIndex = -1;
    private int solutionIndex = -1;

    //private BufferedReader writer; // write to process
    private BufferedReader reader; // reader for process output
    private BufferedReader error;  // error reader for process error messages

    public ATPThread (Process process, Vector<String> solved[], 
                      Vector<String> startSoln[], Vector<String> endSoln[]) {
      this.process = process;
      this.solved = solved;
      this.startSoln = startSoln;
      this.endSoln = endSoln;
      setDaemon(true);
    }
    
    private void checkSolved (String responseLine) {
      if (solvedIndex != -1) {
        // already have a solved status
        return;
      } else {
        // check if responseLine has a solved status in it
        int size = solved[0].size();
        for (int i = 0; i < size; i++) {
          if (responseLine.contains(solved[1].elementAt(i))) {
            solvedIndex = i;
            status = solved[0].elementAt(i);
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
        if (responseLine.contains(endSoln[1].elementAt(solutionIndex))) {
          solutionIndex = -2;
        } else {
          solution += responseLine + "\n";
        }
      } else {
        // check if this is start of solution
        int size = startSoln[0].size();
        for (int i = 0; i < size; i++) {
          if (responseLine.contains(startSoln[1].elementAt(i))) {
            solutionIndex = i;
            solution = startSoln[0].elementAt(i) + "\n";
          } else {
            System.out.println("--------------------------------------------------");
            System.out.println("not solution: " + responseLine);
            System.out.println("compared to: " + startSoln[1].elementAt(i));
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
    
    public long getTime () {
      return (stopTime - startTime);
    }
    
    public String getResponse () {
      System.out.println("\n-----");
      System.out.println(response);
      System.out.println("\n-----");
      return response;
    }

    public String getSolution () {
      return solution;
    }

    public String getStatus () {
      return status;
    }
  }

  public static ArrayList<String> listSystems () {
    ArrayList<String> systems = new ArrayList<String>();
    try {
      systems.add("EP---0.999");
    } catch (Exception err) {
      System.out.println(err);      
    }
    return systems;
  }

  public static String SystemOnTPTP (String system, String version, int limit, String filename) {
    // read in SystemsInfo.xml and find prover (system---version)
    // retrieve necessary info (Command, Solved, StartSoln/EndSoln, etc)
    // IMPLEMENT
    try {
      FileReader file = new FileReader(new File(SystemsInfo));
      BufferedReader bin = new BufferedReader(file);
      String info = "";
      String res;
      while ((res = bin.readLine()) != null) {
        info += res + "\n";
      }
      System.out.println("info: " + info);

      BasicXMLparser bp = new BasicXMLparser(info);
      System.out.print("Parse completed.  Number of elements: ");
      System.out.println(bp.elements.size());
      System.out.println(bp.toString());
      
      
      ArrayList elements = bp.elements;
      for (int i = 0; i < elements.size(); i++) {
        ArrayList subelements = ((BasicXMLelement)elements.get(i)).subelements;
        System.out.println("-Main element: " + elements.get(i).toString());
        for (int j = 0; j < subelements.size(); j++) {
          System.out.println("--sub element: " + subelements.get(j).toString());
          ArrayList subsubelements = ((BasicXMLelement)subelements.get(j)).subelements;
          for (int k = 0; k < subsubelements.size(); k++) {
            System.out.println("----subsub element: " + subsubelements.get(k).toString());
          }
        }
      }
    } catch (Exception err) {
      System.out.println(err);
    }


    String command;

    Vector<String> [] solved = new Vector[2];
    solved[0] = new Vector();
    solved[1] = new Vector();
    Vector<String> [] startSoln = new Vector[2];
    startSoln[0] = new Vector();
    startSoln[1] = new Vector();
    Vector<String> [] endSoln = new Vector[2];
    endSoln[0] = new Vector();
    endSoln[1] = new Vector();

    String commandLine;
    ATPThread atp;

    // hard coded for now
    command = "eproof --print-statistics -xAuto -tAuto --cpu-limit=%d --memory-limit=Auto --tstp-in --tstp-out %s";

    solved[0].add("Theorem");
    solved[1].add("SZS status Theorem");
    solved[0].add("CounterSatisfiable");
    solved[1].add("SZS status CounterSatisfiable");
    solved[0].add("Unsatisfiable");
    solved[1].add("SZS status Unsatisfiable");
    solved[0].add("Satisfiable");
    solved[1].add("SZS status Satisfiable");
    
    startSoln[0].add("CNFRefutation");
    startSoln[1].add("SZS output start CNFRefutation");
    endSoln[0].add("CNFRefutation");
    endSoln[1].add("SZS output end CNFRefutation");
    startSoln[0].add("Saturation");
    startSoln[1].add("SZS output start Saturation");
    endSoln[0].add("Saturation");
    endSoln[1].add("SZS output end Saturation");

    // build command line
    commandLine = String.format(SystemsDirectory + "/" + system + "---" + version + "/" + command, limit, filename);

    try {
      // create process for atp call
      System.out.println("command line: " + commandLine);
      SystemOnTPTP.process = Runtime.getRuntime().exec(commandLine);
      
      // create thread for atp call    
      atp = new ATPThread(process, solved, startSoln, endSoln);
      // start atp system on the problem
      atp.start();
      // time limit is in seconds
      atp.join(Integer.valueOf(limit).intValue() * 1000);
      // times up (stop process if still going)
      process.destroy();
      return atp.getResponse() + "\n\nStatus: " + atp.getStatus() + "\n\nSolution: " + atp.getSolution();
    } catch (Exception e) {
      System.out.println("Error: " + e);
      return "SystemOnTPTP.java Error: " + e;
    }
  }

  public static String getSystemsDir () { return SystemsDirectory; }
  public static String getSystemsInfo () { return SystemsInfo; }
  
  public static void main(String[] args) throws Exception {
    KBmanager.getMgr().initializeOnce();
    System.out.println("SystemsDir: " + SystemsDirectory);
    System.out.println("SystemsInfo: " + SystemsInfo);
    String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",300,SystemsDirectory + "/../problem.p");
    //String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",300,"/home/graph/tptp/TPTP/Problems/PUZ/PUZ001+1.p");
    //String result = SystemOnTPTP.SystemOnTPTP("EP","0.999",Integer.valueOf(args[0]).intValue(),args[1]);

    /*
    String xml = 
      "<systems>\n" +
      "  <system value='EP'>\n"+
      "    <version value='0.999'/>\n"+
      "    <url value='http://www.eprover.org/'/>\n"+
      "    <command value='eproof --print-statistics -xAuto -tAuto --cpu-limit=%d --memory-limit=Auto --tstp-in --tstp-out %s'/>\n"+
      "    <statuses>\n"+
      "      <status name='Theorem' value='SZS status Theorem'/>\n"+
      "      <status name='CounterSatisfiable' value='SZS status CounterSatisfiable'/>\n"+
      "      <status name='Unsatisfiable' value='SZS status Unsatisfiable'/>\n"+
      "      <status name='Satisfiable' value='SZS status Satisfiable'/>\n"+
      "    </statuses>\n"+
      "    <solutions>\n"+
      "      <solution name='CNFRefutation'>\n"+
      "        <start value='SZS output start CNFRefutation'/>\n"+
      "        <end value='SZS output end CNFRefutation'/>\n"+
      "      </solution>\n"+
      "      <solution name='Saturation'>\n"+
      "        <start value='SZS output start Saturation'/>\n"+
      "        <end value='SZS output end Saturation'/>\n"+
      "      </solution>\n"+
      "    </solutions>\n"+
      "    <status value='READY'/>\n"+
      "  </system>\n" +
      "</systems>\n";
    
    BasicXMLparser bp = new BasicXMLparser(xml);
    System.out.print("Parse completed.  Number of elements: ");
    System.out.println(bp.elements.size());
    System.out.println(bp.toString());


    ArrayList elements = bp.elements;
    for (int i = 0; i < elements.size(); i++) {
      ArrayList subelements = ((BasicXMLelement)elements.get(i)).subelements;
      System.out.println("-Main element: " + elements.get(i).toString());
      for (int j = 0; j < subelements.size(); j++) {
        System.out.println("--sub element: " + subelements.get(j).toString());
        ArrayList subsubelements = ((BasicXMLelement)subelements.get(j)).subelements;
        for (int k = 0; k < subsubelements.size(); k++) {
          System.out.println("----subsub element: " + subsubelements.get(k).toString());
        }
      }
    }
    */
    System.out.println("Result: \n" + result);
  }

}

