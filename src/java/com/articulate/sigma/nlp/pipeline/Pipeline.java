package com.articulate.sigma.nlp.pipeline;

import edu.stanford.nlp.pipeline.*;

import java.util.*;

public class Pipeline {

    final StanfordCoreNLP pipeline;

    public Pipeline() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref, entitymentions");

        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        pipeline = new StanfordCoreNLP(props);
    }

    public Annotation annotate(String text) {
        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        return document;
    }

}
