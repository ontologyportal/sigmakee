package com.ipsoft.sigma.editor.scanner;

import org.eclipse.jface.text.rules.IWordDetector;

public class FunctionWordDetector implements IWordDetector {

	@Override
	public boolean isWordStart (char c) {
		if(Character.isLowerCase(c)) return true;
		return false;
	}

	@Override
	public boolean isWordPart(char c) {
		if(Character.isLetter(c)|| c=='_') return true;
		return false;
	}

}
