/*
 * Copyright 2014-2015 IPsoft
 *
 * Author: Andrei Holub andrei.holub@ipsoft.com
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
package com.articulate.sigma.semRewrite.substitutor;

import com.articulate.sigma.IntegrationTestBase;
import com.articulate.sigma.nlp.pipeline.Pipeline;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import org.junit.Test;

import static com.articulate.sigma.semRewrite.substitutor.CoreLabelSequence.EMPTY_SEQUENCE;
import static org.junit.Assert.assertEquals;

public class IdiomSubstitutorTest extends IntegrationTestBase {

    @Test
    public void testIdiomSubstitution() {
        String input = "I know Sir John will go, though he was sure it would rain cats and dogs outside.";
        Annotation document = Pipeline.toAnnotation(input);

        IdiomSubstitutor substitutor = new IdiomSubstitutor(document.get(TokensAnnotation.class));

        assertEquals(EMPTY_SEQUENCE, substitutor.getGrouped("sure-11"));
        assertEquals("rain_cats_and_dogs-14", substitutor.getGrouped("rain-14").toLabelString().get());
        assertEquals("rain_cats_and_dogs-14", substitutor.getGrouped("dogs-17").toLabelString().get());
        assertEquals(EMPTY_SEQUENCE, substitutor.getGrouped("outside-18"));
    }

    @Test
    public void testFoundingFathers() {
        String input = "Who is the founding father?";
        Annotation document = Pipeline.toAnnotation(input);

        IdiomSubstitutor substitutor = new IdiomSubstitutor(document.get(TokensAnnotation.class));

        assertEquals("founding_father-4", substitutor.getGrouped("founding-4").toLabelString().get());
    }

}
