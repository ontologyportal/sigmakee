package com.articulate.sigma.semRewrite;

import static com.articulate.sigma.nlp.pipeline.SentenceUtil.toDependenciesList;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;

import com.articulate.sigma.KBmanager;
import com.articulate.sigma.semRewrite.substitutor.NounSubstitutor;
import com.articulate.sigma.semRewrite.substitutor.SubstitutionUtil;
import com.articulate.sigma.test.JsonReader;

import org.json.simple.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.articulate.sigma.nlp.pipeline.Pipeline;
import com.articulate.sigma.semRewrite.datesandnumber.InterpretNumerics;
import com.google.common.collect.Lists;

import edu.stanford.nlp.pipeline.Annotation;

@RunWith(Parameterized.class)
public class DateAndNumberGenerationTest {

	public static InterpretNumerics interpNum;

	@Parameterized.Parameter(value= 0)
	public String fInput;
	@Parameterized.Parameter(value= 1)
	public String fExpected;   

	/** ***************************************************************
	 */
	@BeforeClass
	public static void initInterpreter() {
		KBmanager.getMgr().initializeOnce();
	}
	/** ***************************************************************
	 */
	@Parameters(name="{0}")
	public static Collection<String[]> prepare() {

		return JsonReader.transform("resources/Date_and_number_test.json", new Function<JSONObject, String[]>() {
			@Override
			public String[] apply(JSONObject jo) {
				String text = (String) jo.get("text");
				String kif = (String) jo.get("kif");
				return new String[]{text, kif};
			}
		});
	}
	 /** ***************************************************************
     */
	@Test
	public void test() {
		System.out.println("Running date number tests for sentence : "+fInput);
		String fout = InterpretNumerics.getSumoTerms(fInput, getNERGroupedClauses (fInput)).toString();
		System.out.println(fout);
		System.out.println(fExpected);
		assertEquals(fExpected, fout);
	}
	 /** ***************************************************************
     */
	private NounSubstitutor getNERGroupedClauses (String fInputString) {
		Pipeline pipeline = new Pipeline();
        Annotation document = pipeline.annotate(fInputString);
		List<String> results = Lists.newArrayList();
		List<String> dependenciesList = toDependenciesList(document);
		results.addAll(dependenciesList);

		NounSubstitutor cg = new NounSubstitutor(results);
		Iterator<String> clauseIterator = results.iterator();
        while(clauseIterator.hasNext()) {
            String clause = clauseIterator.next();
            Matcher m = SubstitutionUtil.CLAUSE_SPLITTER.matcher(clause);
            if(m.matches()) {
                String attr1 = m.group(2);
                String attr2 = m.group(3);
                String attr1Grouped = cg.getGrouped(attr1);
                String attr2Grouped = cg.getGrouped(attr2);
                if(!attr1.equals(attr1Grouped) || !attr2.equals(attr2Grouped)) {
                    clauseIterator.remove();
                }
            }
        }
        return cg;
	}

}