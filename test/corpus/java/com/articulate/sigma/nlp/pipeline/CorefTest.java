package com.articulate.sigma.nlp.pipeline;

import com.articulate.sigma.test.JsonReader;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by aholub on 3/9/15.
 */
@RunWith(Parameterized.class)
public class CorefTest extends TestCase {

    private static Map<String, Collection<CorefChain>> parsedDocuments = Maps.newHashMap();

    @Parameterized.Parameter(value= 0)
    public String fileName;
    @Parameterized.Parameter(value= 1)
    public CorefPram corefA;
    @Parameterized.Parameter(value= 2)
    public CorefPram corefB;

    @AfterClass
    public static void cleanUp() {
        parsedDocuments.clear();
    }

    @Parameterized.Parameters(name="<{0}> {1} ↔ {2}")
    public static Collection<Object[]> prepare() {
        return JsonReader.transform("resources/corefTests.json", new Function<JSONObject, Object[]>() {
            @Override
            public Object[] apply(JSONObject jo) {
                String fileName = (String) jo.get("file");
                Long sentence = (Long) jo.get("sentence");
                Long sentenceA = sentence != null ? sentence : (Long) jo.get("sentenceA");
                Long sentenceB = sentence != null ? sentence : (Long) jo.get("sentenceB");

                String valueA = (String) jo.get("valueA");
                String valueB = (String) jo.get("valueB");
                return new Object[]{fileName,
                        valueA == null ? CorefPram.of(sentenceA, (Long) jo.get("indexA")) : CorefPram.of(sentenceA, valueA),
                        valueB == null ? CorefPram.of(sentenceB, (Long) jo.get("indexB")) : CorefPram.of(sentenceB, valueB)
                };
            }
        });
    }

    private Collection<CorefChain> getCorefChain(String fileName) throws IOException {
        Collection<CorefChain> corefs = parsedDocuments.get(fileName);
        if(!parsedDocuments.containsKey(fileName)) {
            URL url = Resources.getResource("resources/textfiles/" + fileName);
            Pipeline pipeline = new Pipeline();
            Annotation document = pipeline.annotate(Resources.toString(url, Charsets.UTF_8));
            corefs = document.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();
            parsedDocuments.put(fileName, corefs);
        }
        return corefs;
    }

    @Test
    public void test() throws IOException {
        Collection<CorefChain> corefs = getCorefChain(fileName);
        boolean hasBoth = FluentIterable.from(corefs)
                .filter(new Predicate<CorefChain>() {
                    @Override
                    public boolean apply(CorefChain corefChain) {
                        return FluentIterable.from(corefChain.getMentionsInTextualOrder())
                                .anyMatch(new Predicate<CorefChain.CorefMention>() {
                                    @Override
                                    public boolean apply(CorefChain.CorefMention corefMention) {
                                        return corefA.equals(corefMention);
                                    }
                                });
                    }
                })
                .anyMatch(new Predicate<CorefChain>() {
                    @Override
                    public boolean apply(CorefChain corefChain) {
                        return FluentIterable.from(corefChain.getMentionsInTextualOrder())
                                .anyMatch(new Predicate<CorefChain.CorefMention>() {
                                    @Override
                                    public boolean apply(CorefChain.CorefMention corefMention) {
                                        return corefB.equals(corefMention);
                                    }
                                });
                    }
                });
        if(!hasBoth) {
            fail("Not found: " + corefA + " ↔ " + corefB);
        }
    }
}
