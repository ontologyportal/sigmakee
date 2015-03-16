package com.articulate.sigma.nlp;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.articulate.sigma.test.JsonReader;
import edu.stanford.nlp.pipeline.Annotation;
import junit.framework.TestCase;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.function.Function;

/**
 * Created by aholub on 3/10/15.
 */
@RunWith(Parameterized.class)
public class CorefSubstitutorLarge extends TestCase {

    static Pipeline pipeline = new Pipeline();

    @Parameterized.Parameter(value= 0)
    public String input;
    @Parameterized.Parameter(value= 1)
    public String expected;


    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> prepare() {
        return JsonReader.transform("resources/corefsSubstitution.json", new Function<JSONObject, Object[]>() {
            @Override
            public Object[] apply(JSONObject jo) {
                String input = (String) jo.get("in");
                String expected = (String) jo.get("out");
                return new Object[]{input, expected};
            }
        });
    }


    @Test
    public void test() {
        Annotation document = pipeline.annotate(input);
        CorefSubstitutor substitutor = new CorefSubstitutor(document);
        assertEquals(expected, substitutor.substitute());
    }
}
