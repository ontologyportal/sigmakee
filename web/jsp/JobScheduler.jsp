<%@ include file="fragments/universal/Prelude.jspf" %>
<%@ page import="java.time.DayOfWeek" %>
<%@ page import="java.time.ZonedDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.nio.file.Path" %>

<%
if (!"admin".equalsIgnoreCase(role)) {
    response.sendRedirect("KBs.jsp");
    return;
}

Path jobsPath = Jobs.schedulePath(KBmanager.configuration.getConfigFilePath());
Jobs jobs = Jobs.load(jobsPath);
String jobMessage = null;
String jobError = null;

if ("POST".equalsIgnoreCase(request.getMethod())) {
    try {
        jobs.setZoneId(request.getParameter("timezone"));
        for (Jobs.ScheduledJob scheduledJob : jobs.getJobs()) {
            String id = scheduledJob.getId();
            String[] selectedDays = request.getParameterValues("days." + id);
            jobs.update(id,
                    request.getParameter("enabled." + id) != null,
                    request.getParameter("time." + id),
                    Arrays.asList(selectedDays == null ? new String[0] : selectedDays));
        }
        jobs.save(jobsPath);
        jobMessage = "Job schedules saved.";
    }
    catch (IllegalArgumentException e) {
        jobError = e.getMessage();
    }
    catch (IOException e) {
        jobError = "Could not save job schedules: " + e.getMessage();
    }
}

String pageName = "Jobs";
String pageString = "Scheduled Jobs";
DateTimeFormatter nextRunFormat = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a z");
List<String> commonZones = Arrays.asList(
        "UTC", "America/Los_Angeles", "America/Denver", "America/Chicago",
        "America/New_York", "Europe/London", "Europe/Berlin", "Asia/Tokyo");
if (!commonZones.contains(jobs.getZoneId().getId())) {
    commonZones = new ArrayList<>(commonZones);
    commonZones.add(jobs.getZoneId().getId());
}
%>

<HTML>
<HEAD>
    <TITLE>Sigma Knowledge Engineering Environment - Scheduled Jobs</TITLE>
    <style>
        .jobs-intro { max-width: 1050px; margin: 14px 0; }
        .jobs-notice { padding: 10px 12px; margin: 12px 0; max-width: 1025px; border-radius: 3px; }
        .jobs-success { color: #205c20; background: #eef8ee; border: 1px solid #8cbd8c; }
        .jobs-error { color: #8b1a1a; background: #fff0f0; border: 1px solid #d89a9a; }
        .jobs-table { border-collapse: collapse; width: 100%; max-width: 1050px; background: #fff; }
        .jobs-table th, .jobs-table td { border: 1px solid #999; padding: 9px 10px; text-align: left; vertical-align: top; }
        .jobs-table th { background: #e8e8e8; }
        .jobs-table tbody tr:nth-child(even) { background: #f7f7f7; }
        .jobs-table tbody tr:hover { background: #eef5ff; }
        .job-name { font-weight: bold; white-space: nowrap; }
        .job-description { display: block; margin-top: 4px; color: #555; font-size: 0.9em; }
        .job-days { min-width: 210px; }
        .job-days label { display: inline-block; width: 48px; margin-bottom: 5px; }
        .job-condition { white-space: nowrap; }
        .jobs-controls { margin: 15px 0; max-width: 1050px; }
        .jobs-controls label { margin-right: 15px; }
        .jobs-footnote { color: #555; max-width: 1050px; }
    </style>
</HEAD>
<BODY BGCOLOR=#FFFFFF>
    <%@include file="fragments/universal/CommonHeader.jspf" %>

    <p class="jobs-intro">
        Configure when Sigma maintenance and validation jobs should run. Schedules are stored in
        <code><%=jobsPath%></code>.
    </p>
    <p class="jobs-notice jobs-error">
        Scheduling configuration is available now; execution runners are not connected yet.
        Enabling a row records its schedule but does not launch the operation.
    </p>

    <% if (jobMessage != null) { %>
        <div class="jobs-notice jobs-success"><%=jobMessage%></div>
    <% } %>
    <% if (jobError != null) { %>
        <div class="jobs-notice jobs-error"><%=jobError%></div>
    <% } %>

    <form method="POST" action="Jobs.jsp">
        <input type="hidden" name="kb" value="<%=kbName%>">
        <div class="jobs-controls">
            <label for="timezone"><strong>Time zone</strong></label>
            <select id="timezone" name="timezone">
                <% for (String zone : commonZones) { %>
                    <option value="<%=zone%>" <%=zone.equals(jobs.getZoneId().getId()) ? "selected" : ""%>><%=zone%></option>
                <% } %>
            </select>
        </div>

        <table class="jobs-table">
            <thead>
                <tr>
                    <th>Enabled</th>
                    <th>Job</th>
                    <th>Run condition</th>
                    <th>Time</th>
                    <th>Days</th>
                    <th>Next scheduled run</th>
                </tr>
            </thead>
            <tbody>
            <% for (Jobs.ScheduledJob scheduledJob : jobs.getJobs()) {
                   ZonedDateTime nextRun = jobs.nextRun(scheduledJob); %>
                <tr>
                    <td>
                        <input type="checkbox" name="enabled.<%=scheduledJob.getId()%>"
                               aria-label="Enable <%=scheduledJob.getName()%>"
                               <%=scheduledJob.isEnabled() ? "checked" : ""%>>
                    </td>
                    <td>
                        <span class="job-name"><%=scheduledJob.getName()%></span>
                        <span class="job-description"><%=scheduledJob.getDescription()%></span>
                    </td>
                    <td class="job-condition"><%=scheduledJob.getCondition().getLabel()%></td>
                    <td>
                        <input type="time" name="time.<%=scheduledJob.getId()%>"
                               value="<%=scheduledJob.getTimeValue()%>" required>
                    </td>
                    <td class="job-days">
                        <% for (DayOfWeek day : DayOfWeek.values()) { %>
                            <label>
                                <input type="checkbox" name="days.<%=scheduledJob.getId()%>"
                                       value="<%=day.name()%>"
                                       <%=scheduledJob.getDays().contains(day) ? "checked" : ""%>>
                                <%=day.name().substring(0, 2)%>
                            </label>
                        <% } %>
                    </td>
                    <td><%=nextRun == null ? "Disabled" : nextRunFormat.format(nextRun)%></td>
                </tr>
            <% } %>
            </tbody>
        </table>

        <div class="jobs-controls">
            <input type="submit" value="Save schedules">
        </div>
    </form>
    <p class="jobs-footnote">
        "Only when SUMO has updates" means the runner should skip execution when the source revision
        has not changed since that job's last successful run.
    </p>

    <%@ include file="fragments/universal/Postlude.jspf" %>
</BODY>
</HTML>
