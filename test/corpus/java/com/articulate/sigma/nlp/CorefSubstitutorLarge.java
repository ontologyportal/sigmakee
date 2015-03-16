package com.articulate.sigma.nlp;

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
