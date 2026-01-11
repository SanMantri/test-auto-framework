package com.framework.domains.dashboard.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.dashboard.models.Widget;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * DashboardPage - Dashboard viewing page object
 *
 * Handles interactions with rendered dashboards:
 * - Widget rendering verification
 * - Data display validation
 * - Filter interactions
 * - Export functionality
 * - Visual testing
 */
@Slf4j
public class DashboardPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Dashboard Container
    private static final String DASHBOARD_CONTAINER = "[data-testid='dashboard-container'], .dashboard-container";
    private static final String DASHBOARD_TITLE = "[data-testid='dashboard-title'], h1.dashboard-title";
    private static final String LOADING_OVERLAY = "[data-testid='loading-overlay'], .loading-overlay";

    // Widgets
    private static final String WIDGET_CONTAINER = "[data-testid='widget-%s'], .widget[data-id='%s']";
    private static final String ALL_WIDGETS = "[data-testid^='widget-'], .widget";
    private static final String WIDGET_TITLE = "[data-testid='widget-%s'] .widget-title";
    private static final String WIDGET_LOADING = "[data-testid='widget-%s'] .widget-loading";
    private static final String WIDGET_ERROR = "[data-testid='widget-%s'] .widget-error";
    private static final String WIDGET_NO_DATA = "[data-testid='widget-%s'] .widget-no-data";

    // Chart Elements
    private static final String CHART_SVG = "[data-testid='widget-%s'] svg, [data-testid='widget-%s'] canvas";
    private static final String CHART_LEGEND = "[data-testid='widget-%s'] .chart-legend";
    private static final String CHART_TOOLTIP = ".chart-tooltip";

    // KPI Widgets
    private static final String KPI_VALUE = "[data-testid='widget-%s'] .kpi-value, [data-testid='kpi-%s-value']";
    private static final String KPI_TREND = "[data-testid='widget-%s'] .kpi-trend";
    private static final String KPI_CHANGE = "[data-testid='widget-%s'] .kpi-change";
    private static final String KPI_TARGET = "[data-testid='widget-%s'] .kpi-target";

    // Table Widgets
    private static final String TABLE_CONTAINER = "[data-testid='widget-%s'] table, [data-testid='widget-%s'] .data-table";
    private static final String TABLE_ROWS = "[data-testid='widget-%s'] tbody tr";
    private static final String TABLE_HEADERS = "[data-testid='widget-%s'] thead th";
    private static final String TABLE_CELL = "[data-testid='widget-%s'] tbody tr:nth-child(%d) td:nth-child(%d)";
    private static final String TABLE_PAGINATION = "[data-testid='widget-%s'] .pagination";

    // Global Filters
    private static final String FILTER_CONTAINER = "[data-testid='filter-container'], .filter-bar";
    private static final String FILTER_DROPDOWN = "[data-testid='filter-%s']";
    private static final String DATE_RANGE_PICKER = "[data-testid='date-range-picker']";
    private static final String DATE_START = "[data-testid='date-start']";
    private static final String DATE_END = "[data-testid='date-end']";
    private static final String APPLY_FILTERS_BUTTON = "[data-testid='apply-filters'], button:has-text('Apply')";
    private static final String RESET_FILTERS_BUTTON = "[data-testid='reset-filters'], button:has-text('Reset')";

    // Time Range
    private static final String TIME_RANGE_SELECTOR = "[data-testid='time-range-selector']";
    private static final String TIME_RANGE_OPTION = "[data-testid='time-range-%s']";

    // Actions
    private static final String REFRESH_BUTTON = "[data-testid='refresh-dashboard'], button:has-text('Refresh')";
    private static final String EDIT_BUTTON = "[data-testid='edit-dashboard']";
    private static final String SHARE_BUTTON = "[data-testid='share-dashboard']";
    private static final String EXPORT_BUTTON = "[data-testid='export-dashboard']";
    private static final String EXPORT_PDF_OPTION = "[data-testid='export-pdf']";
    private static final String EXPORT_CSV_OPTION = "[data-testid='export-csv']";

    // Full Screen
    private static final String FULLSCREEN_BUTTON = "[data-testid='fullscreen-%s']";
    private static final String FULLSCREEN_OVERLAY = "[data-testid='fullscreen-overlay']";
    private static final String EXIT_FULLSCREEN = "[data-testid='exit-fullscreen']";

    // Last Refresh
    private static final String LAST_REFRESH_TIME = "[data-testid='last-refresh']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardPage(Page page) {
        super(page);
    }

    @Step("Navigate to dashboard: {dashboardId}")
    public DashboardPage navigate(String dashboardId) {
        navigateTo("/dashboards/" + dashboardId);
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(DASHBOARD_CONTAINER);
        waitForAllWidgetsToLoad();
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(DASHBOARD_CONTAINER);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD INFO
    // ═══════════════════════════════════════════════════════════════════════════

    public String getDashboardTitle() {
        return getText(DASHBOARD_TITLE);
    }

    public String getLastRefreshTime() {
        return getText(LAST_REFRESH_TIME);
    }

    public int getWidgetCount() {
        return page.locator(ALL_WIDGETS).count();
    }

    public List<String> getWidgetIds() {
        return page.locator(ALL_WIDGETS).all().stream()
            .map(l -> l.getAttribute("data-id"))
            .filter(id -> id != null)
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET STATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Wait for all widgets to load")
    public DashboardPage waitForAllWidgetsToLoad() {
        log.info("Waiting for all widgets to load");

        // Wait for loading overlay to disappear
        if (isVisible(LOADING_OVERLAY)) {
            waitForHidden(LOADING_OVERLAY, 30000);
        }

        // Wait for each widget to finish loading
        List<Locator> widgets = page.locator(ALL_WIDGETS).all();
        for (Locator widget : widgets) {
            String widgetId = widget.getAttribute("data-id");
            if (widgetId != null) {
                waitForWidgetToLoad(widgetId);
            }
        }

        return this;
    }

    @Step("Wait for widget to load: {widgetId}")
    public DashboardPage waitForWidgetToLoad(String widgetId) {
        String loadingLocator = String.format(WIDGET_LOADING, widgetId, widgetId);

        // Wait for loading indicator to disappear
        page.locator(loadingLocator).waitFor(new Locator.WaitForOptions()
            .setState(WaitForSelectorState.HIDDEN)
            .setTimeout(30000));

        return this;
    }

    public boolean isWidgetLoaded(String widgetId) {
        String loadingLocator = String.format(WIDGET_LOADING, widgetId, widgetId);
        return !isVisible(loadingLocator);
    }

    public boolean hasWidgetError(String widgetId) {
        String errorLocator = String.format(WIDGET_ERROR, widgetId, widgetId);
        return isVisible(errorLocator);
    }

    public boolean hasWidgetNoData(String widgetId) {
        String noDataLocator = String.format(WIDGET_NO_DATA, widgetId, widgetId);
        return isVisible(noDataLocator);
    }

    public String getWidgetTitle(String widgetId) {
        String locator = String.format(WIDGET_TITLE, widgetId);
        return getText(locator);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KPI WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Get KPI value for widget: {widgetId}")
    public String getKpiValue(String widgetId) {
        log.info("Getting KPI value for widget: {}", widgetId);
        String locator = String.format(KPI_VALUE, widgetId, widgetId);
        return getText(locator);
    }

    @Step("Get KPI numeric value for widget: {widgetId}")
    public double getKpiNumericValue(String widgetId) {
        String valueStr = getKpiValue(widgetId);
        // Remove currency symbols, commas, etc.
        String numericStr = valueStr.replaceAll("[^0-9.-]", "");
        return Double.parseDouble(numericStr);
    }

    public String getKpiTrend(String widgetId) {
        String locator = String.format(KPI_TREND, widgetId, widgetId);
        return getAttribute(locator, "data-trend");
    }

    public String getKpiChange(String widgetId) {
        String locator = String.format(KPI_CHANGE, widgetId, widgetId);
        return getText(locator);
    }

    public String getKpiTarget(String widgetId) {
        String locator = String.format(KPI_TARGET, widgetId, widgetId);
        return getText(locator);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLE WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Get table row count for widget: {widgetId}")
    public int getTableRowCount(String widgetId) {
        String locator = String.format(TABLE_ROWS, widgetId, widgetId);
        return page.locator(locator).count();
    }

    public List<String> getTableHeaders(String widgetId) {
        String locator = String.format(TABLE_HEADERS, widgetId, widgetId);
        return page.locator(locator).all().stream()
            .map(l -> l.textContent())
            .toList();
    }

    public String getTableCellValue(String widgetId, int row, int column) {
        String locator = String.format(TABLE_CELL, widgetId, widgetId, row, column);
        return getText(locator);
    }

    @Step("Get table data for widget: {widgetId}")
    public List<Map<String, String>> getTableData(String widgetId) {
        List<String> headers = getTableHeaders(widgetId);
        String rowsLocator = String.format(TABLE_ROWS, widgetId, widgetId);
        List<Locator> rows = page.locator(rowsLocator).all();

        return rows.stream().map(row -> {
            List<Locator> cells = row.locator("td").all();
            Map<String, String> rowData = new java.util.HashMap<>();
            for (int i = 0; i < Math.min(headers.size(), cells.size()); i++) {
                rowData.put(headers.get(i), cells.get(i).textContent());
            }
            return rowData;
        }).toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHART WIDGETS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isChartRendered(String widgetId) {
        String locator = String.format(CHART_SVG, widgetId, widgetId);
        return isVisible(locator);
    }

    public boolean hasChartLegend(String widgetId) {
        String locator = String.format(CHART_LEGEND, widgetId, widgetId);
        return isVisible(locator);
    }

    @Step("Hover over chart element in widget: {widgetId}")
    public DashboardPage hoverChartElement(String widgetId, String selector) {
        String baseLocator = String.format(WIDGET_CONTAINER, widgetId, widgetId);
        page.locator(baseLocator + " " + selector).hover();
        return this;
    }

    public boolean isTooltipVisible() {
        return isVisible(CHART_TOOLTIP);
    }

    public String getTooltipText() {
        return getText(CHART_TOOLTIP);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select filter value: {filterName} = {value}")
    public DashboardPage selectFilter(String filterName, String value) {
        log.info("Selecting filter {} = {}", filterName, value);
        String locator = String.format(FILTER_DROPDOWN, filterName);
        selectOption(locator, value);
        return this;
    }

    @Step("Set date range: {start} to {end}")
    public DashboardPage setDateRange(String start, String end) {
        log.info("Setting date range: {} to {}", start, end);
        fill(DATE_START, start);
        fill(DATE_END, end);
        return this;
    }

    @Step("Select time range preset: {preset}")
    public DashboardPage selectTimeRange(String preset) {
        log.info("Selecting time range: {}", preset);
        click(TIME_RANGE_SELECTOR);
        String optionLocator = String.format(TIME_RANGE_OPTION, preset.toLowerCase().replace(" ", "_"));
        click(optionLocator);
        return this;
    }

    @Step("Apply filters")
    public DashboardPage applyFilters() {
        log.info("Applying filters");
        click(APPLY_FILTERS_BUTTON);
        waitForAllWidgetsToLoad();
        return this;
    }

    @Step("Reset filters")
    public DashboardPage resetFilters() {
        log.info("Resetting filters");
        click(RESET_FILTERS_BUTTON);
        waitForAllWidgetsToLoad();
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Refresh dashboard")
    public DashboardPage refreshDashboard() {
        log.info("Refreshing dashboard");
        click(REFRESH_BUTTON);
        waitForAllWidgetsToLoad();
        return this;
    }

    @Step("Open widget fullscreen: {widgetId}")
    public DashboardPage openWidgetFullscreen(String widgetId) {
        log.info("Opening widget fullscreen: {}", widgetId);
        String locator = String.format(FULLSCREEN_BUTTON, widgetId);
        click(locator);
        waitForVisible(FULLSCREEN_OVERLAY);
        return this;
    }

    @Step("Exit fullscreen")
    public DashboardPage exitFullscreen() {
        click(EXIT_FULLSCREEN);
        waitForHidden(FULLSCREEN_OVERLAY, SHORT_TIMEOUT);
        return this;
    }

    @Step("Navigate to edit dashboard")
    public DashboardBuilderPage editDashboard() {
        click(EDIT_BUTTON);
        return new DashboardBuilderPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Export dashboard as PDF")
    public DashboardPage exportAsPdf() {
        log.info("Exporting dashboard as PDF");
        click(EXPORT_BUTTON);
        click(EXPORT_PDF_OPTION);
        // Wait for download to start
        waitFor(2000);
        return this;
    }

    @Step("Export dashboard as CSV")
    public DashboardPage exportAsCsv() {
        log.info("Exporting dashboard as CSV");
        click(EXPORT_BUTTON);
        click(EXPORT_CSV_OPTION);
        // Wait for download to start
        waitFor(2000);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VISUAL TESTING
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Take dashboard screenshot")
    public byte[] takeDashboardScreenshot() {
        return page.locator(DASHBOARD_CONTAINER).screenshot();
    }

    @Step("Take widget screenshot: {widgetId}")
    public byte[] takeWidgetScreenshot(String widgetId) {
        String locator = String.format(WIDGET_CONTAINER, widgetId, widgetId);
        return page.locator(locator).screenshot();
    }

    @Step("Save dashboard screenshot to: {path}")
    public DashboardPage saveDashboardScreenshot(Path path) {
        page.locator(DASHBOARD_CONTAINER).screenshot(new Locator.ScreenshotOptions()
            .setPath(path));
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSERTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean allWidgetsLoaded() {
        List<String> widgetIds = getWidgetIds();
        return widgetIds.stream().allMatch(this::isWidgetLoaded);
    }

    public boolean noWidgetsHaveErrors() {
        List<String> widgetIds = getWidgetIds();
        return widgetIds.stream().noneMatch(this::hasWidgetError);
    }

    public boolean allChartsRendered() {
        List<String> widgetIds = getWidgetIds();
        // This would need widget type info to be accurate
        return widgetIds.stream()
            .filter(id -> {
                String locator = String.format(CHART_SVG, id, id);
                return page.locator(locator).count() > 0 || !isVisible(locator);
            })
            .allMatch(this::isChartRendered);
    }
}
