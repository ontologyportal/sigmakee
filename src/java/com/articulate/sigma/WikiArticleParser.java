package com.articulate.sigma;
/**
 * This class parses the dumped English Wikipedia articles into plain texts.
 * The format of the English Wikipedia article is as follows:
 * 
 *  
 	{{Infobox economist
	| name             = Charles Kolstad
	| nationality      = [[United States|American]]
	| field            = [[Environmental Economics]]
	| alma_mater       = [[Stanford University]] <small>(Ph.D.)</small><br>[[University of Rochester]] <small>(M.A.)</small><br>[[Bates College]] <small>(B.A.)</small>
	| influences       = 
	| repec_prefix = e | repec_id = pko133
	}}
	'''Charles D. Kolstad''' (1954) is an [[United States|American]] economist. 
	He currently Chair of the Economics Department and a professor of environmental economics 
	at the [[University of California, Santa Barbara]], appointed in both the Bren School of 
	Environmental Science & Management and the Department of Economics. He has held a wide 
	variety of academic positions, including the [[University of Illinois]], [[Stanford University]], 
	[[MIT]], and the [[New Economic School]] (Moscow). He spend two years as a peace corps volunteer in Ghana.

	==Education==
	Kolstad holds a [[Doctor of Philosophy|Ph.D.]] in economics from Stanford, 
	an [[Master of Arts (postgraduate)|M.A.]] in mathematics from the University of Rochester, 
	and a [[Bachelor of Arts|B.A.]] in mathematics from Bates College.
 *
 */
public class WikiArticleParser {
	public String mWikiRawText = null;
	
	public WikiArticleParser(String wikiRawText){
		mWikiRawText = wikiRawText;
	}
	/**
	 * Get the plain text from Wikipedia article.
	 * @return
	 */
	public String getPlainText(){
		String text = mWikiRawText.replaceAll("&gt;", ">");
		text = text.replaceAll("&lt;", "<");
		text = text.replace("&ndash;", "-");
		text = text.replaceAll("<ref>.*?</ref>", " ");
		text = text.replaceAll("</?.*?>", " ").trim();
		
		String[] lines = text.split("\n");
		
		String plainText = "";
		for(String line : lines){	
			
			if(line.startsWith("{{") || line.startsWith("}}")) continue;
			if(line.startsWith("|") || line.startsWith("==")) continue;
			if(line.startsWith("*") || line.startsWith("[[")) continue;
			
			line = line.replaceAll("\\{\\{.*?\\}\\}", " ");
			line = line.replaceAll("\\[\\[", "");
			line = line.replaceAll("\\]\\]", "");
			line = line.replaceAll("\\'+", "");
			plainText += line + " ";
		}
		
		return plainText.trim();
	}
}
