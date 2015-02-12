package com.articulate.sigma.semRewrite.datesandnumber;

public class Tokens {
	Integer id;
	String word;
	String ner;
	String normalizedNer;
	int charBegin;
	
	public int getCharBegin() {
		return charBegin;
	}
	public void setCharBegin(int charBegin) {
		this.charBegin = charBegin;
	}
	public int getCharEnd() {
		return charEnd;
	}
	public void setCharEnd(int charEnd) {
		this.charEnd = charEnd;
	}
	int charEnd; 
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public String getNer() {
		return ner;
	}
	public void setNer(String ner) {
		this.ner = ner;
	}
	public String getNormalizedNer() {
		return normalizedNer;
	}
	public void setNormalizedNer(String normalizedNer) {
		this.normalizedNer = normalizedNer;
	}

}
