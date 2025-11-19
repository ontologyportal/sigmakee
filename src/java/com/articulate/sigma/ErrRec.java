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
        return sev + " in " + file + " at line " + (line) + " from " + start + " to " + end + ": " + msg;
    }

    /** ***************************************************************
     * Override equals() for deep comparison of all fields.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ErrRec other = (ErrRec) obj;
        return this.type == other.type &&
               this.line == other.line &&
               this.start == other.start &&
               this.end == other.end &&
               (this.file == null ? other.file == null : this.file.equals(other.file)) &&
               (this.msg == null ? other.msg == null : this.msg.equals(other.msg));
    }
}