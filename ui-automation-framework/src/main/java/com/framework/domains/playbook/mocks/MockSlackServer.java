package com.framework.domains.playbook.mocks;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * MockSlackServer - Mock Slack webhook server for testing
 *
 * Provides a local HTTP server that simulates Slack webhook endpoints.
 * Used to verify that playbook Slack integrations work correctly.
 *
 * Features:
 * - Captures all incoming webhook requests
 * - Configurable response behavior
 * - Request history for assertions
 * - Simulated failures for negative testing
 */
@Slf4j
public class MockSlackServer {

    private HttpServer server;
    private final int port;
    private final List<SlackMessage> receivedMessages = new CopyOnWriteArrayList<>();
    private boolean shouldFail = false;
    private int failureStatusCode = 500;
    private String failureMessage = "Internal Server Error";
    private int responseDelayMs = 0;

    public MockSlackServer(int port) {
        this.port = port;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SERVER LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Starts the mock server.
     */
    public void start() throws IOException {
        log.info("Starting Mock Slack Server on port {}", port);

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/webhook", new SlackWebhookHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        log.info("Mock Slack Server started at http://localhost:{}/webhook", port);
    }

    /**
     * Stops the mock server.
     */
    public void stop() {
        if (server != null) {
            log.info("Stopping Mock Slack Server");
            server.stop(0);
            server = null;
        }
    }

    /**
     * Gets the webhook URL for this mock server.
     */
    public String getWebhookUrl() {
        return "http://localhost:" + port + "/webhook";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configures the server to return success responses.
     */
    public MockSlackServer configureSuccess() {
        this.shouldFail = false;
        return this;
    }

    /**
     * Configures the server to return failure responses.
     */
    public MockSlackServer configureFailure(int statusCode, String message) {
        this.shouldFail = true;
        this.failureStatusCode = statusCode;
        this.failureMessage = message;
        return this;
    }

    /**
     * Configures response delay for timeout testing.
     */
    public MockSlackServer configureDelay(int delayMs) {
        this.responseDelayMs = delayMs;
        return this;
    }

    /**
     * Resets the server to default success configuration.
     */
    public MockSlackServer reset() {
        this.shouldFail = false;
        this.failureStatusCode = 500;
        this.failureMessage = "Internal Server Error";
        this.responseDelayMs = 0;
        this.receivedMessages.clear();
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MESSAGE RETRIEVAL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets all received messages.
     */
    public List<SlackMessage> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }

    /**
     * Gets the most recent message.
     */
    public SlackMessage getLastMessage() {
        if (receivedMessages.isEmpty()) {
            return null;
        }
        return receivedMessages.get(receivedMessages.size() - 1);
    }

    /**
     * Gets message count.
     */
    public int getMessageCount() {
        return receivedMessages.size();
    }

    /**
     * Clears received messages.
     */
    public void clearMessages() {
        receivedMessages.clear();
    }

    /**
     * Waits for a message to be received.
     */
    public SlackMessage waitForMessage(int timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int initialCount = receivedMessages.size();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (receivedMessages.size() > initialCount) {
                return receivedMessages.get(receivedMessages.size() - 1);
            }
            Thread.sleep(100);
        }

        throw new RuntimeException("No message received within " + timeoutMs + "ms");
    }

    /**
     * Waits for a specific number of messages.
     */
    public void waitForMessages(int count, int timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (receivedMessages.size() >= count) {
                return;
            }
            Thread.sleep(100);
        }

        throw new RuntimeException(String.format(
            "Expected %d messages but received %d within %dms",
            count, receivedMessages.size(), timeoutMs));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSERTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Asserts that a message containing the text was received.
     */
    public void assertMessageContains(String text) {
        boolean found = receivedMessages.stream()
            .anyMatch(m -> m.getText() != null && m.getText().contains(text));

        if (!found) {
            throw new AssertionError("No message containing '" + text + "' was received. " +
                "Received messages: " + receivedMessages);
        }
    }

    /**
     * Asserts that a message was sent to a specific channel.
     */
    public void assertMessageToChannel(String channel) {
        boolean found = receivedMessages.stream()
            .anyMatch(m -> channel.equals(m.getChannel()));

        if (!found) {
            throw new AssertionError("No message to channel '" + channel + "' was received");
        }
    }

    /**
     * Asserts no messages were received.
     */
    public void assertNoMessages() {
        if (!receivedMessages.isEmpty()) {
            throw new AssertionError("Expected no messages but received " + receivedMessages.size());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HANDLER
    // ═══════════════════════════════════════════════════════════════════════════

    private class SlackWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            log.debug("Received webhook request");

            try {
                // Read request body
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                log.debug("Webhook body: {}", body);

                // Parse the Slack message
                SlackMessage message = parseSlackMessage(body);
                message.setTimestamp(System.currentTimeMillis());
                message.setRawBody(body);
                receivedMessages.add(message);

                log.info("Received Slack message: channel={}, text={}",
                    message.getChannel(), truncate(message.getText(), 50));

                // Apply configured delay
                if (responseDelayMs > 0) {
                    Thread.sleep(responseDelayMs);
                }

                // Send response
                if (shouldFail) {
                    sendResponse(exchange, failureStatusCode, failureMessage);
                } else {
                    sendResponse(exchange, 200, "ok");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendResponse(exchange, 500, "Interrupted");
            } catch (Exception e) {
                log.error("Error handling webhook", e);
                sendResponse(exchange, 500, e.getMessage());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private SlackMessage parseSlackMessage(String body) {
            SlackMessage message = new SlackMessage();

            // Simple JSON parsing (for production use a proper JSON library)
            if (body.contains("\"text\"")) {
                String text = extractJsonValue(body, "text");
                message.setText(text);
            }
            if (body.contains("\"channel\"")) {
                String channel = extractJsonValue(body, "channel");
                message.setChannel(channel);
            }
            if (body.contains("\"username\"")) {
                String username = extractJsonValue(body, "username");
                message.setUsername(username);
            }
            if (body.contains("\"icon_emoji\"")) {
                String iconEmoji = extractJsonValue(body, "icon_emoji");
                message.setIconEmoji(iconEmoji);
            }

            return message;
        }

        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\"\\s*:\\s*\"";
            int start = json.indexOf(pattern);
            if (start == -1) return null;

            start += pattern.length();
            int end = json.indexOf("\"", start);
            if (end == -1) return null;

            return json.substring(start, end);
        }

        private String truncate(String text, int maxLength) {
            if (text == null) return null;
            if (text.length() <= maxLength) return text;
            return text.substring(0, maxLength) + "...";
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASS
    // ═══════════════════════════════════════════════════════════════════════════

    @Data
    public static class SlackMessage {
        private String text;
        private String channel;
        private String username;
        private String iconEmoji;
        private long timestamp;
        private String rawBody;

        // For attachments (if needed)
        private List<Attachment> attachments;

        @Data
        public static class Attachment {
            private String fallback;
            private String color;
            private String title;
            private String text;
            private List<Field> fields;
        }

        @Data
        public static class Field {
            private String title;
            private String value;
            private boolean shortField;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates and starts a mock server on a random available port.
     */
    public static MockSlackServer createAndStart() throws IOException {
        return createAndStart(findAvailablePort());
    }

    /**
     * Creates and starts a mock server on the specified port.
     */
    public static MockSlackServer createAndStart(int port) throws IOException {
        MockSlackServer server = new MockSlackServer(port);
        server.start();
        return server;
    }

    private static int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8089; // Fallback port
        }
    }
}
