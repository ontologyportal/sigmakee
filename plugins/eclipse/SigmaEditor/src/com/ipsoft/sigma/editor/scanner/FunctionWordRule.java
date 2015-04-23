package com.ipsoft.sigma.editor.scanner;

import java.util.ArrayList;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordPatternRule;
import org.eclipse.jface.text.rules.WordRule;

import com.ipsoft.sigma.editor.SigmaUtil;

public class FunctionWordRule {

	ArrayList<IRule> rules = new ArrayList<IRule>();

	public FunctionWordRule () {
		IRule r;
		rules.add(new EndOfLineRule(";", SigmaTokens.comment));
		// Rule for variable-----work
		rules.add(new WordPatternRule(new VariableWordDetector(), "?", null,
				SigmaTokens.variable));
		// rule for CNF function
		WordRule wr = new WordRule(new IWordDetector() {

			@Override
			public boolean isWordStart (char c) {
				if (c == '=' || c == '?' || c == '/')
					return true;
				return false;
			}

			@Override
			public boolean isWordPart (char c) {
				if (c == '=' || c == '>' || c == '-')
					return true;
				return false;
			}
		}, SigmaTokens.def);
		wr.addWord("==>", SigmaTokens.functionname);
		wr.addWord("?=>", SigmaTokens.functionname);
		wr.addWord("/-", SigmaTokens.functionname);
		rules.add(wr);
		// rules.add(new WordPatternRule(new
		// FunctionWordDetector(),"\\s","(",SigmaTokens.functionname));
		// rules.add(new WordPatternRule(new FunctionWordDetector(),
		// ",","(",SigmaTokens.functionname));
		//
		// rules.add(new WordPatternRule(new FunctionWordDetector(),
		// "\n","(",SigmaTokens.functionname));
		rules.add(new WhitespaceRule(new WhitespaceDetector()));
	}

	public IRule[] getRule () {
		return rules.toArray(new IRule[rules.size()]);
	}
}
