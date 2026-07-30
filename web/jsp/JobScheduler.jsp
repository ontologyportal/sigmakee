<%@ include file="fragments/universal/Prelude.jspf" %>

<%@ page import="com.articulate.sigma.jobscheduler.Job" %>
<%@ page import="com.articulate.sigma.jobscheduler.JobScheduler" %>
<%@ page import="com.articulate.sigma.jobscheduler.Schedule" %>
<%@ page import="com.articulate.sigma.jobscheduler.ConsistencyCheckJob" %>
<%@ page import="com.articulate.sigma.jobscheduler.EAxFilterContradictionJob" %>
<%@ page import="com.articulate.sigma.jobscheduler.SumoUpdateJob" %>

<%@ page import="java.time.DayOfWeek" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.LocalTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%
if (!"admin".equalsIgnoreCase(role)) {
    response.sendRedirect("KBs.jsp");
    return;
}

JobScheduler scheduler =
        (JobScheduler) application.getAttribute("jobScheduler");

String jobMessage = null;
String jobError = null;

if (scheduler == null) {
    jobError = "The job scheduler has not been initialized.";
}

if (scheduler != null
        && "POST".equalsIgnoreCase(request.getMethod())) {

    try {
        String action = request.getParameter("action");
        String removeJobId = request.getParameter("removeJobId");
        String runJobId = request.getParameter("runJobId");
        if (runJobId != null && !runJobId.isBlank()) {
            scheduler.runJobNow(runJobId);
            jobMessage = "Job \"" + runJobId + "\" completed.";
        }
        if (removeJobId != null && !removeJobId.isBlank()) {
            scheduler.removeJob(removeJobId);
            jobMessage = "Job removed.";
        }
        else if ("add".equals(action)) {

            String id = request.getParameter("newJobId");
            String type = request.getParameter("newJobType");
            String newKbName = request.getParameter("newKbName");

            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Job ID cannot be empty.");
            }

            id = id.trim();

            LocalTime runTime =
                    LocalTime.parse(
                            request.getParameter(
                                    "newRunTime"));

            Schedule schedule =
                    Schedule.dailyAt(runTime);

            Job newJob;

            switch (type) {

                case "sumo-update":
                    newJob =
                            new SumoUpdateJob(
                                    id,
                                    schedule);
                    break;

                case "consistency-check":
                    if (newKbName == null
                            || newKbName.isBlank()) {

                        throw new IllegalArgumentException(
                                "A knowledge base is required.");
                    }

                    newJob =
                            new ConsistencyCheckJob(
                                    id,
                                    newKbName.trim(),
                                    schedule);
                    break;

                case "eaxfilter-contradiction":
                    if (newKbName == null
                            || newKbName.isBlank()) {

                        throw new IllegalArgumentException(
                                "A knowledge base is required.");
                    }

                    int cnfTimeout =
                            Integer.parseInt(
                                    request.getParameter(
                                            "newCnfTimeout"));

                    int filterTimeout =
                            Integer.parseInt(
                                    request.getParameter(
                                            "newFilterTimeout"));

                    int vampireTimeout =
                            Integer.parseInt(
                                    request.getParameter(
                                            "newVampireTimeout"));

                    newJob =
                            new EAxFilterContradictionJob(
                                    id,
                                    newKbName.trim(),
                                    schedule,
                                    cnfTimeout,
                                    filterTimeout,
                                    vampireTimeout);
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown job type: " + type);
            }

            Job.ExecutionMode executionMode =
                    Job.ExecutionMode.valueOf(
                            request.getParameter(
                                    "newExecutionMode"));

            newJob.setExecutionMode(executionMode);
            newJob.setEnabled(
                    request.getParameter(
                            "newEnabled") != null);

            scheduler.addJob(newJob);

            jobMessage =
                    "Job \""
                    + newJob.getTitle()
                    + "\" added.";
        }
        else if ("remove".equals(action)) {

            String jobId =
                    request.getParameter("jobId");

            scheduler.removeJob(jobId);

            jobMessage = "Job removed.";
        }
        else if ("update".equals(action)) {

            // Parse all submitted values before changing any jobs.
            Map<String,Schedule> submittedSchedules =
                    new HashMap<>();

            Map<String,Job.ExecutionMode> submittedModes =
                    new HashMap<>();

            for (Job job : scheduler.getJobs()) {

                String id = job.getId();

                Schedule.Frequency frequency =
                        Schedule.Frequency.valueOf(
                                request.getParameter(
                                        "frequency." + id));

                LocalTime runTime =
                        LocalTime.parse(
                                request.getParameter(
                                        "time." + id));

                Schedule schedule;

                switch (frequency) {

                    case ONCE:
                        LocalDate runDate =
                                LocalDate.parse(
                                        request.getParameter(
                                                "date." + id));

                        schedule =
                                Schedule.onceAt(
                                        LocalDateTime.of(
                                                runDate,
                                                runTime));
                        break;

                    case DAILY:
                        schedule =
                                Schedule.dailyAt(runTime);
                        break;

                    case WEEKLY:
                        DayOfWeek dayOfWeek =
                                DayOfWeek.valueOf(
                                        request.getParameter(
                                                "weekday." + id));

                        schedule =
                                Schedule.weeklyAt(
                                        dayOfWeek,
                                        runTime);
                        break;

                    case MONTHLY:
                        int dayOfMonth =
                                Integer.parseInt(
                                        request.getParameter(
                                                "monthday." + id));

                        schedule =
                                Schedule.monthlyAt(
                                        dayOfMonth,
                                        runTime);
                        break;

                    default:
                        throw new IllegalArgumentException(
                                "Unsupported frequency: "
                                + frequency);
                }

                submittedSchedules.put(id, schedule);

                submittedModes.put(
                        id,
                        Job.ExecutionMode.valueOf(
                                request.getParameter(
                                        "executionMode." + id)));
            }

            for (Job job : scheduler.getJobs()) {

                String id = job.getId();

                boolean enabled =
                        request.getParameter(
                                "enabled." + id) != null;

                scheduler.updateJob(
                        id,
                        enabled,
                        submittedModes.get(id),
                        submittedSchedules.get(id));
            }

            jobMessage = "Job schedules saved.";
        }
    }
    catch (NumberFormatException exception) {
        jobError =
                "Timeout values must be valid integers.";
    }
    catch (IllegalArgumentException exception) {
        jobError =
                "Invalid job configuration: "
                + exception.getMessage();
    }
    catch (IllegalStateException exception) {
        jobError =
                "Could not save jobs: "
                + exception.getMessage();
    }
    catch (Exception exception) {
        jobError =
                "Job execution failed: "
                + exception.getMessage();
    }
}

String pageName = "Jobs";
String pageString = "Scheduled Jobs";

DateTimeFormatter nextRunFormat =
        DateTimeFormatter.ofPattern(
                "EEE, MMM d, yyyy 'at' h:mm a");

List<Job> jobs =
        scheduler == null
                ? java.util.Collections.emptyList()
                : scheduler.getJobs();
%>

<HTML>
<HEAD>
    <TITLE>
        Sigma Knowledge Engineering Environment - Scheduled Jobs
    </TITLE>

    <style>
        .jobs-add-table {
            border-collapse: collapse;
            width: 100%;
            max-width: 750px;
            background: #fff;
        }

        .jobs-add-table th,
        .jobs-add-table td {
            border: 1px solid #aaa;
            padding: 8px 10px;
            text-align: left;
            vertical-align: top;
        }

        .jobs-add-table th {
            width: 190px;
            background: #e8e8e8;
        }

        .jobs-add-table label {
            margin-right: 12px;
        }
        .jobs-intro {
            max-width: 1200px;
            margin: 14px 0;
        }

        .jobs-notice {
            padding: 10px 12px;
            margin: 12px 0;
            max-width: 1175px;
            border-radius: 3px;
        }

        .jobs-success {
            color: #205c20;
            background: #eef8ee;
            border: 1px solid #8cbd8c;
        }

        .jobs-error {
            color: #8b1a1a;
            background: #fff0f0;
            border: 1px solid #d89a9a;
        }

        .jobs-table {
            border-collapse: collapse;
            width: 100%;
            max-width: 1200px;
            background: #fff;
        }

        .jobs-table th,
        .jobs-table td {
            border: 1px solid #999;
            padding: 9px 10px;
            text-align: left;
            vertical-align: top;
        }

        .jobs-table th {
            background: #e8e8e8;
        }

        .jobs-table tbody tr:nth-child(even) {
            background: #f7f7f7;
        }

        .jobs-table tbody tr:hover {
            background: #eef5ff;
        }

        .job-name {
            font-weight: bold;
            white-space: nowrap;
        }

        .job-description {
            display: block;
            margin-top: 4px;
            color: #555;
            font-size: 0.9em;
        }
        .job-run-now,
        .job-remove {
            margin-top: 8px;
            margin-right: 6px;
        }
        .schedule-details {
            margin-top: 7px;
        }

        .schedule-details label {
            display: block;
            margin-top: 5px;
        }

        .jobs-controls {
            margin: 15px 0;
            max-width: 1200px;
        }

        .jobs-footnote {
            color: #555;
            max-width: 1200px;
        }
    </style>
</HEAD>

<BODY BGCOLOR=#FFFFFF>

<%@ include file="fragments/universal/CommonHeader.jspf" %>

<p class="jobs-intro">
    Configure when Sigma maintenance and validation jobs run.

    <% if (scheduler != null) { %>
        Schedules are stored in
        <code><%= scheduler.getScheduleFile() %></code>.
    <% } %>
</p>

<% if (jobMessage != null) { %>
    <div class="jobs-notice jobs-success">
        <%= jobMessage %>
    </div>
<% } %>

<% if (jobError != null) { %>
    <div class="jobs-notice jobs-error">
        <%= jobError %>
    </div>
<% } %>

<% if (scheduler != null) { %>


<h3>Add job</h3>

<form method="POST" action="JobScheduler.jsp"
      class="add-job-form">

    <input type="hidden" name="action" value="add">
    <input type="hidden" name="kb" value="<%= kbName %>">

    <table class="jobs-add-table">
        <tr>
            <th>
                <label for="newJobId">Job ID</label>
            </th>
            <td>
                <input
                    id="newJobId"
                    name="newJobId"
                    type="text"
                    pattern="[A-Za-z0-9._-]+"
                    placeholder="weekly-sumo-check"
                    required>
            </td>
        </tr>

        <tr>
            <th>
                <label for="newJobType">Job type</label>
            </th>
            <td>
                <select
                    id="newJobType"
                    name="newJobType"
                    onchange="updateNewJobFields()">

                    <option value="sumo-update">
                        Update SUMO
                    </option>

                    <option value="consistency-check">
                        Consistency Check
                    </option>

                    <option value="eaxfilter-contradiction">
                        EAxFilter Contradiction Search
                    </option>
                </select>
            </td>
        </tr>

        <tr id="newKbRow">
            <th>
                <label for="newKbName">
                    Knowledge base
                </label>
            </th>
            <td>
                <input
                    id="newKbName"
                    name="newKbName"
                    type="text"
                    value="<%= kbName %>">
            </td>
        </tr>

        <tr>
            <th>
                <label for="newExecutionMode">
                    Execution mode
                </label>
            </th>
            <td>
                <select
                    id="newExecutionMode"
                    name="newExecutionMode">

                    <option value="TOMCAT">
                        Tomcat
                    </option>

                    <option value="CRON">
                        Cron
                    </option>
                </select>
            </td>
        </tr>

        <tr>
            <th>
                <label for="newRunTime">
                    Initial run time
                </label>
            </th>
            <td>
                <input
                    id="newRunTime"
                    name="newRunTime"
                    type="time"
                    value="02:00"
                    required>

                <span class="job-description">
                    New jobs initially use a daily schedule.
                    You can change the frequency after adding it.
                </span>
            </td>
        </tr>

        <tr id="eaxTimeoutRows">
            <th>EAxFilter timeouts</th>
            <td>
                <label>
                    CNF
                    <input
                        name="newCnfTimeout"
                        type="number"
                        min="1"
                        value="300">
                </label>

                <label>
                    Filter
                    <input
                        name="newFilterTimeout"
                        type="number"
                        min="1"
                        value="300">
                </label>

                <label>
                    Vampire
                    <input
                        name="newVampireTimeout"
                        type="number"
                        min="1"
                        value="30">
                </label>
            </td>
        </tr>

        <tr>
            <th>Enabled</th>
            <td>
                <label>
                    <input
                        name="newEnabled"
                        type="checkbox"
                        checked>
                    Enable this job
                </label>
            </td>
        </tr>
    </table>

    <div class="jobs-controls">
        <input type="submit" value="Add job">
    </div>
</form>

<form method="POST" action="JobScheduler.jsp">
    <input type="hidden" name="action" value="update">
    <input type="hidden" name="kb" value="<%= kbName %>">
    <table class="jobs-table">
        <thead>
            <tr>
                <th>Enabled</th>
                <th>Job</th>
                <th>Schedule</th>
                <th>Next scheduled run</th>
            </tr>
        </thead>

        <tbody>

        <% for (Job job : jobs) {
               Schedule schedule = job.getSchedule();

               LocalDateTime nextRun =
                       job.isEnabled()
                               ? schedule.nextRunAfter(
                                       LocalDateTime.now())
                               : null;
        %>

            <tr>
                <td>
                    <input
                        type="checkbox"
                        name="enabled.<%= job.getId() %>"
                        aria-label="Enable <%= job.getTitle() %>"
                        <%= job.isEnabled()
                                ? "checked"
                                : "" %>>
                </td>

                <td>
                    <span class="job-name">
                        <%= job.getTitle() %>
                    </span>

                    <span class="job-description">
                        <%= job.getDescription() %>
                    </span>

                    <button
                        type="submit"
                        name="removeJobId"
                        value="<%= job.getId() %>"
                        class="job-remove"
                        onclick="return confirm('Remove this scheduled job?');">
                        Remove
                    </button>
                    <button
                        type="submit"
                        name="runJobId"
                        value="<%= job.getId() %>"
                        class="job-run-now"
                        <%= job.isEnabled() ? "" : "disabled" %>>
                        Run now
                    </button>
                </td>

                <td>
                    <label>
                        Run with

                        <select name="executionMode.<%= job.getId() %>">
                            <% for (Job.ExecutionMode mode
                                    : Job.ExecutionMode.values()) { %>

                                <option
                                    value="<%= mode.name() %>"
                                    <%= mode == job.getExecutionMode()
                                            ? "selected"
                                            : "" %>>
                                    <%= mode == Job.ExecutionMode.TOMCAT
                                            ? "Tomcat"
                                            : "Cron" %>
                                </option>

                            <% } %>
                        </select>
                    </label>

                    <span class="job-description">
                        Cron jobs require a separately configured operating-system cron entry.
                    </span>

                    <label>
                        Frequency

                        <select
                            name="frequency.<%= job.getId() %>"
                            class="frequency-select">

                            <% for (Schedule.Frequency frequency
                                    : Schedule.Frequency.values()) { %>

                                <option
                                    value="<%= frequency.name() %>"
                                    <%= frequency
                                            == schedule.getFrequency()
                                                ? "selected"
                                                : "" %>>
                                    <%= frequency.name() %>
                                </option>

                            <% } %>
                        </select>
                    </label>

                    <div class="schedule-details">
                        <label>
                            Time
                            <input
                                type="time"
                                name="time.<%= job.getId() %>"
                                value="<%= schedule.getRunTime() %>"
                                required>
                        </label>

                        <label>
                            One-time date
                            <input
                                type="date"
                                name="date.<%= job.getId() %>"
                                value="<%=
                                    schedule.getRunDate() == null
                                        ? LocalDate.now()
                                                   .plusDays(1)
                                        : schedule.getRunDate()
                                %>">
                        </label>

                        <label>
                            Weekly day
                            <select
                                name="weekday.<%= job.getId() %>">

                                <% for (DayOfWeek day
                                        : DayOfWeek.values()) { %>

                                    <option
                                        value="<%= day.name() %>"
                                        <%= day
                                            == schedule.getDayOfWeek()
                                                ? "selected"
                                                : "" %>>
                                        <%= day %>
                                    </option>

                                <% } %>
                            </select>
                        </label>

                        <label>
                            Monthly day
                            <input
                                type="number"
                                name="monthday.<%= job.getId() %>"
                                min="1"
                                max="31"
                                value="<%=
                                    schedule.getDayOfMonth() == null
                                        ? 1
                                        : schedule.getDayOfMonth()
                                %>">
                        </label>
                    </div>
                </td>

                <td>
                    <% if (!job.isEnabled()) { %>
                        Disabled
                    <% }
                       else if (nextRun == null) { %>
                        No future run
                    <% }
                       else { %>
                        <%= nextRunFormat.format(nextRun) %>
                    <% } %>
                </td>
            </tr>

        <% } %>

        <% if (jobs.isEmpty()) { %>
            <tr>
                <td colspan="4">
                    No scheduled jobs have been configured.
                </td>
            </tr>
        <% } %>

        </tbody>
    </table>

    <div class="jobs-controls">
        <input
            type="submit"
            value="Save schedules">
    </div>
</form>

<p class="jobs-footnote">
    Times use the Tomcat server's local time zone:
    <strong>
        <%= java.time.ZoneId.systemDefault() %>
    </strong>.
</p>

<% } %>

<%@ include file="fragments/universal/Postlude.jspf" %>
<script>
function updateNewJobFields() {

    const type =
        document.getElementById("newJobType").value;

    document.getElementById("newKbRow").hidden =
        type === "sumo-update";

    document.getElementById("eaxTimeoutRows").hidden =
        type !== "eaxfilter-contradiction";
}

updateNewJobFields();
</script>
</BODY>
</HTML>