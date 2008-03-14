package TPTPWorld;

import java.util.*;
import java.io.*;

public class ATPSystem {
  public String name;
  public String version;
  public String command;
  public String preCommand;
  public String url;
  
  public Vector<String> solved[];
  public Vector<String> startSoln[];
  public Vector<String> endSoln[];
  public Vector<String> status;
  
  public ATPSystem (String name,
                    String version,
                    String preCommand,
                    String command,
                    String url,
                    Vector<String> solved[],
                    Vector<String> startSoln[],
                    Vector<String> endSoln[],
                    Vector<String> status) {
    this.name = name;
    this.version = version;
    this.preCommand = preCommand;
    this.command = command;
    this.url = url;
    this.solved = solved;
    this.startSoln = startSoln;
    this.endSoln = endSoln;
    this.status = status;

    /*
    System.out.println("New System");
    System.out.println("name: " + name);
    System.out.println("version: " + version);
    System.out.println("command: " + command);
    System.out.println("url: " + url);
    System.out.println("# solved status: " + solved[0].size());
    for (int i = 0; i < solved[0].size(); i++) {
      System.out.println("  solved tag: " + solved[0].elementAt(i));
      System.out.println("  solved value: " + solved[1].elementAt(i));
    }
    System.out.println("# start Soln: " + startSoln[0].size());
    for (int i = 0; i < startSoln[0].size(); i++) {
      System.out.println("  startSoln tag: " + startSoln[0].elementAt(i));
      System.out.println("  startSoln value: " + startSoln[1].elementAt(i));
    }
    System.out.println("# end Soln: " + endSoln[0].size());
    for (int i = 0; i < endSoln[0].size(); i++) {
      System.out.println("  endSoln tag: " + endSoln[0].elementAt(i));
      System.out.println("  endSoln value: " + endSoln[1].elementAt(i));
    }
    System.out.println("# statuses: " + status.size());
    for (int i = 0; i < status.size(); i++) {
      System.out.println("  status: " + status.elementAt(i));
    }
    */
  }

}
