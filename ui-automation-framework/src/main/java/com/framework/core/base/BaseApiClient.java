package com.framework.core.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.framework.core.auth.AuthenticationManager;
import com.framework.core.auth.AuthenticationManager.UserRole;
import com.framework.core.config.FrameworkConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * BaseApiClient - Foundation for all API clients
 *
 * Provides:
 * - Pre-configured RestAssured with auth
 * - Common HTTP methods (GET, POST, PUT, DELETE, PATCH)
 * - Allure integration for API logging
 * - Response validation helpers
 */
@Slf4j
public abstract class BaseApiClient {

    @Autowired
    protected FrameworkConfig config;

    @Autowired
    protected AuthenticationManager authManager;

    protected static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Override to specify the base path for this API client.
     * Example: "/api/v1/orders"
     */
    protected abstract String getBasePath();

    /**
     * Override to specify required auth role.
     * Default is STANDARD_USER.
     */
    protected UserRole getRequiredRole() {
        return UserRole.STANDARD_USER;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REQUEST SPECIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates base request specification with auth and common headers.
     */
    protected RequestSpecification getRequestSpec() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
            .setBaseUri(config.getApiUrl())
            .setBasePath(getBasePath())
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .setConfig(RestAssuredConfig.config()
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory((cls, charset) -> objectMapper)));

        // Add auth header if not guest
        if (getRequiredRole() != UserRole.GUEST) {
            String token = authManager.getAuthToken(getRequiredRole());
            if (token != null) {
                builder.addHeader("Authorization", "Bearer " + token);
            }
        }

        // Add logging for debugging
        if (log.isDebugEnabled()) {
            builder.log(LogDetail.ALL);
        }

        return builder.build();
    }

    /**
     * Creates request spec without auth (for guest/public endpoints).
     */
    protected RequestSpecification getGuestRequestSpec() {
        return new RequestSpecBuilder()
            .setBaseUri(config.getApiUrl())
            .setBasePath(getBasePath())
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HTTP METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    protected Response get(String path) {
        log.debug("GET {}{}", getBasePath(), path);
        return RestAssured.given()
            .spec(getRequestSpec())
            .get(path);
    }

    protected Response get(String path, Map<String, ?> queryParams) {
        log.debug("GET {}{} with params: {}", getBasePath(), path, queryParams);
        return RestAssured.given()
            .spec(getRequestSpec())
            .queryParams(queryParams)
            .get(path);
    }

    protected Response post(String path, Object body) {
        log.debug("POST {}{}", getBasePath(), path);
        return RestAssured.given()
            .spec(getRequestSpec())
            .body(body)
            .post(path);
    }

    protected Response post(String path) {
        log.debug("POST {}{} (no body)", getBasePath(), path);
        return RestAssured.given()
            .spec(getRequestSpec())
            .post(path);
    }

    protected Response put(String path, Object body) {
        log.debug("PUT {}{}", getBasePath(), path);
        return RestAssured.given()
            .spec(getRequestSpec())
            .body(body)
            .put(path);
    }

    protected Response patch(String path, Object body) {
        log.debug("PATCH {}{}", getBasePath(), path);
        return RestAssured.given()
            .spec(getRequestSpec())
            .body(body)
            .patch(path);
    }

    protected Response delete(String path) {
        log.debug("DELETE {}{}", getBasePath(), path);
        return RestAssured.given()
            .spec(getRequestSpec())
            .delete(path);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESPONSE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates response status and returns body as specified type.
     */
    protected <T> T getAs(Response response, int expectedStatus, Class<T> responseType) {
        validateStatus(response, expectedStatus);
        return response.as(responseType);
    }

    /**
     * Validates response status is 200 and returns body.
     */
    protected <T> T getOkAs(Response response, Class<T> responseType) {
        return getAs(response, 200, responseType);
    }

    /**
     * Validates response status is 201 and returns body.
     */
    protected <T> T getCreatedAs(Response response, Class<T> responseType) {
        return getAs(response, 201, responseType);
    }

    /**
     * Validates response has expected status.
     */
    protected void validateStatus(Response response, int expectedStatus) {
        if (response.statusCode() != expectedStatus) {
            log.error("Expected status {} but got {}. Response: {}",
                expectedStatus, response.statusCode(), response.asString());
            throw new AssertionError(String.format(
                "Expected status %d but got %d. Response: %s",
                expectedStatus, response.statusCode(), response.asString()));
        }
    }

    /**
     * Checks if response is successful (2xx).
     */
    protected boolean isSuccessful(Response response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    /**
     * Extracts value from JSON path.
     */
    protected <T> T extractJsonPath(Response response, String path) {
        return response.jsonPath().get(path);
    }

    /**
     * Extracts string from JSON path.
     */
    protected String extractString(Response response, String path) {
        return response.jsonPath().getString(path);
    }

    /**
     * Extracts integer from JSON path.
     */
    protected Integer extractInt(Response response, String path) {
        return response.jsonPath().getInt(path);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Converts object to JSON string.
     */
    protected String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }

    /**
     * Converts JSON string to object.
     */
    protected <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
