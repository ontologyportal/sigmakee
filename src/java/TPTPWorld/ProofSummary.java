package TPTPWorld;

import java.util.*;
import java.io.*;
import tptp_parser.*;

public class ProofSummary {

  private static ArrayList<TPTPFormula> extractLemmas (TPTPFormula formula, ArrayList<TPTPFormula> conjectures, ArrayList<TPTPFormula> lemmas) {
    for (TPTPFormula parent : formula.parent) {
      if (!lemmas.contains(parent) && !conjectures.contains(parent)) {
        lemmas.add(parent);
      }
    }
    for (TPTPFormula child : formula.child) {
      lemmas = extractLemmas(child, conjectures, lemmas);
    }
    return lemmas;
  }

  // identify all conjectures on the vine
  // future: store in a sorted set for faster search in extractLemmas
  private static ArrayList<TPTPFormula> identifyConjectures (TPTPFormula formula, ArrayList<TPTPFormula> conjectures) {
    conjectures.add(formula);
    for (TPTPFormula child : formula.child) {
      conjectures = identifyConjectures(child, conjectures);
    }
    return conjectures;
  }

  // given a conjecture: return the immediate lemmas in the vine
  public static ArrayList<TPTPFormula> getLemmaVine (TPTPFormula conjecture) {
    ArrayList<TPTPFormula> lemmas = new ArrayList();
    ArrayList<TPTPFormula> conjectures = new ArrayList();

    if (conjecture == null) {
      return lemmas;
    }

    conjectures = identifyConjectures(conjecture, conjectures);
    for (TPTPFormula child : conjecture.child) {
      lemmas = extractLemmas(child, conjectures, lemmas);
    }
    return lemmas;    
  }

  public static void main (String args[]) throws Exception {

    TPTPParser.checkArguments(args);
    TPTPParser parser = TPTPParser.parse(args[0]);
    TPTPFormula conjecture = AnswerExtractor.extractVine(parser.ftable);
    if (conjecture == null) {
      System.out.println("% ERROR: No fof conjecture in proof -> no lemmas -> proof summary failed");
    }
    ArrayList<TPTPFormula> lemmas = getLemmaVine(conjecture);
    if (lemmas.isEmpty()) {
      System.out.println("% Given a proof with a valid fof conjecture, no lemmas found in ProofSummary");
    }
    for (TPTPFormula lemma : lemmas) {
      System.out.println(lemma.fofify() + "\n");
    }    
  }
}
