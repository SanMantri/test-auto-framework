# Dashboard Domain - Low-Level Design (LLD)

## Document Information
| Attribute | Value |
|-----------|-------|
| Domain | Dashboard (Visualizations & Analytics) |
| Version | 1.0 |
| Dependencies | Master HLD |

---

## 1. Domain Overview

### 1.1 What We're Testing

Dashboards present data through visual elements that require specialized testing approaches:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           DASHBOARD DOMAIN SCOPE                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  DASHBOARD COMPONENTS:                                                                   │
│  ─────────────────────                                                                   │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                                  │   │
│  │   CHARTS              WIDGETS           FILTERS           INTERACTIVITY        │   │
│  │   ──────              ───────           ───────           ─────────────        │   │
│  │                                                                                  │   │
│  │   • Line charts       • KPI cards       • Date range      • Drill-down         │   │
│  │   • Bar charts        • Gauges          • Dropdowns       • Zoom               │   │
│  │   • Pie/Donut         • Counters        • Multi-select    • Pan                │   │
│  │   • Area charts       • Sparklines      • Search          • Hover tooltips     │   │
│  │   • Scatter plots     • Tables          • Sliders         • Click actions      │   │
│  │   • Heatmaps          • Progress bars   • Toggles         • Export             │   │
│  │   • Geo maps          • Status lights   • Checkboxes      • Refresh            │   │
│  │                                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  TESTING CHALLENGES:                                                                     │
│  ────────────────────                                                                    │
│                                                                                          │
│  1. VISUAL RENDERING: Charts render as Canvas/SVG - no direct DOM access               │
│  2. DATA ACCURACY: Numbers shown must match source data                                 │
│  3. DYNAMIC UPDATES: Real-time data changes                                             │
│  4. RESPONSIVE DESIGN: Different layouts at different screen sizes                      │
│  5. PERFORMANCE: Charts with large datasets                                             │
│  6. ACCESSIBILITY: Color contrast, screen reader support                                │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Testing Philosophy for Dashboards

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        DASHBOARD TESTING PHILOSOPHY                                      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  PRINCIPLE: "Seed Data via API, View via UI, Verify Data + Visuals"                    │
│                                                                                          │
│  THREE-LAYER TESTING:                                                                    │
│  ─────────────────────                                                                   │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                                  │   │
│  │  LAYER 1: DATA ACCURACY (API)                                                    │   │
│  │  ─────────────────────────────                                                   │   │
│  │  • Dashboard API returns correct aggregations                                    │   │
│  │  • Filters applied correctly server-side                                         │   │
│  │  • Calculations (sum, avg, etc.) are accurate                                    │   │
│  │  • Date range boundaries respected                                               │   │
│  │                                                                                  │   │
│  │  LAYER 2: UI DISPLAY (Hybrid)                                                    │   │
│  │  ────────────────────────────                                                    │   │
│  │  • KPI values displayed match API data                                           │   │
│  │  • Filter UI works (select, date picker)                                         │   │
│  │  • Drill-down navigation works                                                   │   │
│  │  • Export downloads correct data                                                 │   │
│  │                                                                                  │   │
│  │  LAYER 3: VISUAL INTEGRITY (Visual Testing)                                      │   │
│  │  ───────────────────────────────────────                                         │   │
│  │  • Charts render without visual defects                                          │   │
│  │  • Colors, legends, labels are correct                                           │   │
│  │  • Responsive layouts work                                                       │   │
│  │  • No overlapping or clipped elements                                            │   │
│  │                                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  VISUAL TESTING APPROACHES:                                                              │
│  ──────────────────────────                                                              │
│                                                                                          │
│  ┌──────────────────────────┬──────────────────────────┬──────────────────────────┐    │
│  │  PIXEL COMPARISON        │  AI VISUAL TESTING       │  DATA-POINT EXTRACTION   │    │
│  │  ────────────────        │  ──────────────────      │  ──────────────────────  │    │
│  │                          │                          │                          │    │
│  │  • Screenshot baseline   │  • Applitools Eyes       │  • Parse SVG elements    │    │
│  │  • Pixel-by-pixel diff   │  • Percy.io              │  • Extract data attrs    │    │
│  │  • Tolerance thresholds  │  • Ignores anti-aliasing │  • Canvas getImageData   │    │
│  │                          │  • Smart element matching│  • Compare to source     │    │
│  │                          │                          │                          │    │
│  │  Pros: Simple, precise   │  Pros: Robust, smart     │  Pros: Data accuracy     │    │
│  │  Cons: Brittle, noise    │  Cons: Cost              │  Cons: Complex, limited  │    │
│  │                          │                          │                          │    │
│  │  Best for: Static charts │  Best for: Dynamic UI    │  Best for: Critical KPIs │    │
│  └──────────────────────────┴──────────────────────────┴──────────────────────────┘    │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Architecture

### 2.1 Dashboard Module Structure

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          DASHBOARD MODULE COMPONENTS                                     │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  dashboard/                                                                              │
│  ├── api/                              # API Layer                                      │
│  │   ├── DashboardApiClient.java       # Dashboard CRUD                                 │
│  │   ├── WidgetApiClient.java          # Widget data queries                            │
│  │   ├── MetricsApiClient.java         # Raw metrics/aggregations                       │
│  │   └── ExportApiClient.java          # Export data endpoints                          │
│  │                                                                                       │
│  ├── pages/                            # UI Layer (Page Objects)                        │
│  │   ├── DashboardListPage.java        # Dashboard catalog                              │
│  │   ├── DashboardViewPage.java        # Dashboard viewer                               │
│  │   ├── DashboardEditorPage.java      # Dashboard builder/editor                       │
│  │   └── components/                   # Reusable UI components                         │
│  │       ├── ChartComponent.java       # Generic chart wrapper                          │
│  │       ├── LineChartComponent.java   # Line chart specific                            │
│  │       ├── BarChartComponent.java    # Bar chart specific                             │
│  │       ├── PieChartComponent.java    # Pie/donut chart                                │
│  │       ├── KPICardComponent.java     # KPI metric card                                │
│  │       ├── DataTableComponent.java   # Data grid/table                                │
│  │       ├── FilterPanelComponent.java # Filter controls                                │
│  │       └── DateRangePickerComponent.java                                              │
│  │                                                                                       │
│  ├── models/                           # Data Models                                    │
│  │   ├── Dashboard.java                # Dashboard definition                           │
│  │   ├── Widget.java                   # Widget configuration                           │
│  │   ├── ChartData.java                # Chart data structure                           │
│  │   ├── Filter.java                   # Filter state                                   │
│  │   ├── DateRange.java                # Date range model                               │
│  │   └── builders/                                                                      │
│  │       ├── DashboardDataBuilder.java # Seed test data                                 │
│  │       └── FilterBuilder.java        # Build filter configs                           │
│  │                                                                                       │
│  ├── visual/                           # Visual Testing Utilities                       │
│  │   ├── ChartScreenshotHelper.java    # Capture chart screenshots                      │
│  │   ├── VisualComparator.java         # Pixel comparison logic                         │
│  │   ├── ChartDataExtractor.java       # Extract data from charts                       │
│  │   └── AppliToolsIntegration.java    # AI visual testing                              │
│  │                                                                                       │
│  ├── playbooks/                        # Reusable Workflows                             │
│  │   ├── DashboardDataSetupPlaybook.java  # Seed known data                             │
│  │   ├── FilterVerificationPlaybook.java  # Verify filter behavior                      │
│  │   └── ExportVerificationPlaybook.java  # Verify export content                       │
│  │                                                                                       │
│  ├── tests/                            # Test Classes                                   │
│  │   ├── KPIAccuracyTests.java         # KPI value accuracy                             │
│  │   ├── ChartDataAccuracyTests.java   # Chart data matches source                      │
│  │   ├── FilterTests.java              # Filter functionality                           │
│  │   ├── DrillDownTests.java           # Drill-down navigation                          │
│  │   ├── ExportTests.java              # Export functionality                           │
│  │   ├── VisualRegressionTests.java    # Visual testing                                 │
│  │   └── ResponsiveTests.java          # Different viewport tests                       │
│  │                                                                                       │
│  └── data/                             # Test Data                                      │
│      ├── dashboard-configs.json        # Dashboard configurations                       │
│      ├── test-metrics.json             # Seed metrics data                              │
│      └── visual-baselines/             # Baseline screenshots                           │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Page Objects Design

### 3.1 DashboardViewPage Implementation

```java
/**
 * DashboardViewPage - Dashboard viewer interface
 *
 * Handles dashboard viewing, filtering, and interaction with widgets.
 */
public class DashboardViewPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Dashboard Container
    private static final String DASHBOARD_CONTAINER = "[data-testid='dashboard-container']";
    private static final String DASHBOARD_TITLE = "[data-testid='dashboard-title']";
    private static final String DASHBOARD_LOADING = ".dashboard-loading";

    // Widgets
    private static final String WIDGET = "[data-testid='widget-%s']";
    private static final String WIDGET_TITLE = "[data-testid='widget-%s-title']";
    private static final String WIDGET_LOADING = "[data-testid='widget-%s-loading']";

    // Filters
    private static final String FILTER_PANEL = "[data-testid='filter-panel']";
    private static final String DATE_RANGE_PICKER = "[data-testid='date-range-picker']";
    private static final String FILTER_DROPDOWN = "[data-testid='filter-%s']";
    private static final String APPLY_FILTERS_BUTTON = "[data-testid='apply-filters']";

    // Export
    private static final String EXPORT_BUTTON = "[data-testid='export-btn']";
    private static final String EXPORT_FORMAT_DROPDOWN = "[data-testid='export-format']";

    // Refresh
    private static final String REFRESH_BUTTON = "[data-testid='refresh-btn']";
    private static final String LAST_UPDATED = "[data-testid='last-updated']";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardViewPage(Page page) {
        super(page);
    }

    public DashboardViewPage navigate(String dashboardId) {
        page.navigate(baseUrl + "/dashboards/" + dashboardId);
        waitForDashboardLoad();
        return this;
    }

    private void waitForDashboardLoad() {
        // Wait for dashboard container
        page.waitForSelector(DASHBOARD_CONTAINER);

        // Wait for loading spinner to disappear
        page.waitForSelector(DASHBOARD_LOADING,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(30000));  // Dashboards can be slow
    }

    public void waitForAllWidgetsLoaded() {
        // Wait for all widget loading indicators to disappear
        page.waitForFunction(
            "document.querySelectorAll('[data-testid$=\"-loading\"]').length === 0"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WIDGET ACCESS
    // ═══════════════════════════════════════════════════════════════════════════

    public KPICardComponent getKPICard(String widgetId) {
        waitForWidgetLoaded(widgetId);
        return new KPICardComponent(page, widgetId);
    }

    public LineChartComponent getLineChart(String widgetId) {
        waitForWidgetLoaded(widgetId);
        return new LineChartComponent(page, widgetId);
    }

    public BarChartComponent getBarChart(String widgetId) {
        waitForWidgetLoaded(widgetId);
        return new BarChartComponent(page, widgetId);
    }

    public PieChartComponent getPieChart(String widgetId) {
        waitForWidgetLoaded(widgetId);
        return new PieChartComponent(page, widgetId);
    }

    public DataTableComponent getDataTable(String widgetId) {
        waitForWidgetLoaded(widgetId);
        return new DataTableComponent(page, widgetId);
    }

    private void waitForWidgetLoaded(String widgetId) {
        String loadingLocator = String.format(WIDGET_LOADING, widgetId);
        page.waitForSelector(loadingLocator,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(15000));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTERING
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardViewPage setDateRange(LocalDate start, LocalDate end) {
        page.click(DATE_RANGE_PICKER);

        // Use date picker dialog
        page.fill("[data-testid='start-date-input']", start.toString());
        page.fill("[data-testid='end-date-input']", end.toString());
        page.click("[data-testid='apply-date-range']");

        return this;
    }

    public DashboardViewPage setDateRangePreset(DateRangePreset preset) {
        page.click(DATE_RANGE_PICKER);
        page.click(String.format("[data-testid='preset-%s']", preset.getValue()));
        return this;
    }

    public DashboardViewPage selectFilter(String filterName, String... values) {
        String locator = String.format(FILTER_DROPDOWN, filterName);
        page.click(locator);

        for (String value : values) {
            page.click(String.format("[data-testid='filter-option-%s']", value));
        }

        // Close dropdown
        page.keyboard().press("Escape");
        return this;
    }

    public DashboardViewPage applyFilters() {
        page.click(APPLY_FILTERS_BUTTON);
        waitForAllWidgetsLoaded();
        return this;
    }

    public DashboardViewPage clearAllFilters() {
        page.click("[data-testid='clear-filters']");
        waitForAllWidgetsLoaded();
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPORT
    // ═══════════════════════════════════════════════════════════════════════════

    public Path exportDashboard(ExportFormat format) {
        page.click(EXPORT_BUTTON);
        page.selectOption(EXPORT_FORMAT_DROPDOWN, format.getValue());

        // Set up download handler
        Download download = page.waitForDownload(() -> {
            page.click("[data-testid='confirm-export']");
        });

        // Save to temp file
        Path downloadPath = Paths.get("target/downloads", download.suggestedFilename());
        download.saveAs(downloadPath);

        return downloadPath;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFRESH & STATE
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardViewPage refresh() {
        page.click(REFRESH_BUTTON);
        waitForAllWidgetsLoaded();
        return this;
    }

    public LocalDateTime getLastUpdatedTime() {
        String text = page.locator(LAST_UPDATED).textContent();
        // Parse "Last updated: 2024-01-15 10:30:00"
        return LocalDateTime.parse(text.replace("Last updated: ", ""),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DRILL-DOWN
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardViewPage drillDownOnWidget(String widgetId, String dataPoint) {
        // Click on specific data point in chart
        String locator = String.format("[data-testid='widget-%s'] [data-point='%s']",
            widgetId, dataPoint);
        page.click(locator);

        // Wait for drill-down view
        page.waitForSelector("[data-testid='drill-down-view']");
        return this;
    }

    public boolean isDrillDownOpen() {
        return page.locator("[data-testid='drill-down-view']").isVisible();
    }

    public DashboardViewPage closeDrillDown() {
        page.click("[data-testid='close-drill-down']");
        page.waitForSelector("[data-testid='drill-down-view']",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VIEWPORT TESTING
    // ═══════════════════════════════════════════════════════════════════════════

    public DashboardViewPage setViewportSize(int width, int height) {
        page.setViewportSize(width, height);
        // Wait for responsive layout adjustment
        page.waitForTimeout(500);
        return this;
    }
}
```

### 3.2 Chart Components

```java
/**
 * LineChartComponent - Line chart specific interactions and data extraction
 */
public class LineChartComponent extends ChartComponent {

    public LineChartComponent(Page page, String widgetId) {
        super(page, widgetId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA EXTRACTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Extracts data points from line chart
     * Works with both SVG and Canvas-based charts
     */
    public List<DataPoint> getDataPoints() {
        String chartType = getChartImplementationType();

        if (chartType.equals("svg")) {
            return extractFromSVG();
        } else if (chartType.equals("canvas")) {
            return extractFromCanvas();
        } else {
            throw new UnsupportedOperationException("Unknown chart type: " + chartType);
        }
    }

    private List<DataPoint> extractFromSVG() {
        List<DataPoint> points = new ArrayList<>();

        // Find all data points in SVG
        List<Locator> dataPoints = page.locator(
            String.format("[data-testid='widget-%s'] svg circle[data-point]", widgetId)
        ).all();

        for (Locator point : dataPoints) {
            String xValue = point.getAttribute("data-x");
            String yValue = point.getAttribute("data-y");
            points.add(new DataPoint(xValue, Double.parseDouble(yValue)));
        }

        return points;
    }

    private List<DataPoint> extractFromCanvas() {
        // For Canvas-based charts, use JavaScript to get underlying data
        Object data = page.evaluate(String.format(
            "window.__chartInstances['%s'].data.datasets[0].data", widgetId));

        List<DataPoint> points = new ArrayList<>();
        // Parse the returned data structure
        // (Implementation depends on chart library - Chart.js, Highcharts, etc.)
        return points;
    }

    /**
     * Gets the value at a specific x-axis label
     */
    public Double getValueAt(String label) {
        return getDataPoints().stream()
            .filter(p -> p.getLabel().equals(label))
            .findFirst()
            .map(DataPoint::getValue)
            .orElse(null);
    }

    /**
     * Gets the series names/legend labels
     */
    public List<String> getSeriesNames() {
        return page.locator(String.format(
            "[data-testid='widget-%s'] [data-testid='legend-item']", widgetId
        )).allTextContents();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INTERACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Hovers over a data point and gets tooltip content
     */
    public String getTooltipAt(String label) {
        // Find the data point
        Locator point = page.locator(String.format(
            "[data-testid='widget-%s'] [data-point][data-x='%s']", widgetId, label));

        point.hover();

        // Wait for tooltip
        page.waitForSelector("[data-testid='chart-tooltip']");
        return page.locator("[data-testid='chart-tooltip']").textContent();
    }

    /**
     * Toggles a series visibility via legend click
     */
    public LineChartComponent toggleSeries(String seriesName) {
        page.click(String.format(
            "[data-testid='widget-%s'] [data-testid='legend-item'][data-series='%s']",
            widgetId, seriesName));
        return this;
    }

    /**
     * Zooms into a specific range
     */
    public LineChartComponent zoomToRange(String startLabel, String endLabel) {
        // Click and drag from start to end on the chart
        Locator startPoint = page.locator(String.format(
            "[data-testid='widget-%s'] [data-point][data-x='%s']", widgetId, startLabel));
        Locator endPoint = page.locator(String.format(
            "[data-testid='widget-%s'] [data-point][data-x='%s']", widgetId, endLabel));

        startPoint.dragTo(endPoint);

        // Wait for zoom to apply
        page.waitForTimeout(500);
        return this;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VISUAL VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Captures chart screenshot for visual comparison
     */
    public byte[] captureScreenshot() {
        return page.locator(String.format("[data-testid='widget-%s']", widgetId))
            .screenshot();
    }

    /**
     * Gets the trend direction (UP, DOWN, FLAT)
     */
    public TrendDirection getTrendDirection() {
        List<DataPoint> points = getDataPoints();
        if (points.size() < 2) return TrendDirection.FLAT;

        double first = points.get(0).getValue();
        double last = points.get(points.size() - 1).getValue();

        double percentChange = ((last - first) / first) * 100;

        if (percentChange > 5) return TrendDirection.UP;
        if (percentChange < -5) return TrendDirection.DOWN;
        return TrendDirection.FLAT;
    }
}

/**
 * KPICardComponent - KPI/Metric card component
 */
public class KPICardComponent {

    private final Page page;
    private final String widgetId;

    public KPICardComponent(Page page, String widgetId) {
        this.page = page;
        this.widgetId = widgetId;
    }

    /**
     * Gets the main KPI value
     */
    public String getValue() {
        return page.locator(String.format(
            "[data-testid='widget-%s'] [data-testid='kpi-value']", widgetId
        )).textContent();
    }

    /**
     * Gets the numeric value (strips currency, commas, etc.)
     */
    public BigDecimal getNumericValue() {
        String text = getValue();
        // Remove non-numeric characters except decimal point
        String numeric = text.replaceAll("[^\\d.-]", "");
        return new BigDecimal(numeric);
    }

    /**
     * Gets the comparison/change value (e.g., "+15%")
     */
    public String getChangeValue() {
        return page.locator(String.format(
            "[data-testid='widget-%s'] [data-testid='kpi-change']", widgetId
        )).textContent();
    }

    /**
     * Gets the change direction (positive/negative)
     */
    public ChangeDirection getChangeDirection() {
        String change = getChangeValue();
        if (change.startsWith("+") || change.contains("↑")) {
            return ChangeDirection.POSITIVE;
        } else if (change.startsWith("-") || change.contains("↓")) {
            return ChangeDirection.NEGATIVE;
        }
        return ChangeDirection.NEUTRAL;
    }

    /**
     * Gets the sparkline trend data (if present)
     */
    public List<Double> getSparklineData() {
        // Sparklines are typically SVG paths or small charts
        String pathData = page.locator(String.format(
            "[data-testid='widget-%s'] [data-testid='sparkline'] path", widgetId
        )).getAttribute("d");

        return parseSparklinePath(pathData);
    }

    private List<Double> parseSparklinePath(String pathData) {
        // Parse SVG path data to extract Y values
        // Format: "M0,50 L10,45 L20,55 L30,40..."
        List<Double> values = new ArrayList<>();
        // Implementation depends on SVG path format
        return values;
    }
}
```

---

## 4. Data Accuracy Testing

### 4.1 KPI Accuracy Tests

```java
/**
 * KPIAccuracyTests - Verify KPI values match source data
 */
@Test(groups = {"dashboard", "accuracy", "kpi"})
public class KPIAccuracyTests extends BaseDashboardTest {

    private MetricsApiClient metricsApi;
    private DashboardDataBuilder dataBuilder;

    @BeforeClass
    public void setup() {
        metricsApi = getBean(MetricsApiClient.class);
        dataBuilder = getBean(DashboardDataBuilder.class);
    }

    @BeforeMethod
    public void seedTestData() {
        // Create known dataset for testing
        dataBuilder.clearData();
        dataBuilder.seedSalesData(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31),
            Map.of(
                "2024-01-01", new BigDecimal("1000.00"),
                "2024-01-15", new BigDecimal("1500.00"),
                "2024-01-31", new BigDecimal("2000.00")
            )
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VALUE ACCURACY TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Total Revenue KPI matches sum of all transactions")
    public void totalRevenueKPIAccuracy() {
        // Get expected value from API
        BigDecimal expectedTotal = metricsApi.getTotalRevenue(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );

        // Navigate to dashboard
        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.setDateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );
        dashboard.applyFilters();

        // Get displayed value
        KPICardComponent revenueKPI = dashboard.getKPICard("total-revenue");
        BigDecimal displayedValue = revenueKPI.getNumericValue();

        // Assert with tolerance (for rounding)
        assertThat(displayedValue)
            .isCloseTo(expectedTotal, Percentage.withPercentage(0.01));
    }

    @Test(description = "Average Order Value KPI calculated correctly")
    public void averageOrderValueAccuracy() {
        BigDecimal expectedAOV = metricsApi.getAverageOrderValue(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.setDateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );
        dashboard.applyFilters();

        KPICardComponent aovKPI = dashboard.getKPICard("avg-order-value");
        BigDecimal displayedAOV = aovKPI.getNumericValue();

        assertThat(displayedAOV)
            .isCloseTo(expectedAOV, Percentage.withPercentage(0.1));
    }

    @Test(description = "Percentage change calculated correctly")
    public void percentageChangeAccuracy() {
        // Current period: Jan 2024
        // Previous period: Dec 2023
        dataBuilder.seedSalesData(
            LocalDate.of(2023, 12, 1),
            LocalDate.of(2023, 12, 31),
            Map.of("total", new BigDecimal("3500.00"))  // Dec total
        );
        // Jan total = 4500 (sum of 1000 + 1500 + 2000)
        // Expected change = ((4500 - 3500) / 3500) * 100 = 28.57%

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.setDateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );
        dashboard.applyFilters();

        KPICardComponent revenueKPI = dashboard.getKPICard("total-revenue");
        String change = revenueKPI.getChangeValue();

        // Should show approximately +28.57%
        assertThat(change).matches("\\+28\\.\\d+%");
        assertThat(revenueKPI.getChangeDirection()).isEqualTo(ChangeDirection.POSITIVE);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILTER IMPACT TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Filter correctly reduces KPI values")
    public void filterAffectsKPIValues() {
        // Seed data with categories
        dataBuilder.seedCategorizedSales(Map.of(
            "Electronics", new BigDecimal("5000"),
            "Clothing", new BigDecimal("3000"),
            "Books", new BigDecimal("2000")
        ));

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");

        // Get total before filter
        KPICardComponent revenueKPI = dashboard.getKPICard("total-revenue");
        BigDecimal totalBeforeFilter = revenueKPI.getNumericValue();
        assertThat(totalBeforeFilter).isEqualByComparingTo(new BigDecimal("10000"));

        // Apply category filter
        dashboard.selectFilter("category", "Electronics");
        dashboard.applyFilters();

        // Verify filtered value
        BigDecimal filteredTotal = revenueKPI.getNumericValue();
        assertThat(filteredTotal).isEqualByComparingTo(new BigDecimal("5000"));
    }
}
```

### 4.2 Chart Data Accuracy Tests

```java
/**
 * ChartDataAccuracyTests - Verify chart data matches source
 */
@Test(groups = {"dashboard", "accuracy", "chart"})
public class ChartDataAccuracyTests extends BaseDashboardTest {

    @Test(description = "Line chart data points match API data")
    public void lineChartDataAccuracy() {
        // Seed known data
        Map<String, BigDecimal> dailySales = new LinkedHashMap<>();
        dailySales.put("Jan 1", new BigDecimal("100"));
        dailySales.put("Jan 2", new BigDecimal("150"));
        dailySales.put("Jan 3", new BigDecimal("120"));
        dailySales.put("Jan 4", new BigDecimal("180"));
        dailySales.put("Jan 5", new BigDecimal("200"));

        dataBuilder.seedDailySales(dailySales);

        // Navigate to dashboard
        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-trends");

        // Get chart component
        LineChartComponent lineChart = dashboard.getLineChart("daily-sales-chart");

        // Extract and compare data points
        List<DataPoint> chartData = lineChart.getDataPoints();

        for (Map.Entry<String, BigDecimal> expected : dailySales.entrySet()) {
            Double chartValue = lineChart.getValueAt(expected.getKey());
            assertThat(chartValue)
                .as("Value at " + expected.getKey())
                .isCloseTo(expected.getValue().doubleValue(), Offset.offset(0.01));
        }
    }

    @Test(description = "Pie chart percentages sum to 100")
    public void pieChartPercentagesSum() {
        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("category-breakdown");

        PieChartComponent pieChart = dashboard.getPieChart("category-pie");

        List<PieSlice> slices = pieChart.getSlices();

        double totalPercentage = slices.stream()
            .mapToDouble(PieSlice::getPercentage)
            .sum();

        assertThat(totalPercentage).isCloseTo(100.0, Offset.offset(0.5));
    }

    @Test(description = "Bar chart values match API aggregations")
    public void barChartAggregationAccuracy() {
        // Get expected aggregations from API
        Map<String, BigDecimal> expectedByRegion = metricsApi.getSalesByRegion();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("regional-sales");

        BarChartComponent barChart = dashboard.getBarChart("sales-by-region");

        for (Map.Entry<String, BigDecimal> expected : expectedByRegion.entrySet()) {
            Double chartValue = barChart.getValueForCategory(expected.getKey());
            assertThat(chartValue)
                .as("Value for " + expected.getKey())
                .isCloseTo(expected.getValue().doubleValue(), Percentage.withPercentage(0.5));
        }
    }
}
```

---

## 5. Visual Testing

### 5.1 Visual Regression Tests

```java
/**
 * VisualRegressionTests - Chart visual integrity
 */
@Test(groups = {"dashboard", "visual"})
public class VisualRegressionTests extends BaseDashboardTest {

    private Eyes eyes;  // Applitools Eyes

    @BeforeMethod
    public void setupEyes() {
        eyes = new Eyes();
        eyes.setApiKey(System.getenv("APPLITOOLS_API_KEY"));
        eyes.open(page, "Dashboard Visual Tests", testName.getMethodName());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL DASHBOARD VISUAL TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Dashboard renders correctly with data")
    public void dashboardRendersCorrectly() {
        // Seed consistent data for visual baseline
        seedVisualTestData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.waitForAllWidgetsLoaded();

        // Full page visual check
        eyes.check("Full Dashboard", Target.window().fully());
    }

    @Test(description = "Dashboard renders correctly in dark mode")
    public void dashboardDarkMode() {
        seedVisualTestData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");

        // Enable dark mode
        page.click("[data-testid='theme-toggle']");
        dashboard.waitForAllWidgetsLoaded();

        eyes.check("Dashboard Dark Mode", Target.window().fully());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INDIVIDUAL WIDGET VISUAL TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Line chart renders with correct styling")
    public void lineChartVisual() {
        seedTrendData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("trends");

        LineChartComponent chart = dashboard.getLineChart("revenue-trend");

        // Check just the chart widget
        eyes.check("Revenue Trend Chart",
            Target.region(By.cssSelector("[data-testid='widget-revenue-trend']")));
    }

    @Test(description = "KPI cards render with correct colors based on values")
    public void kpiCardColors() {
        // Seed data that will trigger positive and negative indicators
        dataBuilder.seedKPIData(Map.of(
            "revenue", new KPIData(5000, 4000, "UP"),      // Positive
            "costs", new KPIData(3000, 2500, "UP"),        // Negative (costs up is bad)
            "customers", new KPIData(100, 100, "FLAT")     // Neutral
        ));

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("overview");

        // Check KPI row
        eyes.check("KPI Cards",
            Target.region(By.cssSelector("[data-testid='kpi-row']")));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESPONSIVE VISUAL TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Dashboard adapts to tablet viewport")
    public void tabletViewport() {
        seedVisualTestData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.setViewportSize(768, 1024);  // iPad
        dashboard.navigate("sales-dashboard");
        dashboard.waitForAllWidgetsLoaded();

        eyes.check("Dashboard Tablet", Target.window().fully());
    }

    @Test(description = "Dashboard adapts to mobile viewport")
    public void mobileViewport() {
        seedVisualTestData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.setViewportSize(375, 812);  // iPhone X
        dashboard.navigate("sales-dashboard");
        dashboard.waitForAllWidgetsLoaded();

        eyes.check("Dashboard Mobile", Target.window().fully());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EDGE CASE VISUAL TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Charts render correctly with no data")
    public void chartsWithNoData() {
        // Don't seed any data
        dataBuilder.clearData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.waitForAllWidgetsLoaded();

        // Should show empty states, not broken charts
        eyes.check("Dashboard Empty State", Target.window().fully());
    }

    @Test(description = "Charts handle large numbers without overflow")
    public void chartsWithLargeNumbers() {
        // Seed data with large values
        dataBuilder.seedLargeValueData(
            new BigDecimal("1234567890.99")  // 1.2 billion
        );

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.waitForAllWidgetsLoaded();

        // Numbers should be formatted (1.2B) not truncated
        eyes.check("Dashboard Large Numbers", Target.window().fully());
    }

    @AfterMethod
    public void cleanupEyes() {
        eyes.closeAsync();
    }

    @AfterClass
    public void abortEyes() {
        eyes.abortAsync();
    }
}
```

### 5.2 Alternative: Pixel Comparison (Without AI Tools)

```java
/**
 * PixelComparisonTests - Screenshot-based visual testing
 *
 * Use when Applitools/Percy not available
 */
@Test(groups = {"dashboard", "visual", "pixel"})
public class PixelComparisonTests extends BaseDashboardTest {

    private static final Path BASELINE_DIR = Paths.get("src/test/resources/visual-baselines");
    private static final Path DIFF_DIR = Paths.get("target/visual-diffs");
    private static final double TOLERANCE = 0.01;  // 1% pixel difference allowed

    @Test(description = "Chart renders same as baseline")
    public void chartMatchesBaseline() throws IOException {
        seedVisualTestData();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");
        dashboard.waitForAllWidgetsLoaded();

        // Capture current screenshot
        byte[] actual = page.locator("[data-testid='widget-revenue-trend']")
            .screenshot();

        // Load baseline
        Path baselinePath = BASELINE_DIR.resolve("revenue-trend-baseline.png");

        if (!Files.exists(baselinePath)) {
            // First run - save as baseline
            Files.write(baselinePath, actual);
            throw new SkipException("Baseline created. Re-run test to compare.");
        }

        byte[] baseline = Files.readAllBytes(baselinePath);

        // Compare
        ImageComparisonResult result = compareImages(baseline, actual);

        if (result.getDifferencePercent() > TOLERANCE) {
            // Save diff image for debugging
            Files.createDirectories(DIFF_DIR);
            Files.write(DIFF_DIR.resolve("revenue-trend-diff.png"), result.getDiffImage());
            Files.write(DIFF_DIR.resolve("revenue-trend-actual.png"), actual);

            fail("Visual difference detected: " + result.getDifferencePercent() + "% changed");
        }
    }

    private ImageComparisonResult compareImages(byte[] baseline, byte[] actual) {
        BufferedImage baselineImg = ImageIO.read(new ByteArrayInputStream(baseline));
        BufferedImage actualImg = ImageIO.read(new ByteArrayInputStream(actual));

        // Simple pixel comparison
        int width = Math.min(baselineImg.getWidth(), actualImg.getWidth());
        int height = Math.min(baselineImg.getHeight(), actualImg.getHeight());
        int totalPixels = width * height;
        int differentPixels = 0;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int baselinePixel = baselineImg.getRGB(x, y);
                int actualPixel = actualImg.getRGB(x, y);

                if (baselinePixel != actualPixel) {
                    differentPixels++;
                    diffImage.setRGB(x, y, Color.RED.getRGB());  // Highlight difference
                } else {
                    diffImage.setRGB(x, y, baselinePixel);
                }
            }
        }

        double diffPercent = (double) differentPixels / totalPixels * 100;

        return new ImageComparisonResult(diffPercent, toBytes(diffImage));
    }
}
```

---

## 6. Filter & Drill-Down Tests

```java
/**
 * FilterTests - Dashboard filter functionality
 */
@Test(groups = {"dashboard", "filters"})
public class FilterTests extends BaseDashboardTest {

    @Test(description = "Date range filter affects all widgets")
    public void dateRangeAffectsAllWidgets() {
        // Seed 3 months of data
        dataBuilder.seedMonthlyData(
            YearMonth.of(2024, 1), new BigDecimal("10000"),
            YearMonth.of(2024, 2), new BigDecimal("15000"),
            YearMonth.of(2024, 3), new BigDecimal("20000")
        );

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");

        // Verify total shows all 3 months
        KPICardComponent totalKPI = dashboard.getKPICard("total-revenue");
        assertThat(totalKPI.getNumericValue())
            .isEqualByComparingTo(new BigDecimal("45000"));

        // Filter to January only
        dashboard.setDateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );
        dashboard.applyFilters();

        // Verify total updated
        assertThat(totalKPI.getNumericValue())
            .isEqualByComparingTo(new BigDecimal("10000"));

        // Verify chart also updated
        LineChartComponent chart = dashboard.getLineChart("revenue-trend");
        assertThat(chart.getDataPoints()).allMatch(
            p -> p.getLabel().startsWith("Jan")
        );
    }

    @Test(description = "Multi-select filter combines values")
    public void multiSelectFilter() {
        dataBuilder.seedCategorizedSales(Map.of(
            "Electronics", new BigDecimal("5000"),
            "Clothing", new BigDecimal("3000"),
            "Books", new BigDecimal("2000"),
            "Home", new BigDecimal("1000")
        ));

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");

        // Select multiple categories
        dashboard.selectFilter("category", "Electronics", "Clothing");
        dashboard.applyFilters();

        // Should show sum of Electronics + Clothing
        KPICardComponent totalKPI = dashboard.getKPICard("total-revenue");
        assertThat(totalKPI.getNumericValue())
            .isEqualByComparingTo(new BigDecimal("8000"));
    }
}

/**
 * DrillDownTests - Dashboard drill-down functionality
 */
@Test(groups = {"dashboard", "drilldown"})
public class DrillDownTests extends BaseDashboardTest {

    @Test(description = "Click on chart data point opens drill-down")
    public void chartDrillDown() {
        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");

        // Click on a specific data point
        dashboard.drillDownOnWidget("revenue-trend", "2024-01-15");

        // Verify drill-down opened
        assertThat(dashboard.isDrillDownOpen()).isTrue();

        // Verify drill-down shows correct data
        DataTableComponent drillDownTable = new DataTableComponent(page, "drill-down-table");
        List<Map<String, String>> rows = drillDownTable.getAllRows();

        // All rows should be from Jan 15
        assertThat(rows).allMatch(
            row -> row.get("date").contains("2024-01-15")
        );
    }

    @Test(description = "Drill-down respects parent filters")
    public void drillDownRespectsFilters() {
        dataBuilder.seedCategorizedSalesWithDates();

        DashboardViewPage dashboard = new DashboardViewPage(page);
        dashboard.navigate("sales-dashboard");

        // Apply category filter first
        dashboard.selectFilter("category", "Electronics");
        dashboard.applyFilters();

        // Then drill down
        dashboard.drillDownOnWidget("revenue-trend", "2024-01-15");

        // Drill-down should only show Electronics for Jan 15
        DataTableComponent drillDownTable = new DataTableComponent(page, "drill-down-table");
        List<Map<String, String>> rows = drillDownTable.getAllRows();

        assertThat(rows)
            .allMatch(row -> row.get("category").equals("Electronics"))
            .allMatch(row -> row.get("date").contains("2024-01-15"));
    }
}
```

---

## 7. Key Test Scenarios Summary

| Category | Test | Type | Priority |
|----------|------|------|----------|
| **KPI Accuracy** | Total matches sum | API + UI | P0 |
| **KPI Accuracy** | Percentage change calculated | API + UI | P1 |
| **KPI Accuracy** | Filter updates KPIs | UI | P1 |
| **Chart Accuracy** | Line chart points match data | Hybrid | P0 |
| **Chart Accuracy** | Pie chart sums to 100% | UI | P1 |
| **Chart Accuracy** | Bar chart aggregations | Hybrid | P1 |
| **Visual** | Full dashboard renders | Visual | P1 |
| **Visual** | Responsive layouts | Visual | P2 |
| **Visual** | Empty state handling | Visual | P2 |
| **Filters** | Date range affects all widgets | UI | P0 |
| **Filters** | Multi-select combines | UI | P1 |
| **Drill-down** | Opens on click | UI | P1 |
| **Drill-down** | Respects parent filters | UI | P1 |
| **Export** | CSV contains correct data | API + File | P1 |
| **Export** | PDF renders charts | File | P2 |

---

*Document End - Dashboard LLD v1.0*
