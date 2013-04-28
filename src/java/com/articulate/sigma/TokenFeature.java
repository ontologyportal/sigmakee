package com.articulate.sigma;

public class TokenFeature {
	
	public String mFeatureText = null;
	public int mPosition;
	/** the part of speech of the token */
	public String mPOS = null;
	/** name entity type of token */
	public String mNER = null;
	/** lemma of a token */
	public String mLemma = null;

	public TokenFeature(String featureText, int position){
		mFeatureText = featureText;
		mPosition = position;
	}
	
	/**
	 * Set the part of speech of a token.
	 * @param pos
	 */
	public void setPOS(String pos){
		mPOS = pos;
	}
	/**
	 * Get the part of speech of a token
	 * @return
	 */
	public String getPOS(){
		return mPOS;
	}
	/**
	 * Set the named entity type of a token
	 * @param ner
	 */
	public void setNER(String ner){
		mNER = ner;
	}
	/**
	 * Get the named entity type of a token
	 * @return
	 */
	public String getNER(){
		return mNER;
	}
	/**
	 * Set lemma of a token.
	 * @param lemma
	 */
	public void setLemma(String lemma){
		mLemma = lemma;
	}
	/**
	 * Get lemma of a token.
	 * @return
	 */
	public String getLemma(){
		return mLemma;
	}
	/**
	 * Get position
	 * @return
	 */
	public int getPosition(){
		return mPosition;
	}
	/**
	 * Get feature text.
	 * @return
	 */
	public String getFeatureText(){
		return mFeatureText;
	}
	
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(!(obj instanceof TokenFeature))
			return false;
		TokenFeature feature = (TokenFeature) obj;
		return mPosition == feature.getPosition() && mFeatureText.equalsIgnoreCase(feature.getFeatureText()); 
	}
	
	public String toString(){
		return mFeatureText + "\t" + mPosition + "\t" + mPOS;
	}
}
