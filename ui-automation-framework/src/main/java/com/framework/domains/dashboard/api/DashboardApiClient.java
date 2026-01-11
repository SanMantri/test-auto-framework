package com.framework.domains.dashboard.api;

import com.framework.core.base.BaseApiClient;
import com.framework.domains.dashboard.models.Dashboard;
import com.framework.domains.dashboard.models.Widget;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DashboardApiClient - API operations for dashboards
 *
 * Handles dashboard CRUD, widget management, and data retrieval.
 * Used for test setup and verification of dashboard data accuracy.
 */
@Slf4j
@Component
public class DashboardApiClient extends BaseApiClient {

    @Override
    protected String getBasePath() {
        return "/api/v1/dashboards";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new dashboard.
     */
    public Dashboard createDashboard(Dashboard dashboard) {
        log.info("Creating dashboard: {}", dashboard.getName());
        Response response = post("", dashboard);
        return getCreatedAs(response, Dashboard.class);
    }

    /**
     * Creates a dashboard from template.
     */
    public Dashboard createFromTemplate(String templateId, String name) {
        log.info("Creating dashboard from template: {}", templateId);
        Response response = post("/from-template", Map.of(
            "templateId", templateId,
            "name", name
        ));
        return getCreatedAs(response, Dashboard.class);
    }

    /**
     * Gets a dashboard by ID.
     */
    public Dashboard getDashboard(String dashboardId) {
        log.info("Getting dashboard: {}", dashboardId);
        Response response = get("/" + dashboardId);
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Gets all dashboards for the current user.
     */
    public List<Dashboard> getMyDashboards() {
        log.info("Getting my dashboards");
        Response response = get("");
        return List.of(getOkAs(response, Dashboard[].class));
    }

    /**
     * Gets dashboards with filters.
     */
    public List<Dashboard> getDashboards(Map<String, Object> filters) {
        log.info("Getting dashboards with filters: {}", filters);
        Response response = get("", filters);
        return List.of(getOkAs(response, Dashboard[].class));
    }

    /**
     * Updates a dashboard.
     */
    public Dashboard updateDashboard(String dashboardId, Dashboard dashboard) {
        log.info("Updating dashboard: {}", dashboardId);
        Response response = put("/" + dashboardId, dashboard);
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Deletes a dashboard.
     */
    public void deleteDashboard(String dashboardId) {
        log.info("Deleting dashboard: {}", dashboardId);
        Response response = delete("/" + dashboardId);
        assertNoContent(response);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD STATUS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Publishes a dashboard.
     */
    public Dashboard publishDashboard(String dashboardId) {
        log.info("Publishing dashboard: {}", dashboardId);
        Response response = post("/" + dashboardId + "/publish");
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Archives a dashboard.
     */
    public Dashboard archiveDashboard(String dashboardId) {
        log.info("Archiving dashboard: {}", dashboardId);
        Response response = post("/" + dashboardId + "/archive");
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Duplicates a dashboard.
     */
    public Dashboard duplicateDashboard(String dashboardId, String newName) {
        log.info("Duplicating dashboard: {} as {}", dashboardId, newName);
        Response response = post("/" + dashboardId + "/duplicate", Map.of(
            "name", newName
        ));
        return getCreatedAs(response, Dashboard.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Adds a widget to dashboard.
     */
    public Dashboard addWidget(String dashboardId, Widget widget) {
        log.info("Adding widget to dashboard: {}", dashboardId);
        Response response = post("/" + dashboardId + "/widgets", widget);
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Updates a widget.
     */
    public Dashboard updateWidget(String dashboardId, String widgetId, Widget widget) {
        log.info("Updating widget {} in dashboard: {}", widgetId, dashboardId);
        Response response = put("/" + dashboardId + "/widgets/" + widgetId, widget);
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Removes a widget.
     */
    public Dashboard removeWidget(String dashboardId, String widgetId) {
        log.info("Removing widget {} from dashboard: {}", widgetId, dashboardId);
        Response response = delete("/" + dashboardId + "/widgets/" + widgetId);
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Updates widget positions (for drag-drop reordering).
     */
    public Dashboard updateWidgetPositions(String dashboardId, List<WidgetPositionUpdate> positions) {
        log.info("Updating widget positions in dashboard: {}", dashboardId);
        Response response = put("/" + dashboardId + "/widgets/positions", Map.of(
            "positions", positions
        ));
        return getOkAs(response, Dashboard.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET DATA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets data for a specific widget.
     */
    public WidgetData getWidgetData(String dashboardId, String widgetId) {
        log.info("Getting data for widget {} in dashboard: {}", widgetId, dashboardId);
        Response response = get("/" + dashboardId + "/widgets/" + widgetId + "/data");
        return getOkAs(response, WidgetData.class);
    }

    /**
     * Gets data for a widget with filters.
     */
    public WidgetData getWidgetData(String dashboardId, String widgetId, Map<String, Object> filters) {
        log.info("Getting filtered data for widget {} in dashboard: {}", widgetId, dashboardId);
        Response response = get("/" + dashboardId + "/widgets/" + widgetId + "/data", filters);
        return getOkAs(response, WidgetData.class);
    }

    /**
     * Refreshes all widget data in a dashboard.
     */
    public Dashboard refreshDashboard(String dashboardId) {
        log.info("Refreshing dashboard: {}", dashboardId);
        Response response = post("/" + dashboardId + "/refresh");
        return getOkAs(response, Dashboard.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KPI DATA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets KPI values for verification.
     */
    public KpiValue getKpiValue(String dashboardId, String widgetId) {
        log.info("Getting KPI value for widget {} in dashboard: {}", widgetId, dashboardId);
        Response response = get("/" + dashboardId + "/widgets/" + widgetId + "/kpi");
        return getOkAs(response, KpiValue.class);
    }

    /**
     * Gets expected KPI value from source data (for verification).
     */
    public KpiValue calculateExpectedKpi(String query, String timeRange) {
        log.info("Calculating expected KPI for query");
        Response response = post("/calculate-kpi", Map.of(
            "query", query,
            "timeRange", timeRange
        ));
        return getOkAs(response, KpiValue.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SHARING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Shares dashboard with users.
     */
    public Dashboard shareWith(String dashboardId, List<String> userIds, String permission) {
        log.info("Sharing dashboard {} with users: {}", dashboardId, userIds);
        Response response = post("/" + dashboardId + "/share", Map.of(
            "userIds", userIds,
            "permission", permission
        ));
        return getOkAs(response, Dashboard.class);
    }

    /**
     * Gets a public share link.
     */
    public ShareLink getShareLink(String dashboardId) {
        log.info("Getting share link for dashboard: {}", dashboardId);
        Response response = get("/" + dashboardId + "/share-link");
        return getOkAs(response, ShareLink.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Exports dashboard as PDF.
     */
    public ExportResult exportAsPdf(String dashboardId) {
        log.info("Exporting dashboard as PDF: {}", dashboardId);
        Response response = post("/" + dashboardId + "/export/pdf");
        return getOkAs(response, ExportResult.class);
    }

    /**
     * Exports dashboard data as CSV.
     */
    public ExportResult exportAsCsv(String dashboardId) {
        log.info("Exporting dashboard as CSV: {}", dashboardId);
        Response response = post("/" + dashboardId + "/export/csv");
        return getOkAs(response, ExportResult.class);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    public static class WidgetPositionUpdate {
        public String widgetId;
        public int x;
        public int y;
        public int width;
        public int height;
    }

    public static class WidgetData {
        public String widgetId;
        public List<Map<String, Object>> data;
        public Map<String, Object> metadata;
        public long dataTimestamp;
        public int rowCount;
    }

    public static class KpiValue {
        public BigDecimal value;
        public BigDecimal previousValue;
        public BigDecimal target;
        public BigDecimal changePercent;
        public String trend;
        public String formattedValue;
    }

    public static class ShareLink {
        public String url;
        public String expiresAt;
        public boolean passwordProtected;
    }

    public static class ExportResult {
        public String downloadUrl;
        public String fileName;
        public String format;
        public long fileSize;
    }
}
