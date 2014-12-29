package com.articulate.sigma.semRewrite;

import java.util.ArrayList;
import java.util.List;

import com.articulate.sigma.Clausifier;
import com.articulate.sigma.Formula;

public class Interpreter {

    // canonicalize |,- into CNF
    // then unify
    // Note that because the language is so simple we only have to handle moving
    // negations and disjunctions
    // thisFormula = negationsIn();
    // thisFormula = disjunctionsIn();
    
  public static RuleSet rs = null;
  
  /** *************************************************************
   */
  public Interpreter (RuleSet rsin) {
      
      rs = rsin;
  }

  /** *************************************************************
   */
  public static void canon(RuleSet rsin) {
      
      //Clausifier.clausify(rs);
  }
  
  /** ***************************************************************
   */
  public static void main(String[] args) {  
  }
}
