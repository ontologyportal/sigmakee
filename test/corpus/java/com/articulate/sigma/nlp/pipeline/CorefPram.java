package com.articulate.sigma.nlp.pipeline;

import edu.stanford.nlp.dcoref.CorefChain;

/**
 * Created by aholub on 3/9/15.
 */
public class CorefPram {
    static final String NO_VALUE = "N/A";
    static final Long NO_INDEX = Long.MIN_VALUE;

    private final Long sentence;
    private final String value;
    private final Long index;

    public CorefPram(Long sentence, String value, Long index) {
        this.sentence = sentence;
        this.value = value;
        this.index = index;
    }

    public static CorefPram of(Long sentence, String value) {
        return new CorefPram(sentence, value, NO_INDEX);
    }

    public static CorefPram of(Long sentence, Long index) {
        return new CorefPram(sentence, NO_VALUE, index);
    }

    @Override
    public String toString() {
        return sentence + ":" + (NO_INDEX == index?value:index);
    }

    public boolean equals(CorefChain.CorefMention mention) {
        return mention.sentNum == sentence
                && (NO_INDEX == index ? mention.mentionSpan.equals(value) : mention.startIndex == index);
    }
}
