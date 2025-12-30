package com.articulate.sigma.nlg;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public class OllamaClient {

    private final String baseUrl;   // e.g., "http://127.0.0.1:11434"
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public OllamaClient(String baseUrl) {
        this(baseUrl, 10000, 40000); // tighter defaults to avoid hangs
    }

    public OllamaClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
        if (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        this.baseUrl = baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /** Backwards-compatible convenience wrapper. */
    public String generate(String model, String prompt) throws IOException {
        return generate(model, prompt, null, false);
    }

    /**
     * One-shot text generation via /api/generate (no conversation state).
     * @param model  e.g., "llama3.2"
     * @param prompt your prompt
     * @return generated text (response field) or raw JSON if parsing fails
     * @throws IOException on network errors
     */
    /**
     * One-shot text generation via /api/generate (no conversation state).
     *
     * @param model     e.g., "llama3.2"
     * @param prompt    prompt
     * @param options   Ollama options (e.g., temperature, top_p, num_predict, seed)
     * @param requestJsonFormat  if true, requests JSON-mode output when supported by Ollama/model
     */
    public String generate(String model, String prompt, Map<String,Object> options, boolean requestJsonFormat) throws IOException {
        String endpoint = baseUrl + "/api/generate";

        StringBuilder body = new StringBuilder();
        body.append("{")
                .append("\"model\":\"").append(escapeJson(model)).append("\",")
                .append("\"prompt\":\"").append(escapeJson(prompt)).append("\",")
                .append("\"stream\":false");

        // Request JSON mode when supported
        if (requestJsonFormat) body.append(",\"format\":\"json\"");

        // Options block (temperature, seed, num_predict, etc.)
        if (options != null && !options.isEmpty()) {
            body.append(",\"options\":{");
            boolean first = true;
            for (Map.Entry<String,Object> e : options.entrySet()) {
                if (!first) body.append(",");
                first = false;
                body.append("\"").append(escapeJson(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v == null) body.append("null");
                else if (v instanceof Number || v instanceof Boolean) body.append(v.toString());
                else body.append("\"").append(escapeJson(String.valueOf(v))).append("\"");
            }
            body.append("}");
        }

        body.append("}");

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Connection", "close");
        System.setProperty("java.net.useSystemProxies", "false");

        byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bodyBytes.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String json = readAll(is);

        // Extract "response"
        String marker = "\"response\":\"";
        int i = json.indexOf(marker);
        if (i >= 0) {
            int start = i + marker.length();
            int end = findJsonStringEnd(json, start);
            if (end > start) {
                String raw = json.substring(start, end);
                return unescapeJsonString(raw);
            }
        }
        return json;
    }

    public String chat(String model, String prompt, Map<String,Object> options, boolean jsonMode) throws IOException {
        String endpoint = baseUrl + "/api/chat";

        StringBuilder body = new StringBuilder();
        body.append("{")
                .append("\"model\":\"").append(escapeJson(model)).append("\",")
                .append("\"messages\":[")
                .append("{\"role\":\"user\",\"content\":\"").append(escapeJson(prompt)).append("\"}")
                .append("],")
                .append("\"stream\":false");

        if (jsonMode) body.append(",\"format\":\"json\"");

        if (options != null && !options.isEmpty()) {
            body.append(",\"options\":{");
            boolean first = true;
            for (Map.Entry<String,Object> e : options.entrySet()) {
                if (!first) body.append(",");
                first = false;
                body.append("\"").append(escapeJson(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v == null) body.append("null");
                else if (v instanceof Number || v instanceof Boolean) body.append(v.toString());
                else body.append("\"").append(escapeJson(String.valueOf(v))).append("\"");
            }
            body.append("}");
        }

        body.append("}");

        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Connection", "close");

        byte[] bodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(bodyBytes.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
            os.flush();
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String json = readAll(is);

        // Extract message.content (chat response)
        String marker = "\"content\":\"";
        int i = json.indexOf(marker);
        if (i >= 0) {
            int start = i + marker.length();
            int end = findJsonStringEnd(json, start);
            if (end > start) return unescapeJsonString(json.substring(start, end));
        }
        return json;
    }

    // ===== Helpers =====

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"':  b.append("\\\""); break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                case '\b': b.append("\\b");  break;
                case '\f': b.append("\\f");  break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.toString();
    }

    private static String unescapeJsonString(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case 'n': out.append('\n'); i += 2; break;
                    case 'r': out.append('\r'); i += 2; break;
                    case 't': out.append('\t'); i += 2; break;
                    case 'b': out.append('\b'); i += 2; break;
                    case 'f': out.append('\f'); i += 2; break;
                    case '\\': out.append('\\'); i += 2; break;
                    case '"': out.append('"'); i += 2; break;
                    case 'u':
                        if (i + 6 <= s.length()) {
                            String hex = s.substring(i + 2, i + 6);
                            try { out.append((char) Integer.parseInt(hex, 16)); }
                            catch (NumberFormatException e) { out.append("\\u").append(hex); }
                            i += 6;
                        } else { out.append("\\u"); i += 2; }
                        break;
                    default: out.append(n); i += 2; break;
                }
            } else {
                out.append(c); i++;
            }
        }
        return out.toString();
    }

    // find the true end of a JSON string value (accounts for escaped quotes)
    private static int findJsonStringEnd(String json, int start) {
        boolean esc = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') return i;
        }
        return -1;
    }

    public boolean isHealthy() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/api/version"); // lightweight endpoint
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            int code = conn.getResponseCode();
            return (200 <= code && code < 300);
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ===== Demo CLI =====
    // Usage:
    //   java -Djava.net.preferIPv4Stack=true com.articulate.sigma.nlg.OllamaClient "http://127.0.0.1:11434" "llama3.2" "Explain SUO-KIF in one sentence."
    public static void main(String[] args) throws Exception {
        System.err.println("MAIN start, args=" + Arrays.toString(args));
        System.err.println("Loaded from: " +
                OllamaClient.class.getProtectionDomain().getCodeSource().getLocation());

        System.setProperty("java.net.preferIPv4Stack", "true"); // prefer 127.0.0.1 over ::1
        if (args.length < 3) {
            System.err.println("Usage: java com.articulate.sigma.nlg.OllamaClient <baseUrl> <model> <prompt...>");
            System.err.println("Example: java com.articulate.sigma.nlg.OllamaClient http://127.0.0.1:11434 llama3.2 Hi!");
            System.exit(2);
        }
        String base = args[0];
        String model = args[1];
        String prompt = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        OllamaClient client = new OllamaClient(base);
        System.err.println("Calling generate...");
        String text;
        try {
            text = client.generate(model, prompt);
        }catch (Throwable t) {
            t.printStackTrace(); return;
        }
        System.out.println("Answer: " + text);
    }
}
