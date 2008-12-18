package TPTPWorld;

import java.io.*;
import java.util.*;
import tptp_parser.*;

public class TPTPFormula {
  public int id;
  public String type = "";
  public SimpleTptpParserOutput.TopLevelItem item;
  public SimpleTptpParserOutput.Annotations annotations = null;
  public SimpleTptpParserOutput.Source source = null;
  public ArrayList<TPTPFormula> parent;
  public ArrayList<TPTPFormula> child;

  public TPTPFormula (SimpleTptpParserOutput.TopLevelItem item, int id) {
    this.item = item;
    this.id = id;
    type = TPTPParser.getType(item);
    parent = new ArrayList();
    child = new ArrayList();
    
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      annotations = AF.getAnnotations();
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      annotations = AC.getAnnotations();
    } 
    if (annotations != null) {
      source = annotations.getSource();
    }
  }

  public void addParent (TPTPFormula that) {
      if (that != null) {
	  this.parent.add(that);
	  that.child.add(this);
      } else {
	  System.out.println("%WARNING: Trying to add parent null to "+this.toString());
      }
  }

  // given a cnf formula, turn it into an fof formula (universally quantify all variables)  
  public String fofify () {
    String UniversalQuantifier = "!";
    String res = "";
    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
      SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
      res += "fof(";
      res += TPTPParser.getName(item) + ",";
      res += AF.getRole().toString();
      res += ",(" + "\n";
      res += "    " + AF.getFormula().toString(4);
      res += " )).";      
    } else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
      SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
      ArrayList<String> variables = new ArrayList();
      variables = TPTPParser.identifyClauseVariables(AC.getClause(), variables);
      res += "fof(";
      res += TPTPParser.getName(item) + ",";
      //      res += (conjecture) ? AC.getRole().toString() : "axiom";
      res += AC.getRole().toString();
      res += ", (" + "\n";
      if (!variables.isEmpty()) {
        res += "    " + UniversalQuantifier + " [";
        res += variables.get(0);
        for (int i = 1; i < variables.size(); i++) {
          res += "," + variables.get(i);
        }
        res += "] : " + "\n";
      }
      res += (!variables.isEmpty()) ? "      ( " : "    ";
      res += (!variables.isEmpty()) ? AC.getClause().toString(8) : AC.getClause().toString(4); 
      res += (!variables.isEmpty()) ? " )" : "";
      res += " )).";
    } 
    return res;
  }

}

