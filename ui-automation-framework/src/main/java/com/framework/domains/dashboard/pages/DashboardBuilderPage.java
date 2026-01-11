package com.framework.domains.dashboard.pages;

import com.framework.core.base.BasePage;
import com.framework.domains.dashboard.models.Widget;
import com.framework.domains.dashboard.models.Widget.WidgetType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * DashboardBuilderPage - Dashboard builder/editor page object
 *
 * Handles dashboard creation and editing:
 * - Adding/configuring widgets
 * - Layout management
 * - Filter configuration
 * - Publishing dashboards
 */
@Slf4j
public class DashboardBuilderPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Builder Canvas
    private static final String BUILDER_CANVAS = "[data-testid='dashboard-builder'], .dashboard-builder";
    private static final String WIDGET_PALETTE = "[data-testid='widget-palette'], .widget-palette";
    private static final String WIDGET_GRID = "[data-testid='widget-grid'], .widget-grid";

    // Dashboard Metadata
    private static final String DASHBOARD_NAME_INPUT = "[data-testid='dashboard-name'], #dashboard-name";
    private static final String DASHBOARD_DESC_INPUT = "[data-testid='dashboard-desc'], #dashboard-description";

    // Widget Palette Items
    private static final String WIDGET_TYPE_ITEM = "[data-testid='widget-type-%s']";
    private static final String ADD_WIDGET_BUTTON = "[data-testid='add-widget'], button:has-text('Add Widget')";

    // Widget in Grid
    private static final String WIDGET_IN_GRID = "[data-testid='grid-widget-%s']";
    private static final String ALL_GRID_WIDGETS = "[data-testid^='grid-widget-']";
    private static final String WIDGET_DRAG_HANDLE = "[data-testid='grid-widget-%s'] .drag-handle";
    private static final String WIDGET_RESIZE_HANDLE = "[data-testid='grid-widget-%s'] .resize-handle";
    private static final String WIDGET_DELETE_BUTTON = "[data-testid='grid-widget-%s'] .delete-widget";
    private static final String WIDGET_EDIT_BUTTON = "[data-testid='grid-widget-%s'] .edit-widget";

    // Widget Configuration Panel
    private static final String CONFIG_PANEL = "[data-testid='widget-config-panel'], .config-panel";
    private static final String WIDGET_TITLE_INPUT = "[data-testid='widget-title'], #widget-title";
    private static final String DATA_SOURCE_SELECT = "[data-testid='data-source'], #data-source";
    private static final String QUERY_EDITOR = "[data-testid='query-editor'], .query-editor";

    // Chart Configuration
    private static final String X_AXIS_SELECT = "[data-testid='x-axis'], #x-axis";
    private static final String Y_AXIS_SELECT = "[data-testid='y-axis'], #y-axis";
    private static final String GROUP_BY_SELECT = "[data-testid='group-by'], #group-by";
    private static final String AGGREGATION_SELECT = "[data-testid='aggregation'], #aggregation";
    private static final String COLOR_PALETTE_SELECT = "[data-testid='color-palette'], #color-palette";

    // KPI Configuration
    private static final String KPI_METRIC_SELECT = "[data-testid='kpi-metric'], #kpi-metric";
    private static final String KPI_FORMAT_SELECT = "[data-testid='kpi-format'], #kpi-format";
    private static final String KPI_TARGET_INPUT = "[data-testid='kpi-target'], #kpi-target";
    private static final String KPI_COMPARISON_SELECT = "[data-testid='kpi-comparison'], #kpi-comparison";

    // Table Configuration
    private static final String TABLE_COLUMNS_INPUT = "[data-testid='table-columns']";
    private static final String ADD_COLUMN_BUTTON = "[data-testid='add-column']";
    private static final String COLUMN_FIELD_INPUT = "[data-testid='column-field-%d']";
    private static final String COLUMN_HEADER_INPUT = "[data-testid='column-header-%d']";
    private static final String ENABLE_PAGINATION_CHECKBOX = "[data-testid='enable-pagination']";
    private static final String PAGE_SIZE_INPUT = "[data-testid='page-size']";

    // Filter Configuration Tab
    private static final String FILTERS_TAB = "[data-testid='filters-tab']";
    private static final String ADD_FILTER_BUTTON = "[data-testid='add-filter']";
    private static final String FILTER_NAME_INPUT = "[data-testid='filter-name']";
    private static final String FILTER_TYPE_SELECT = "[data-testid='filter-type']";
    private static final String FILTER_FIELD_SELECT = "[data-testid='filter-field']";

    // Layout Tab
    private static final String LAYOUT_TAB = "[data-testid='layout-tab']";
    private static final String LAYOUT_COLUMNS_INPUT = "[data-testid='layout-columns']";
    private static final String LAYOUT_ROW_HEIGHT_INPUT = "[data-testid='row-height']";

    // Actions
    private static final String SAVE_WIDGET_BUTTON = "[data-testid='save-widget'], button:has-text('Save')";
    private static final String CANCEL_WIDGET_BUTTON = "[data-testid='cancel-widget'], button:has-text('Cancel')";
    private static final String SAVE_DASHBOARD_BUTTON = "[data-testid='save-dashboard'], button:has-text('Save Dashboard')";
    private static final String PUBLISH_BUTTON = "[data-testid='publish-dashboard'], button:has-text('Publish')";
    private static final String PREVIEW_BUTTON = "[data-testid='preview-dashboard'], button:has-text('Preview')";

    // Status
    private static final String SAVE_SUCCESS_TOAST = "[data-testid='save-success'], .toast-success";
    private static final String VALIDATION_ERRORS = "[data-testid='validation-errors']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardBuilderPage(Page page) {
        super(page);
    }

    @Step("Navigate to create new dashboard")
    public DashboardBuilderPage navigateToCreate() {
        navigateTo("/dashboards/new");
        return this;
    }

    @Step("Navigate to edit dashboard: {dashboardId}")
    public DashboardBuilderPage navigateToEdit(String dashboardId) {
        navigateTo("/dashboards/" + dashboardId + "/edit");
        return this;
    }

    @Override
    protected void waitForPageLoad() {
        waitForVisible(BUILDER_CANVAS);
    }

    @Override
    public boolean isDisplayed() {
        return isVisible(BUILDER_CANVAS);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD METADATA
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Set dashboard name: {name}")
    public DashboardBuilderPage setDashboardName(String name) {
        log.info("Setting dashboard name: {}", name);
        fill(DASHBOARD_NAME_INPUT, name);
        return this;
    }

    @Step("Set dashboard description")
    public DashboardBuilderPage setDescription(String description) {
        log.info("Setting dashboard description");
        fill(DASHBOARD_DESC_INPUT, description);
        return this;
    }

    public String getDashboardName() {
        return getValue(DASHBOARD_NAME_INPUT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Add widget of type: {widgetType}")
    public DashboardBuilderPage addWidget(WidgetType widgetType) {
        log.info("Adding widget of type: {}", widgetType);
        click(ADD_WIDGET_BUTTON);

        String typeLocator = String.format(WIDGET_TYPE_ITEM, widgetType.name().toLowerCase());
        click(typeLocator);

        waitForVisible(CONFIG_PANEL);
        return this;
    }

    @Step("Set widget title: {title}")
    public DashboardBuilderPage setWidgetTitle(String title) {
        log.info("Setting widget title: {}", title);
        fill(WIDGET_TITLE_INPUT, title);
        return this;
    }

    @Step("Select widget: {widgetId}")
    public DashboardBuilderPage selectWidget(String widgetId) {
        log.info("Selecting widget: {}", widgetId);
        String locator = String.format(WIDGET_IN_GRID, widgetId);
        click(locator);
        return this;
    }

    @Step("Edit widget: {widgetId}")
    public DashboardBuilderPage editWidget(String widgetId) {
        log.info("Editing widget: {}", widgetId);
        String locator = String.format(WIDGET_EDIT_BUTTON, widgetId);
        click(locator);
        waitForVisible(CONFIG_PANEL);
        return this;
    }

    @Step("Delete widget: {widgetId}")
    public DashboardBuilderPage deleteWidget(String widgetId) {
        log.info("Deleting widget: {}", widgetId);
        String locator = String.format(WIDGET_DELETE_BUTTON, widgetId);
        click(locator);
        return this;
    }

    @Step("Save widget configuration")
    public DashboardBuilderPage saveWidget() {
        log.info("Saving widget configuration");
        click(SAVE_WIDGET_BUTTON);
        waitForHidden(CONFIG_PANEL, SHORT_TIMEOUT);
        return this;
    }

    @Step("Cancel widget configuration")
    public DashboardBuilderPage cancelWidget() {
        click(CANCEL_WIDGET_BUTTON);
        waitForHidden(CONFIG_PANEL, SHORT_TIMEOUT);
        return this;
    }

    public int getWidgetCount() {
        return page.locator(ALL_GRID_WIDGETS).count();
    }

    public List<String> getWidgetIds() {
        return page.locator(ALL_GRID_WIDGETS).all().stream()
            .map(l -> l.getAttribute("data-testid").replace("grid-widget-", ""))
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHART CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure chart axes")
    public DashboardBuilderPage configureChartAxes(String xAxis, String yAxis) {
        log.info("Configuring chart axes: x={}, y={}", xAxis, yAxis);
        selectOption(X_AXIS_SELECT, xAxis);
        selectOption(Y_AXIS_SELECT, yAxis);
        return this;
    }

    @Step("Set group by: {groupBy}")
    public DashboardBuilderPage setGroupBy(String groupBy) {
        log.info("Setting group by: {}", groupBy);
        selectOption(GROUP_BY_SELECT, groupBy);
        return this;
    }

    @Step("Set aggregation: {aggregation}")
    public DashboardBuilderPage setAggregation(String aggregation) {
        log.info("Setting aggregation: {}", aggregation);
        selectOption(AGGREGATION_SELECT, aggregation);
        return this;
    }

    @Step("Set color palette: {palette}")
    public DashboardBuilderPage setColorPalette(String palette) {
        log.info("Setting color palette: {}", palette);
        selectOption(COLOR_PALETTE_SELECT, palette);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KPI CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Configure KPI widget")
    public DashboardBuilderPage configureKpi(String metric, String format, String target) {
        log.info("Configuring KPI: metric={}, format={}", metric, format);
        selectOption(KPI_METRIC_SELECT, metric);
        selectOption(KPI_FORMAT_SELECT, format);
        if (target != null) {
            fill(KPI_TARGET_INPUT, target);
        }
        return this;
    }

    @Step("Set KPI comparison period")
    public DashboardBuilderPage setKpiComparison(String comparison) {
        log.info("Setting KPI comparison: {}", comparison);
        selectOption(KPI_COMPARISON_SELECT, comparison);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Add table column: {field} as {header}")
    public DashboardBuilderPage addTableColumn(String field, String header, int index) {
        log.info("Adding table column: {} as {}", field, header);

        if (index > 0) {
            click(ADD_COLUMN_BUTTON);
        }

        String fieldLocator = String.format(COLUMN_FIELD_INPUT, index);
        String headerLocator = String.format(COLUMN_HEADER_INPUT, index);

        fill(fieldLocator, field);
        fill(headerLocator, header);

        return this;
    }

    @Step("Enable pagination with page size: {pageSize}")
    public DashboardBuilderPage enablePagination(int pageSize) {
        log.info("Enabling pagination with page size: {}", pageSize);
        click(ENABLE_PAGINATION_CHECKBOX);
        fill(PAGE_SIZE_INPUT, String.valueOf(pageSize));
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA SOURCE
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Select data source: {dataSource}")
    public DashboardBuilderPage selectDataSource(String dataSource) {
        log.info("Selecting data source: {}", dataSource);
        selectOption(DATA_SOURCE_SELECT, dataSource);
        return this;
    }

    @Step("Set query")
    public DashboardBuilderPage setQuery(String query) {
        log.info("Setting query");
        fill(QUERY_EDITOR, query);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Open filters tab")
    public DashboardBuilderPage openFiltersTab() {
        click(FILTERS_TAB);
        return this;
    }

    @Step("Add filter: {name}")
    public DashboardBuilderPage addFilter(String name, String type, String field) {
        log.info("Adding filter: {} ({}) on field {}", name, type, field);

        click(ADD_FILTER_BUTTON);
        fill(FILTER_NAME_INPUT, name);
        selectOption(FILTER_TYPE_SELECT, type);
        selectOption(FILTER_FIELD_SELECT, field);

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Open layout tab")
    public DashboardBuilderPage openLayoutTab() {
        click(LAYOUT_TAB);
        return this;
    }

    @Step("Set layout columns: {columns}")
    public DashboardBuilderPage setLayoutColumns(int columns) {
        log.info("Setting layout columns: {}", columns);
        fill(LAYOUT_COLUMNS_INPUT, String.valueOf(columns));
        return this;
    }

    @Step("Set row height: {height}")
    public DashboardBuilderPage setRowHeight(int height) {
        log.info("Setting row height: {}", height);
        fill(LAYOUT_ROW_HEIGHT_INPUT, String.valueOf(height));
        return this;
    }

    @Step("Drag widget to position")
    public DashboardBuilderPage dragWidget(String widgetId, int toX, int toY) {
        log.info("Dragging widget {} to ({}, {})", widgetId, toX, toY);

        String handleLocator = String.format(WIDGET_DRAG_HANDLE, widgetId);
        Locator handle = page.locator(handleLocator);

        // Get the grid element for calculating position
        Locator grid = page.locator(WIDGET_GRID);
        var gridBox = grid.boundingBox();

        // Calculate target position
        double targetX = gridBox.x + (toX * 100); // Assuming 100px per grid unit
        double targetY = gridBox.y + (toY * 100);

        handle.dragTo(page.locator(WIDGET_GRID), new Locator.DragToOptions()
            .setTargetPosition(targetX, targetY));

        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Save dashboard")
    public DashboardBuilderPage saveDashboard() {
        log.info("Saving dashboard");
        click(SAVE_DASHBOARD_BUTTON);
        waitForVisible(SAVE_SUCCESS_TOAST, 5000);
        return this;
    }

    @Step("Publish dashboard")
    public DashboardBuilderPage publishDashboard() {
        log.info("Publishing dashboard");
        click(PUBLISH_BUTTON);
        waitFor(1000);
        return this;
    }

    @Step("Preview dashboard")
    public DashboardPage previewDashboard() {
        log.info("Previewing dashboard");
        click(PREVIEW_BUTTON);
        return new DashboardPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean hasValidationErrors() {
        return isVisible(VALIDATION_ERRORS);
    }

    public String getValidationErrors() {
        if (hasValidationErrors()) {
            return getText(VALIDATION_ERRORS);
        }
        return null;
    }

    public boolean isSaved() {
        return isVisible(SAVE_SUCCESS_TOAST);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUILDER PATTERN FOR COMPLETE DASHBOARDS
    // ═══════════════════════════════════════════════════════════════════════════

    @Step("Create simple KPI dashboard")
    public DashboardBuilderPage createKpiDashboard(String name, List<Map<String, String>> kpis) {
        log.info("Creating KPI dashboard: {} with {} KPIs", name, kpis.size());

        setDashboardName(name);
        setDescription("KPI Dashboard");

        for (Map<String, String> kpi : kpis) {
            addWidget(WidgetType.KPI);
            setWidgetTitle(kpi.get("title"));
            configureKpi(kpi.get("metric"), kpi.get("format"), kpi.get("target"));
            saveWidget();
        }

        return this;
    }

    @Step("Create chart dashboard")
    public DashboardBuilderPage createChartDashboard(String name, WidgetType chartType,
                                                      String xAxis, String yAxis, String groupBy) {
        log.info("Creating chart dashboard: {}", name);

        setDashboardName(name);
        setDescription("Chart Dashboard");

        addWidget(chartType);
        setWidgetTitle(name + " Chart");
        configureChartAxes(xAxis, yAxis);
        if (groupBy != null) {
            setGroupBy(groupBy);
        }
        saveWidget();

        return this;
    }
}
