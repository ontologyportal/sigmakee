package com.articulate.sigma.nlg;

import com.articulate.sigma.Formula;
import com.articulate.sigma.KB;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
* Hold properties associated with a SumoProcessCollector. Properties refers to adjectives (SUO-Kif "attributes") as well
 * as any other expression or phrase modifying an entity.
*/
public class SumoProcessEntityProperty implements Comparable {
    private final Formula formula;
    private final String predicate;
    private List<String> arguments = Lists.newArrayList();

    /***********************************************************************************
     *
     * @param form
     */
    public SumoProcessEntityProperty(Formula form) {
        formula = form;
        predicate = form.car();

        String formAsString = form.getFormula();
        // Remove enclosing parens.
        formAsString = formAsString.substring(1, formAsString.length() - 1);
        arguments = Arrays.asList(formAsString.split(" "));
    }

    /***********************************************************************************
     * For a given property, combine it with the given noun into a natural-sounding expression. E.g.,
     * "hat" + black-attribute -> "black hat"; "hat" + location-on-dresser-attribute -> "hat on the dresser".
     * @param noun
     * @return
     */
    public String getSurfaceFormForNoun(String noun, KB kb)    {
        if (predicate.equals("attribute"))   {
            String attribute = arguments.get(2);
            String kbForm = kb.getTermFormatMap("EnglishLanguage").get(attribute);
            if (kbForm == null)  {
                // For some reason the formatted version of the term is not in the KBs. Maybe we'll have luck with the process name in the original formula.
                kbForm = attribute;
            }
            return kbForm + " " + noun;
        }

        return noun;
    }

    /***********************************************************************************
     *
     * @return
     */
    @Override
    public String toString()    {
        return formula.getFormula();
    }

    /***********************************************************************************
     * Allows sortable collections of these objects.
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        SumoProcessEntityProperty p1 = (SumoProcessEntityProperty)o;
        return this.toString().compareTo(p1.toString());
    }
}
