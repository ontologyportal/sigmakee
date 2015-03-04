package com.articulate.sigma.nlp.pipeline;

import edu.stanford.nlp.pipeline.Annotation;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by aholub on 2/27/15.
 */
public class PipelineTest extends TestCase {

    @Test
    public void testOutput() {
        Pipeline pipeline = new Pipeline();

        String text = "John kicked the cart to East New Britain. He went there too.";
        Annotation document = pipeline.annotate(text);

        SentenceUtil.printSentences(document);
        SentenceUtil.printCorefChain(document);
    }
}