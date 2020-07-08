package TPTPWorld;
;
import tptp_parser.SimpleTptpParserOutput;

import java.util.ArrayList;

public class TPTPFormula {

  public int id;
  public String type = "";
  public SimpleTptpParserOutput.TopLevelItem item;
  public SimpleTptpParserOutput.Annotations annotations = null;
  public SimpleTptpParserOutput.Source source = null;
  public ArrayList<TPTPFormula> parent;
  public ArrayList<TPTPFormula> child;

  /** ***************************************************************
   */
  public TPTPFormula(SimpleTptpParserOutput.TopLevelItem item, int id) {

      this.item = item;
      this.id = id;
      type = TPTPParser.getType(item);
      parent = new ArrayList();
      child = new ArrayList();

      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
          SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula) item);
          annotations = AF.getAnnotations();
      }
      else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
          SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause) item);
          annotations = AC.getAnnotations();
      }
      if (annotations != null) {
          source = annotations.getSource();
      }
  }

  /** ***************************************************************
   */
  public void addParent (TPTPFormula that) {

      if (that != null) {
	      this.parent.add(that);
	      that.child.add(this);
      }
      else {
	    //  System.out.println("%WARNING: Trying to add parent null to " + this.toString());
      }
  }

  /** ***************************************************************
   * given a cnf formula, turn it into an fof formula (universally quantify all variables)
   */
  public String fofify () {

      String UniversalQuantifier = "!";
      StringBuffer res = new StringBuffer();
      if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
          SimpleTptpParserOutput.AnnotatedFormula AF = ((SimpleTptpParserOutput.AnnotatedFormula)item);
          res.append("fof(");
          res.append(TPTPParser.getName(item) + ",");
          res.append(AF.getRole().toString());
          res.append(",(" + "\n");
          res.append("    " + AF.getFormula().toString(4));
          res.append(" )).");
      }
      else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
          SimpleTptpParserOutput.AnnotatedClause AC = ((SimpleTptpParserOutput.AnnotatedClause)item);
          ArrayList<String> variables = new ArrayList();
          variables = TPTPParser.identifyClauseVariables(AC.getClause(), variables);
          res.append("fof(");
          res.append(TPTPParser.getName(item) + ",");
          //      res += (conjecture) ? AC.getRole().toString() : "axiom";
          res.append(AC.getRole().toString());
          res.append(", (" + "\n");
          if (!variables.isEmpty()) {
              res.append("    " + UniversalQuantifier + " [");
              res.append(variables.get(0));
              for (int i = 1; i < variables.size(); i++) {
                  res.append("," + variables.get(i));
              }
              res.append("] : " + "\n");
          }
          res.append((!variables.isEmpty()) ? "      ( " : "    ");
          res.append((!variables.isEmpty()) ? AC.getClause().toString(8) : AC.getClause().toString(4));
          res.append((!variables.isEmpty()) ? " )" : "");
          res.append(" )).");
      }
      return res.toString();
  }

  /** ***************************************************************
   */
    public String getRole () {

	    if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Formula) {
	        return ((SimpleTptpParserOutput.AnnotatedFormula) item).getRole().toString();
	    }
	    else if (item.getKind() == SimpleTptpParserOutput.TopLevelItem.Kind.Clause) {
	        return ((SimpleTptpParserOutput.AnnotatedClause) item).getRole().toString();
	    }
	    return "plain";
    }
}

