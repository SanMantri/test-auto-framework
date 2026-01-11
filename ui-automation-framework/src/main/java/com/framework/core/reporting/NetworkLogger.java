package com.framework.core.reporting;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import io.qameta.allure.Allure;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NetworkLogger - Captures network requests/responses for debugging
 *
 * Attaches to a Page and records all network activity.
 * Can filter to relevant domains/paths.
 * Exports to Allure report as attachment.
 */
@Slf4j
public class NetworkLogger {

    private final List<NetworkEntry> entries = new ArrayList<>();
    private final List<String> filterPatterns = new ArrayList<>();
    private boolean captureRequestBody = true;
    private boolean captureResponseBody = true;
    private int maxBodyLength = 10000;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Add URL pattern to filter (only matching URLs are captured).
     * If no patterns added, all URLs are captured.
     */
    public NetworkLogger filterUrl(String pattern) {
        filterPatterns.add(pattern);
        return this;
    }

    public NetworkLogger setCaptureRequestBody(boolean capture) {
        this.captureRequestBody = capture;
        return this;
    }

    public NetworkLogger setCaptureResponseBody(boolean capture) {
        this.captureResponseBody = capture;
        return this;
    }

    public NetworkLogger setMaxBodyLength(int length) {
        this.maxBodyLength = length;
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTACHMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Attaches to a Playwright Page to capture network activity.
     */
    public void attachToPage(Page page) {
        page.onRequest(this::onRequest);
        page.onResponse(this::onResponse);
        page.onRequestFailed(this::onRequestFailed);
        log.debug("NetworkLogger attached to page");
    }

    private void onRequest(Request request) {
        if (!shouldCapture(request.url())) {
            return;
        }

        NetworkEntry entry = new NetworkEntry();
        entry.setTimestamp(Instant.now());
        entry.setMethod(request.method());
        entry.setUrl(request.url());
        entry.setResourceType(request.resourceType());

        if (captureRequestBody && request.postData() != null) {
            entry.setRequestBody(truncate(request.postData(), maxBodyLength));
        }

        // Store headers
        entry.setRequestHeaders(request.headers().toString());

        entries.add(entry);
        log.trace("Request: {} {}", request.method(), request.url());
    }

    private void onResponse(Response response) {
        if (!shouldCapture(response.url())) {
            return;
        }

        // Find matching entry
        NetworkEntry entry = findEntry(response.url(), response.request().method());
        if (entry != null) {
            entry.setStatus(response.status());
            entry.setStatusText(response.statusText());
            entry.setResponseHeaders(response.headers().toString());
            entry.setDuration(java.time.Duration.between(entry.getTimestamp(), Instant.now()).toMillis());

            if (captureResponseBody) {
                try {
                    String body = response.text();
                    entry.setResponseBody(truncate(body, maxBodyLength));
                } catch (Exception e) {
                    entry.setResponseBody("[Binary or unreadable content]");
                }
            }

            log.trace("Response: {} {} - {}", response.request().method(), response.url(), response.status());
        }
    }

    private void onRequestFailed(Request request) {
        if (!shouldCapture(request.url())) {
            return;
        }

        NetworkEntry entry = findEntry(request.url(), request.method());
        if (entry != null) {
            entry.setStatus(-1);
            entry.setStatusText("FAILED: " + request.failure());
            log.warn("Request failed: {} {} - {}", request.method(), request.url(), request.failure());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Attaches network log to Allure report.
     */
    public void attachToAllureReport() {
        if (entries.isEmpty()) {
            return;
        }

        String report = generateReport();
        Allure.addAttachment("Network Log", "text/plain", report);
        log.debug("Network log attached to Allure ({} entries)", entries.size());
    }

    /**
     * Generates human-readable report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("                     NETWORK LOG                                \n");
        sb.append("═══════════════════════════════════════════════════════════════\n\n");

        for (int i = 0; i < entries.size(); i++) {
            NetworkEntry entry = entries.get(i);
            sb.append(String.format("[%d] %s %s\n", i + 1, entry.getMethod(), entry.getUrl()));
            sb.append(String.format("    Status: %d %s\n", entry.getStatus(), entry.getStatusText()));
            sb.append(String.format("    Duration: %dms\n", entry.getDuration()));

            if (entry.getRequestBody() != null && !entry.getRequestBody().isEmpty()) {
                sb.append("    Request Body:\n");
                sb.append("    ").append(entry.getRequestBody().replace("\n", "\n    ")).append("\n");
            }

            if (entry.getResponseBody() != null && !entry.getResponseBody().isEmpty()) {
                sb.append("    Response Body:\n");
                sb.append("    ").append(entry.getResponseBody().replace("\n", "\n    ")).append("\n");
            }

            sb.append("\n");
        }

        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append(String.format("Total requests: %d\n", entries.size()));
        sb.append(String.format("Failed requests: %d\n", getFailedCount()));
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    public List<NetworkEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public List<NetworkEntry> getEntriesForUrl(String urlPattern) {
        return entries.stream()
            .filter(e -> e.getUrl().contains(urlPattern))
            .collect(Collectors.toList());
    }

    public List<NetworkEntry> getFailedRequests() {
        return entries.stream()
            .filter(e -> e.getStatus() < 0 || e.getStatus() >= 400)
            .collect(Collectors.toList());
    }

    public int getFailedCount() {
        return (int) entries.stream()
            .filter(e -> e.getStatus() < 0 || e.getStatus() >= 400)
            .count();
    }

    public void clear() {
        entries.clear();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private boolean shouldCapture(String url) {
        if (filterPatterns.isEmpty()) {
            return true;
        }
        return filterPatterns.stream().anyMatch(url::contains);
    }

    private NetworkEntry findEntry(String url, String method) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            NetworkEntry entry = entries.get(i);
            if (entry.getUrl().equals(url) && entry.getMethod().equals(method)) {
                return entry;
            }
        }
        return null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "... [truncated]";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASS
    // ═══════════════════════════════════════════════════════════════════════════

    @Data
    public static class NetworkEntry {
        private Instant timestamp;
        private String method;
        private String url;
        private String resourceType;
        private String requestHeaders;
        private String requestBody;
        private int status;
        private String statusText;
        private String responseHeaders;
        private String responseBody;
        private long duration;
    }
}
