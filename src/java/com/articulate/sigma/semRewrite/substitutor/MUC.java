package com.articulate.sigma.semRewrite.substitutor;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.articulate.sigma.nlp.pipeline.SentenceUtil;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Copyright 2014-2015 IPsoft
 *
 * Author: Adam Pease adam.pease@ipsoft.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA  02111-1307 USA
 *
 * Processing to handle the MUC-6 dataset for coreference
 */
public class MUC {

    public static Annotation document = null;
    public static HashSet<Coref> stanfordCorefs = new HashSet<>();

    /** ***************************************************************
     */
    public class Coref {

        int ID;
        String token;
        int ref = -1;
        HashMap<String,String> info = new HashMap<>();
        int sentNum;
        int firstToken;
        int lastToken;
        int chainID;

        public String toString() {
            return Integer.toString(ID) + ":" +
                    token + ":" +
                    info + ":" +
                    Integer.toString(sentNum) + ":" +
                    Integer.toString(firstToken) + ":" +
                    Integer.toString(lastToken);
        }
    }

    /** ***************************************************************
     */
    public static List<Coref> buildCorefList(String input) {

        ArrayList<Coref> corefs = new ArrayList<Coref>();
        return corefs;
    }

    /** ***************************************************************
     */
    public static String listToString (List<String> input) {

        StringBuffer sb = new StringBuffer();
        for (String s : input)
            sb.append(s + " ");
        return sb.toString();
    }

    /** ***************************************************************
     */
    public static List<String> toSentences (String input) {

        List<String> results = new ArrayList<String>();
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        document = new Annotation(input);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            System.out.println(sentence);
            results.add(sentence.get(CoreAnnotations.TextAnnotation.class));
        }
        return results;
    }

    /** ***************************************************************
     * Convert Stanford corefs into MUC-style coreference chains.
     */
    public HashMap<Integer,Coref> stanfordToCoref (Annotation document) {

        HashMap<Integer,Coref> result = new HashMap<>();
        Map<Integer, CorefChain> graph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        for (CorefChain cc : graph.values()) {
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();
            if (mentions.size() > 1) {
                for (CorefChain.CorefMention ment : mentions) {
                    Coref c = new Coref();
                    c.ID = ment.mentionID;
                    c.token = ment.mentionSpan;
                    HashMap<String,String> info = new HashMap<>();
                    c.sentNum = ment.sentNum;
                    c.firstToken = ment.headIndex;
                    int lastToken;
                    result.put(c.ID,c);
                }
            }
        }
        return result;
    }

    /** ***************************************************************
     * @return a list of sentences with tokens
     */
    public ArrayList<ArrayList<String>> toCoref (String input) {

        //System.out.println("INFO in MUC.toCoref(): " + input);
        List<Coref> corefs = buildCorefList(input);
        ArrayList<ArrayList<String>> results = new ArrayList<ArrayList<String>>();
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        props.setProperty("tokenize.options", "ptb3Escaping=false");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        document = new Annotation(input);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        //SentenceUtil.printCorefChain(document);

        Map<Integer, CorefChain> graph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        for (CorefChain cc : graph.values()) {
            List<CorefChain.CorefMention> mentions = cc.getMentionsInTextualOrder();
            if (mentions.size() > 1) {
                for (CorefChain.CorefMention ment : mentions) {
                    Coref c = new Coref();
                    c.ID = ment.mentionID;
                    c.token = ment.mentionSpan;
                    HashMap<String,String> info = new HashMap<>();
                    c.sentNum = ment.sentNum;
                    c.firstToken = ment.headIndex;
                    int lastToken;
                    System.out.println(ment.sentNum + " : " + ment.headIndex + " : " + ment.mentionSpan);
                }
                System.out.println();
            }
        }

        for (CoreMap sentence : sentences) {
            System.out.println(sentence);
            ArrayList<String> tokenList = new ArrayList<>();
            //results.add(sentence.get(CoreAnnotations.TextAnnotation.class));
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            for (CoreLabel t : tokens) {
                String t2 = t.toString();
                if (t2.startsWith("-LRB-"))
                    t2 = t2.replace("-LRB-","(");
                if (t2.startsWith("-RRB-"))
                    t2 = t2.replace("-RRB-",")");
                if (t2.startsWith("``"))
                    t2 = t2.replace("``","\"");
                if (t2.startsWith("''"))
                    t2 = t2.replace("''","\"");
                // -LCB-,  -RCB-, ???
                System.out.print(t2 + " ");
                tokenList.add(t2);
            }
            results.add(tokenList);
            System.out.println();
        }
        return results;
    }

    /** ***************************************************************
     */
    public static List<String> getDocuments (String filename) {

        List<String> lines = new ArrayList<String>();
        System.out.println("INFO in MUC.cleanSGML(): Reading files");
        LineNumberReader lr = null;
        try {
            String line;
            StringBuffer doc = new StringBuffer();
            File nounFile = new File(filename);
            if (nounFile == null) {
                System.out.println("Error in MUC.cleanSGML(): The file does not exist ");
                return lines;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(nounFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                line = line.trim();
                line = line.replace("<HL>", "");
                line = line.replace("</HL>", "");
                line = line.replace("<s>", "");
                line = line.replace("</s>", "");
                line = line.replace("----", "");
                line = line.replaceAll("^\\@", "");
                if (line.contains("</DOC>")) {
                    lines.add(doc.toString());
                    doc = new StringBuffer();
                }
                else if (line.contains("<DOC>") ||
                        line.contains("<DOCID>") ||
                        line.contains("<DOCNO>") ||
                        line.contains("<p>") ||
                        line.contains("</p>") ||
                        line.contains("<DD>") ||
                        line.contains("<AN>") ||
                        line.contains("<CODER>") ||
                        line.contains("<SO>") ||
                        line.contains("<DATELINE>") ||
                        line.contains("<TXT>") ||
                        line.contains("<CO>") ||
                        line.contains("</TXT>") ||
                        line.contains("<CO>") ||
                        line.contains("<IN>") ||
                        line.contains("</IN>")) { }
                 else
                    doc.append(line + " ");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (lr != null) {
                    lr.close();
                }
            }
            catch (Exception ex) {
            }
        }
        return lines;
    }

    /** ***************************************************************
     */
    public static List<String> cleanSGML (String filename) {

        List<String> lines = new ArrayList<String>();
        System.out.println("INFO in MUC.cleanSGML(): Reading files");
        LineNumberReader lr = null;
        try {
            String line;
            File nounFile = new File(filename);
            if (nounFile == null) {
                System.out.println("Error in MUC.cleanSGML(): The file does not exist ");
                return lines;
            }
            long t1 = System.currentTimeMillis();
            FileReader r = new FileReader(nounFile);
            lr = new LineNumberReader(r);
            while ((line = lr.readLine()) != null) {
                if (lr.getLineNumber() % 1000 == 0)
                    System.out.print('.');
                line = line.trim();
                line = line.replaceAll("<[^>]+>", "");
                line = line.replaceAll("<[^>]+$", "");
                line = line.replaceAll("^[^>]+>", "");
                lines.add(line);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (lr != null) {
                    lr.close();
                }
            }
            catch (Exception ex) {
            }
        }
        return lines;
    }

    /** ***************************************************************
     */
    private static void removeToken(StringBuffer sb, String token) {

        if (sb == null || sb.length() > 1)
            return;
        while (Character.isWhitespace(sb.toString().charAt(0)))
            sb = sb.deleteCharAt(0);
        if (sb.toString().startsWith(token)) {
            sb = sb.delete(0,token.length());
        }
        else {
            System.out.println("Error - no match for " + token + " in " + sb);
        }
    }

    /** ***************************************************************
     */
    private static void expandCurrentToken(String token,
                                           Stack<Integer> currentCoref,
                                           HashMap<Integer,String> corefTokens) {

        Integer id = currentCoref.peek();
        String tok = corefTokens.get(id);
        if (tok.isEmpty())
            corefTokens.put(id,token);
        else
            corefTokens.put(id,tok + " " + token);
    }

    /** ***************************************************************
     */
    private static void leadingTrim(StringBuffer sb) {

        if (sb == null || sb.length() > 1)
            return;
        while (Character.isWhitespace(sb.toString().charAt(0)))
            sb = sb.deleteCharAt(0);
    }

    /** ***************************************************************
     */
    private static void processParams(Coref c, HashMap<Integer,String> corefParams) {

        String paramlist = corefParams.get(c.ID);
        while (paramlist.indexOf('"') > -1) {
            int space = paramlist.indexOf(' ');
            int equals = paramlist.indexOf('=');
            int quote1 = paramlist.indexOf('"');
            int quote2 = paramlist.indexOf('"', quote1 + 1);
            String key = paramlist.substring(space + 1, equals);

            String value = paramlist.substring(quote1 + 1,quote2);
            if (key.equals("REF"))
                c.ref = Integer.parseInt(value);
            c.info.put(key,value);
            paramlist = paramlist.substring(quote2+1);
        }
    }

    /** ***************************************************************
     */
    private static String getTag(StringBuffer sb, Matcher m) {

        if (sb.indexOf("<") > -1)
            sb = sb.delete(0,sb.indexOf("<"));
        String tag = sb.toString().substring(0, sb.indexOf(">") + 1);
        sb = sb.delete(0, sb.indexOf(">") + 1);
        return tag;
    }

    /** ***************************************************************
     * Build chains of coreferences based on their pairwise references
     */
    private static HashMap<Integer,HashSet<Coref>> buildChains (HashMap<Integer,Coref> corefs) {

        HashMap<Integer,Integer> chainMap = new HashMap<>(); // coref id to chain id
        HashMap<Integer,HashSet<Coref>> chains = new HashMap<>(); // chain id to members
        int chainNum = 0;
        boolean first = true;
        Coref firstC = null;
        for (Integer i : corefs.keySet()) {
            Coref c = corefs.get(i);
            if (first) {
                firstC = c;
                first = false;
            }
            if (!chainMap.keySet().contains(c.ID)) {
                if (c.ref == -1) {
                    chainMap.put(c.ID, chainNum);
                    HashSet<Coref> chain = new HashSet<>();
                    chain.add(c);
                    c.chainID = chainNum;
                    chains.put(chainNum,chain);
                    chainNum++;
                }
                else if (!chainMap.keySet().contains(c.ref)) {
                    chainMap.put(c.ID, chainNum);
                    chainMap.put(c.ref, chainNum);
                    HashSet<Coref> chain = new HashSet<>();
                    c.chainID = chainNum;
                    chain.add(c);
                    Coref cref = corefs.get(c.ref);
                    if (cref != null)
                        chain.add(cref);
                    else
                        System.out.println("No coref for id: " + c.ref);
                    chains.put(chainNum,chain);
                    chainNum++;
                }
                else {
                    int ref = chainMap.get(c.ref);
                    chains.get(ref).add(c);
                    chainMap.put(c.ID,ref);
                }
            }
            else {
                if (c.ref == -1) {} // no reference so do nothing
                else if (!chainMap.keySet().contains(c.ref)) {
                    chainMap.put(c.ID, chainNum);
                    chainMap.put(c.ref, chainNum);
                    HashSet<Coref> chain = new HashSet<>();
                    c.chainID = chainNum;
                    chain.add(c);
                    Coref cref = corefs.get(c.ref);
                    if (cref != null)
                        chain.add(cref);
                    else
                        System.out.println("No coref for id: " + c.ref);
                    chain.add(cref);
                    chains.put(chainNum,chain);
                    chainNum++;
                }
                else {
                    int ref = chainMap.get(c.ref);
                    chains.get(ref).add(c);
                }
            }
        }
        return chains;
    }

    /** ***************************************************************
     */
    private static void printChains (HashMap<Integer,HashSet<Coref>> corefs) {

        for (Integer i : corefs.keySet()) {
            System.out.println(corefs.get(i));
            System.out.println();
        }
    }

    /** ***************************************************************
     * Compare Stanford and MUC coreference chains.  Create a map for
     * each token in MUC whether it is in Stanford and in which chain
     * ID and which Stanford token it corresponds to. Use that map to
     * score which tokens are not found (errors of omission, or false
     * negatives).  Mark the tokens that are found in both MUC and
     * Stanford in a separate map. Use that map to create a third map
     * of tokens that are in Stanford but not in MUC.
     */
    public void compareChains(HashMap<Integer,HashSet<Coref>> chains,
                              Annotation document) {


    }

    /** ***************************************************************
     * Pick tokens off the input sentence string, capturing corefXML
     * when present and aligning the corefXML with token numbers
     */
    public void makeCorefList(List<String> sentsDirty,
                              ArrayList<ArrayList<String>> tokenized) {

        HashMap<Integer,Coref> corefs = new HashMap<>();
        HashMap<Integer,String> corefTokens = new HashMap<>();
        HashMap<Integer,String> corefParams = new HashMap<>();
        HashMap<Integer,Integer> references = new HashMap<>();
        Stack<Integer> currentCoref = new Stack<>();
        Pattern p1 = Pattern.compile("^\\s*(<COREF[^>]+>)");
        Pattern p2 = Pattern.compile("^\\s*(</COREF>)");
        int sentNum = 0;
        int level = 0;
        int tokenNum = 0;
        int firstToken = 0;
        boolean openTag = false;
        String tag = "";
        for (String s : sentsDirty) {
            StringBuffer sb = new StringBuffer(s);
            ArrayList<String> tokens = tokenized.get(sentNum);
            sentNum++;
            for (String t : tokens) {
                boolean open = false;
                boolean close = false;
                do {
                    Matcher m1 = p1.matcher(sb.toString());
                    Matcher m2 = p2.matcher(sb.toString());
                    open = m1.find();
                    close = m2.find();
                    if (open) {
                        tag = getTag(sb, m1);
                        //System.out.println(tag);
                        level++;
                        int quoteIndex = tag.indexOf("\"");
                        String id = tag.substring(quoteIndex + 1, tag.indexOf("\"", quoteIndex + 1));
                        currentCoref.push(Integer.parseInt(id));
                        corefTokens.put(Integer.parseInt(id), "");
                        corefParams.put(Integer.parseInt(id),tag);
                        int refIndex = tag.indexOf("REF=");
                        if (refIndex > -1) {
                            int refQuoteIndex = tag.indexOf("\"", refIndex + 1);
                            String ref = tag.substring(refQuoteIndex + 1, tag.indexOf("\"", refQuoteIndex + 1));
                            references.put(Integer.parseInt(id), Integer.parseInt(ref));
                        }
                        openTag = true;
                    }
                    if (close) {
                        if (sb.indexOf("<") > -1)
                            sb.delete(0, sb.indexOf("<"));
                        sb.delete(0, sb.indexOf(">") + 1);
                        if (currentCoref.size() < 1) {
                            System.out.println("Error in MUC.makeCorefList(): no open tag for close tag");
                            System.out.println(sb);
                            return;
                        }
                        Integer cid = currentCoref.pop();
                        Coref c = new Coref();
                        c.ID = cid;
                        c.token = corefTokens.get(cid);
                        c.firstToken = firstToken;
                        c.lastToken = tokenNum;
                        processParams(c,corefParams);
                        c.sentNum = sentNum;
                        corefs.put(c.ID, c);
                        //int tokenNum
                        level--;
                    }
                } while (open || close);
                String tokenNumStr = t.substring(t.lastIndexOf("-") + 1, t.length());
                tokenNum = Integer.parseInt(tokenNumStr);
                //System.out.println("token num: " + tokenNum + " " + open);
                if (openTag)
                    firstToken = tokenNum;
                String token = t.substring(0,t.lastIndexOf("-"));
                leadingTrim(sb);
                removeToken(sb, token);
                if (level > 0) {
                    expandCurrentToken(token, currentCoref, corefTokens);
                }
                //System.out.println("current coref: " + currentCoref);
                //System.out.println("sb: " + sb);
                openTag = false;
            }
        }
        System.out.println(references);
        System.out.println(corefs);
        HashMap<Integer,HashSet<Coref>> chains = buildChains(corefs);
        System.out.println();
        printChains(chains);
        compareChains(chains,document);
    }

    /** ***************************************************************
     */
    public static void main(String[] args) {

        //String line = "@  To Be Strong Candidate to Head <COREF ID=\"3\">SEC</COREF>";
        //line = line.replaceAll("^\\@","");
        //System.out.println(line);
        //List<String> lines = cleanSGML("/home/apease/IPsoft/corpora/muc6/data/keys/formal-tst.CO.key.cleanup.09jul96");
        List<String> lines = getDocuments("/home/apease/IPsoft/corpora/muc6/data/keys/formal-tst.CO.key.cleanup.09jul96");
        //List<String> lines = getDocuments("/home/apease/IPsoft/corpora/muc6/data/keys/891101-0056.co.v0.sgm" + "");
        MUC muc = new MUC();
        for (String s : lines) {
            String cleanedInput = s.replaceAll("<COREF[^>]+>", "");
            cleanedInput = cleanedInput.replace("</COREF>","");
            List<String> sentsClean = toSentences(cleanedInput);
            List<String> sentsDirty = toSentences(s);
            String allClean = listToString(sentsClean);
            ArrayList<ArrayList<String>> tokenized = muc.toCoref(listToString(sentsClean));
            muc.makeCorefList(sentsDirty, tokenized);
        }
    }
}
