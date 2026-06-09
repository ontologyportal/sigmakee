package com.articulate.sigma;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.articulate.sigma.utils.ValidationUtils;
import com.articulate.sigma.user.EmailService;

public class ServerHealthChecker {

    private static final List<String> URLS = List.of(
            "https://fsg.nps.edu/sigma/KBs.jsp",
            "https://ontology.nps.edu/sigma/KBs.jsp",
            "https://sigma.ontologyportal.org/sigma/KBs.jsp"
    );

    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /********************************************************************
     * Runs one health check and sends an email only if one or more URLs are down.
     */
    private void run() {

        List<String> downUrls = new ArrayList<>();
        Map<String, String> failureReasons = new LinkedHashMap<>();
        for (String url : URLS) {
            CheckResult result = checkWithRetries(url);
            String status = result.ok ? "UP" : "DOWN";
            if (!result.ok) {
                downUrls.add(url);
                failureReasons.put(url, result.reason);
            }
        }
        if (downUrls.isEmpty()) {
            System.out.println("All SigmaKEE URLs are up. No email sent.");
            return;
        }
        sendDownEmail(downUrls, failureReasons);
    }

    /********************************************************************
     * Checks all configured URLs and prints results without sending email.
     */
    private void dryRun() {

        System.out.println("ServerHealthChecker dry run");
        System.out.println("No email will be sent.");
        System.out.println();
        for (String url : URLS) {
            CheckResult result = checkWithRetries(url);
            String status = result.ok ? "UP" : "DOWN";
            System.out.println(status + "  " + url);
            System.out.println("      " + result.reason);
        }
    }

    private CheckResult checkWithRetries(String url) {

        CheckResult lastResult = null;
        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            lastResult = checkOnce(url);
            if (lastResult.ok) return lastResult;
            if (i < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(3000L);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new CheckResult(false, "Interrupted while retrying");
                }
            }
        }

        return lastResult;
    }

    private CheckResult checkOnce(String url) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .header("User-Agent", "SigmaKEE-HealthCheck/1.0")
                    .build();
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();
            if (status == 503) return new CheckResult(false, "HTTP 503 Service Unavailable");
            String lowerBody = body.toLowerCase(Locale.ROOT);
            if (lowerBody.contains("service unavailable") || lowerBody.contains("temporarily unable to service your request")) return new CheckResult(false, "Service unavailable page detected");
            if (status < 200 || status >= 400) return new CheckResult(false, "Unexpected HTTP status: " + status);
            return new CheckResult(true, "OK HTTP " + status);
        }
        catch (IOException e) {
            return new CheckResult(false, "I/O error: " + e.getMessage());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CheckResult(false, "Interrupted");
        }
        catch (Exception e) {
            return new CheckResult(false, "Unexpected error: " + e.getMessage());
        }
    }

    private void sendDownEmail(List<String> downUrls, Map<String, String> failureReasons) {

        String subject = "[SigmaKEE Alert] Service unavailable";
        StringBuilder body = new StringBuilder();
        body.append("One or more SigmaKEE URLs appear to be down.\n\n");
        body.append("Time: ").append(Instant.now()).append(" UTC\n\n");
        for (String url : downUrls) {
            body.append("DOWN: ").append(url).append("\n");
            body.append("Reason: ").append(failureReasons.get(url)).append("\n\n");
        }
        body.append("This alert was generated by the SigmaKEE URL health checker.\n");
        sendEmail(subject, body.toString());
    }

    /********************************************************************
     * Sends a health checker email to all SigmaKEE admin users.
     * @param subject the email subject
     * @param body the plain text email body
     */
    private void sendEmail(String subject, String body) {

        KBmanager.getMgr().initializeOnce();
        String htmlBody =
                "<h2>SigmaKEE Server Health Check</h2>" +
                "<pre style='font-family:monospace;white-space:pre-wrap'>" +
                ValidationUtils.sanitizeString(body) +
                "</pre>" +
                "<hr>" +
                "<p style='font-size:12px;color:#666'>This email was generated automatically by SigmaKEE.</p>";
        EmailService emailService = new EmailService();
        boolean sent = emailService.sendAdminNotification(subject, htmlBody);
    }

    private record CheckResult(boolean ok, String reason) {
    }

    /********************************************************************
     * Prints command-line usage options for ServerHealthChecker.
     */
    private static void showHelp() {

        System.out.println("ServerHealthChecker:");
        System.out.println("  -h, --help          Show this help message");
        System.out.println("  -d, --dry-run       Check URLs and print results without sending email");
        System.out.println("  -c, --cron          Run normal check; sends email only if one or more URLs are down");
        System.out.println();
        System.out.println("If no argument is provided, --cron mode is used.");
    }

    /********************************************************************
     * Command line entry point for server health checks.
     * @param args command line arguments
     */
    public static void main(String[] args) {

        ServerHealthChecker checker = new ServerHealthChecker();
        if (args == null || args.length == 0) {
            checker.run();
            return;
        }
        switch (args[0]) {
            case "-h":
            case "--help":
                showHelp();
                break;
            case "-d":
            case "--dry-run":
                checker.dryRun();
                break;
            case "-c":
            case "--cron":
                checker.run();
                break;
            default:
                System.err.println("Unknown option: " + args[0]);
                showHelp();
                System.exit(1);
        }
    }
}