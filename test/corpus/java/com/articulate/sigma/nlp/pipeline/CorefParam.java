package com.articulate.sigma.nlp.pipeline;

/*
Copyright 2014-2015 IPsoft

Author: Andrei Holub andrei.holub@ipsoft.com

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

import edu.stanford.nlp.dcoref.CorefChain;

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
