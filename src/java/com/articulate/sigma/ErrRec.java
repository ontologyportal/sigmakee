package com.articulate.sigma;

/**
 * Represents a single diagnostic error or warning produced by KIF checking.
 * This class is immutable.
 */
public class ErrRec {

    public static final int ERROR = 0;
    public static final int WARNING = 1;

    public final int type;
    public final String file;
    public final int line;
    public final int start;
    public final int end;
    public final String msg;

    public ErrRec(int type, String file, int line, int start, int end, String msg) {
        this.type = type;
        this.file = file;
        this.line = line;
        this.start = start;
        this.end = end;
        this.msg = msg;
    }

    @Override
    public String toString() {
        String sev = (type == WARNING) ? "WARNING" : "ERROR";
        return sev + " in " + file + " at line " + (line + 1) + ": " + msg;
    }
}