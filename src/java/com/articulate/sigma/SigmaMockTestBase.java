package com.articulate.sigma;

import com.google.common.collect.ImmutableList;

/**
 * Created by geraldkurlandski on 1/23/15.
 */
public class SigmaMockTestBase {
    protected KB kbMock = new KBMock("dummyString");

    private static final ImmutableList<String> RECOGNIZED_PROCESSES = ImmutableList.of(
            "Process",
            "Driving");

    /**
     * Mock of kb component held by SumoProcess class.
     */
    protected class KBMock extends KB     {
        /**
         * ************************************************************
         * Constructor
         *
         * @param dummyStr
         */
        public KBMock(String dummyStr) {
            super(dummyStr);
        }

        @Override
        public boolean isSubclass(String str1, String str2)    {
            if (RECOGNIZED_PROCESSES.contains(str1))    {
                return true;
            }
                return false;
        }
    }


}
