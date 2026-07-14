package com.articulate.sigma.jobscheduler;

public class ConsistencyCheckJob extends Job {

    private final String kbName;

    public ConsistencyCheckJob(String id,String kbName, Schedule schedule) {
        super(
            id,
            "Consistency Check",
            "Check " + kbName + " for inconsistencies",
            schedule
        );
        this.kbName = kbName;
    }

    @Override
    public void run() throws Exception {
        runConsistencyCheck(kbName);
    }

    @Override
    public String getType() {
        return "consistency-check";
    }

    public String getKbName() {
        return kbName;
    }

    private void runConsistencyCheck(String kbName) throws Exception {
        System.out.println("cCheck");
    }
}