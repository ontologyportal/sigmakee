package com.ipsoft.sigma.editor.scanner;

import java.util.regex.Pattern;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class SigmaScanner extends RuleBasedScanner {

	public SigmaScanner() {
		IRule[] rules = new FunctionWordRule().getRule();
		setRules(rules);
	}
}
