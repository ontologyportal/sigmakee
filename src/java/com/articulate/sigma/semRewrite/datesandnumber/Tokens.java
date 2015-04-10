package com.articulate.sigma.semRewrite.datesandnumber;

/*
Copyright 2014-2015 IPsoft

Author: Nagaraj Bhat nagaraj.bhat@ipsoft.com
        Rashmi Rao

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

public class Tokens {
	
	Integer id;
	String word;
	String ner;
	String normalizedNer;
	String lemma;
	String pos;
	int charBegin;
	
	String tokenType = "DEFAULT";
	
	public String getTokenType() {
		return tokenType;
	}
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
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
	
	public String getLemma() {
		return lemma;
	}
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	
public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}
public boolean equals(Tokens token) {
	    
		boolean wordFlag,tokenTypeFlag;
		wordFlag  = tokenTypeFlag = true;
		if ((this.word == null) && (this.tokenType == null)) {
			wordFlag = tokenTypeFlag = false;
		}
		if ((token.word != null) && (this.word != null) && !(token.word.equalsIgnoreCase(word))) {
			wordFlag = false;
		}
		if ((token.tokenType != null) && (this.tokenType != null) && !(token.tokenType.equalsIgnoreCase(tokenType))) {
			tokenTypeFlag = false;
		}
		return (wordFlag && tokenTypeFlag);
	}
	
	/** ***************************************************************
     */
	public int hashCode() {
	    
        int hash = 2;
        hash = 49 * hash + (this.word != null  ? this.word.hashCode() : 0);
        hash = 49 * hash + (this.tokenType != null  ? this.tokenType.hashCode() : 0);
        return hash;
    }
	
}
