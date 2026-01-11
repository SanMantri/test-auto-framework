package com.framework.domains.dashboard.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dashboard - Analytics dashboard model
 *
 * Represents a dashboard with widgets, layout, filters, and data sources.
 * Used for testing dashboard rendering, data accuracy, and visual consistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {

    private String id;
    private String name;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DashboardStatus status;

    // Layout configuration
    private LayoutConfig layout;

    // Widgets in the dashboard
    private List<Widget> widgets;

    // Global filters that apply to all widgets
    private List<Filter> globalFilters;

    // Time range for the dashboard
    private TimeRange timeRange;

    // Data refresh settings
    private RefreshConfig refreshConfig;

    // Permissions
    private List<String> sharedWith;
    private boolean isPublic;
    private boolean isTemplate;

    // Tags for categorization
    private List<String> tags;

    public enum DashboardStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LayoutConfig {
        private LayoutType type;
        private int columns;
        private int rowHeight;
        private boolean isDraggable;
        private boolean isResizable;
    }

    public enum LayoutType {
        GRID,
        FREE_FORM,
        FIXED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {
        private String id;
        private String name;
        private FilterType type;
        private String field;
        private Object defaultValue;
        private List<Object> options;
        private boolean required;
    }

    public enum FilterType {
        DROPDOWN,
        MULTI_SELECT,
        DATE_RANGE,
        TEXT,
        NUMERIC_RANGE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {
        private TimeRangeType type;
        private String start;  // For custom: ISO date
        private String end;    // For custom: ISO date
        private String preset; // e.g., "last_7_days", "last_30_days"
    }

    public enum TimeRangeType {
        PRESET,
        CUSTOM,
        RELATIVE
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshConfig {
        private boolean autoRefresh;
        private int intervalSeconds;
        private boolean showLastRefresh;
    }

    // Helper methods
    public boolean isPublished() {
        return status == DashboardStatus.PUBLISHED;
    }

    public boolean isDraft() {
        return status == DashboardStatus.DRAFT;
    }

    public int getWidgetCount() {
        return widgets != null ? widgets.size() : 0;
    }

    public Widget getWidget(String widgetId) {
        if (widgets == null) return null;
        return widgets.stream()
            .filter(w -> w.getId().equals(widgetId))
            .findFirst()
            .orElse(null);
    }

    public List<Widget> getWidgetsByType(Widget.WidgetType type) {
        if (widgets == null) return List.of();
        return widgets.stream()
            .filter(w -> w.getType() == type)
            .toList();
    }
}
