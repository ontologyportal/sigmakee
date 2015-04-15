package com.articulate.sigma;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.articulate.sigma.semRewrite.Interpreter;
import com.articulate.sigma.semRewrite.substitutor.SubstitutorsUnion;
import com.google.common.collect.ImmutableList;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

/**
 * Created by areed on 3/30/15.
 */
public class LemmatizerTest {

    Pipeline p = new Pipeline();

    @Test
    public void testLemmatization() {
        String input = "I had a car.";

        Annotation document = p.annotate(input);
        List<CoreLabel> labels = document.get(CoreAnnotations.TokensAnnotation.class);

        List<String> results = ImmutableList.of("test(had-2,anything-0)", "testing(PAST,had-2)", "testing2(had-2,ANYTHING)");

        List<String> actual = Interpreter.lemmatizeResults(results, labels, SubstitutorsUnion.of());

        String[] expected = {
                "test(have-2,anything-0)",
                "testing(PAST,have-2)",
                "testing2(have-2,ANYTHING)"
        };

        assertThat(actual, hasItems(expected));

    }

}
