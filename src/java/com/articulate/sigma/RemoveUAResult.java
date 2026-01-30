package com.articulate.sigma;


public class RemoveUAResult {


    public enum RemoveUAStatus { OK, FAIL_TRANSITIVE_GROUND, FAIL_NO_CACHE }

    public final RemoveUAStatus status;
    public final String predicate;
    public final String formula;
    public RemoveUAResult(RemoveUAStatus status, String predicate, String formula) {
        this.status = status; this.predicate = predicate; this.formula = formula;
    }

    public static RemoveUAResult ok() {
        return new RemoveUAResult(RemoveUAStatus.OK, null, null);
    }

    public static RemoveUAResult fail(RemoveUAStatus st, String pred, String form) {
        return new RemoveUAResult(st, pred, form);
    }
}


