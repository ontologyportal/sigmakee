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
package com.articulate.sigma.semRewrite.substitutor;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.nlp.pipeline.Pipeline;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import org.junit.Test;

import static com.articulate.sigma.semRewrite.substitutor.CoreLabelSequence.EMPTY_SEQUENCE;
import static org.junit.Assert.assertEquals;

public class NounSubstitutorTest extends IntegrationTestBase {

    @Test
    public void testGetGrouped() {
        String input = "Amelia Mary Earhart (July 24, 1897 - July 2, 1937) was an American aviator.";
        Annotation document = Pipeline.toAnnotation(input);

        NounSubstitutor cg = new NounSubstitutor(document.get(CoreAnnotations.TokensAnnotation.class));

        assertEquals("Amelia_Mary_Earhart-1", cg.getGrouped("Amelia-1").toLabelString().get());
        assertEquals("Amelia_Mary_Earhart-1", cg.getGrouped("Mary-2").toLabelString().get());
        assertEquals("Amelia_Mary_Earhart-1", cg.getGrouped("Earhart-3").toLabelString().get());
        assertEquals(EMPTY_SEQUENCE, cg.getGrouped("aviator-18"));
    }
}
