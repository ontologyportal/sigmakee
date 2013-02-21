package com.articulate.sigma;

import java.util.Map;

public class SentenceInstance {
	/** words to their lemma */
	public Map<String, String> mWords2Lemma;
	/** words to their part-of-speech */
	public Map<String, String> mWords2POS;
	/**indices to words */
	public Map<Integer, String> mIndex2Word;
	/**words to NER */
	public Map<String, String> mWords2NER;
	
	/**
	 * Set words to their lemma.
	 * @param words2Lemma
	 */
	public void setWords2Lemma(Map<String, String> words2Lemma){
		mWords2Lemma = words2Lemma;
	}
	/**
	 * Get words to lemma mapping.
	 * @return
	 */
	public Map<String, String> getWords2Lemma(){
		return mWords2Lemma;
	}
	/**
	 * Set words to their part of speech
	 * @param words2POS
	 */
	public void setWords2POS(Map<String, String> words2POS){
		mWords2POS = words2POS;
	}
	/**
	 * Get words to part-of-speech mapping
	 * @return
	 */
	public Map<String, String> getWords2POS(){
		return mWords2POS;
	}
	/**
	 * Set indices to words.
	 * @param index2Word
	 */
	public void setIndex2Word(Map<Integer, String> index2Word){
		mIndex2Word = index2Word;
	}
	/**
	 * Get indices to words
	 * @return
	 */
	public Map<Integer, String> getIndex2Word(){
		return mIndex2Word;
	}
	/**
	 * Set words to NER mapping.
	 * @param words2NER
	 */
	public void setWords2NER(Map<String, String> words2NER){
		mWords2NER = words2NER;
	}
	/**
	 * Get words to NER mapping.
	 * @return
	 */
	public Map<String, String> getWords2NER(){
		return mWords2NER;
	}
	
	
}
