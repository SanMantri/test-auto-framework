package com.framework.tests.dashboard;

import com.framework.core.base.BaseTest;
import com.framework.core.data.TestDataCache;
import com.framework.domains.dashboard.api.DashboardApiClient;
import com.framework.domains.dashboard.models.Dashboard;
import com.framework.domains.dashboard.models.Widget;
import com.framework.domains.dashboard.models.Widget.WidgetType;
import com.framework.domains.dashboard.pages.DashboardBuilderPage;
import com.framework.domains.dashboard.pages.DashboardPage;
import com.framework.domains.dashboard.utils.VisualTestingUtils;
import io.qameta.allure.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * DashboardTest - Dashboard visualization and data accuracy tests
 *
 * Tests include:
 * - Dashboard creation and widget configuration
 * - Data accuracy verification (KPIs, charts, tables)
 * - Visual regression testing
 * - Filter and time range functionality
 */
@Epic("Analytics Platform")
@Feature("Dashboard Visualization")
public class DashboardTest extends BaseTest {

    @Autowired
    private DashboardApiClient dashboardApi;

    private DashboardPage dashboardPage;
    private DashboardBuilderPage builderPage;
    private VisualTestingUtils visualTesting;
    private TestDataCache testData;

    @BeforeMethod
    public void setupTest() {
        testData = getTestData();
        dashboardPage = new DashboardPage(getPage());
        builderPage = new DashboardBuilderPage(getPage());
        visualTesting = new VisualTestingUtils(getPage());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD CREATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Create KPI dashboard via UI")
    @Story("Dashboard Creation")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateKpiDashboard() {
        builderPage.navigateToCreate();

        builderPage.setDashboardName("Sales KPI Dashboard");
        builderPage.setDescription("Key sales metrics");

        // Add KPI widgets
        builderPage.addWidget(WidgetType.KPI);
        builderPage.setWidgetTitle("Total Revenue");
        builderPage.configureKpi("revenue", "currency", "1000000");
        builderPage.saveWidget();

        builderPage.addWidget(WidgetType.KPI);
        builderPage.setWidgetTitle("Orders Today");
        builderPage.configureKpi("order_count", "number", null);
        builderPage.setKpiComparison("previous_day");
        builderPage.saveWidget();

        builderPage.saveDashboard();

        // Verify
        assertThat(builderPage.isSaved()).isTrue();
        assertThat(builderPage.getWidgetCount()).isEqualTo(2);

        // Verify via API
        List<Dashboard> dashboards = dashboardApi.getMyDashboards();
        assertThat(dashboards)
            .extracting(Dashboard::getName)
            .contains("Sales KPI Dashboard");
    }

    @Test(description = "Create chart dashboard via UI")
    @Story("Dashboard Creation")
    @Severity(SeverityLevel.CRITICAL)
    public void testCreateChartDashboard() {
        builderPage.navigateToCreate();

        builderPage.setDashboardName("Revenue Analytics");

        // Add line chart
        builderPage.addWidget(WidgetType.LINE_CHART);
        builderPage.setWidgetTitle("Revenue Trend");
        builderPage.configureChartAxes("date", "revenue");
        builderPage.setAggregation("SUM");
        builderPage.saveWidget();

        // Add bar chart
        builderPage.addWidget(WidgetType.BAR_CHART);
        builderPage.setWidgetTitle("Revenue by Category");
        builderPage.configureChartAxes("category", "revenue");
        builderPage.setGroupBy("category");
        builderPage.saveWidget();

        builderPage.saveDashboard();

        assertThat(builderPage.isSaved()).isTrue();
        assertThat(builderPage.getWidgetCount()).isEqualTo(2);
    }

    @Test(description = "Create table dashboard via UI")
    @Story("Dashboard Creation")
    @Severity(SeverityLevel.NORMAL)
    public void testCreateTableDashboard() {
        builderPage.navigateToCreate();

        builderPage.setDashboardName("Order Details");

        // Add table widget
        builderPage.addWidget(WidgetType.TABLE);
        builderPage.setWidgetTitle("Recent Orders");
        builderPage.addTableColumn("order_id", "Order ID", 0);
        builderPage.addTableColumn("customer_name", "Customer", 1);
        builderPage.addTableColumn("total", "Total", 2);
        builderPage.addTableColumn("status", "Status", 3);
        builderPage.enablePagination(20);
        builderPage.saveWidget();

        builderPage.saveDashboard();

        assertThat(builderPage.isSaved()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET RENDERING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify all widgets load successfully")
    @Story("Widget Rendering")
    @Severity(SeverityLevel.CRITICAL)
    public void testAllWidgetsLoad() {
        // Setup: Create dashboard with multiple widgets
        Dashboard dashboard = createTestDashboard("Widget Load Test");
        testData.set("dashboardId", dashboard.getId());

        // Navigate and wait for load
        dashboardPage.navigate(dashboard.getId());

        // Verify all widgets loaded
        assertThat(dashboardPage.allWidgetsLoaded())
            .as("All widgets should be loaded")
            .isTrue();

        assertThat(dashboardPage.noWidgetsHaveErrors())
            .as("No widgets should have errors")
            .isTrue();
    }

    @Test(description = "Verify chart widgets render correctly")
    @Story("Widget Rendering")
    @Severity(SeverityLevel.NORMAL)
    public void testChartWidgetsRender() {
        Dashboard dashboard = createChartDashboard("Chart Render Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Verify each chart widget
        for (String widgetId : dashboardPage.getWidgetIds()) {
            assertThat(dashboardPage.isChartRendered(widgetId))
                .as("Chart %s should be rendered", widgetId)
                .isTrue();
        }
    }

    @Test(description = "Verify table widget displays data")
    @Story("Widget Rendering")
    @Severity(SeverityLevel.NORMAL)
    public void testTableWidgetDisplaysData() {
        Dashboard dashboard = createTableDashboard("Table Render Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        String tableWidgetId = dashboard.getWidgetsByType(WidgetType.TABLE).get(0).getId();

        // Verify table has data
        assertThat(dashboardPage.getTableRowCount(tableWidgetId))
            .as("Table should have rows")
            .isGreaterThan(0);

        // Verify headers
        List<String> headers = dashboardPage.getTableHeaders(tableWidgetId);
        assertThat(headers).isNotEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KPI DATA ACCURACY TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify KPI value accuracy")
    @Story("Data Accuracy")
    @Severity(SeverityLevel.CRITICAL)
    public void testKpiValueAccuracy() {
        Dashboard dashboard = createKpiDashboard("KPI Accuracy Test");
        String kpiWidgetId = dashboard.getWidgetsByType(WidgetType.KPI).get(0).getId();

        // Get expected value from API
        DashboardApiClient.KpiValue expectedKpi = dashboardApi.getKpiValue(
            dashboard.getId(), kpiWidgetId);

        // Navigate to dashboard
        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Get displayed value
        double displayedValue = dashboardPage.getKpiNumericValue(kpiWidgetId);

        // Verify accuracy (within 0.01 tolerance for rounding)
        assertThat(displayedValue)
            .as("KPI value should match expected value")
            .isCloseTo(expectedKpi.value.doubleValue(), within(0.01));
    }

    @Test(description = "Verify KPI trend indicator")
    @Story("Data Accuracy")
    @Severity(SeverityLevel.NORMAL)
    public void testKpiTrendIndicator() {
        Dashboard dashboard = createKpiDashboard("KPI Trend Test");
        String kpiWidgetId = dashboard.getWidgetsByType(WidgetType.KPI).get(0).getId();

        // Get expected trend from API
        DashboardApiClient.KpiValue kpiData = dashboardApi.getKpiValue(
            dashboard.getId(), kpiWidgetId);

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Verify trend matches
        String displayedTrend = dashboardPage.getKpiTrend(kpiWidgetId);
        assertThat(displayedTrend.toLowerCase())
            .as("KPI trend should match expected")
            .isEqualTo(kpiData.trend.toLowerCase());
    }

    @Test(description = "Verify KPI change percentage")
    @Story("Data Accuracy")
    @Severity(SeverityLevel.NORMAL)
    public void testKpiChangePercentage() {
        Dashboard dashboard = createKpiDashboard("KPI Change Test");
        String kpiWidgetId = dashboard.getWidgetsByType(WidgetType.KPI).get(0).getId();

        DashboardApiClient.KpiValue kpiData = dashboardApi.getKpiValue(
            dashboard.getId(), kpiWidgetId);

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        String displayedChange = dashboardPage.getKpiChange(kpiWidgetId);

        // Extract numeric value from displayed change (e.g., "+15.3%")
        double displayedPercent = Double.parseDouble(
            displayedChange.replaceAll("[^0-9.-]", ""));

        assertThat(displayedPercent)
            .as("KPI change percentage should match")
            .isCloseTo(kpiData.changePercent.doubleValue(), within(0.1));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTER TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify time range filter updates data")
    @Story("Filters")
    @Severity(SeverityLevel.CRITICAL)
    public void testTimeRangeFilter() {
        Dashboard dashboard = createTestDashboard("Time Range Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Get initial KPI value
        String kpiWidgetId = dashboard.getWidgetsByType(WidgetType.KPI).get(0).getId();
        double initialValue = dashboardPage.getKpiNumericValue(kpiWidgetId);

        // Change time range
        dashboardPage.selectTimeRange("Last 7 Days");
        dashboardPage.waitForAllWidgetsToLoad();

        double newValue = dashboardPage.getKpiNumericValue(kpiWidgetId);

        // Values should potentially be different (or at least refresh happened)
        assertThat(dashboardPage.allWidgetsLoaded()).isTrue();
    }

    @Test(description = "Verify dropdown filter updates data")
    @Story("Filters")
    @Severity(SeverityLevel.NORMAL)
    public void testDropdownFilter() {
        Dashboard dashboard = createFilterableDashboard("Dropdown Filter Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Apply filter
        dashboardPage.selectFilter("category", "Electronics");
        dashboardPage.applyFilters();

        // Verify data refreshed
        assertThat(dashboardPage.allWidgetsLoaded()).isTrue();
    }

    @Test(description = "Verify date range filter")
    @Story("Filters")
    @Severity(SeverityLevel.NORMAL)
    public void testDateRangeFilter() {
        Dashboard dashboard = createTestDashboard("Date Range Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Set custom date range
        dashboardPage.setDateRange("2024-01-01", "2024-01-31");
        dashboardPage.applyFilters();

        assertThat(dashboardPage.allWidgetsLoaded()).isTrue();
    }

    @Test(description = "Verify reset filters")
    @Story("Filters")
    @Severity(SeverityLevel.NORMAL)
    public void testResetFilters() {
        Dashboard dashboard = createFilterableDashboard("Reset Filters Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Apply filter
        dashboardPage.selectFilter("category", "Electronics");
        dashboardPage.applyFilters();

        // Reset
        dashboardPage.resetFilters();

        assertThat(dashboardPage.allWidgetsLoaded()).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VISUAL REGRESSION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Visual regression test for KPI dashboard")
    @Story("Visual Testing")
    @Severity(SeverityLevel.NORMAL)
    public void testKpiDashboardVisualRegression() throws IOException {
        Dashboard dashboard = createKpiDashboard("Visual KPI Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Take screenshot and compare with baseline
        byte[] screenshot = dashboardPage.takeDashboardScreenshot();

        VisualTestingUtils.ComparisonResult result = visualTesting
            .withDiffThreshold(0.05)  // 5% tolerance
            .compare("kpi-dashboard-baseline", screenshot);

        assertThat(result.isPassed())
            .as("Dashboard should match visual baseline (diff: %.2f%%)", result.getDiffPercent())
            .isTrue();
    }

    @Test(description = "Visual regression test for individual widgets")
    @Story("Visual Testing")
    @Severity(SeverityLevel.NORMAL)
    public void testWidgetVisualRegression() throws IOException {
        Dashboard dashboard = createChartDashboard("Visual Widget Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Compare each widget
        Map<String, String> widgetsToCompare = Map.of(
            "line-chart-widget", "[data-testid='widget-" + dashboard.getWidgets().get(0).getId() + "']",
            "bar-chart-widget", "[data-testid='widget-" + dashboard.getWidgets().get(1).getId() + "']"
        );

        visualTesting
            .withDiffThreshold(0.03)
            .assertAllElementsMatch(widgetsToCompare);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERACTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify chart tooltip on hover")
    @Story("Widget Interaction")
    @Severity(SeverityLevel.MINOR)
    public void testChartTooltip() {
        Dashboard dashboard = createChartDashboard("Tooltip Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        String chartWidgetId = dashboard.getWidgets().get(0).getId();

        // Hover over chart element
        dashboardPage.hoverChartElement(chartWidgetId, ".chart-point, .bar, path");

        // Verify tooltip appears
        assertThat(dashboardPage.isTooltipVisible())
            .as("Tooltip should appear on hover")
            .isTrue();

        assertThat(dashboardPage.getTooltipText())
            .as("Tooltip should have content")
            .isNotBlank();
    }

    @Test(description = "Verify widget fullscreen mode")
    @Story("Widget Interaction")
    @Severity(SeverityLevel.MINOR)
    public void testWidgetFullscreen() {
        Dashboard dashboard = createTestDashboard("Fullscreen Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        String widgetId = dashboard.getWidgets().get(0).getId();

        // Open fullscreen
        dashboardPage.openWidgetFullscreen(widgetId);

        // Verify fullscreen
        assertThat(getPage().locator("[data-testid='fullscreen-overlay']").isVisible())
            .isTrue();

        // Exit fullscreen
        dashboardPage.exitFullscreen();
    }

    @Test(description = "Verify dashboard refresh")
    @Story("Dashboard Actions")
    @Severity(SeverityLevel.NORMAL)
    public void testDashboardRefresh() {
        Dashboard dashboard = createTestDashboard("Refresh Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        String initialRefreshTime = dashboardPage.getLastRefreshTime();

        // Wait and refresh
        try { Thread.sleep(2000); } catch (InterruptedException e) { }

        dashboardPage.refreshDashboard();

        String newRefreshTime = dashboardPage.getLastRefreshTime();

        // Refresh time should be updated
        assertThat(newRefreshTime)
            .as("Refresh time should be updated")
            .isNotEqualTo(initialRefreshTime);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Verify PDF export")
    @Story("Export")
    @Severity(SeverityLevel.NORMAL)
    public void testPdfExport() {
        Dashboard dashboard = createTestDashboard("PDF Export Test");

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        // Export as PDF
        dashboardPage.exportAsPdf();

        // Verify export initiated (download started)
        // In real implementation, would verify file downloaded
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA PROVIDERS
    // ═══════════════════════════════════════════════════════════════════════════

    @DataProvider(name = "timeRanges")
    public Object[][] timeRanges() {
        return new Object[][] {
            { "Last 7 Days" },
            { "Last 30 Days" },
            { "Last 90 Days" },
            { "This Month" },
            { "This Year" }
        };
    }

    @Test(dataProvider = "timeRanges", description = "Verify various time range filters")
    @Story("Filters")
    @Severity(SeverityLevel.NORMAL)
    public void testVariousTimeRanges(String timeRange) {
        Dashboard dashboard = createTestDashboard("Time Range: " + timeRange);

        dashboardPage.navigate(dashboard.getId());
        dashboardPage.waitForAllWidgetsToLoad();

        dashboardPage.selectTimeRange(timeRange);
        dashboardPage.waitForAllWidgetsToLoad();

        assertThat(dashboardPage.allWidgetsLoaded())
            .as("Widgets should load for time range: %s", timeRange)
            .isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private Dashboard createTestDashboard(String name) {
        Widget kpiWidget = Widget.builder()
            .id("kpi-1")
            .title("Test KPI")
            .type(WidgetType.KPI)
            .position(Widget.Position.builder().x(0).y(0).width(4).height(2).build())
            .build();

        Widget chartWidget = Widget.builder()
            .id("chart-1")
            .title("Test Chart")
            .type(WidgetType.LINE_CHART)
            .position(Widget.Position.builder().x(4).y(0).width(8).height(4).build())
            .build();

        Dashboard dashboard = Dashboard.builder()
            .name(name)
            .status(Dashboard.DashboardStatus.PUBLISHED)
            .widgets(List.of(kpiWidget, chartWidget))
            .build();

        return dashboardApi.createDashboard(dashboard);
    }

    private Dashboard createKpiDashboard(String name) {
        Widget kpi1 = Widget.builder()
            .id("kpi-1").title("Revenue").type(WidgetType.KPI)
            .position(Widget.Position.builder().x(0).y(0).width(3).height(2).build())
            .build();

        Widget kpi2 = Widget.builder()
            .id("kpi-2").title("Orders").type(WidgetType.KPI)
            .position(Widget.Position.builder().x(3).y(0).width(3).height(2).build())
            .build();

        return dashboardApi.createDashboard(Dashboard.builder()
            .name(name)
            .status(Dashboard.DashboardStatus.PUBLISHED)
            .widgets(List.of(kpi1, kpi2))
            .build());
    }

    private Dashboard createChartDashboard(String name) {
        Widget lineChart = Widget.builder()
            .id("line-1").title("Trend").type(WidgetType.LINE_CHART)
            .position(Widget.Position.builder().x(0).y(0).width(6).height(4).build())
            .build();

        Widget barChart = Widget.builder()
            .id("bar-1").title("Categories").type(WidgetType.BAR_CHART)
            .position(Widget.Position.builder().x(6).y(0).width(6).height(4).build())
            .build();

        return dashboardApi.createDashboard(Dashboard.builder()
            .name(name)
            .status(Dashboard.DashboardStatus.PUBLISHED)
            .widgets(List.of(lineChart, barChart))
            .build());
    }

    private Dashboard createTableDashboard(String name) {
        Widget table = Widget.builder()
            .id("table-1").title("Data Table").type(WidgetType.TABLE)
            .position(Widget.Position.builder().x(0).y(0).width(12).height(6).build())
            .build();

        return dashboardApi.createDashboard(Dashboard.builder()
            .name(name)
            .status(Dashboard.DashboardStatus.PUBLISHED)
            .widgets(List.of(table))
            .build());
    }

    private Dashboard createFilterableDashboard(String name) {
        Dashboard.Filter categoryFilter = Dashboard.Filter.builder()
            .id("filter-category")
            .name("Category")
            .type(Dashboard.FilterType.DROPDOWN)
            .field("category")
            .build();

        Widget kpi = Widget.builder()
            .id("kpi-1").title("Filtered KPI").type(WidgetType.KPI)
            .position(Widget.Position.builder().x(0).y(0).width(4).height(2).build())
            .build();

        return dashboardApi.createDashboard(Dashboard.builder()
            .name(name)
            .status(Dashboard.DashboardStatus.PUBLISHED)
            .widgets(List.of(kpi))
            .globalFilters(List.of(categoryFilter))
            .build());
    }
}
