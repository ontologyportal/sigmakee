/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

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
package com.articulate.sigma.nlp.pipeline;

import com.articulate.sigma.test.CVSExporter;
import com.articulate.sigma.test.JsonReader;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.FluentIterable;
import com.google.common.io.Resources;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CorefTest extends TestCase {

    private static LoadingCache<String, Annotation> DOCUMENTS_CACHED = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Annotation>() {
                @Override
                public Annotation load(String fileName) throws Exception {

                    URL url = Resources.getResource("QATests1/" + fileName);
                    Annotation document = Pipeline.toAnnotation(Resources.toString(url, Charsets.UTF_8));
                    return document;
                }
            });

    @Parameter(value = 0)
    public String fileName;
    @Parameter(value = 1)
    public CorefParam corefA;
    @Parameter(value = 2)
    public CorefParam corefB;

    // swallows output by default
    private static final CVSExporter exportResults = new CVSExporter(false, "Filename,Result,Coref A,Coref B,Sentence A, Sentence B");

    @AfterClass
    public static void cleanUp() throws IOException {

        DOCUMENTS_CACHED.invalidateAll();
        exportResults.flushIfEnabled();
    }

    @Parameters(name = "<{0}> {1} ↔ {2}")
    public static Collection<Object[]> prepare() {

        return JsonReader.transform("QATests1/corefTests.json", (JSONObject jo) -> {
            String fileName = (String) jo.get("file");
            Long sentence = (Long) jo.get("sentence");
            Long sentenceA = sentence != null ? sentence : (Long) jo.get("sentenceA");
            Long sentenceB = sentence != null ? sentence : (Long) jo.get("sentenceB");

            String valueA = (String) jo.get("valueA");
            String valueB = (String) jo.get("valueB");
            return new Object[]{fileName,
                    valueA == null ? CorefParam.of(sentenceA, (Long) jo.get("indexA")) : CorefParam.of(sentenceA, valueA),
                    valueB == null ? CorefParam.of(sentenceB, (Long) jo.get("indexB")) : CorefParam.of(sentenceB, valueB)
            };
        });
    }

    @Test
    public void test() throws Exception {

        Annotation document = DOCUMENTS_CACHED.get(fileName);
        Collection<CorefChain> corefs = document.get(CorefChainAnnotation.class).values();
        boolean hasBoth = FluentIterable.from(corefs)
                .filter(corefChain -> {

                    return FluentIterable.from(corefChain.getMentionsInTextualOrder())
                            .anyMatch(corefMention -> {

                                boolean same = corefA.equals(corefMention);
                                if(same) {
                                    System.out.println("1st pass: " + corefMention.toString());
                                }
                                return same;
                            });
                })
                .anyMatch((CorefChain corefChain) ->

                        FluentIterable.from(corefChain.getMentionsInTextualOrder())
                                .anyMatch(corefMention -> {

                                    boolean same = corefB.equals(corefMention);
                                    if (same) {
                                        System.out.println("2nd pass: " + corefMention.toString());
                                    }
                                    return same;
                                }));

        // Export results if export enabled
        List<CoreMap> coreMaps = document.get(CoreAnnotations.SentencesAnnotation.class);
        exportResults.addRow(new String[]{fileName, hasBoth ? "OK" : "FAILED"
                , corefA.getRef(), corefB.getRef()
                , coreMaps.get(corefA.getSentenceIndex()).toString()
                , corefA.getSentenceIndex() == corefB.getSentenceIndex() ? "" : coreMaps.get(corefB.getSentenceIndex()).toString()});

        if (!hasBoth) {
            for (CorefChain chain : corefs) {
                System.out.println(chain);
            }
            fail("Not found: " + corefA + " ↔ " + corefB);
        }
    }
}
