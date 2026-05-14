package com.articulate.sigma;

public class LoggingUtils {

    public static void printProgressBar(String before, int current, int total) {

        int width = 20;
        if (total <= 0)
            total = 1;
        float progress = (float) current / total;
        int completedBlocks = (int) (progress * width);
        StringBuilder bar = new StringBuilder();
        bar.append("\r");
        bar.append(before);
        bar.append(" [");
        for (int i = 0; i < width; i++) {
            if (i < completedBlocks)
                bar.append("█");
            else
                bar.append("░");
        }
        bar.append(String.format("] %d%%", (int) (progress * 100)));
        System.out.print(bar.toString());
        if (current >= total)
            System.out.println();
    }
    
    public static void main(String[] args) {

        int total = 100;

        for (int current = 0; current <= total; current++) {
            printProgressBar("Generating TFF file", current, total);

            try {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Progress test interrupted.");
                return;
            }
        }

        System.out.println("Done.");
    }
}
