package TPTPWorld;

import java.io.*;
import java.util.*;
import tptp_parser.*;

public class Binding {
  public String variable;
  public String binding;
  public Binding next;

  // for unbinded variables
  public Binding (String variable) {
    this.variable = variable;
    this.binding = null;
    this.next = null;
  }
  // for binded variables
  public Binding (String variable, String binding) {
    this.variable = variable;
    this.binding = binding;
    this.next = null;        
  }

  public Binding addBinding (String bind) {
    this.binding = bind;
    char c = bind.charAt(0);
    boolean upper = Character.isUpperCase(c);
    if (upper) {
      this.next = new Binding(bind);
      return this.next;
    } 
    return null;
  }

  // if next is not null, then binding is not final binding, recurse
  public String getFinalBinding () {
    if (next == null) {
      return binding;
    } 
    return next.getFinalBinding();        
  }

  public static String getBinding (String variable, ArrayList<Binding> binds) {          
    for (Binding bind : binds) {
      assert bind.binding != null;
      if (variable.equals(bind.variable)) {
        return bind.binding;
      }
      //      System.out.println(bind.variable + " = " + bind.binding);
    }
    System.out.println("\n% ERROR: Variable did not bind properly in AnswerFinder: " + variable);
    System.exit(0);
    return "";
  }
}

