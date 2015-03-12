package com.articulate.sigma.test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

/**
 * Created by aholub on 3/12/15.
 */
public class CVSExporter {

    final boolean enabled;
    StringBuilder collectedOutput = new StringBuilder();

    public CVSExporter(boolean enabled, String header) {
        this.enabled = enabled;
        if(enabled) {
            collectedOutput.append(header).append("\n");
        }
    }

    public void addRow(String[] row) {
        if(enabled) {
            for(int i = 0; i<row.length; i++) {
                String col = row[i];
                if(i != 0) {
                    collectedOutput.append(", ");
                }
                if(col.contains(",")) {
                    collectedOutput.append("\"").append(col).append("\"");
                } else {
                    collectedOutput.append(col);
                }
            }
            collectedOutput.append("\n");
        }
    }

    public void flushIfEnabled() {
        if(enabled) {
            try {
                File file = new File("testresults-exported-" + System.currentTimeMillis() + ".csv");
                Files.write(collectedOutput.toString(), file, Charsets.UTF_8);
                System.out.println("Results exported to: " + file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            collectedOutput = null;
        }
    }
}
