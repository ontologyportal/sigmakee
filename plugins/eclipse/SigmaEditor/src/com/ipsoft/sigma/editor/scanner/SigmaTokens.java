package com.ipsoft.sigma.editor.scanner;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class SigmaTokens {
	public static ColorManager manager=new ColorManager();
	public static IToken variable =
			new Token(
				new TextAttribute(manager.getColor(ColorConstants.VARIABLE)));
	public static 	IToken comment =
			new Token(
				new TextAttribute(manager.getColor(ColorConstants.COMMENT)));
	public static 	IToken functionname =
			new Token(
				new TextAttribute(manager.getColor(ColorConstants.FUNCTIONNAME)));
	public static 	IToken sumo =
			new Token(
				new TextAttribute(manager.getColor(ColorConstants.SUMO)));
	public static 	IToken def =
			new Token(
				new TextAttribute(manager.getColor(ColorConstants.DEFAULT)));
	
}
