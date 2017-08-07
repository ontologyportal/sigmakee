package com.articulate.sigma.wordNet;

import com.articulate.sigma.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/** **************************************************************
 * Copyright 2016 Articulate Software
 *
 * Author: Adam Pease apease@articulatesoftware.com
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
 */
public class BrownCorpus {

    public class Token {
        public String tok;
        public String type;

        public String getTok() { return tok; }
        public String getType() { return type; }
        public String toString() {
            return tok + "/" + type + " ";
        }
    }

    public class Sentence {
        public ArrayList<Token> tokens = new ArrayList<>();

        public String toString() {
            return tokens.toString() + "\n";
        }

        public String toText() {

            return tokens.stream().map(Token::getTok).reduce("", (a,b) -> a + " " + b) + "\n";
        }
    }

    public class Para {
        public ArrayList<Sentence> sents = new ArrayList<>();

        public String toString() {
            return sents.toString() + "\n";
        }
    }

    public class Doc {
        public ArrayList<Para> paras = new ArrayList<>();

        public String toString() {
            return paras.toString() + "\n";
        }
    }

    public static ArrayList<Doc> docs = new ArrayList<>();

    /***************************************************************
     */
    public Sentence parse(String st) {

        StringBuffer sb = new StringBuffer();
        Sentence sent = new Sentence();
        int i = 0;
        while (i < st.length()-1) {
            char ch = st.charAt(i);
            if (ch != '\t' && ch != ' ') {
                String tk = "";
                while (ch != '/' && i < st.length()-1) {
                    tk = tk + ch;
                    i++;
                    ch = st.charAt(i);
                }
                if (ch != '/')
                    System.out.println("Error in BrownCorpus.parse(): bad token at end of : " + st);
                else {
                    i++;
                    ch = st.charAt(i);
                }
                String type = "";
                if (i == st.length()-1)
                    type = type + ch;
                while (ch != ' ' && ch !='\n' && i < st.length()-1) {
                    type = type + ch;
                    i++;
                    ch = st.charAt(i);
                }
                Token tok = new Token();
                tok.tok = tk;
                tok.type = type;
                sent.tokens.add(tok);
            }
            else
                i++;
        }
        return sent;
    }

    /***************************************************************
     */
    public Doc parse(LineNumberReader lnr) {

        Doc doc = new Doc();
        Para para = new Para();
        Sentence sent = new Sentence();
        int linecount = 0;
        int r;
        try {
            String line = "";
            while (lnr.ready()) {
                String content = lnr.readLine();
                //System.out.println("in BrownCorpus.parse(): content: " + content);
                if (StringUtil.emptyString(content)) {
                    if (para.sents.size() > 0)
                        doc.paras.add(para);
                    para = new Para();
                }
                else {
                    sent = parse(content);
                    para.sents.add(sent);
                }

            }
            doc.paras.add(para);
            //System.out.println("Info in BrownCorpus.parse():" + doc);
            return doc;
        }
        catch (Exception ex) {
            System.out.println("Error in BrownCorpus.readFile():" + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    /***************************************************************
     */
    public Doc readFile(Path filename) {

        //System.out.println("BrownCorpus.readFile():" + filename);
        Doc doc = null;
        File f = null;
        FileReader fr = null;
        Exception exThr = null;
        try {
            FileReader in = new FileReader(filename.toString());
            LineNumberReader buffer = new LineNumberReader(in);
            doc = parse(buffer);
        }
        catch (Exception ex) {
            exThr = ex;
            String er = ex.getMessage() + ((ex instanceof ParseException)
                    ? " at line " + ((ParseException)ex).getErrorOffset()
                    : "");
            //System.out.println("Error in BrownCorpus.readFile():" + er);
            ex.printStackTrace();
            return null;
        }
        return doc;
    }

    /***************************************************************
     * Read all Brown Corpus files in a directory
     *
     * @param filename is the directory
     */
    public void read(String filename) {

        try {
            Files.walk(Paths.get(filename)).forEach(filePath -> {
                //
                if (Files.isRegularFile(filePath) &&
                        filePath.toString().charAt(filePath.toString().lastIndexOf(File.separator)+1) == 'c' &&
                        filePath.toString().indexOf('.') == -1) {
                    Doc doc = readFile(filePath);
                    docs.add(doc);
                }
            });
        }
        catch (IOException ioe) {
            System.out.println("Error in BrownCorpus.read(): " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }

    /***************************************************************
     */
    public void process() {

        for (Doc doc : docs) {
            for (Para para : doc.paras) {
                for (Sentence sent : para.sents) {
                    if (sent.tokens.size() > 0) {
                        Token tok = sent.tokens.get(0);
                        if (tok.type.startsWith("vb") &&
                                !tok.type.startsWith("vbg") &&
                                !tok.type.startsWith("vbz") &&
                                !tok.type.startsWith("vbn"))
                            System.out.println(sent.toText());
                    }
                }
            }
        }
    }

    /** *************************************************************
     */
    public static void main(String[] args) {

        BrownCorpus bc = new BrownCorpus();
        bc.read(System.getProperty("user.home") + "/ontology/brown");
        bc.process();
    }
}
