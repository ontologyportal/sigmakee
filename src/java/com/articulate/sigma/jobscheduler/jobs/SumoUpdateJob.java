package com.articulate.sigma.jobscheduler;

public class SumoUpdateJob extends Job {

    public SumoUpdateJob(String id, Schedule schedule) {

        super(
            id,
            "Update SUMO",
            "Pull the latest SUMO files and reload the knowledge base",
            schedule
        );
    }

    @Override
    public void run() throws Exception {
        updateSumo();
    }

    private void updateSumo() throws Exception {
        // Implementation will go here.
    }

    @Override
    public String getType() {
        return "sumo-update";
    }
}