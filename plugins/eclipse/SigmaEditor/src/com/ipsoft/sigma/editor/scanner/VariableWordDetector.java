package com.ipsoft.sigma.editor.scanner;

import org.eclipse.jface.text.rules.IWordDetector;

public class VariableWordDetector implements IWordDetector{

	
	@Override
	public boolean isWordStart(char c) {
		if((c<='Z'&& c>='A')|| (c>='a'&& c<='z')) return true;
		return false;
	}
	
	@Override
	public boolean isWordPart(char c) {
		if((c<='Z'&& c>='A')|| c=='-' || (c>='a'&& c<='z') ||(c<='9'&&c>='0')) return true;
		
		return false;
	}
	 
}
