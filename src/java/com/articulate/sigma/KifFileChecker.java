package com.articulate.sigma;

import com.articulate.sigma.utils.StringUtil;
import java.util.*;
import com.articulate.sigma.trans.SUMOtoTFAform;
/**
 * Simple .kif file validator.
 *
 * Delegates most checking to KButilities.isValidFormula().
 * Adds extra checks for quantifiers, variable usage, arity, and
 * term-below-Entity violations (skipping locally declared symbols).
 */
public class KifFileChecker {
    private static boolean debug = true;
    private static final class FormSpan {
        final String text;
        final int startLine;
        FormSpan(String t, int s) { text = t; startLine = s; }
    }

    /**
     * Validate a KIF file line by line using KButilities.isValidFormula().
     *
     * @param kb           Knowledge base used for validation.
     * @param lines        File contents as a list of lines.
     * @param includeBelow If true, enforces "term must be below Entity"
     *                     (but skips local symbols defined in the file).
     * @return A list of error messages, each prefixed with a line number.
     */
    public static List<String> check(KB kb, List<String> lines, boolean includeBelow) {

        if (lines == null) return Collections.emptyList();
        List<String> results = new ArrayList<>();
        KButilities.clearErrors();
        List<FormSpan> forms = splitKifFormsWithLines(String.join("\n", lines));
        
        // pass 1: Do one pass to collect declared instances and classes from the file.
        Set<String> localInstances = new HashSet<>();
        Set<String> localClasses = new HashSet<>();
        Map<String, String> localInstanceTypes = new HashMap<>();
        Map<String, String> localClassSupers = new HashMap<>();
        if (debug) System.out.println("\nKifFileChecker.check() -> collectingLocalSymbols()");
        collectLocalSymbols(forms, kb, localInstances, localClasses, localInstanceTypes, localClassSupers);
        if (debug) System.out.println("KifFileChecker.check() localInstances Found: " + localInstances);
        if (debug) System.out.println("KifFileChecker.check() localClasses Found: " + localClasses);
        
        // pass 2: Now that local instances and classes have been discovered go through each independednt axiom and find errors.
        /* Example Form:
            ;All men are mortal.
            (=>
                (instance ?M Man)
                (attribute ?M Mortal))     
         */
        int formulaCount = 0;
        for (FormSpan fs : forms) {
            String src = fs.text.trim();
            //If the axiom is somehow empty, or is a comment, move on to the next.
            if (src.isEmpty() || src.startsWith(";")) continue;
            //Create a formula object out of the axiom src string.
            if (debug) System.out.println("\n--------------------------------------------------------\n" + 
                                          "KifFileChecker.check() Checking formula #" + formulaCount + " on line #" + fs.startLine + ":");
            formulaCount++;
            if (debug) System.out.println(fs.text);
            Formula f = new Formula(src);
            //If the formula is a simple clause, then....
            boolean isSimpleClauseFormula = f.isSimpleClause(kb);
            // System.out.println("\nKifFileChecker.check() Formula.isSimpleClause() = " + isSimpleClauseFormula);
            // if (isSimpleClauseFormula) {
            //     String predicate = f.getStringArgument(0);
            //     List<String> signature = kb.kbCache.getSignature(predicate);
            //     List<String> args = f.argumentsToArrayListString(1);

            //     // Special handling for 'instance' predicate
            //     if ("instance".equals(predicate)) {
            //         String ind = f.getStringArgument(1);
            //         String cls = f.getStringArgument(2);

            //         // Check that ind is not a class
            //         boolean isClass = kb.isInstanceOf(ind, "Class") || kb.isSubclass(ind, "Class");
            //         if (isClass) {
            //             results.add("Line " + fs.startLine + ": instance arg1 (" + ind + ") should be an Entity, but is a Class.");
            //         }

            //         // Check that cls is a class
            //         boolean isClassType = kb.isInstanceOf(cls, "Class") || kb.isSubclass(cls, "Entity");
            //         if (!isClassType) {
            //             results.add("Line " + fs.startLine + ": instance arg2 (" + cls + ") should be a Class.");
            //         }
            //     }

            //     // General type checking for all arguments
            //     for (int i = 0; i < args.size(); i++) {
            //         String arg = args.get(i);
            //         String expectedType = (signature != null && signature.size() > i+1) ? signature.get(i+1) : null;
            //         expectedType = expectedType.replaceAll("\\+", "");
            //         if (expectedType != null && !Formula.isVariable(arg) && !StringUtil.isQuotedString(arg)) {
            //             // Local symbol shortcuts
            //             if ("Entity".equals(expectedType) && localInstances.contains(arg)) continue;
            //             if ("Class".equals(expectedType) && localClasses.contains(arg)) continue;

            //             boolean matchesRequiredType = false;

            //             // Direct instance, subclass, or exact match
            //             System.out.println("\nKifFileChecker.check() -> Checking Simple Clause, Arg: " + arg + ", Expected Type: " + expectedType);

            //             if (localInstances.contains(arg)) {
            //                 String localType = localInstanceTypes.get(arg);
            //                 if (localType != null && (kb.isSubclass(localType, expectedType) || localType.equals(expectedType))) {
            //                     matchesRequiredType = true;
            //                 }
            //             }
            //             else if (localClasses.contains(arg)) {
            //                 String localType = localClassSupers.get(arg);
            //                 if (localType != null && (kb.isSubclass(localType, expectedType) || localType.equals(expectedType))) {
            //                     matchesRequiredType = true;
            //                 }
            //             } else {
            //                 // For non-local terms, print instance types if available
            //                 Set<String> types = kb.getInstanceTypes(arg);
            //                 if (types != null && !types.isEmpty())
            //                     System.out.println("    Arg '" + arg + "' is an instance of: " + types);
            //                 else
            //                     System.out.println("    Arg '" + arg + "' has no explicit instance types.");
            //             }
            //             if (expectedType.equals("Class") && !Diagnostics.termNotBelowEntity(arg, kb)) matchesRequiredType = true;
            //             if (kb.isInstanceOf(arg, expectedType) || kb.isSubclass(arg, expectedType) || arg.equals(expectedType)) {
            //                 matchesRequiredType = true;
            //             } else {
            //                 // Indirect: arg is an instance of a class that is a subclass of expectedType
            //                 Set<String> argTypes = kb.getInstanceTypes(arg);
            //                 for (String type : argTypes) {
            //                     if (kb.isSubclass(type, expectedType) || type.equals(expectedType)) {
            //                         matchesRequiredType = true;
            //                         break;
            //                     }
            //                 }
            //             }
            //             if (!matchesRequiredType) {
            //                 results.add("Line " + fs.startLine + ": Argument " + (i+1) + " of '" + predicate +
            //                     "' expected type '" + expectedType + "', found '" + arg + "' of type '" + localClassSupers.get(arg));
            //             }
            //         }
            //     }
            // }

            // --- Syntax & Parsing errors ---------------------------------------------------------------------
            // System.out.println("KifFileChecker.check() Checking formula: \n" + src);
            if (!KButilities.isValidFormula(kb, src)) {
                for (String er : KButilities.errors) {
                    results.add("Line " + fs.startLine + ": " + er);
                    if (debug) System.out.println("\nKifFileChecker.check() -> Syntax Error Found:\n" + er);
                }
                KButilities.clearErrors();
                continue;
            }
            // if (debug) System.out.println("\nKifFileChecker.check() -> Syntax Errors: None");
            
            // --- Type Checking Errors ---------------------------------------------------------------------
            // boolean isTypeError = false;
            // if (debug) System.out.println("\nKifFileChecker.check() -> Checking Types:\n" + SUMOtoTFAform.varmap);
            // if (!KButilities.hasCorrectTypes(kb, f)) {
            //     System.out.println("\n\n\n\n\n\n\n\n\n\n hasCorrectTypes = false!!!!!!\n\n\n\n\n");
            //     for (String er : KButilities.errors) {
            //         results.add("Line " + fs.startLine + ": " + er);
            //         isTypeError = true;
            //         if (debug) System.out.println("\nKifFileChecker.check() -> Type Checking Error Found:\n" + er);
            //     }
            //     KButilities.clearErrors();
            //     continue;
            // }
            // // if (debug) System.out.println("\nKifFileChecker.check() -> Type Requirements InstancesOf:\n" + kb.instancesOf());
            // if (SUMOtoTFAform.varmap != null) {
            //     for (Map.Entry<String, Set<String>> entry : SUMOtoTFAform.varmap.entrySet()) {
            //         String var = entry.getKey();
            //         Set<String> types = entry.getValue();
                    
            //         String requiredType = null;
            //         for (String t : types) {
            //             if (kb.isInstanceOf(t, "Class") || kb.isSubclass(t, "Entity")) {
            //                 requiredType = t;
            //                 break;
            //             }
            //         }
            //         if (requiredType == null && !types.isEmpty()) {
            //             // Fallback: pick the first type as required
            //             requiredType = types.iterator().next();
            //         }
                    
            //         // if (debug) System.out.println("\nKifFileChecker.check() -> Type Requirements:\n" + SUMOtoTFAform.varmap);
            //         Object[] arr = SUMOtoTFAform.varmap.get(var).toArray();
            //         String mappedVar = (String) SUMOtoTFAform.varmap.get(var).toArray()[arr.length-1];
            //         Set<String> instanceTypes = new HashSet<>();
            //         for (Formula form : kb.instancesOf(mappedVar)) {
            //             String cls = form.getStringArgument(2); // get the class name from (instance Plumber ClassName)
            //             if (cls != null) instanceTypes.add(cls);
            //         }

            //         // Now check if any of these classes is a subclass of the required type
            //         boolean foundSubclass = false;
            //         for (String cls : instanceTypes) {
            //             if (kb.isSubclass(cls, requiredType) || cls.equals(requiredType)) {
            //                 foundSubclass = true;
            //                 break;
            //             }
            //         }

            //         if (debug) System.out.println("\nKifFileChecker.check() -> Type Checking \n" + mappedVar + " subclass found " + foundSubclass);

            //         // Now check each explicit type
            //         boolean foundType = false;
            //         for (String explicit : types) {
                        
            //             System.out.println("\n\nReq TYPES: " + requiredType);
            //             System.out.println("\n\nTYPES: " + types);
            //             if (!foundType && !explicit.equals(requiredType) && !kb.isSubclass(explicit, requiredType) && !foundSubclass) {
            //                 String er = "Line " + fs.startLine + ": Type error: variable " + var +
            //                     " has explicit type " + explicit + " which is not a subclass of required type " + requiredType;
            //                 results.add(er);
            //                 isTypeError = true;
            //                 if (debug) System.out.println("KifFileChecker.check() -> Type Checking Error Found: \n" + er);
            //             } else {
            //                 foundType = true;
            //             }
            //         }
            //     }
            // }
            // if (debug && !isTypeError) System.out.println("KifFileChecker.check() -> Type Checking Errors: None");
            
            // // --- Quantifier placement ------------------------------------------------------------------------
            // if (Diagnostics.quantifierNotInStatement(f)) {
            //     results.add("Line " + fs.startLine + ": Quantifier not in statement");
            // }

            // // --- Variables used once ------------------------------------------------------------------------
            // Set<String> singleUse = Diagnostics.singleUseVariables(f);
            // if (singleUse != null && !singleUse.isEmpty()) {
            //     results.add("Line " + fs.startLine + ": Variable(s) only used once: " + singleUse);
            // } else if (debug) System.out.println("KifFileChecker.check() -> Single Use Variable Errors: None");

            // // --- Unquantified variables in consequents ------------------------------------------------------
            // Set<String> unquant = Diagnostics.unquantInConsequent(f);
            // if (unquant != null && !unquant.isEmpty()) {
            //     results.add("Line " + fs.startLine + ": Unquantified var(s) in consequent: " + unquant);
            // } else if (debug) System.out.println("KifFileChecker.check() -> Unquantified Variable Errors: None");

            // // --- Arity errors -------------------------------------------------------------------------------
            // String badPred = PredVarInst.hasCorrectArity(f, kb);
            // if (!StringUtil.emptyString(badPred)) {
            //     results.add("Line " + fs.startLine + ": Arity error in predicate " + badPred);
            // } else if (debug) System.out.println("KifFileChecker.check() -> Arity Errors: None");
            
            // // --- Term-below-Entity checks ------------------------------------------------------------------
            // if (includeBelow) {
            //     for (String t : f.collectTerms()) {
            //         if (Formula.isVariable(t) || StringUtil.isQuotedString(t)) continue;
            //         if (localInstances.contains(t) || localClasses.contains(t)) continue; // ✅ skip locals
            //         if (Diagnostics.termNotBelowEntity(t, kb)) {
            //             results.add("Line " + fs.startLine + ": term not below Entity: " + t);
            //         } else if (debug) System.out.println("KifFileChecker.check() -> Term Not Below Entity Errors: None");
            //     }
            // }
        }
        return results;
    }

    /**
     * Collect locally declared individuals and classes from instance/subclass forms,
     * and record their types/superclasses for local type checking.
     */
    private static void collectLocalSymbols(List<FormSpan> forms, KB kb,
                                        Set<String> localInstances,
                                        Set<String> localClasses,
                                        Map<String, String> localInstanceTypes,
                                        Map<String, String> localClassSupers) {

        for (FormSpan fs : forms) {
            Formula f = new Formula(fs.text.trim());
            if (!f.isSimpleClause(kb)) continue;
            String pred = f.getStringArgument(0);
            if ("instance".equals(pred)) {
                String ind = f.getStringArgument(1);
                String cls = f.getStringArgument(2);
                if (!Formula.isVariable(ind) && !StringUtil.isQuotedString(ind)) {
                    localInstances.add(ind);
                    // Record the type for this local instance
                    if (!Formula.isVariable(cls) && !StringUtil.isQuotedString(cls)) {
                        localInstanceTypes.put(ind, cls);
                    }
                }
                if (!Formula.isVariable(cls) && !StringUtil.isQuotedString(cls) && Diagnostics.termNotBelowEntity(cls, kb)) {
                    localClasses.add(cls);
                }
            }
            else if ("subclass".equals(pred)) {
                String sub = f.getStringArgument(1);
                String sup = f.getStringArgument(2);
                if (!Formula.isVariable(sub) && !StringUtil.isQuotedString(sub)) {
                    localClasses.add(sub);
                    // Record the superclass for this local class
                    if (!Formula.isVariable(sup) && !StringUtil.isQuotedString(sup)) {
                        localClassSupers.put(sub, sup);
                    }
                }
                if (!Formula.isVariable(sup) && !StringUtil.isQuotedString(sup)) {
                    localClasses.add(sup);
                }
            }
        }
    }

    /**
     * Splits a block of SUO-KIF text into balanced top-level forms,
     * preserving their starting line numbers.
     */
    private static List<FormSpan> splitKifFormsWithLines(String text) {
        List<FormSpan> forms = new ArrayList<>();
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        boolean inStr = false;
        int formStart = 1;

        for (int ln = 0; ln < lines.length; ln++) {
            String rawLine = lines[ln];
            String line = rawLine;
            // strip ; comments outside strings
            StringBuilder sb = new StringBuilder();
            boolean cut = false;
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) inStr = !inStr;
                if (!inStr && c == ';') { cut = true; break; }
                sb.append(c);
            }
            if (cut) line = sb.toString();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) inStr = !inStr;
                else if (!inStr) {
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                }
            }
            if (cur.length() == 0) formStart = ln + 1;
            if (cur.length() > 0) cur.append('\n');
            cur.append(rawLine);
            if (depth == 0 && !inStr && cur.toString().trim().length() > 0) {
                forms.add(new FormSpan(cur.toString(), formStart));
                cur.setLength(0);
            }
        }

        if (cur.toString().trim().length() > 0) {
            forms.add(new FormSpan(cur.toString(), formStart));
        }
        return forms;
    }
}
