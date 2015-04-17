/*
Copyright 2014-2015 IPsoft

Author: Adam Pease adam.pease@ipsoft.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program ; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston,
MA  02111-1307 USA
 */
package com.articulate.sigma.nlp.constants;

/**
 * POS Tag is derived from Penn POS
 * http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html
 * <p/>
 * Dependency Labels is from Stanford Typed Dependencies.
 * http://nlp.stanford.edu/software/dependencies_manual.pdf
 * <p/>
 * Created by Maochen on 12/8/14.
 */
public class LangLib {
    public static final String POS_CC = "CC"; //Coordinating conjunction
    public static final String POS_CD = "CD"; //Cardinal number
    public static final String POS_DT = "DT"; //Determiner
    public static final String POS_EX = "EX"; //Existential there
    public static final String POS_FW = "FW"; //Foreign word
    public static final String POS_IN = "IN"; //Preposition or subordinating conjunction
    public static final String POS_JJ = "JJ"; //Adjective
    public static final String POS_JJR = "JJR"; //Adjective, comparative
    public static final String POS_JJS = "JJS"; //Adjective, superlative
    public static final String POS_LS = "LS"; //List item marker
    public static final String POS_MD = "MD"; //Modal
    public static final String POS_NN = "NN"; //Noun, singular or mass
    public static final String POS_NNS = "NNS"; //Noun, plural
    public static final String POS_NNP = "NNP"; //Proper noun, singular
    public static final String POS_NNPS = "NNPS"; //Proper noun, plural
    public static final String POS_PDT = "PDT"; //Predeterminer
    public static final String POS_POS = "POS"; //Possessive ending
    public static final String POS_PRP = "PRP"; //Personal pronoun
    public static final String POS_PRPS = "PRP$"; //Possessive pronoun
    public static final String POS_RB = "RB"; //Adverb
    public static final String POS_RBR = "RBR"; //Adverb, comparative
    public static final String POS_RBS = "RBS"; //Adverb, superlative
    public static final String POS_RP = "RP"; //Particle
    public static final String POS_SYM = "SYM"; //Symbol
    public static final String POS_TO = "TO"; //to
    public static final String POS_UH = "UH"; //Interjection
    public static final String POS_VB = "VB"; //Verb, base form
    public static final String POS_VBD = "VBD"; //Verb, past tense
    public static final String POS_VBG = "VBG"; //Verb, gerund or present participle
    public static final String POS_VBN = "VBN"; //Verb, past participle
    public static final String POS_VBP = "VBP"; //Verb, non-3rd person singular present
    public static final String POS_VBZ = "VBZ"; //Verb, 3rd person singular present
    public static final String POS_WDT = "WDT"; //Wh-determiner
    public static final String POS_WP = "WP"; //Wh-pronoun
    public static final String POS_WPS = "WP$"; //Possessive wh-pronoun
    public static final String POS_WRB = "WRB"; //Wh-adverb

    /**
     * Named Entity Tags.
     * This is based on MUC-7.
     * http://www-nlpir.nist.gov/related_projects/muc/proceedings/ne_task.html
     */
    public static final String NE_TIME = "TIME";
    public static final String NE_LOC = "LOCATION";
    public static final String NE_ORG = "ORGANIZATION";
    public static final String NE_PERSON = "PERSON";
    public static final String NE_MONEY = "MONEY";
    public static final String NE_PERCENT = "PERCENT";
    public static final String NE_DATE = "DATE";
    public static final String NE_MISC = "MISC";

    public static final String DEP_PASS = "pass"; // passive dependency label
    public static final String DEP_SUBJ = "subj"; // subjects dependency label
    public static final String DEP_ACOMP = "acomp"; // adjectival complements dependency label
    public static final String DEP_ADVCL = "advcl"; // adverbial clause modifiers
    public static final String DEP_ADVMOD = "advmod"; // adverbial modifiers
    public static final String DEP_AGENT = "agent"; // agents
    public static final String DEP_AMOD = "amod"; // adjectival modifiers
    public static final String DEP_APPOS = "appos"; // appositional modifiers
    public static final String DEP_ATTR = "attr"; // attributes
    public static final String DEP_AUX = "aux"; // auxiliary verbs
    public static final String DEP_AUXPASS = DEP_AUX + DEP_PASS; // passive auxiliary verbs.
    public static final String DEP_CC = "cc"; // coordinating conjunctions
    public static final String DEP_CCOMP = "ccomp"; // clausal complements
    public static final String DEP_COMPLM = "complm"; // complementizers
    public static final String DEP_CONJ = "conj"; // conjuncts
    public static final String DEP_COP = "cop"; // copula verb
    public static final String DEP_CSUBJ = "c" + DEP_SUBJ; // clausal subjects
    public static final String DEP_CSUBJPASS = DEP_CSUBJ + DEP_PASS; // clausal passive subjects
    public static final String DEP_DEP = "dep"; // UNKNOWN dependencies
    public static final String DEP_DET = "det"; // determiners
    public static final String DEP_DOBJ = "dobj"; // direct objects
    public static final String DEP_EXPL = "expl"; // expletives
    public static final String DEP_HMOD = "hmod"; // modifiers in hyphenation
    public static final String DEP_HYPH = "hyph"; // hyphenation
    public static final String DEP_IOBJ = "iobj"; // indirect objects
    public static final String DEP_INTJ = "intj"; // interjections
    public static final String DEP_MARK = "mark"; // markers
    public static final String DEP_META = "meta"; // meta modifiers
    public static final String DEP_NEG = "neg"; // negation modifiers
    public static final String DEP_NFMOD = "nfmod"; // non-finite modifiers
    public static final String DEP_INFMOD = "infmod"; // infinitival modifiers
    public static final String DEP_NMOD = "nmod"; // noun phrase modifiers
    public static final String DEP_NN = "nn"; // noun compound modifiers
    public static final String DEP_NPADVMOD = "npadvmod"; // adverbial modifiers
    public static final String DEP_NSUBJ = "n" + DEP_SUBJ; // nominal subjects
    public static final String DEP_NSUBJPASS = DEP_NSUBJ + DEP_PASS; // nominal passive subjects
    public static final String DEP_NUM = "num"; // numeric modifiers
    public static final String DEP_NUMBER = "number"; // elements of compound numbers
    public static final String DEP_OPRD = "oprd"; // object predicates
    public static final String DEP_PARATAXIS = "parataxis"; // parataxis
    public static final String DEP_PARTMOD = "partmod"; // participial modifiers
    public static final String DEP_PMOD = "pmod"; // modifiers of prepositions
    public static final String DEP_PCOMP = "pcomp"; // prepositional complements
    public static final String DEP_POBJ = "pobj"; // objects of prepositions
    public static final String DEP_POSS = "poss"; // possession modifiers
    public static final String DEP_POSSESSIVE = "possessive"; // possessive modifiers
    public static final String DEP_PRECONJ = "preconj"; // pre-conjuncts
    public static final String DEP_PREDET = "predet"; // pre-determiners
    public static final String DEP_PREP = "prep"; // prepositional modifiers
    public static final String DEP_PRT = "prt"; // particles
    public static final String DEP_PUNCT = "punct"; // punctuation
    public static final String DEP_QMOD = "qmod"; // modifiers of quantifiers
    public static final String DEP_QUANTMOD = "quantmod"; // quantifier phrase modifiers
    public static final String DEP_RCMOD = "rcmod"; // relative clause modifiers
    public static final String DEP_ROOT = "root"; // roots
    // Only from Stanford
    public static final String DEP_VMOD = "vmod";
    public static final String DEP_XCOMP = "xcomp"; // open clausal modifiers
    public static final String DEP_XSUBJ = "x" + DEP_SUBJ; // open clausal subjects

    /**
     * Number and Tense static strings
     */
    public static final String TENSE_PRESENT = "PRESENT";
    public static final String TENSE_PAST = "PAST";
    public static final String TENSE_FUTURE = "FUTURE";
    public static final String NUMBER_SINGULAR = "SINGULAR";
    public static final String NUMBER_PLURAL = "PLURAL";
    public static final String ASPECT_PROGRESSIVE = "PROGRESSIVE";
    public static final String ASPECT_PERFECT = "PERFECT";
    public static final String ASPECT_PROGRESSIVE_PERFECT = "PROGRESSIVEPERFECT";

}
