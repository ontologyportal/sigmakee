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

import com.articulate.sigma.nlp.pipeline.Pipeline;
import edu.stanford.nlp.pipeline.Annotation;
import junit.framework.TestCase;
import org.junit.Test;

public class LocationSubstitutorTest extends TestCase {

    @Test
    public void testLocationSubstitution() {
        String input = "David J. Bronczek, vice president and general manager of Federal Express Canada Ltd., was named senior vice president, Europe, Africa and Mediterranean, at this air-express concern. Mr. Bronczek, who is 39 years old and started at the company as a courier in 1976, succeeds Kenneth Newell, 55, who was named to the new post of senior vice president, retail service operations. Jon W. Slangerup, who is 43 and has been director of customer service in Canada, succeeds Mr. Bronczek as vice president and general manager there.";
        Annotation document = Pipeline.toAnnotation(input);
        LocationSubstitutor ls = new LocationSubstitutor(document);

        assertEquals("in_Canada", ls.getGrouped("there-27").toText());
    }
}