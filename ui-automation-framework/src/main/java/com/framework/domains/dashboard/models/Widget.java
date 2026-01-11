package com.framework.domains.dashboard.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Widget - Dashboard widget model
 *
 * Represents individual visualization components within a dashboard.
 * Supports various chart types, KPIs, tables, and custom visualizations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Widget {

    private String id;
    private String title;
    private String description;
    private WidgetType type;

    // Position and size in the dashboard grid
    private Position position;

    // Data source configuration
    private DataSource dataSource;

    // Visualization configuration
    private VisualizationConfig visualization;

    // Drill-down configuration
    private DrillDownConfig drillDown;

    // Conditional formatting
    private List<ConditionalFormat> conditionalFormats;

    // Loading state (for API responses)
    private WidgetState state;
    private String errorMessage;

    public enum WidgetType {
        LINE_CHART,
        BAR_CHART,
        PIE_CHART,
        DONUT_CHART,
        AREA_CHART,
        SCATTER_PLOT,
        HEATMAP,
        KPI,
        TABLE,
        PIVOT_TABLE,
        MAP,
        GAUGE,
        FUNNEL,
        TEXT,
        IMAGE
    }

    public enum WidgetState {
        LOADING,
        LOADED,
        ERROR,
        NO_DATA
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private int x;
        private int y;
        private int width;
        private int height;
        private int minWidth;
        private int minHeight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSource {
        private DataSourceType type;
        private String query;           // SQL or API query
        private String datasetId;       // Reference to a dataset
        private String apiEndpoint;     // For API data source
        private Map<String, Object> parameters;
        private int cacheSeconds;
    }

    public enum DataSourceType {
        SQL,
        API,
        DATASET,
        STATIC
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualizationConfig {
        // Common properties
        private String xAxis;
        private String yAxis;
        private String groupBy;
        private AggregationType aggregation;

        // Colors and styling
        private List<String> colorPalette;
        private String backgroundColor;

        // Legend configuration
        private LegendConfig legend;

        // Axis configuration
        private AxisConfig xAxisConfig;
        private AxisConfig yAxisConfig;

        // Chart-specific options
        private Map<String, Object> options;
    }

    public enum AggregationType {
        SUM,
        AVG,
        COUNT,
        MIN,
        MAX,
        DISTINCT_COUNT
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegendConfig {
        private boolean show;
        private LegendPosition position;
    }

    public enum LegendPosition {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AxisConfig {
        private String label;
        private String format;  // e.g., "currency", "percentage", "date"
        private boolean showGridLines;
        private Double min;
        private Double max;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrillDownConfig {
        private boolean enabled;
        private String targetDashboardId;
        private String targetWidgetId;
        private Map<String, String> parameterMappings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionalFormat {
        private String field;
        private ConditionalOperator operator;
        private Object value;
        private String color;
        private String backgroundColor;
        private String icon;
    }

    public enum ConditionalOperator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        BETWEEN,
        CONTAINS
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KPI SPECIFIC
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * KPI data for KPI widgets
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiData {
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private BigDecimal target;
        private String format;  // e.g., "currency", "percentage", "number"
        private TrendDirection trend;
        private BigDecimal changePercent;
    }

    public enum TrendDirection {
        UP,
        DOWN,
        FLAT
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLE SPECIFIC
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Table configuration for table widgets
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableConfig {
        private List<TableColumn> columns;
        private boolean showPagination;
        private int pageSize;
        private boolean allowSorting;
        private boolean allowFiltering;
        private boolean showTotals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableColumn {
        private String field;
        private String header;
        private String format;
        private int width;
        private boolean sortable;
        private boolean filterable;
        private Alignment alignment;
    }

    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isChart() {
        return type == WidgetType.LINE_CHART ||
               type == WidgetType.BAR_CHART ||
               type == WidgetType.PIE_CHART ||
               type == WidgetType.DONUT_CHART ||
               type == WidgetType.AREA_CHART ||
               type == WidgetType.SCATTER_PLOT;
    }

    public boolean isKpi() {
        return type == WidgetType.KPI;
    }

    public boolean isTable() {
        return type == WidgetType.TABLE || type == WidgetType.PIVOT_TABLE;
    }

    public boolean isLoaded() {
        return state == WidgetState.LOADED;
    }

    public boolean hasError() {
        return state == WidgetState.ERROR;
    }

    public boolean hasNoData() {
        return state == WidgetState.NO_DATA;
    }
}
