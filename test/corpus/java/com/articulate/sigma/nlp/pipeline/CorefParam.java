package com.articulate.sigma.nlp.pipeline;

import edu.stanford.nlp.dcoref.CorefChain;

/**
 * Created by aholub on 3/9/15.
 */
public class CorefParam {
    static final String NO_VALUE = "N/A";
    static final Long NO_INDEX = Long.MIN_VALUE;

    private final Long sentenceNum;
    private final String value;
    private final Long index;

    public CorefParam(Long sentence, String value, Long index) {
        this.sentenceNum = sentence;
        this.value = value;
        this.index = index;
    }

    public static CorefParam of(Long sentence, String value) {
        return new CorefParam(sentence, value, NO_INDEX);
    }

    public static CorefParam of(Long sentence, Long index) {
        return new CorefParam(sentence, NO_VALUE, index);
    }

    public String getRef() {
        return NO_INDEX == index ?  value : Long.toString(index);
    }

    public int getSentenceIndex() {
        return sentenceNum.intValue() - 1;
    }

    @Override
    public String toString() {
        return sentenceNum + ":" + getRef();
    }

    public boolean equals(CorefChain.CorefMention mention) {
        return mention.sentNum == sentenceNum
                && (NO_INDEX == index ? mention.mentionSpan.equals(value) : mention.startIndex == index);
    }
}
