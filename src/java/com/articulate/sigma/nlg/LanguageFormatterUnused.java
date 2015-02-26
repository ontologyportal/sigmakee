package com.articulate.sigma.nlg;

import com.articulate.sigma.KB;

/**
 * These methods were taken from LanguageFormatter and put here because they are not currently used.
 * We may decide to delete this class altogether.
 */
@Deprecated
class LanguageFormatterUnused {

    /** *************************************************************
     * Print out set of all format and termFormat expressions
     */
    private void generateAllLanguageFormats(KB kb) {

        System.out.println(";;-------------- Terms ---------------");
        System.out.println(allTerms(kb));
        for (int i = 1; i < 5; i++) {
            System.out.println(";;-------------- Arity " + i + " Functions ---------------");
            System.out.println(allFunctionsOfArity(kb,i));
            System.out.println(";;-------------- Arity " + i + " Relations ---------------");
            System.out.println(allRelationsOfArity(kb,i+1));
        }
    }

    /** *************************************************************
     *  @return a string with termFormat expressions created for all
     *  the terms in the knowledge base
     */
    private String allTerms(KB kb) {

        StringBuilder result = new StringBuilder();
        for (String term : kb.getTerms()) {
            result.append("(termFormat EnglishLanguage ");
            result.append(term);
            result.append(" \"");
            result.append(NLGUtils.prettyPrint(term));
            result.append("\")\n");
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String functionFormat(String term, int i) {

        switch (i) {
            case 1: return "the &%" + NLGUtils.prettyPrint(term) + " of %1";
            case 2: return "the &%" + NLGUtils.prettyPrint(term) + " of %1 and %2";
            case 3: return "the &%" + NLGUtils.prettyPrint(term) + " of %1, %2 and %3";
            case 4: return "the &%" + NLGUtils.prettyPrint(term) + " of %1, %2, %3 and %4";
        }
        return "";
    }

    /** *************************************************************
     * FIXME: write unit tests on this and replace string concatentation inside StringBuilder.append( ) below
     * with successive appends.
     */
    private String allFunctionsOfArity(KB kb, int i) {

        String parent = "";
        switch (i) {
            case 1: parent = "UnaryFunction"; break;
            case 2: parent = "BinaryFunction"; break;
            case 3: parent = "TernaryFunction"; break;
            case 4: parent = "QuaternaryFunction"; break;
        }
        if (parent.isEmpty())
            return "";
        StringBuilder result = new StringBuilder();
        for (String term : kb.getTerms()) {
            if (kb.kbCache.transInstOf(term, parent))
                result.append("(format EnglishLanguage ").append(term).append(" \"").append(functionFormat(term, i)).append("\")\n");
        }
        return result.toString();
    }

    /** *************************************************************
     */
    private String relationFormat(String term, int i) {

        switch (i) {
            case 2: return ("%2 is %n "
                    + NLGUtils.getArticle(term, 1, 1, "EnglishLanguage")
                    + "&%" + NLGUtils.prettyPrint(term) + " of %1");
            case 3: return "%1 %n{doesn't} &%" + NLGUtils.prettyPrint(term) + " %2 for %3";
            case 4: return "%1 %n{doesn't} &%" + NLGUtils.prettyPrint(term) + " %2 for %3 with %4";
            case 5: return "%1 %n{doesn't} &%" + NLGUtils.prettyPrint(term) + " %2 for %3 with %4 and %5";
        }
        return "";
    }

    /** *************************************************************
     */
    private String allRelationsOfArity(KB kb, int i) {

        String parent = "";
        switch (i) {
            case 2: parent = "BinaryPredicate"; break;
            case 3: parent = "TernaryPredicate"; break;
            case 4: parent = "QuaternaryPredicate"; break;
            case 5: parent = "QuintaryPredicate"; break;
        }
        if (parent.isEmpty())
            return "";
        StringBuilder result = new StringBuilder();
        for (String term : kb.getTerms()) {
            if (kb.kbCache.transInstOf(term, parent))
                result.append("(format EnglishLanguage ").append(term).append(" \"").append(relationFormat(term, i)).append("\")\n");
        }
        return result.toString();
    }

    /** ***************************************************************
     * For debugging ...
     */
    private static void printSpaces(int depth) {

        for (int i = 0 ; i <= depth ; i++)
            System.out.print("  ");
        System.out.print(depth + ":");
    }


}
