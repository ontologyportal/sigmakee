package com.articulate.sigma.nlp;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;

import java.util.Map;

/**
 * Created by aholub on 3/9/15.
 */
public class CorefSubstitutor {

    public static Annotation annotate(String input) {
        Pipeline pipeline = new Pipeline();
        Annotation document = pipeline.annotate(input);
        return document;
    }

    private static boolean needSpaceBefore(CoreLabel label) {
        String text = label.get(CoreAnnotations.TextAnnotation.class);
        boolean skipSpace = text.length() == 1 && (
                ",".equals(text)
                        || ".".equals(text)
                        || ")".equals(text)
                        || "!".equals(text)
                        || "?".equals(text)
        );
        return !skipSpace;
    }

    private static String replaceCoref(final CoreLabel label, Map<Integer, CorefChain> corefs) {
        String text = label.get(CoreAnnotations.TextAnnotation.class);
        Integer corefClusterId = label.get(CorefCoreAnnotations.CorefClusterIdAnnotation.class);
        if(corefClusterId != null) {
            if(corefs.get(corefClusterId).getMentionsInTextualOrder().size() > 1) {
                Integer index = label.get(CoreAnnotations.IndexAnnotation.class);
                Integer sentence = 1 + label.get(CoreAnnotations.SentenceIndexAnnotation.class);
                CorefChain.CorefMention mention = corefs.get(corefClusterId).getMentionsInTextualOrder().get(0);
                if (sentence != mention.sentNum || index < mention.startIndex || index >= mention.endIndex) {
                    text = mention.mentionSpan;
                }
            }
        }

        return text;
    }

    public static String substitute(String input) {
        Annotation document = annotate(input);
        return substitute(document);
    }

    public static String substitute(Annotation document) {
        Map<Integer, CorefChain> corefs = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        StringBuilder builder = new StringBuilder();
        for(CoreLabel label : document.get(CoreAnnotations.TokensAnnotation.class)) {
            if(builder.length() != 0 && needSpaceBefore(label)) {
                builder.append(" ");
            }
            builder.append(replaceCoref(label, corefs));
        }

        return builder.toString();
    }
}
