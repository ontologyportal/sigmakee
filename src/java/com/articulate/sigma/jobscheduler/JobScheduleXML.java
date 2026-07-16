package com.articulate.sigma.jobscheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/********************************************************************
 * Reads and writes the persistent SigmaKEE job schedule.
 */
public class JobScheduleXML {

    private static final String FILE_NAME = "job-schedule.xml";
    private final Path scheduleFile;

    /********************************************************************
     * Creates a store using ~/.sigmakee/job-schedule.xml.
     */
    public JobScheduleXML() {

        this(Path.of(System.getProperty("user.home"), ".sigmakee", FILE_NAME));
    }

    /********************************************************************
     * Creates a store using a specified XML file.
     * @param scheduleFile XML schedule file
     */
    public JobScheduleXML(Path scheduleFile) {

        if (scheduleFile == null) {
            throw new IllegalArgumentException("Schedule file cannot be null");
        }
        this.scheduleFile = scheduleFile.toAbsolutePath();
        initializeFile();
    }

    /********************************************************************
     * Loads jobs from the XML schedule
     * @return jobs stored in the schedule file
     * @throws IOException if the XML cannot be read or parsed
     */
    public List<Job> load() throws IOException {

        try {
            DocumentBuilderFactory factory = createDocumentBuilderFactory();
            Document document = factory.newDocumentBuilder().parse(scheduleFile.toFile());
            Element root = document.getDocumentElement();
            if (root == null || !"job-schedule".equals(root.getTagName())) {
                throw new IOException("Expected <job-schedule> root element in " + scheduleFile);
            }
            List<Job> jobs = new ArrayList<>();
            NodeList children = root.getChildNodes();
            for (int index = 0; index < children.getLength(); index++) {
                Node node = children.item(index);
                if (node instanceof Element element && "job".equals(element.getTagName())) jobs.add(parseJob(element));
            }
            return jobs;
        }
        catch (IOException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new IOException("Could not read job schedule: " + scheduleFile, exception);
        }
    }

    /********************************************************************
     * Saves jobs to the XML schedule.
     *
     * @param jobs jobs to save
     * @throws IOException if the XML cannot be written
     */
    public synchronized void save(List<Job> jobs)
            throws IOException {

        if (jobs == null) {
            throw new IllegalArgumentException(
                    "Jobs cannot be null");
        }

        try {
            DocumentBuilderFactory factory =
                    createDocumentBuilderFactory();

            Document document =
                    factory.newDocumentBuilder()
                            .newDocument();

            Element root =
                    document.createElement(
                            "job-schedule");

            document.appendChild(root);

            for (Job job : jobs) {
                root.appendChild(
                        createJobElement(
                                document,
                                job));
            }

            writeDocument(document);
        }
        catch (IOException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new IOException(
                    "Could not save job schedule: "
                    + scheduleFile,
                    exception);
        }
    }

    /********************************************************************
     * Creates the schedule directory and an empty valid schedule file
     * when necessary.
     */
    private void initializeFile() {

        try {
            Path parent = scheduleFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(scheduleFile)) {
                save(Collections.emptyList());
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not initialize job schedule file: "
                    + scheduleFile,
                    exception);
        }
    }

    /********************************************************************
     * Creates an XML element for a job.
     */
private Element createJobElement(Document document, Job job) throws IOException {
        if (job == null) throw new IOException("Cannot save a null job");
        if (job.getSchedule() == null) throw new IOException("Job " + job.getId() + " does not have a schedule");
        Element jobElement = document.createElement("job");
        jobElement.setAttribute("id", job.getId());
        jobElement.setAttribute("type", job.getType());
        jobElement.setAttribute("enabled", Boolean.toString(job.isEnabled()));
        jobElement.setAttribute("executionMode", job.getExecutionMode().name());
        jobElement.appendChild(createScheduleElement(document, job.getSchedule()));
        if (job instanceof EAxFilterContradictionJob axJob) {
                appendTextElement(document, jobElement, "kbName", axJob.getKbName());
                appendTextElement(document, jobElement, "cnfTimeout", Integer.toString(axJob.getCnfTimeout()));
                appendTextElement(document, jobElement, "filterTimeout", Integer.toString(axJob.getFilterTimeout()));
                appendTextElement(document, jobElement, "vampireTimeout", Integer.toString(axJob.getVampireTimeout()));
        }
        else if (job instanceof ConsistencyCheckJob consistencyJob) appendTextElement(document, jobElement, "kbName", consistencyJob.getKbName());
        else if (!(job instanceof SumoUpdateJob)) throw new IOException("Unsupported job class: " + job.getClass().getName());
        return jobElement;
}

private void appendTextElement(Document document, Element parent, String name, String value) {
        Element element = document.createElement(name);
        element.setTextContent(value);
        parent.appendChild(element);
}

    /********************************************************************
     * Creates an XML element for a schedule.
     */
    private Element createScheduleElement(
            Document document,
            Schedule schedule) {

        Element scheduleElement =
                document.createElement("schedule");

        scheduleElement.setAttribute(
                "frequency",
                schedule.getFrequency().name());

        scheduleElement.setAttribute(
                "runTime",
                schedule.getRunTime().toString());

        switch (schedule.getFrequency()) {
            case ONCE:
                scheduleElement.setAttribute(
                        "runDate",
                        schedule.getRunDate().toString());
                break;

            case WEEKLY:
                scheduleElement.setAttribute(
                        "dayOfWeek",
                        schedule.getDayOfWeek().name());
                break;

            case MONTHLY:
                scheduleElement.setAttribute(
                        "dayOfMonth",
                        schedule.getDayOfMonth().toString());
                break;

            case DAILY:
                break;

            default:
                throw new IllegalArgumentException(
                        "Unsupported frequency: "
                        + schedule.getFrequency());
        }

        return scheduleElement;
    }

    /********************************************************************
     * Parses one job element.
     */
    private Job parseJob(Element jobElement) throws IOException {

        String id = requireAttribute(jobElement, "id");
        String type = requireAttribute(jobElement, "type");
        boolean enabled = parseBooleanAttribute(jobElement, "enabled");
        Element scheduleElement = requireChild(jobElement, "schedule");
        Schedule schedule = parseSchedule(scheduleElement);
        Job job;
        switch (type) {
            case "eaxfilter-contradiction":
                job = new EAxFilterContradictionJob(
                id,
                requireChildText(jobElement, "kbName"),
                schedule,
                parseRequiredInt(jobElement, "cnfTimeout"),
                parseRequiredInt(jobElement, "filterTimeout"),
                parseRequiredInt(jobElement, "vampireTimeout"));
        break;
            case "consistency-check":
                job = new ConsistencyCheckJob(id, requireChildText(jobElement, "kbName"), schedule);
                break;
            case "sumo-update":
                job = new SumoUpdateJob(id, schedule);
                break;
            default:
                throw new IOException("Unknown job type: " + type);
        }
        String executionModeText = requireAttribute(jobElement, "executionMode");
        try {
            job.setExecutionMode(Job.ExecutionMode.valueOf(executionModeText));
        }
        catch (IllegalArgumentException exception) {
            throw new IOException("Unknown execution mode: " + executionModeText, exception);
        }
        job.setEnabled(enabled);
        return job;
    }

    /********************************************************************
     * Parses a schedule element.
     */
    private Schedule parseSchedule(Element scheduleElement) throws IOException {

        String frequencyText = requireAttribute(scheduleElement, "frequency");
        Schedule.Frequency frequency;
        try {
            frequency = Schedule.Frequency.valueOf(frequencyText);
        }
        catch (IllegalArgumentException exception) {
            throw new IOException("Unknown schedule frequency: " + frequencyText, exception);
        }
        LocalTime runTime;
        try {
            runTime = LocalTime.parse(
                    requireAttribute(
                            scheduleElement,
                            "runTime"));
        }
        catch (Exception exception) {
            throw new IOException(
                    "Invalid schedule run time",
                    exception);
        }

        try {
            switch (frequency) {
                case ONCE:
                    LocalDate runDate =
                            LocalDate.parse(
                                    requireAttribute(
                                            scheduleElement,
                                            "runDate"));

                    return Schedule.onceAt(
                            LocalDateTime.of(
                                    runDate,
                                    runTime));

                case DAILY:
                    return Schedule.dailyAt(
                            runTime);

                case WEEKLY:
                    DayOfWeek dayOfWeek =
                            DayOfWeek.valueOf(
                                    requireAttribute(
                                            scheduleElement,
                                            "dayOfWeek"));

                    return Schedule.weeklyAt(
                            dayOfWeek,
                            runTime);

                case MONTHLY:
                    int dayOfMonth =
                            Integer.parseInt(
                                    requireAttribute(
                                            scheduleElement,
                                            "dayOfMonth"));

                    return Schedule.monthlyAt(
                            dayOfMonth,
                            runTime);

                default:
                    throw new IOException(
                            "Unsupported frequency: "
                            + frequency);
            }
        }
        catch (IOException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new IOException(
                    "Invalid " + frequency
                    + " schedule",
                    exception);
        }
    }

    /********************************************************************
     * Writes the document to a temporary file, then replaces the
     * schedule file.
     */
    private void writeDocument(Document document)
            throws IOException, TransformerException {

        Path parent = scheduleFile.getParent();

        Path temporaryFile =
                Files.createTempFile(
                        parent,
                        "job-schedule-",
                        ".xml.tmp");

        boolean moved = false;

        try {
            TransformerFactory factory =
                    TransformerFactory.newInstance();

            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true);

            Transformer transformer =
                    factory.newTransformer();

            transformer.setOutputProperty(
                    OutputKeys.INDENT,
                    "yes");

            transformer.setOutputProperty(
                    OutputKeys.ENCODING,
                    StandardCharsets.UTF_8.name());

            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}"
                    + "indent-amount",
                    "4");

            try (OutputStream output =
                         Files.newOutputStream(
                                 temporaryFile)) {

                transformer.transform(
                        new DOMSource(document),
                        new StreamResult(output));
            }

            try {
                Files.move(
                        temporaryFile,
                        scheduleFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                        temporaryFile,
                        scheduleFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        }
        finally {
            if (!moved) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    /********************************************************************
     * Creates a securely configured XML parser.
     */
    private DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/" + "disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/" + "external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/" + "external-parameter-entities",false);
        factory.setFeature("http://apache.org/xml/features/" + "nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    /********************************************************************
     * Returns a required XML attribute.
     */
    private String requireAttribute(
            Element element,
            String attributeName)
            throws IOException {

        String value =
                element.getAttribute(attributeName);

        if (value == null || value.isBlank()) {
            throw new IOException(
                    "Missing attribute '"
                    + attributeName
                    + "' on <"
                    + element.getTagName()
                    + ">");
        }
        return value;
    }

    /********************************************************************
     * Parses a required boolean attribute.
     */
    private boolean parseBooleanAttribute(Element element, String attributeName) throws IOException {
        
        String value = requireAttribute(element, attributeName);
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IOException("Attribute '" + attributeName + "' must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    /********************************************************************
     * Returns a required direct child element.
     */
    private Element requireChild(Element parent, String childName) throws IOException {

        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && childName.equals(element.getTagName())) return element;
        }
        throw new IOException("Missing <" + childName + "> inside <" + parent.getTagName() + ">");
    }

    /********************************************************************
     * Returns the text of a required child element.
     */
    private String requireChildText(Element parent, String childName) throws IOException {

        String text = requireChild(parent, childName).getTextContent();
        if (text == null || text.isBlank()) throw new IOException("<" + childName + "> cannot be empty");
        return text.trim();
    }

    private int parseRequiredInt(Element parent, String childName) throws IOException {
        String value = requireChildText(parent, childName);
        try {
                int number = Integer.parseInt(value);
                if (number <= 0) throw new NumberFormatException("value must be positive");
                return number;
        }
        catch (NumberFormatException exception) {
                throw new IOException("<" + childName + "> must contain a positive integer: " + value, exception);
        }
    }

    /********************************************************************
     * Returns the schedule file.
     */
    public Path getScheduleFile() {
        return scheduleFile;
    }
}