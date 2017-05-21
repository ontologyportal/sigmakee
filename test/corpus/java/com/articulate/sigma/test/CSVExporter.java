package com.articulate.sigma.test;

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

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class CSVExporter {

    final boolean enabled;
    StringBuilder collectedOutput = new StringBuilder();

    public CSVExporter(boolean enabled, String header) {

        this.enabled = enabled;
        if (enabled) {
            collectedOutput.append(header).append("\n");
        }
    }

    public void addRow(String[] row) {

        if (enabled) {
            for (int i = 0; i<row.length; i++) {
                String col = row[i];
                if (i != 0) {
                    collectedOutput.append(", ");
                }
                if (col.contains(",")) {
                    collectedOutput.append("\"").append(col).append("\"");
                }
                else {
                    collectedOutput.append(col);
                }
            }
            collectedOutput.append("\n");
        }
    }

    public void flushIfEnabled() {

        if (enabled) {
            try {
                File file = new File("testresults-exported-" + System.currentTimeMillis() + ".csv");
                Files.write(collectedOutput.toString(), file, Charsets.UTF_8);
                System.out.println("Results exported to: " + file.getPath());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            collectedOutput = null;
        }
    }
}
