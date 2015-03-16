package com.articulate.sigma.utils;

/**
 * Created by aholub on 3/13/15.
 */
public class ProgressPrinter {

    int progress = 0;
    final int ticksToSkip;

    public ProgressPrinter(int ticksToSkip) {
        this.ticksToSkip = ticksToSkip;
    }

    public void tick() {
        progress = ++progress % ticksToSkip;
        if(progress == 0) {
            System.out.print(".");
        }
    }
}
