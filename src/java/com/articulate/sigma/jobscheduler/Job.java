package com.articulate.sigma.jobscheduler;

/***************************************************************
 */
public abstract class Job {
    
    /**  */
    private final String id;
    /**  */
    private final String title;
    /**  */
    private final String description;
    /**  */
    private boolean enabled = true;
    /**  */
    private Schedule schedule;
    /**  */
    public enum ExecutionMode {TOMCAT, CRON};
    /**  */
    private ExecutionMode executionMode = ExecutionMode.TOMCAT;

    /***************************************************************
     */
    protected Job(String id, String title, String description, Schedule schedule) {

        this.id = id;
        this.title = title;
        this.description = description;
        this.schedule = schedule;
    }

    /***************************************************************
     */
    public abstract void run() throws Exception;

    /***************************************************************
     */
    public String getId() {return id;}

    /***************************************************************
     */
    public abstract String getType();

    /***************************************************************
     */
    public ExecutionMode getExecutionMode() {return executionMode;}

    /***************************************************************
     */
    public String getTitle() {return title;}

    /***************************************************************
     */
    public String getDescription() {return description;}

    /***************************************************************
     */
    public boolean isEnabled() {return enabled;}

    /***************************************************************
     */
    public void setEnabled(boolean enabled) {this.enabled = enabled;}

    /***************************************************************
     */
    public Schedule getSchedule() {return schedule;}

    /***************************************************************
     */
    public void setSchedule(Schedule schedule) {this.schedule = schedule;}
}